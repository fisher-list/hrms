package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.entity.AtLeaveBalance;
import com.hrms.common.attendance.entity.AtLeaveBalanceLog;
import com.hrms.common.attendance.entity.AtLeaveQuotaRule;
import com.hrms.common.attendance.mapper.AtLeaveBalanceLogMapper;
import com.hrms.common.attendance.mapper.AtLeaveBalanceMapper;
import com.hrms.common.attendance.mapper.AtLeaveQuotaRuleMapper;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 缺勤配额自动生辰服务。
 * <p>按司龄/职级自动生成年假天数。支持单个员工和批量生成。</p>
 * <p>规则匹配逻辑：按司龄和职级匹配最合适的规则，生成对应的假期配额。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveQuotaService {

    private final AtLeaveQuotaRuleMapper ruleMapper;
    private final AtLeaveBalanceMapper balanceMapper;
    private final AtLeaveBalanceLogMapper balanceLogMapper;
    private final HrEmployeeMapper employeeMapper;

    /**
     * 查询所有配额规则
     */
    public List<AtLeaveQuotaRule> listRules() {
        return ruleMapper.selectList(
                new LambdaQueryWrapper<AtLeaveQuotaRule>()
                        .eq(AtLeaveQuotaRule::getEnabled, true)
                        .orderByAsc(AtLeaveQuotaRule::getSortNo));
    }

    /**
     * 创建配额规则
     */
    @Transactional
    public AtLeaveQuotaRule createRule(AtLeaveQuotaRule rule) {
        rule.setEnabled(true);
        ruleMapper.insert(rule);
        log.info("创建配额规则: code={}, name={}", rule.getCode(), rule.getName());
        return rule;
    }

    /**
     * 更新配额规则
     */
    @Transactional
    public AtLeaveQuotaRule updateRule(Long id, AtLeaveQuotaRule rule) {
        AtLeaveQuotaRule existing = ruleMapper.selectById(id);
        if (existing == null) {
            throw new BizException(BizCode.BAD_REQUEST, "配额规则不存在: " + id);
        }
        if (rule.getName() != null) existing.setName(rule.getName());
        if (rule.getCode() != null) existing.setCode(rule.getCode());
        if (rule.getLeaveTypeId() != null) existing.setLeaveTypeId(rule.getLeaveTypeId());
        if (rule.getSeniorityMin() != null) existing.setSeniorityMin(rule.getSeniorityMin());
        if (rule.getSeniorityMax() != null) existing.setSeniorityMax(rule.getSeniorityMax());
        if (rule.getGradeMin() != null) existing.setGradeMin(rule.getGradeMin());
        if (rule.getGradeMax() != null) existing.setGradeMax(rule.getGradeMax());
        if (rule.getQuotaDays() != null) existing.setQuotaDays(rule.getQuotaDays());
        if (rule.getEnabled() != null) existing.setEnabled(rule.getEnabled());
        if (rule.getSortNo() != null) existing.setSortNo(rule.getSortNo());

        ruleMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除（禁用）配额规则
     */
    @Transactional
    public void disableRule(Long id) {
        AtLeaveQuotaRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException(BizCode.BAD_REQUEST, "配额规则不存在: " + id);
        }
        rule.setEnabled(false);
        ruleMapper.updateById(rule);
    }

    /**
     * 为单个员工自动生成年假配额。
     * <p>根据员工司龄匹配规则，计算应得年假天数，生成或更新假期余额。</p>
     *
     * @param employeeId 员工ID
     * @param year       生成年份
     * @return 生成的配额天数，无匹配规则返回0
     */
    @Transactional
    public BigDecimal generateQuotaForEmployee(Long employeeId, Integer year) {
        HrEmployee emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw new BizException(BizCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + employeeId);
        }
        if (emp.getHireDate() == null) {
            throw new BizException(BizCode.BAD_REQUEST, "员工入职日期为空，无法计算司龄: " + employeeId);
        }

        // 计算司龄（年）
        int seniority = calculateSeniority(emp.getHireDate(), year);

        // 查找匹配的规则
        AtLeaveQuotaRule matchedRule = findMatchingRule(seniority, null);
        if (matchedRule == null) {
            log.info("未匹配到配额规则: employeeId={}, seniority={}", employeeId, seniority);
            return BigDecimal.ZERO;
        }

        // 生成或更新假期余额
        return applyQuota(employeeId, matchedRule.getLeaveTypeId(), year, matchedRule.getQuotaDays());
    }

    /**
     * 批量为所有在职员工生成年假配额。
     *
     * @param year 生成年份
     * @return 成功生成的员工数量
     */
    @Transactional
    public int generateQuotaForAll(Integer year) {
        List<HrEmployee> activeEmployees = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .in(HrEmployee::getStatus, "ACTIVE", "PROBATION", "ON_LEAVE")
                        .isNotNull(HrEmployee::getHireDate));

        int count = 0;
        for (HrEmployee emp : activeEmployees) {
            try {
                BigDecimal quota = generateQuotaForEmployee(emp.getId(), year);
                if (quota.compareTo(BigDecimal.ZERO) > 0) {
                    count++;
                }
            } catch (Exception e) {
                log.warn("生成配额失败: employeeId={}, error={}", emp.getId(), e.getMessage());
            }
        }
        log.info("批量生成年假配额完成: year={}, total={}, success={}", year, activeEmployees.size(), count);
        return count;
    }

    /**
     * 计算司龄（年）
     */
    private int calculateSeniority(LocalDate hireDate, int year) {
        LocalDate refDate = LocalDate.of(year, 12, 31);
        return (int) ChronoUnit.YEARS.between(hireDate, refDate);
    }

    /**
     * 根据司龄和职级匹配最合适的规则。
     * <p>匹配逻辑：按sortNo升序，取第一个司龄范围匹配的规则。</p>
     */
    private AtLeaveQuotaRule findMatchingRule(int seniority, Integer grade) {
        List<AtLeaveQuotaRule> rules = listRules();
        for (AtLeaveQuotaRule rule : rules) {
            boolean seniorityMatch = seniority >= rule.getSeniorityMin()
                    && (rule.getSeniorityMax() == null || seniority < rule.getSeniorityMax());
            boolean gradeMatch = grade == null
                    || (rule.getGradeMin() == null && rule.getGradeMax() == null)
                    || (rule.getGradeMin() != null && grade >= rule.getGradeMin()
                    && (rule.getGradeMax() == null || grade < rule.getGradeMax()));
            if (seniorityMatch && gradeMatch) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 应用配额：如果已有余额记录则更新，否则新建。
     */
    private BigDecimal applyQuota(Long employeeId, Long leaveTypeId, Integer year, BigDecimal quotaDays) {
        AtLeaveBalance existing = balanceMapper.selectOne(
                new LambdaQueryWrapper<AtLeaveBalance>()
                        .eq(AtLeaveBalance::getEmployeeId, employeeId)
                        .eq(AtLeaveBalance::getLeaveTypeId, leaveTypeId)
                        .eq(AtLeaveBalance::getYear, year));

        if (existing != null) {
            // 已有记录，更新配额
            BigDecimal diff = quotaDays.subtract(existing.getQuota());
            existing.setQuota(quotaDays);
            existing.setRemaining(existing.getRemaining().add(diff));
            balanceMapper.updateById(existing);

            // 记录变更日志
            recordLog(existing.getId(), "ADJUST", diff, "自动配额调整");
            log.info("更新假期余额: employeeId={}, leaveTypeId={}, year={}, quota={}", employeeId, leaveTypeId, year, quotaDays);
        } else {
            // 新建余额记录
            AtLeaveBalance balance = new AtLeaveBalance();
            balance.setEmployeeId(employeeId);
            balance.setLeaveTypeId(leaveTypeId);
            balance.setYear(year);
            balance.setQuota(quotaDays);
            balance.setUsed(BigDecimal.ZERO);
            balance.setRemaining(quotaDays);
            balanceMapper.insert(balance);

            // 记录日志
            recordLog(balance.getId(), "INIT", quotaDays, "自动配额初始化");
            log.info("新建假期余额: employeeId={}, leaveTypeId={}, year={}, quota={}", employeeId, leaveTypeId, year, quotaDays);
        }
        return quotaDays;
    }

    /**
     * 记录余额变更日志
     */
    private void recordLog(Long balanceId, String changeType, BigDecimal changeValue, String remark) {
        AtLeaveBalanceLog logEntry = new AtLeaveBalanceLog();
        logEntry.setBalanceId(balanceId);
        logEntry.setChangeType(changeType);
        logEntry.setChangeValue(changeValue);
        logEntry.setRemark(remark);
        balanceLogMapper.insert(logEntry);
    }
}
