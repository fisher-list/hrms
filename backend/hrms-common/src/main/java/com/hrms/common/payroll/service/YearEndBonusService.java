package com.hrms.common.payroll.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.dto.YearEndBonusCreateDto;
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
 * 年终奖计算服务。
 *
 * <p>支持三种模式：
 * <ul>
 *   <li>STANDALONE - 单独计税：年终奖÷12找税率档，再计算税额</li>
 *   <li>MERGED - 并入综合所得：将年终奖并入当月工资一起计税</li>
 *   <li>THIRTEENTH - 十三薪：固定发一个月基本工资，按月度工资计税</li>
 * </ul>
 *
 * <p>计算公式：年终奖 = 基本工资 × 奖励月数 × 出勤系数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YearEndBonusService {

    private final PyYearEndBonusMapper yearEndBonusMapper;
    private final PyCompensationMasterMapper compensationMapper;
    private final HrEmployeeMapper employeeMapper;
    private final PyIitBracketMapper iitBracketMapper;
    private final PyCumulativeTaxLedgerMapper ledgerMapper;
    private final PySocialInsuranceRateMapper socialRateMapper;

    /** 单独计税的年度速算扣除表（年终奖专用，与月度不同） */
    private static final BigDecimal TWELVE = new BigDecimal("12");

    /**
     * 计算年终奖（单个或批量）。
     *
     * @param dto 年终奖创建DTO
     * @return 计算后的年终奖记录列表
     */
    @Transactional
    public List<PyYearEndBonus> calculate(YearEndBonusCreateDto dto) {
        // 确定要计算的员工ID列表
        List<Long> empIds = resolveEmployeeIds(dto);

        // 加载IIT税率表
        List<PyIitBracket> brackets = iitBracketMapper.selectList(
                new LambdaQueryWrapper<PyIitBracket>()
                        .orderByAsc(PyIitBracket::getLowerLimit));

        // 加载社保比例（用于并入综合所得计税）
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

        // 默认值处理
        BigDecimal bonusMonths = dto.getBonusMonths() != null ? dto.getBonusMonths() : BigDecimal.ONE;
        BigDecimal attendanceCoeff = dto.getAttendanceCoefficient() != null
                ? dto.getAttendanceCoefficient() : BigDecimal.ONE;

        List<PyYearEndBonus> results = new ArrayList<>();

        for (Long empId : empIds) {
            // 查询员工信息
            HrEmployee employee = employeeMapper.selectById(empId);
            if (employee == null || !"ACTIVE".equals(employee.getStatus())) {
                log.warn("员工不存在或非在职状态，跳过：empId={}", empId);
                continue;
            }

            // 查询最新薪酬档案
            PyCompensationMaster comp = compensationMapper.selectOne(
                    new LambdaQueryWrapper<PyCompensationMaster>()
                            .eq(PyCompensationMaster::getEmployeeId, empId)
                            .orderByDesc(PyCompensationMaster::getEffectiveDate)
                            .last("LIMIT 1"));
            if (comp == null) {
                log.warn("员工无薪酬档案，跳过：empId={}", empId);
                continue;
            }

            BigDecimal baseSalary = comp.getBaseSalary() != null ? comp.getBaseSalary() : BigDecimal.ZERO;

            // 计算应发奖金 = 基本工资 × 奖励月数 × 出勤系数
            BigDecimal bonusAmount;
            if ("THIRTEENTH".equals(dto.getTaxMethod())) {
                // 十三薪：固定一个月基本工资，不乘以奖励月数
                bonusAmount = baseSalary.multiply(attendanceCoeff)
                        .setScale(4, RoundingMode.HALF_UP);
            } else {
                bonusAmount = baseSalary.multiply(bonusMonths).multiply(attendanceCoeff)
                        .setScale(4, RoundingMode.HALF_UP);
            }

            // 计算个税
            BigDecimal taxAmount;
            if ("STANDALONE".equals(dto.getTaxMethod())) {
                // 单独计税：年终奖÷12找税率档
                taxAmount = calculateStandaloneTax(bonusAmount, brackets);
            } else if ("MERGED".equals(dto.getTaxMethod())) {
                // 并入综合所得：将奖金并入当月工资一起计税
                taxAmount = calculateMergedTax(empId, dto.getBonusYear(), bonusAmount,
                        socialRate, housingFundRate, brackets);
            } else {
                // THIRTEENTH：十三薪按月度工资方式计税
                taxAmount = calculateMergedTax(empId, dto.getBonusYear(), bonusAmount,
                        socialRate, housingFundRate, brackets);
            }

            BigDecimal netAmount = bonusAmount.subtract(taxAmount).setScale(4, RoundingMode.HALF_UP);

            // 构建并保存年终奖记录
            PyYearEndBonus bonus = new PyYearEndBonus();
            bonus.setEmployeeId(empId);
            bonus.setEmployeeName(employee.getName());
            bonus.setEmpNo(employee.getEmpNo());
            bonus.setBonusYear(dto.getBonusYear());
            bonus.setBaseSalary(baseSalary.setScale(4, RoundingMode.HALF_UP));
            bonus.setBonusMonths(bonusMonths);
            bonus.setAttendanceCoefficient(attendanceCoeff);
            bonus.setBonusAmount(bonusAmount);
            bonus.setTaxMethod(dto.getTaxMethod());
            bonus.setTaxAmount(taxAmount.setScale(4, RoundingMode.HALF_UP));
            bonus.setNetAmount(netAmount);
            bonus.setStatus("CALCULATED");
            yearEndBonusMapper.insert(bonus);

            results.add(bonus);
        }

        return results;
    }

    /**
     * 查询指定年度的年终奖记录列表。
     */
    public List<PyYearEndBonus> listByYear(Integer bonusYear) {
        return yearEndBonusMapper.selectList(
                new LambdaQueryWrapper<PyYearEndBonus>()
                        .eq(PyYearEndBonus::getBonusYear, bonusYear)
                        .orderByAsc(PyYearEndBonus::getEmpNo));
    }

    /**
     * 查询指定员工的年终奖记录。
     */
    public List<PyYearEndBonus> listByEmployee(Long employeeId) {
        return yearEndBonusMapper.selectList(
                new LambdaQueryWrapper<PyYearEndBonus>()
                        .eq(PyYearEndBonus::getEmployeeId, employeeId)
                        .orderByDesc(PyYearEndBonus::getBonusYear));
    }

    /**
     * 标记年终奖为已发放。
     */
    @Transactional
    public PyYearEndBonus markPaid(Long id) {
        PyYearEndBonus bonus = yearEndBonusMapper.selectById(id);
        if (bonus == null) {
            throw new BizException(BizCode.BAD_REQUEST, "年终奖记录不存在");
        }
        if (!"CALCULATED".equals(bonus.getStatus())) {
            throw new BizException(BizCode.PAYROLL_YEAR_END_BONUS_ERROR,
                    "只有CALCULATED状态的年终奖才能标记为已发放");
        }
        bonus.setStatus("PAID");
        yearEndBonusMapper.updateById(bonus);
        return bonus;
    }

    // ---- 私有方法 ----

    /**
     * 确定要计算的员工ID列表。
     */
    private List<Long> resolveEmployeeIds(YearEndBonusCreateDto dto) {
        if (dto.getEmployeeIds() != null && !dto.getEmployeeIds().isEmpty()) {
            return dto.getEmployeeIds();
        }
        if (dto.getEmployeeId() != null) {
            return List.of(dto.getEmployeeId());
        }
        // 未指定员工，默认计算所有在职员工
        List<HrEmployee> allActive = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getStatus, "ACTIVE"));
        return allActive.stream().map(HrEmployee::getId).collect(Collectors.toList());
    }

    /**
     * 单独计税方法。
     * 年终奖÷12，查找对应税率档，计算税额。
     *
     * @param bonusAmount 年终奖金额
     * @param brackets    IIT税率表
     * @return 税额
     */
    private BigDecimal calculateStandaloneTax(BigDecimal bonusAmount,
                                               List<PyIitBracket> brackets) {
        if (bonusAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // 按月均值查找税率档
        BigDecimal monthlyAvg = bonusAmount.divide(TWELVE, 2, RoundingMode.HALF_UP);

        // 找到对应的税率档（用月均值查找，但用总额计算税额）
        for (PyIitBracket bracket : brackets) {
            if (monthlyAvg.compareTo(bracket.getUpperLimit().divide(TWELVE, 2, RoundingMode.HALF_UP)) <= 0
                    || monthlyAvg.compareTo(bracket.getLowerLimit().divide(TWELVE, 2, RoundingMode.HALF_UP)) < 0) {
                // 用总额×税率 - 速算扣除数
                // 注意：单独计税的速算扣除数与月度相同
                BigDecimal tax = bonusAmount.multiply(bracket.getRate())
                        .subtract(bracket.getQuickDeduction());
                return tax.setScale(4, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
            }
        }
        // 使用最高档
        PyIitBracket last = brackets.get(brackets.size() - 1);
        BigDecimal tax = bonusAmount.multiply(last.getRate()).subtract(last.getQuickDeduction());
        return tax.setScale(4, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
    }

    /**
     * 并入综合所得计税方法。
     * 将年终奖并入当月工资，用累计预扣法计算。
     *
     * @param empId          员工ID
     * @param year           年度
     * @param bonusAmount    年终奖金额
     * @param socialRate     社保比例
     * @param housingFundRate 公积金比例
     * @param brackets       IIT税率表
     * @return 年终奖部分的税额（增量税额）
     */
    private BigDecimal calculateMergedTax(Long empId, int year, BigDecimal bonusAmount,
                                           BigDecimal socialRate, BigDecimal housingFundRate,
                                           List<PyIitBracket> brackets) {
        // 查询累计税台账
        PyCumulativeTaxLedger ledger = ledgerMapper.selectOne(
                new LambdaQueryWrapper<PyCumulativeTaxLedger>()
                        .eq(PyCumulativeTaxLedger::getEmployeeId, empId)
                        .eq(PyCumulativeTaxLedger::getTaxYear, year));

        BigDecimal cumGross = BigDecimal.ZERO;
        BigDecimal cumSocial = BigDecimal.ZERO;
        BigDecimal cumHousing = BigDecimal.ZERO;
        BigDecimal cumIit = BigDecimal.ZERO;
        int monthsInYear = 12; // 假设年度累计

        if (ledger != null) {
            cumGross = safe(ledger.getCumulativeGross());
            cumSocial = safe(ledger.getCumulativeSocial());
            cumHousing = safe(ledger.getCumulativeHousingFund());
            cumIit = safe(ledger.getCumulativeIit());
            // 推算已累计月数
            if (cumGross.compareTo(BigDecimal.ZERO) > 0) {
                // 估算月数（假设每月工资大致相同）
                PyCompensationMaster comp = compensationMapper.selectOne(
                        new LambdaQueryWrapper<PyCompensationMaster>()
                                .eq(PyCompensationMaster::getEmployeeId, empId)
                                .orderByDesc(PyCompensationMaster::getEffectiveDate)
                                .last("LIMIT 1"));
                if (comp != null) {
                    BigDecimal monthlyGross = safe(comp.getBaseSalary())
                            .add(safe(comp.getPositionSalary()))
                            .add(safe(comp.getPerformanceBase()))
                            .add(safe(comp.getAllowance()));
                    if (monthlyGross.compareTo(BigDecimal.ZERO) > 0) {
                        monthsInYear = cumGross.divide(monthlyGross, 0, RoundingMode.DOWN).intValue();
                    }
                }
            }
        }

        // 计算加入奖金后的累计应纳税所得额
        BigDecimal bonusSocial = bonusAmount.multiply(socialRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal bonusHousing = bonusAmount.multiply(housingFundRate).setScale(4, RoundingMode.HALF_UP);

        BigDecimal monthlyDeduction = new BigDecimal("5000");
        BigDecimal cumTaxableIncome = cumGross.add(bonusAmount)
                .subtract(cumSocial).subtract(bonusSocial)
                .subtract(cumHousing).subtract(bonusHousing)
                .subtract(monthlyDeduction.multiply(BigDecimal.valueOf(monthsInYear)));

        BigDecimal totalIit = BigDecimal.ZERO;
        if (cumTaxableIncome.compareTo(BigDecimal.ZERO) > 0) {
            totalIit = calculateIitAmount(cumTaxableIncome, brackets);
        }

        // 增量税额 = 含奖金的总税额 - 已预扣税额
        BigDecimal incrementIit = totalIit.subtract(cumIit);
        return incrementIit.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 根据累计应纳税所得额计算个税。
     */
    private BigDecimal calculateIitAmount(BigDecimal taxableIncome, List<PyIitBracket> brackets) {
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
     * 安全获取BigDecimal值，null时返回ZERO。
     */
    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
