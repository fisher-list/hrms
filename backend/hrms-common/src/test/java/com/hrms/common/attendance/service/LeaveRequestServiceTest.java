package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.dto.LeaveRequestCreateDto;
import com.hrms.common.attendance.entity.AtLeaveBalance;
import com.hrms.common.attendance.entity.AtLeaveBalanceLog;
import com.hrms.common.attendance.entity.AtLeaveRequest;
import com.hrms.common.attendance.mapper.AtLeaveBalanceLogMapper;
import com.hrms.common.attendance.mapper.AtLeaveBalanceMapper;
import com.hrms.common.attendance.mapper.AtLeaveRequestMapper;
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
 * Unit tests for LeaveRequestService.
 *
 * Mocks only mapper interfaces to avoid byte-buddy Java 25 compatibility
 * issues with concrete @Slf4j-annotated service classes.
 */
@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private AtLeaveRequestMapper leaveRequestMapper;

    @Mock
    private HrEmployeeMapper employeeMapper;

    @Mock
    private AtLeaveBalanceMapper balanceMapper;

    @Mock
    private AtLeaveBalanceLogMapper balanceLogMapper;

    private LeaveBalanceService leaveBalanceService;
    private LeaveRequestService leaveRequestService;

    private HrEmployee testEmployee;
    private AtLeaveBalance testBalance;
    private LeaveRequestCreateDto testDto;

    @BeforeEach
    void setUp() {
        // Build real services with mock mappers (no @Slf4j mocking needed)
        leaveBalanceService = new LeaveBalanceService(balanceMapper, balanceLogMapper);
        leaveRequestService = new LeaveRequestService(
                leaveRequestMapper, employeeMapper, leaveBalanceService, null);

        testEmployee = new HrEmployee();
        testEmployee.setId(50001L);
        testEmployee.setStatus("ACTIVE");
        testEmployee.setContractEnd(LocalDate.of(2027, 12, 31));

        testBalance = new AtLeaveBalance();
        testBalance.setId(61001L);
        testBalance.setEmployeeId(50001L);
        testBalance.setLeaveTypeId(6001L);
        testBalance.setYear(2026);
        testBalance.setQuota(new BigDecimal("20.0"));
        testBalance.setUsed(new BigDecimal("0.0"));
        testBalance.setRemaining(new BigDecimal("20.0"));

        testDto = new LeaveRequestCreateDto();
        testDto.setLeaveTypeId(6001L);
        testDto.setStartDate(LocalDate.of(2026, 7, 1));
        testDto.setEndDate(LocalDate.of(2026, 7, 3));
        testDto.setDays(new BigDecimal("3.0"));
        testDto.setReason("Family vacation");
    }

    @Test
    @DisplayName("Test 1: create with sufficient balance -> success")
    void testCreateWithSufficientBalance() {
        when(employeeMapper.selectById(50001L)).thenReturn(testEmployee);
        when(balanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testBalance);
        when(leaveRequestMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(leaveRequestMapper.insert(any(AtLeaveRequest.class))).thenReturn(1);

        AtLeaveRequest result = leaveRequestService.create(testDto, 50001L);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(50001L, result.getEmployeeId());
        assertEquals(new BigDecimal("3.0"), result.getDays());
        verify(leaveRequestMapper).insert(any(AtLeaveRequest.class));
    }

    @Test
    @DisplayName("Test 2: create with insufficient balance -> BizException LEAVE_BALANCE_INSUFFICIENT")
    void testCreateWithInsufficientBalance() {
        AtLeaveBalance lowBalance = new AtLeaveBalance();
        lowBalance.setId(61001L);
        lowBalance.setRemaining(new BigDecimal("1.0"));

        when(employeeMapper.selectById(50001L)).thenReturn(testEmployee);
        when(balanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(lowBalance);

        BizException ex = assertThrows(BizException.class,
                () -> leaveRequestService.create(testDto, 50001L));
        assertEquals(BizCode.LEAVE_BALANCE_INSUFFICIENT, ex.getCode());
        verify(leaveRequestMapper, never()).insert(any());
    }

    @Test
    @DisplayName("Test 3: create with date conflict -> BizException LEAVE_DATE_CONFLICT")
    void testCreateWithDateConflict() {
        AtLeaveRequest existing = new AtLeaveRequest();
        existing.setId(999L);
        existing.setStatus("APPROVED");

        when(employeeMapper.selectById(50001L)).thenReturn(testEmployee);
        when(balanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testBalance);
        when(leaveRequestMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(existing));

        BizException ex = assertThrows(BizException.class,
                () -> leaveRequestService.create(testDto, 50001L));
        assertEquals(BizCode.LEAVE_DATE_CONFLICT, ex.getCode());
        verify(leaveRequestMapper, never()).insert(any());
    }

    @Test
    @DisplayName("Test 4: onApproved deducts balance")
    void testOnApprovedDeductsBalance() {
        AtLeaveRequest request = new AtLeaveRequest();
        request.setId(100L);
        request.setEmployeeId(50001L);
        request.setLeaveTypeId(6001L);
        request.setStartDate(LocalDate.of(2026, 7, 1));
        request.setDays(new BigDecimal("3.0"));

        when(leaveRequestMapper.selectById(100L)).thenReturn(request);
        when(balanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testBalance);
        when(balanceMapper.selectById(61001L)).thenReturn(testBalance);
        when(balanceMapper.updateById(any(AtLeaveBalance.class))).thenReturn(1);
        when(balanceLogMapper.insert(any(AtLeaveBalanceLog.class))).thenReturn(1);
        when(leaveRequestMapper.updateById(any(AtLeaveRequest.class))).thenReturn(1);

        leaveRequestService.onApproved(100L);

        // Verify balance was updated: used=3.0, remaining=17.0
        ArgumentCaptor<AtLeaveBalance> balanceCaptor = ArgumentCaptor.forClass(AtLeaveBalance.class);
        verify(balanceMapper).updateById(balanceCaptor.capture());
        assertEquals(0, new BigDecimal("3.0").compareTo(balanceCaptor.getValue().getUsed()));
        assertEquals(0, new BigDecimal("17.0").compareTo(balanceCaptor.getValue().getRemaining()));

        // Verify balance log recorded DEDUCT
        ArgumentCaptor<AtLeaveBalanceLog> logCaptor = ArgumentCaptor.forClass(AtLeaveBalanceLog.class);
        verify(balanceLogMapper).insert(logCaptor.capture());
        assertEquals("DEDUCT", logCaptor.getValue().getChangeType());

        // Verify request status set to APPROVED
        verify(leaveRequestMapper).updateById(argThat(r -> "APPROVED".equals(r.getStatus())));
    }

    @Test
    @DisplayName("Test 5: onRejected does not deduct balance")
    void testOnRejectedDoesNotDeductBalance() {
        AtLeaveRequest request = new AtLeaveRequest();
        request.setId(101L);
        request.setEmployeeId(50001L);
        request.setLeaveTypeId(6001L);

        when(leaveRequestMapper.selectById(101L)).thenReturn(request);
        when(leaveRequestMapper.updateById(any(AtLeaveRequest.class))).thenReturn(1);

        leaveRequestService.onRejected(101L);

        // No balance changes should occur
        verify(balanceMapper, never()).updateById(any());
        verify(balanceLogMapper, never()).insert(any());
        verify(leaveRequestMapper).updateById(argThat(r -> "REJECTED".equals(r.getStatus())));
    }
}
