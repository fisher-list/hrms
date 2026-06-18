package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.entity.*;
import com.hrms.common.attendance.mapper.*;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SettlementService.
 */
@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private AtSettlementBatchMapper settlementBatchMapper;
    @Mock
    private HrEmployeeMapper employeeMapper;
    @Mock
    private AtLeaveBalanceMapper balanceMapper;
    @Mock
    private AtLeaveTypeMapper leaveTypeMapper;
    @Mock
    private AtLeaveBalanceLogMapper balanceLogMapper;
    @Mock
    private AtOvertimeRequestMapper overtimeRequestMapper;

    private SettlementService settlementService;

    private HrEmployee testEmployee;
    private AtLeaveType annualType;
    private AtLeaveType sickType;
    private AtLeaveType compoffType;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
                settlementBatchMapper, employeeMapper, balanceMapper,
                leaveTypeMapper, balanceLogMapper, overtimeRequestMapper);

        testEmployee = new HrEmployee();
        testEmployee.setId(50001L);
        testEmployee.setStatus("ACTIVE");

        annualType = new AtLeaveType();
        annualType.setId(6001L);
        annualType.setCode("ANNUAL");
        annualType.setAnnualQuota(new BigDecimal("20.0"));
        annualType.setCarryOverRule("CARRY_MAX5");

        sickType = new AtLeaveType();
        sickType.setId(6002L);
        sickType.setCode("SICK");
        sickType.setAnnualQuota(new BigDecimal("15.0"));
        sickType.setCarryOverRule("NONE");

        compoffType = new AtLeaveType();
        compoffType.setId(6008L);
        compoffType.setCode("COMPOFF");
        compoffType.setAnnualQuota(BigDecimal.ZERO);
        compoffType.setCarryOverRule("EXPIRE_2M");
    }

    @Test
    @DisplayName("Test 1: ANNUAL CARRY_MAX5 remaining=8 -> carryover=5, expired=3")
    void testAnnualCarryMax5Remaining8() {
        AtLeaveBalance balance = new AtLeaveBalance();
        balance.setId(61001L);
        balance.setEmployeeId(50001L);
        balance.setLeaveTypeId(6001L);
        balance.setYear(2025);
        balance.setRemaining(new BigDecimal("8.0"));

        when(settlementBatchMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(settlementBatchMapper.insert(any(AtSettlementBatch.class))).thenReturn(1);
        when(settlementBatchMapper.updateById(any(AtSettlementBatch.class))).thenReturn(1);
        when(employeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testEmployee));
        when(leaveTypeMapper.selectList(any())).thenReturn(List.of(annualType));
        when(balanceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(balance));
        when(balanceLogMapper.insert(any(AtLeaveBalanceLog.class))).thenReturn(1);
        when(balanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(balanceMapper.insert(any(AtLeaveBalance.class))).thenReturn(1);

        AtSettlementBatch result = settlementService.run(2025);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(1, result.getProcessedCount());
        assertEquals(0, new BigDecimal("5.0").compareTo(result.getTotalCarryoverDays()));
        assertEquals(0, new BigDecimal("3.0").compareTo(result.getTotalExpiredDays()));
    }

    @Test
    @DisplayName("Test 2: ANNUAL CARRY_MAX5 remaining=3 -> carryover=3, expired=0")
    void testAnnualCarryMax5Remaining3() {
        AtLeaveBalance balance = new AtLeaveBalance();
        balance.setId(61001L);
        balance.setEmployeeId(50001L);
        balance.setLeaveTypeId(6001L);
        balance.setYear(2025);
        balance.setRemaining(new BigDecimal("3.0"));

        when(settlementBatchMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(settlementBatchMapper.insert(any(AtSettlementBatch.class))).thenReturn(1);
        when(settlementBatchMapper.updateById(any(AtSettlementBatch.class))).thenReturn(1);
        when(employeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testEmployee));
        when(leaveTypeMapper.selectList(any())).thenReturn(List.of(annualType));
        when(balanceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(balance));
        when(balanceLogMapper.insert(any(AtLeaveBalanceLog.class))).thenReturn(1);
        when(balanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(balanceMapper.insert(any(AtLeaveBalance.class))).thenReturn(1);

        AtSettlementBatch result = settlementService.run(2025);

        assertEquals(0, new BigDecimal("3.0").compareTo(result.getTotalCarryoverDays()));
        assertEquals(0, BigDecimal.ZERO.setScale(1).compareTo(result.getTotalExpiredDays()));
    }

    @Test
    @DisplayName("Test 3: SICK NONE remaining=10 -> carryover=0, all forfeited")
    void testSickNoneForfeitsAll() {
        AtLeaveBalance balance = new AtLeaveBalance();
        balance.setId(61002L);
        balance.setEmployeeId(50001L);
        balance.setLeaveTypeId(6002L);
        balance.setYear(2025);
        balance.setRemaining(new BigDecimal("10.0"));

        when(settlementBatchMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(settlementBatchMapper.insert(any(AtSettlementBatch.class))).thenReturn(1);
        when(settlementBatchMapper.updateById(any(AtSettlementBatch.class))).thenReturn(1);
        when(employeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testEmployee));
        when(leaveTypeMapper.selectList(any())).thenReturn(List.of(sickType));
        when(balanceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(balance));
        when(balanceLogMapper.insert(any(AtLeaveBalanceLog.class))).thenReturn(1);
        when(balanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(balanceMapper.insert(any(AtLeaveBalance.class))).thenReturn(1);

        AtSettlementBatch result = settlementService.run(2025);

        assertEquals(0, BigDecimal.ZERO.setScale(1).compareTo(result.getTotalCarryoverDays()));
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getTotalExpiredDays()));
    }

    @Test
    @DisplayName("Test 4: duplicate year settlement -> SETTLEMENT_ALREADY_COMPLETED")
    void testDuplicateYearSettlementThrows() {
        AtSettlementBatch existing = new AtSettlementBatch();
        existing.setId(999L);
        existing.setSettlementYear(2025);
        existing.setStatus("COMPLETED");

        when(settlementBatchMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        BizException ex = assertThrows(BizException.class,
                () -> settlementService.run(2025));
        assertEquals(BizCode.SETTLEMENT_ALREADY_COMPLETED, ex.getCode());
        verify(settlementBatchMapper, never()).insert(any());
    }

    @Test
    @DisplayName("Test 5: COMPOFF EXPIRE_2M overtime older than 2 months -> expired")
    void testCompoffExpiredOvertime() {
        AtLeaveBalance balance = new AtLeaveBalance();
        balance.setId(61008L);
        balance.setEmployeeId(50001L);
        balance.setLeaveTypeId(6008L);
        balance.setYear(2025);
        balance.setRemaining(new BigDecimal("5.0"));

        // Overtime from Oct 2025, cutoff is Nov 1, 2026 (year+1 Jan - 2 months = Nov 1)
        AtOvertimeRequest overtime = new AtOvertimeRequest();
        overtime.setId(9001L);
        overtime.setEmployeeId(50001L);
        overtime.setOvertimeDate(LocalDate.of(2025, 10, 15));
        overtime.setHours(new BigDecimal("40.0")); // 40 hours = 5 days
        overtime.setStatus("APPROVED");

        when(settlementBatchMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(settlementBatchMapper.insert(any(AtSettlementBatch.class))).thenReturn(1);
        when(settlementBatchMapper.updateById(any(AtSettlementBatch.class))).thenReturn(1);
        when(employeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testEmployee));
        when(leaveTypeMapper.selectList(any())).thenReturn(List.of(compoffType));
        when(balanceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(balance));
        when(overtimeRequestMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(overtime));
        when(balanceLogMapper.insert(any(AtLeaveBalanceLog.class))).thenReturn(1);
        when(balanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(balanceMapper.insert(any(AtLeaveBalance.class))).thenReturn(1);

        AtSettlementBatch result = settlementService.run(2025);

        // 40 hours / 8 = 5 days expired; remaining=5, so all expired, carryover=0
        assertEquals(0, BigDecimal.ZERO.setScale(1).compareTo(result.getTotalCarryoverDays()));
        assertEquals(0, new BigDecimal("5.0").compareTo(result.getTotalExpiredDays()));
    }
}
