package com.hrms.common.payroll.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.dto.SimulationCompareVo;
import com.hrms.common.payroll.dto.SimulationCreateDto;
import com.hrms.common.payroll.entity.*;
import com.hrms.common.payroll.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工资模拟运行服务。
 *
 * <p>核心特性：
 * <ul>
 *   <li>不影响正式数据，所有计算结果保存在独立的模拟表中</li>
 *   <li>支持覆盖社保/公积金比例参数</li>
 *   <li>模拟完成后可与正式运行结果进行对比</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollSimulationService {

    private final PyPayrollSimulationMapper simulationMapper;
    private final PySimulationDetailMapper simulationDetailMapper;
    private final PyPayrollRunMapper runMapper;
    private final PyPayrollDetailMapper payrollDetailMapper;
    private final PyPayrollPeriodMapper periodMapper;
    private final PyCompensationMasterMapper compensationMapper;
    private final PyIitBracketMapper iitBracketMapper;
    private final PySocialInsuranceRateMapper socialRateMapper;
    private final PyCumulativeTaxLedgerMapper ledgerMapper;
    private final HrEmployeeMapper employeeMapper;

    /** 月度基本扣除额 */
    private static final BigDecimal MONTHLY_DEDUCTION = new BigDecimal("5000.0000");

    /**
     * 创建并执行工资模拟运行。
     *
     * @param dto 模拟创建DTO
     * @return 模拟运行信息
     */
    @Transactional
    public PyPayrollSimulation createSimulation(SimulationCreateDto dto) {
        // 验证期间存在
        PyPayrollPeriod period = periodMapper.selectById(dto.getPeriodId());
        if (period == null) {
            throw new BizException(BizCode.BAD_REQUEST, "薪酬期间不存在");
        }

        // 解析期间月份
        int year = Integer.parseInt(period.getPeriodMonth().substring(0, 4));
        int month = Integer.parseInt(period.getPeriodMonth().substring(5, 7));
        java.time.LocalDate firstDayOfMonth = java.time.LocalDate.of(year, month, 1);
        java.time.LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        // 创建模拟记录
        PyPayrollSimulation simulation = new PyPayrollSimulation();
        simulation.setPeriodId(dto.getPeriodId());
        simulation.setName(dto.getName());
        simulation.setStatus("DRAFT");
        simulation.setEmployeeCount(0);
        simulation.setTotalGross(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        simulation.setTotalNet(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        simulation.setCompared(false);

        // 保存参数覆盖配置（简化为JSON字符串）
        if (dto.getSocialInsuranceRateOverride() != null || dto.getHousingFundRateOverride() != null) {
            String overrideJson = buildOverrideJson(dto.getSocialInsuranceRateOverride(),
                    dto.getHousingFundRateOverride());
            simulation.setSocialInsuranceRateOverride(overrideJson);
        }

        simulationMapper.insert(simulation);

        // 加载正式社保比例
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

        // 应用参数覆盖
        if (dto.getSocialInsuranceRateOverride() != null) {
            socialRate = dto.getSocialInsuranceRateOverride();
        }
        if (dto.getHousingFundRateOverride() != null) {
            housingFundRate = dto.getHousingFundRateOverride();
        }

        // 加载IIT税率表
        List<PyIitBracket> brackets = iitBracketMapper.selectList(
                new LambdaQueryWrapper<PyIitBracket>()
                        .orderByAsc(PyIitBracket::getLowerLimit));

        // 查找在职员工
        List<HrEmployee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getStatus, "ACTIVE")
                        .le(HrEmployee::getHireDate, firstDayOfMonth)
                        .and(w -> w.isNull(HrEmployee::getContractEnd)
                                .or()
                                .gt(HrEmployee::getContractEnd, lastDayOfMonth)));

        // 批量预加载薪酬档案和税台账
        List<Long> empIds = employees.stream().map(HrEmployee::getId).collect(Collectors.toList());
        Map<Long, PyCompensationMaster> compMap = loadLatestCompensations(empIds);
        Map<Long, PyCumulativeTaxLedger> ledgerMap = loadLedgers(empIds, year);

        BigDecimal runTotalGross = BigDecimal.ZERO;
        BigDecimal runTotalNet = BigDecimal.ZERO;
        List<PySimulationDetail> allDetails = new ArrayList<>();

        for (HrEmployee emp : employees) {
            PyCompensationMaster comp = compMap.get(emp.getId());
            PyCumulativeTaxLedger ledger = ledgerMap.get(emp.getId());

            PySimulationDetail detail = new PySimulationDetail();
            detail.setSimulationId(simulation.getId());
            detail.setEmployeeId(emp.getId());
            detail.setEmployeeName(emp.getName());
            detail.setEmpNo(emp.getEmpNo());

            if (comp == null) {
                detail.setGrossPay(BigDecimal.ZERO.setScale(4));
                detail.setSocialInsurance(BigDecimal.ZERO.setScale(4));
                detail.setHousingFund(BigDecimal.ZERO.setScale(4));
                detail.setIit(BigDecimal.ZERO.setScale(4));
                detail.setNetPay(BigDecimal.ZERO.setScale(4));
                allDetails.add(detail);
                continue;
            }

            // 计算应发工资
            BigDecimal gross = safe(comp.getBaseSalary())
                    .add(safe(comp.getPositionSalary()))
                    .add(safe(comp.getPerformanceBase()))
                    .add(safe(comp.getAllowance()));

            // 使用（可能被覆盖的）社保/公积金比例计算
            BigDecimal socialInsurance = gross.multiply(socialRate)
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal housingFund = gross.multiply(housingFundRate)
                    .setScale(4, RoundingMode.HALF_UP);

            // 累计值
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

            // 累计应纳税所得额
            BigDecimal cumTaxableIncome = cumGross.add(gross)
                    .subtract(cumSocial).subtract(socialInsurance)
                    .subtract(cumHousing).subtract(housingFund)
                    .subtract(MONTHLY_DEDUCTION.multiply(BigDecimal.valueOf(month)));

            BigDecimal iit = BigDecimal.ZERO;
            if (cumTaxableIncome.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal totalIit = calculateIit(cumTaxableIncome, brackets);
                iit = totalIit.subtract(cumIit);
                if (iit.compareTo(BigDecimal.ZERO) < 0) {
                    iit = BigDecimal.ZERO;
                }
            }
            iit = iit.setScale(4, RoundingMode.HALF_UP);

            BigDecimal net = gross.subtract(socialInsurance).subtract(housingFund).subtract(iit);

            detail.setGrossPay(gross.setScale(4, RoundingMode.HALF_UP));
            detail.setSocialInsurance(socialInsurance);
            detail.setHousingFund(housingFund);
            detail.setIit(iit);
            detail.setNetPay(net.setScale(4, RoundingMode.HALF_UP));
            allDetails.add(detail);

            runTotalGross = runTotalGross.add(gross);
            runTotalNet = runTotalNet.add(net);
        }

        // 批量保存模拟明细
        for (PySimulationDetail detail : allDetails) {
            simulationDetailMapper.insert(detail);
        }

        // 更新模拟汇总
        simulation.setEmployeeCount(employees.size());
        simulation.setTotalGross(runTotalGross.setScale(4, RoundingMode.HALF_UP));
        simulation.setTotalNet(runTotalNet.setScale(4, RoundingMode.HALF_UP));
        simulation.setStatus("CALCULATED");
        simulationMapper.updateById(simulation);

        return simulation;
    }

    /**
     * 将模拟结果与正式运行结果对比。
     *
     * @param simulationId 模拟运行ID
     * @param formalRunId  正式运行ID
     * @return 对比结果列表
     */
    public List<SimulationCompareVo> compareWithFormal(Long simulationId, Long formalRunId) {
        PyPayrollSimulation simulation = simulationMapper.selectById(simulationId);
        if (simulation == null || !"CALCULATED".equals(simulation.getStatus())) {
            throw new BizException(BizCode.PAYROLL_SIMULATION_ERROR,
                    "模拟运行不存在或未计算完成");
        }

        PyPayrollRun formalRun = runMapper.selectById(formalRunId);
        if (formalRun == null) {
            throw new BizException(BizCode.BAD_REQUEST, "正式薪酬运行不存在");
        }

        // 加载模拟明细
        List<PySimulationDetail> simDetails = simulationDetailMapper.selectList(
                new LambdaQueryWrapper<PySimulationDetail>()
                        .eq(PySimulationDetail::getSimulationId, simulationId));

        // 加载正式明细
        List<PyPayrollDetail> formalDetails = payrollDetailMapper.selectList(
                new LambdaQueryWrapper<PyPayrollDetail>()
                        .eq(PyPayrollDetail::getRunId, formalRunId));

        // 构建正式明细Map
        Map<Long, PyPayrollDetail> formalMap = formalDetails.stream()
                .collect(Collectors.toMap(PyPayrollDetail::getEmployeeId, d -> d, (a, b) -> a));

        List<SimulationCompareVo> compareResults = new ArrayList<>();

        for (PySimulationDetail simDetail : simDetails) {
            SimulationCompareVo vo = new SimulationCompareVo();
            vo.setEmployeeId(simDetail.getEmployeeId());
            vo.setEmployeeName(simDetail.getEmployeeName());
            vo.setEmpNo(simDetail.getEmpNo());
            vo.setSimulationNetPay(simDetail.getNetPay());

            PyPayrollDetail formalDetail = formalMap.get(simDetail.getEmployeeId());
            if (formalDetail != null) {
                vo.setFormalNetPay(formalDetail.getNetPay());
                BigDecimal diff = simDetail.getNetPay().subtract(formalDetail.getNetPay());
                vo.setDiffNetPay(diff.setScale(4, RoundingMode.HALF_UP));

                // 计算差异百分比
                if (formalDetail.getNetPay().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal diffPct = diff.multiply(new BigDecimal("100"))
                            .divide(formalDetail.getNetPay(), 4, RoundingMode.HALF_UP);
                    vo.setDiffPercentage(diffPct);
                } else {
                    vo.setDiffPercentage(BigDecimal.ZERO.setScale(4));
                }

                // 同时更新模拟明细中的差异值
                simDetail.setDiffNetPay(diff.setScale(4, RoundingMode.HALF_UP));
                simulationDetailMapper.updateById(simDetail);
            } else {
                vo.setFormalNetPay(BigDecimal.ZERO.setScale(4));
                vo.setDiffNetPay(simDetail.getNetPay());
                vo.setDiffPercentage(new BigDecimal("100.0000"));
            }

            compareResults.add(vo);
        }

        // 标记已对比
        simulation.setCompared(true);
        simulationMapper.updateById(simulation);

        return compareResults;
    }

    /**
     * 查询模拟运行列表。
     */
    public List<PyPayrollSimulation> listSimulations(Long periodId) {
        LambdaQueryWrapper<PyPayrollSimulation> wrapper = new LambdaQueryWrapper<>();
        if (periodId != null) {
            wrapper.eq(PyPayrollSimulation::getPeriodId, periodId);
        }
        wrapper.orderByDesc(PyPayrollSimulation::getCreatedAt);
        return simulationMapper.selectList(wrapper);
    }

    /**
     * 获取模拟运行详情（含明细）。
     */
    public PyPayrollSimulation getSimulation(Long simulationId) {
        return simulationMapper.selectById(simulationId);
    }

    /**
     * 获取模拟运行的明细列表。
     */
    public List<PySimulationDetail> getSimulationDetails(Long simulationId) {
        return simulationDetailMapper.selectList(
                new LambdaQueryWrapper<PySimulationDetail>()
                        .eq(PySimulationDetail::getSimulationId, simulationId)
                        .orderByAsc(PySimulationDetail::getEmpNo));
    }

    /**
     * 删除模拟运行及其明细。
     */
    @Transactional
    public void deleteSimulation(Long simulationId) {
        PyPayrollSimulation simulation = simulationMapper.selectById(simulationId);
        if (simulation == null) {
            throw new BizException(BizCode.BAD_REQUEST, "模拟运行不存在");
        }
        // 删除明细
        simulationDetailMapper.delete(
                new LambdaQueryWrapper<PySimulationDetail>()
                        .eq(PySimulationDetail::getSimulationId, simulationId));
        // 删除模拟记录
        simulationMapper.deleteById(simulationId);
    }

    // ---- 私有方法 ----

    /**
     * 计算个税（累计预扣法）。
     */
    private BigDecimal calculateIit(BigDecimal taxableIncome, List<PyIitBracket> brackets) {
        for (PyIitBracket bracket : brackets) {
            if (taxableIncome.compareTo(bracket.getUpperLimit()) <= 0) {
                return taxableIncome.multiply(bracket.getRate())
                        .subtract(bracket.getQuickDeduction())
                        .setScale(4, RoundingMode.HALF_UP);
            }
        }
        PyIitBracket last = brackets.get(brackets.size() - 1);
        return taxableIncome.multiply(last.getRate())
                .subtract(last.getQuickDeduction())
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 批量加载最新薪酬档案。
     */
    private Map<Long, PyCompensationMaster> loadLatestCompensations(List<Long> empIds) {
        if (empIds == null || empIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PyCompensationMaster> allComps = compensationMapper.selectList(
                new LambdaQueryWrapper<PyCompensationMaster>()
                        .in(PyCompensationMaster::getEmployeeId, empIds)
                        .orderByDesc(PyCompensationMaster::getEffectiveDate));
        Map<Long, PyCompensationMaster> result = new HashMap<>();
        for (PyCompensationMaster comp : allComps) {
            result.putIfAbsent(comp.getEmployeeId(), comp);
        }
        return result;
    }

    /**
     * 批量加载累计税台账。
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

    /**
     * 构建参数覆盖JSON字符串（简化实现，不引入JSON库依赖）。
     */
    private String buildOverrideJson(BigDecimal socialRate, BigDecimal housingRate) {
        StringBuilder sb = new StringBuilder("{");
        if (socialRate != null) {
            sb.append("\"socialRate\":").append(socialRate);
        }
        if (housingRate != null) {
            if (socialRate != null) sb.append(",");
            sb.append("\"housingRate\":").append(housingRate);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 安全获取BigDecimal值。
     */
    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
