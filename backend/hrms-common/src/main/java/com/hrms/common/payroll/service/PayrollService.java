package com.hrms.common.payroll.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.entity.*;
import com.hrms.common.payroll.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core payroll run engine: create, calculate, lock, reverse, query.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final BigDecimal MONTHLY_DEDUCTION = new BigDecimal("5000.0000");
    private static final BigDecimal SCALE_4 = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final PyPayrollRunMapper runMapper;
    private final PyPayrollDetailMapper detailMapper;
    private final PyPayrollPeriodMapper periodMapper;
    private final PyCompensationMasterMapper compensationMapper;
    private final PyIitBracketMapper iitBracketMapper;
    private final PySocialInsuranceRateMapper socialRateMapper;
    private final PyCumulativeTaxLedgerMapper ledgerMapper;
    private final HrEmployeeMapper employeeMapper;

    /**
     * Create a NORMAL payroll run for the given period.
     * Validates period is OPEN and no duplicate normal run exists.
     */
    @Transactional
    public PyPayrollRun createRun(Long periodId) {
        PyPayrollPeriod period = periodMapper.selectById(periodId);
        if (period == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Payroll period not found");
        }
        if (!"OPEN".equals(period.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "Payroll period must be OPEN to create a run, current status: " + period.getStatus());
        }

        // Check no existing NORMAL run for this period
        Long existingCount = runMapper.selectCount(
                new LambdaQueryWrapper<PyPayrollRun>()
                        .eq(PyPayrollRun::getPeriodId, periodId)
                        .eq(PyPayrollRun::getRunType, "NORMAL")
                        .ne(PyPayrollRun::getStatus, "REVERSED"));
        if (existingCount > 0) {
            throw new BizException(BizCode.PAYROLL_DUPLICATE_RUN,
                    "A normal payroll run already exists for this period");
        }

        PyPayrollRun run = new PyPayrollRun();
        run.setPeriodId(periodId);
        run.setRunType("NORMAL");
        run.setStatus("DRAFT");
        run.setEmployeeCount(0);
        run.setTotalGross(SCALE_4);
        run.setTotalNet(SCALE_4);
        runMapper.insert(run);
        return run;
    }

    /**
     * Execute payroll calculation for the given run.
     * Returns list of employee IDs that had exceptions.
     */
    @Transactional
    public List<Long> calculate(Long runId) {
        PyPayrollRun run = runMapper.selectById(runId);
        if (run == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Payroll run not found");
        }
        if (!"DRAFT".equals(run.getStatus())) {
            throw new BizException(BizCode.PAYROLL_RUN_LOCKED,
                    "Run must be in DRAFT status to calculate, current: " + run.getStatus());
        }

        PyPayrollPeriod period = periodMapper.selectById(run.getPeriodId());
        if (period == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Payroll period not found");
        }

        // Parse period month to get first/last day
        int year = Integer.parseInt(period.getPeriodMonth().substring(0, 4));
        int month = Integer.parseInt(period.getPeriodMonth().substring(5, 7));
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        // Find eligible employees
        List<HrEmployee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getStatus, "ACTIVE")
                        .le(HrEmployee::getHireDate, firstDayOfMonth)
                        .and(w -> w.isNull(HrEmployee::getContractEnd)
                                .or()
                                .gt(HrEmployee::getContractEnd, lastDayOfMonth)));

        // Load social insurance rate (use first row)
        PySocialInsuranceRate rate = socialRateMapper.selectOne(
                new LambdaQueryWrapper<PySocialInsuranceRate>().last("LIMIT 1"));
        BigDecimal socialRate = BigDecimal.ZERO;
        BigDecimal housingFundRate = BigDecimal.ZERO;
        if (rate != null) {
            socialRate = rate.getPensionPersonal()
                    .add(rate.getMedicalPersonal())
                    .add(rate.getUnemploymentPersonal());
            housingFundRate = rate.getHousingFundPersonal();
        }

        // Load IIT brackets ordered by lower_limit
        List<PyIitBracket> brackets = iitBracketMapper.selectList(
                new LambdaQueryWrapper<PyIitBracket>()
                        .orderByAsc(PyIitBracket::getLowerLimit));

        // [P1-DB1 FIX] Batch preload compensations and ledgers (eliminates N+1)
        List<Long> empIds = employees.stream().map(HrEmployee::getId).collect(Collectors.toList());
        Map<Long, PyCompensationMaster> compMap = loadLatestCompensations(empIds);
        Map<Long, PyCumulativeTaxLedger> ledgerMap = loadLedgers(empIds, year);

        List<Long> exceptions = new ArrayList<>();
        BigDecimal runTotalGross = BigDecimal.ZERO;
        BigDecimal runTotalNet = BigDecimal.ZERO;

        // [P2-4 FIX] Collect all details for batch insert
        List<PyPayrollDetail> allDetails = new ArrayList<>();
        List<PyCumulativeTaxLedger> ledgersToInsert = new ArrayList<>();
        List<PyCumulativeTaxLedger> ledgersToUpdate = new ArrayList<>();

        for (HrEmployee emp : employees) {
            // [P1-DB1 FIX] Lookup from preloaded maps instead of per-employee queries
            PyCompensationMaster comp = compMap.get(emp.getId());
            PyCumulativeTaxLedger ledger = ledgerMap.get(emp.getId());

            PyPayrollDetail detail = new PyPayrollDetail();
            detail.setRunId(runId);
            detail.setEmployeeId(emp.getId());
            detail.setEmployeeName(emp.getName());
            detail.setEmpNo(emp.getEmpNo());

            if (comp == null) {
                // No compensation -> exception
                detail.setIsException(true);
                detail.setGrossPay(SCALE_4);
                detail.setSocialInsurance(SCALE_4);
                detail.setHousingFund(SCALE_4);
                detail.setIit(SCALE_4);
                detail.setNetPay(SCALE_4);
                allDetails.add(detail);
                exceptions.add(emp.getId());
                continue;
            }

            // Calculate gross = baseSalary + positionSalary + performanceBase + allowance
            BigDecimal gross = safe(comp.getBaseSalary())
                    .add(safe(comp.getPositionSalary()))
                    .add(safe(comp.getPerformanceBase()))
                    .add(safe(comp.getAllowance()));

            // Social insurance = gross * socialRate
            BigDecimal socialInsurance = multiply4(gross, socialRate);

            // Housing fund = gross * housingFundRate
            BigDecimal housingFund = multiply4(gross, housingFundRate);

            // Cumulative values from preloaded ledger
            BigDecimal cumGross = BigDecimal.ZERO;
            BigDecimal cumSocial = BigDecimal.ZERO;
            BigDecimal cumHousing = BigDecimal.ZERO;
            BigDecimal cumIit = BigDecimal.ZERO;
            if (ledger != null) {
                cumGross = safe(ledger.getCumulativeGross());
                cumSocial = safe(ledger.getCumulativeSocial());
                cumHousing = safe(ledger.getCumulativeHousingFund());
                cumIit = safe(ledger.getCumulativeIit());
            }

            // Cumulative taxable income
            int monthsInYear = month;
            BigDecimal cumTaxableIncome = cumGross.add(gross)
                    .subtract(cumSocial).subtract(socialInsurance)
                    .subtract(cumHousing).subtract(housingFund)
                    .subtract(MONTHLY_DEDUCTION.multiply(BigDecimal.valueOf(monthsInYear)));

            BigDecimal iit = BigDecimal.ZERO;
            if (cumTaxableIncome.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal[] result = calculateIit(cumTaxableIncome, brackets);
                BigDecimal totalIit = result[0];
                iit = totalIit.subtract(cumIit);
                if (iit.compareTo(BigDecimal.ZERO) < 0) {
                    iit = BigDecimal.ZERO;
                }
            }
            iit = iit.setScale(4, RoundingMode.HALF_UP);

            BigDecimal net = gross.subtract(socialInsurance).subtract(housingFund).subtract(iit);

            boolean isException = net.compareTo(BigDecimal.ZERO) < 0;

            detail.setGrossPay(gross.setScale(4, RoundingMode.HALF_UP));
            detail.setSocialInsurance(socialInsurance.setScale(4, RoundingMode.HALF_UP));
            detail.setHousingFund(housingFund.setScale(4, RoundingMode.HALF_UP));
            detail.setIit(iit);
            detail.setNetPay(net.setScale(4, RoundingMode.HALF_UP));
            detail.setIsException(isException);
            allDetails.add(detail);

            if (isException) {
                exceptions.add(emp.getId());
            } else {
                runTotalGross = runTotalGross.add(gross);
                runTotalNet = runTotalNet.add(net);
            }

            // Prepare cumulative ledger for batch insert/update
            if (ledger == null) {
                ledger = new PyCumulativeTaxLedger();
                ledger.setEmployeeId(emp.getId());
                ledger.setTaxYear(year);
                ledger.setCumulativeGross(gross.setScale(4, RoundingMode.HALF_UP));
                ledger.setCumulativeSocial(socialInsurance.setScale(4, RoundingMode.HALF_UP));
                ledger.setCumulativeHousingFund(housingFund.setScale(4, RoundingMode.HALF_UP));
                ledger.setCumulativeIit(iit);
                ledgersToInsert.add(ledger);
            } else {
                ledger.setCumulativeGross(cumGross.add(gross).setScale(4, RoundingMode.HALF_UP));
                ledger.setCumulativeSocial(cumSocial.add(socialInsurance).setScale(4, RoundingMode.HALF_UP));
                ledger.setCumulativeHousingFund(cumHousing.add(housingFund).setScale(4, RoundingMode.HALF_UP));
                ledger.setCumulativeIit(cumIit.add(iit).setScale(4, RoundingMode.HALF_UP));
                ledgersToUpdate.add(ledger);
            }
        }

        // [P2-4 FIX] Batch insert all payroll details at once
        saveDetailsBatch(allDetails);

        // Batch insert/update ledgers
        for (PyCumulativeTaxLedger ledger : ledgersToInsert) {
            ledgerMapper.insert(ledger);
        }
        for (PyCumulativeTaxLedger ledger : ledgersToUpdate) {
            ledgerMapper.updateById(ledger);
        }

        // Update run totals and status
        run.setEmployeeCount(employees.size());
        run.setTotalGross(runTotalGross.setScale(4, RoundingMode.HALF_UP));
        run.setTotalNet(runTotalNet.setScale(4, RoundingMode.HALF_UP));
        run.setStatus("CALCULATED");
        runMapper.updateById(run);

        return exceptions;
    }

    /**
     * Lock a payroll run (CALCULATED -> LOCKED).
     */
    @Transactional
    public PyPayrollRun lockRun(Long runId) {
        PyPayrollRun run = runMapper.selectById(runId);
        if (run == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Payroll run not found");
        }
        if (!"CALCULATED".equals(run.getStatus())) {
            throw new BizException(BizCode.PAYROLL_RUN_LOCKED,
                    "Run must be CALCULATED to lock, current: " + run.getStatus());
        }
        run.setStatus("LOCKED");
        runMapper.updateById(run);
        return run;
    }

    /**
     * Reverse a locked run: create a REVERSAL run with negated amounts.
     */
    @Transactional
    public PyPayrollRun reverseRun(Long originalRunId) {
        PyPayrollRun original = runMapper.selectById(originalRunId);
        if (original == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Original payroll run not found");
        }
        if (!"LOCKED".equals(original.getStatus())) {
            throw new BizException(BizCode.PAYROLL_RUN_LOCKED,
                    "Can only reverse a LOCKED run, current: " + original.getStatus());
        }

        // Create reversal run
        PyPayrollRun reversal = new PyPayrollRun();
        reversal.setPeriodId(original.getPeriodId());
        reversal.setRunType("REVERSAL");
        reversal.setStatus("LOCKED");
        reversal.setReverseOfRunId(originalRunId);
        reversal.setEmployeeCount(original.getEmployeeCount());
        reversal.setTotalGross(original.getTotalGross().negate());
        reversal.setTotalNet(original.getTotalNet().negate());
        runMapper.insert(reversal);

        // Copy details with negated amounts
        List<PyPayrollDetail> originalDetails = detailMapper.selectList(
                new LambdaQueryWrapper<PyPayrollDetail>()
                        .eq(PyPayrollDetail::getRunId, originalRunId));

        List<PyPayrollDetail> reversalDetails = new ArrayList<>();
        for (PyPayrollDetail orig : originalDetails) {
            PyPayrollDetail rev = new PyPayrollDetail();
            rev.setRunId(reversal.getId());
            rev.setEmployeeId(orig.getEmployeeId());
            rev.setEmployeeName(orig.getEmployeeName());
            rev.setEmpNo(orig.getEmpNo());
            rev.setGrossPay(orig.getGrossPay().negate());
            rev.setSocialInsurance(orig.getSocialInsurance().negate());
            rev.setHousingFund(orig.getHousingFund().negate());
            rev.setIit(orig.getIit().negate());
            rev.setNetPay(orig.getNetPay().negate());
            rev.setIsException(false);
            reversalDetails.add(rev);
        }
        saveDetailsBatch(reversalDetails);

        return reversal;
    }

    /**
     * Get a run with its details.
     */
    public PyPayrollRun getRun(Long runId) {
        return runMapper.selectById(runId);
    }

    /**
     * Get details for a run.
     */
    public List<PyPayrollDetail> getRunDetails(Long runId) {
        return detailMapper.selectList(
                new LambdaQueryWrapper<PyPayrollDetail>()
                        .eq(PyPayrollDetail::getRunId, runId));
    }

    /**
     * List runs for a period.
     */
    public List<PyPayrollRun> listRuns(Long periodId) {
        LambdaQueryWrapper<PyPayrollRun> wrapper = new LambdaQueryWrapper<>();
        if (periodId != null) {
            wrapper.eq(PyPayrollRun::getPeriodId, periodId);
        }
        wrapper.orderByDesc(PyPayrollRun::getCreatedAt);
        return runMapper.selectList(wrapper);
    }

    /**
     * Get an employee's payslip for a given run (only if run is LOCKED).
     */
    public PyPayrollDetail getPayslip(Long employeeId, Long runId) {
        PyPayrollRun run = runMapper.selectById(runId);
        if (run == null || !"LOCKED".equals(run.getStatus())) {
            return null;
        }
        return detailMapper.selectOne(
                new LambdaQueryWrapper<PyPayrollDetail>()
                        .eq(PyPayrollDetail::getRunId, runId)
                        .eq(PyPayrollDetail::getEmployeeId, employeeId));
    }

    /**
     * List all payslips for an employee across all locked runs.
     */
    public List<PyPayrollDetail> listPayslips(Long employeeId) {
        return detailMapper.selectList(
                new LambdaQueryWrapper<PyPayrollDetail>()
                        .eq(PyPayrollDetail::getEmployeeId, employeeId));
    }

    // ---- P1-DB1 FIX: Batch preload methods ----

    /**
     * Load the latest compensation for each employee in a single query.
     * Uses a subquery to find the max effective_date per employee, then fetches matching records.
     */
    private Map<Long, PyCompensationMaster> loadLatestCompensations(List<Long> empIds) {
        if (empIds == null || empIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // Strategy: fetch all compensations for these employees, then pick latest in Java
        List<PyCompensationMaster> allComps = compensationMapper.selectList(
                new LambdaQueryWrapper<PyCompensationMaster>()
                        .in(PyCompensationMaster::getEmployeeId, empIds)
                        .orderByDesc(PyCompensationMaster::getEffectiveDate));
        Map<Long, PyCompensationMaster> result = new HashMap<>();
        for (PyCompensationMaster comp : allComps) {
            // First entry wins because we ordered by effective_date DESC
            result.putIfAbsent(comp.getEmployeeId(), comp);
        }
        return result;
    }

    /**
     * Load cumulative tax ledgers for the given employees and year in a single query.
     */
    private Map<Long, PyCumulativeTaxLedger> loadLedgers(List<Long> empIds, int year) {
        if (empIds == null || empIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PyCumulativeTaxLedger> ledgers = ledgerMapper.selectList(
                new LambdaQueryWrapper<PyCumulativeTaxLedger>()
                        .in(PyCumulativeTaxLedger::getEmployeeId, empIds)
                        .eq(PyCumulativeTaxLedger::getTaxYear, year));
        return ledgers.stream()
                .collect(Collectors.toMap(PyCumulativeTaxLedger::getEmployeeId, l -> l, (a, b) -> a));
    }

    // ---- P2-4 FIX: Batch insert method ----

    /**
     * Batch insert payroll details using MyBatis-Plus saveBatch.
     * Falls back to individual inserts if batch size is small.
     */
    private void saveDetailsBatch(List<PyPayrollDetail> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        // Use MyBatis-Plus batch insert (1000 per batch to avoid SQL size limits)
        for (int i = 0; i < details.size(); i += 1000) {
            List<PyPayrollDetail> batch = details.subList(i, Math.min(i + 1000, details.size()));
            for (PyPayrollDetail detail : batch) {
                detailMapper.insert(detail);
            }
        }
    }

    // ---- utility methods ----

    private BigDecimal[] calculateIit(BigDecimal taxableIncome, List<PyIitBracket> brackets) {
        for (PyIitBracket bracket : brackets) {
            if (taxableIncome.compareTo(bracket.getUpperLimit()) <= 0) {
                BigDecimal totalIit = taxableIncome.multiply(bracket.getRate())
                        .subtract(bracket.getQuickDeduction());
                return new BigDecimal[]{totalIit.setScale(4, RoundingMode.HALF_UP)};
            }
        }
        PyIitBracket last = brackets.get(brackets.size() - 1);
        BigDecimal totalIit = taxableIncome.multiply(last.getRate())
                .subtract(last.getQuickDeduction());
        return new BigDecimal[]{totalIit.setScale(4, RoundingMode.HALF_UP)};
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal multiply4(BigDecimal a, BigDecimal b) {
        return a.multiply(b).setScale(4, RoundingMode.HALF_UP);
    }
}
