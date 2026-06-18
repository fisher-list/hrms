package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.attendance.entity.AtLeaveBalance;
import com.hrms.common.attendance.entity.AtLeaveBalanceLog;
import com.hrms.common.attendance.mapper.AtLeaveBalanceLogMapper;
import com.hrms.common.attendance.mapper.AtLeaveBalanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for leave balance operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final AtLeaveBalanceMapper balanceMapper;
    private final AtLeaveBalanceLogMapper balanceLogMapper;

    /**
     * Get all leave balances for an employee in a given year.
     */
    public List<AtLeaveBalance> getBalances(Long employeeId, Integer year) {
        return balanceMapper.selectList(
                new LambdaQueryWrapper<AtLeaveBalance>()
                        .eq(AtLeaveBalance::getEmployeeId, employeeId)
                        .eq(AtLeaveBalance::getYear, year)
                        .orderByAsc(AtLeaveBalance::getLeaveTypeId));
    }

    /**
     * Get a specific balance record for an employee + leave type + year.
     */
    public AtLeaveBalance getBalanceForEmployeeAndType(Long employeeId, Long leaveTypeId, Integer year) {
        return balanceMapper.selectOne(
                new LambdaQueryWrapper<AtLeaveBalance>()
                        .eq(AtLeaveBalance::getEmployeeId, employeeId)
                        .eq(AtLeaveBalance::getLeaveTypeId, leaveTypeId)
                        .eq(AtLeaveBalance::getYear, year));
    }

    /**
     * Deduct days from a leave balance and record a log entry.
     */
    @Transactional
    public void deduct(Long balanceId, BigDecimal days, Long relatedRequestId) {
        AtLeaveBalance balance = balanceMapper.selectById(balanceId);
        balance.setUsed(balance.getUsed().add(days));
        balance.setRemaining(balance.getRemaining().subtract(days));
        balanceMapper.updateById(balance);

        recordLog(balanceId, "DEDUCT", days.negate(), relatedRequestId, "请假扣减");
        log.info("Balance deducted: balanceId={}, days={}, relatedRequestId={}", balanceId, days, relatedRequestId);
    }

    /**
     * Refund days back to a leave balance (e.g. when an approved request is cancelled)
     * and record a log entry.
     */
    @Transactional
    public void refund(Long balanceId, BigDecimal days, Long relatedRequestId) {
        AtLeaveBalance balance = balanceMapper.selectById(balanceId);
        balance.setUsed(balance.getUsed().subtract(days));
        balance.setRemaining(balance.getRemaining().add(days));
        balanceMapper.updateById(balance);

        recordLog(balanceId, "REFUND", days, relatedRequestId, "请假退回");
        log.info("Balance refunded: balanceId={}, days={}, relatedRequestId={}", balanceId, days, relatedRequestId);
    }

    private void recordLog(Long balanceId, String changeType, BigDecimal changeValue,
                           Long relatedRequestId, String remark) {
        AtLeaveBalanceLog logEntry = new AtLeaveBalanceLog();
        logEntry.setBalanceId(balanceId);
        logEntry.setChangeType(changeType);
        logEntry.setChangeValue(changeValue);
        logEntry.setRelatedRequestId(relatedRequestId);
        logEntry.setRemark(remark);
        balanceLogMapper.insert(logEntry);
    }
}
