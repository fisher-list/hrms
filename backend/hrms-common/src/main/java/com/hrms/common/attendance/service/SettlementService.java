package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.entity.*;
import com.hrms.common.attendance.mapper.*;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for year-end leave balance settlement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final BigDecimal MAX_CARRYOVER = new BigDecimal("5.0");

    private final AtSettlementBatchMapper settlementBatchMapper;
    private final HrEmployeeMapper employeeMapper;
    private final AtLeaveBalanceMapper balanceMapper;
    private final AtLeaveTypeMapper leaveTypeMapper;
    private final AtLeaveBalanceLogMapper balanceLogMapper;
    private final AtOvertimeRequestMapper overtimeRequestMapper;

    /**
     * Run year-end settlement for the given year.
     * Iterates all ACTIVE employees, settles each leave balance according to carry-over rules,
     * and creates next-year balance records.
     */
    @Transactional
    public AtSettlementBatch run(Integer year) {
        // 1. Check idempotency: no existing COMPLETED batch for this year
        AtSettlementBatch existing = settlementBatchMapper.selectOne(
                new LambdaQueryWrapper<AtSettlementBatch>()
                        .eq(AtSettlementBatch::getSettlementYear, year)
                        .eq(AtSettlementBatch::getStatus, "COMPLETED"));
        if (existing != null) {
            throw new BizException(BizCode.SETTLEMENT_ALREADY_COMPLETED,
                    "Settlement for year " + year + " already completed");
        }

        // 2. Create batch record with status=RUNNING
        AtSettlementBatch batch = new AtSettlementBatch();
        batch.setSettlementYear(year);
        batch.setStatus("RUNNING");
        batch.setProcessedCount(0);
        batch.setTotalCarryoverDays(BigDecimal.ZERO);
        batch.setTotalExpiredDays(BigDecimal.ZERO);
        batch.setStartedAt(LocalDateTime.now());
        settlementBatchMapper.insert(batch);

        // 3. Get all ACTIVE employees
        List<HrEmployee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<HrEmployee>()
                        .eq(HrEmployee::getStatus, "ACTIVE"));

        // Load all leave types into a lookup map
        List<AtLeaveType> leaveTypes = leaveTypeMapper.selectList(null);
        java.util.Map<Long, AtLeaveType> leaveTypeMap = new java.util.HashMap<>();
        for (AtLeaveType lt : leaveTypes) {
            leaveTypeMap.put(lt.getId(), lt);
        }

        BigDecimal totalCarryover = BigDecimal.ZERO;
        BigDecimal totalExpired = BigDecimal.ZERO;

        // 4. For each employee, settle balances
        for (HrEmployee employee : employees) {
            List<AtLeaveBalance> balances = balanceMapper.selectList(
                    new LambdaQueryWrapper<AtLeaveBalance>()
                            .eq(AtLeaveBalance::getEmployeeId, employee.getId())
                            .eq(AtLeaveBalance::getYear, year));

            for (AtLeaveBalance balance : balances) {
                AtLeaveType leaveType = leaveTypeMap.get(balance.getLeaveTypeId());
                if (leaveType == null) {
                    continue;
                }

                BigDecimal remaining = balance.getRemaining();
                String carryOverRule = leaveType.getCarryOverRule();
                BigDecimal carryover = BigDecimal.ZERO;
                BigDecimal expired = BigDecimal.ZERO;

                if ("CARRY_MAX5".equals(carryOverRule)) {
                    // Annual leave: carry over up to 5 days
                    carryover = remaining.min(MAX_CARRYOVER);
                    if (remaining.compareTo(MAX_CARRYOVER) > 0) {
                        expired = remaining.subtract(MAX_CARRYOVER);
                    }
                } else if ("NONE".equals(carryOverRule)) {
                    // All forfeited, no carryover
                    expired = remaining;
                    carryover = BigDecimal.ZERO;
                } else if ("EXPIRE_2M".equals(carryOverRule)) {
                    // COMPOFF: overtime older than 2 months from year-end expires
                    BigDecimal compoffExpired = calculateCompoffExpired(employee.getId(), year);
                    expired = compoffExpired.min(remaining);
                    carryover = remaining.subtract(expired);
                }

                // Log SETTLE for carryover
                createBalanceLog(balance.getId(), "SETTLE", carryover,
                        "年度结算结转: " + carryover + "天");

                // Log expired portion if any
                if (expired.compareTo(BigDecimal.ZERO) > 0) {
                    createBalanceLog(balance.getId(), "SETTLE", expired.negate(),
                            "年度结算过期: " + expired + "天");
                }

                // Create next year balance record
                BigDecimal nextYearQuota = leaveType.getAnnualQuota().add(carryover);
                createNextYearBalance(employee.getId(), balance.getLeaveTypeId(),
                        year + 1, nextYearQuota);

                totalCarryover = totalCarryover.add(carryover);
                totalExpired = totalExpired.add(expired);
            }
        }

        // 5. Update batch status
        batch.setStatus("COMPLETED");
        batch.setProcessedCount(employees.size());
        batch.setTotalCarryoverDays(totalCarryover.setScale(1, RoundingMode.HALF_UP));
        batch.setTotalExpiredDays(totalExpired.setScale(1, RoundingMode.HALF_UP));
        batch.setCompletedAt(LocalDateTime.now());
        settlementBatchMapper.updateById(batch);

        log.info("Settlement completed for year {}: processed={}, carryover={}, expired={}",
                year, employees.size(), totalCarryover, totalExpired);

        return batch;
    }

    /**
     * List all settlement batches.
     */
    public List<AtSettlementBatch> list() {
        return settlementBatchMapper.selectList(
                new LambdaQueryWrapper<AtSettlementBatch>()
                        .orderByDesc(AtSettlementBatch::getSettlementYear));
    }

    /**
     * Get a settlement batch by ID.
     */
    public AtSettlementBatch getById(Long id) {
        return settlementBatchMapper.selectById(id);
    }

    /**
     * Calculate expired comp-off days: approved overtime before (year+1)-01-01 minus 2 months.
     */
    private BigDecimal calculateCompoffExpired(Long employeeId, int year) {
        LocalDate cutoffDate = LocalDate.of(year + 1, 1, 1).minusMonths(2);
        List<AtOvertimeRequest> overtimeRequests = overtimeRequestMapper.selectList(
                new LambdaQueryWrapper<AtOvertimeRequest>()
                        .eq(AtOvertimeRequest::getEmployeeId, employeeId)
                        .eq(AtOvertimeRequest::getStatus, "APPROVED")
                        .lt(AtOvertimeRequest::getOvertimeDate, cutoffDate));

        BigDecimal expiredHours = BigDecimal.ZERO;
        for (AtOvertimeRequest ot : overtimeRequests) {
            expiredHours = expiredHours.add(ot.getHours());
        }
        // Convert hours to days (8 hours = 1 day)
        return expiredHours.divide(new BigDecimal("8"), 1, RoundingMode.HALF_UP);
    }

    private void createBalanceLog(Long balanceId, String changeType, BigDecimal changeValue, String remark) {
        AtLeaveBalanceLog logEntry = new AtLeaveBalanceLog();
        logEntry.setBalanceId(balanceId);
        logEntry.setChangeType(changeType);
        logEntry.setChangeValue(changeValue);
        logEntry.setRemark(remark);
        balanceLogMapper.insert(logEntry);
    }

    private void createNextYearBalance(Long employeeId, Long leaveTypeId, int nextYear, BigDecimal quota) {
        // Check if next year balance already exists
        AtLeaveBalance existing = balanceMapper.selectOne(
                new LambdaQueryWrapper<AtLeaveBalance>()
                        .eq(AtLeaveBalance::getEmployeeId, employeeId)
                        .eq(AtLeaveBalance::getLeaveTypeId, leaveTypeId)
                        .eq(AtLeaveBalance::getYear, nextYear));
        if (existing != null) {
            // Update existing record
            existing.setQuota(existing.getQuota().add(quota));
            existing.setRemaining(existing.getRemaining().add(quota));
            balanceMapper.updateById(existing);
        } else {
            AtLeaveBalance newBalance = new AtLeaveBalance();
            newBalance.setEmployeeId(employeeId);
            newBalance.setLeaveTypeId(leaveTypeId);
            newBalance.setYear(nextYear);
            newBalance.setQuota(quota);
            newBalance.setUsed(BigDecimal.ZERO);
            newBalance.setRemaining(quota);
            balanceMapper.insert(newBalance);
        }
    }
}
