package com.hrms.common.payroll.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.entity.*;
import com.hrms.common.payroll.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PayrollService (S09+S10).
 */
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PyPayrollRunMapper runMapper;
    @Mock
    private PyPayrollDetailMapper detailMapper;
    @Mock
    private PyPayrollPeriodMapper periodMapper;
    @Mock
    private PyCompensationMasterMapper compensationMapper;
    @Mock
    private PyIitBracketMapper iitBracketMapper;
    @Mock
    private PySocialInsuranceRateMapper socialRateMapper;
    @Mock
    private PyCumulativeTaxLedgerMapper ledgerMapper;
    @Mock
    private HrEmployeeMapper employeeMapper;

    private PayrollService payrollService;

    private PyPayrollPeriod openPeriod;
    private PyPayrollRun draftRun;
    private HrEmployee testEmployee;
    private PyCompensationMaster compensation;
    private PySocialInsuranceRate rate;
    private List<PyIitBracket> brackets;

    @BeforeEach
    void setUp() {
        payrollService = new PayrollService(
                runMapper, detailMapper, periodMapper, compensationMapper,
                iitBracketMapper, socialRateMapper, ledgerMapper, employeeMapper);

        openPeriod = new PyPayrollPeriod();
        openPeriod.setId(1001L);
        openPeriod.setPeriodMonth("2026-06");
        openPeriod.setStatus("OPEN");

        draftRun = new PyPayrollRun();
        draftRun.setId(2001L);
        draftRun.setPeriodId(1001L);
        draftRun.setRunType("NORMAL");
        draftRun.setStatus("DRAFT");
        draftRun.setEmployeeCount(0);
        draftRun.setTotalGross(BigDecimal.ZERO.setScale(4));
        draftRun.setTotalNet(BigDecimal.ZERO.setScale(4));

        testEmployee = new HrEmployee();
        testEmployee.setId(50001L);
        testEmployee.setName("Test User");
        testEmployee.setEmpNo("E001");
        testEmployee.setStatus("ACTIVE");
        testEmployee.setHireDate(LocalDate.of(2024, 1, 1));

        compensation = new PyCompensationMaster();
        compensation.setEmployeeId(50001L);
        compensation.setBaseSalary(new BigDecimal("10000.0000"));
        compensation.setPositionSalary(new BigDecimal("2000.0000"));
        compensation.setPerformanceBase(new BigDecimal("1000.0000"));
        compensation.setAllowance(new BigDecimal("500.0000"));

        rate = new PySocialInsuranceRate();
        rate.setPensionPersonal(new BigDecimal("0.08"));
        rate.setMedicalPersonal(new BigDecimal("0.02"));
        rate.setUnemploymentPersonal(new BigDecimal("0.005"));
        rate.setHousingFundPersonal(new BigDecimal("0.12"));

        // Build IIT brackets
        brackets = new java.util.ArrayList<>();
        PyIitBracket b1 = new PyIitBracket();
        b1.setLowerLimit(new BigDecimal("0"));
        b1.setUpperLimit(new BigDecimal("36000"));
        b1.setRate(new BigDecimal("0.03"));
        b1.setQuickDeduction(new BigDecimal("0"));
        brackets.add(b1);

        PyIitBracket b2 = new PyIitBracket();
        b2.setLowerLimit(new BigDecimal("36000"));
        b2.setUpperLimit(new BigDecimal("144000"));
        b2.setRate(new BigDecimal("0.10"));
        b2.setQuickDeduction(new BigDecimal("2520"));
        brackets.add(b2);

        PyIitBracket b3 = new PyIitBracket();
        b3.setLowerLimit(new BigDecimal("144000"));
        b3.setUpperLimit(new BigDecimal("300000"));
        b3.setRate(new BigDecimal("0.20"));
        b3.setQuickDeduction(new BigDecimal("16920"));
        brackets.add(b3);

        PyIitBracket b4 = new PyIitBracket();
        b4.setLowerLimit(new BigDecimal("300000"));
        b4.setUpperLimit(new BigDecimal("420000"));
        b4.setRate(new BigDecimal("0.25"));
        b4.setQuickDeduction(new BigDecimal("31920"));
        brackets.add(b4);

        PyIitBracket b5 = new PyIitBracket();
        b5.setLowerLimit(new BigDecimal("420000"));
        b5.setUpperLimit(new BigDecimal("660000"));
        b5.setRate(new BigDecimal("0.30"));
        b5.setQuickDeduction(new BigDecimal("52920"));
        brackets.add(b5);

        PyIitBracket b6 = new PyIitBracket();
        b6.setLowerLimit(new BigDecimal("660000"));
        b6.setUpperLimit(new BigDecimal("960000"));
        b6.setRate(new BigDecimal("0.35"));
        b6.setQuickDeduction(new BigDecimal("85920"));
        brackets.add(b6);

        PyIitBracket b7 = new PyIitBracket();
        b7.setLowerLimit(new BigDecimal("960000"));
        b7.setUpperLimit(new BigDecimal("99999999"));
        b7.setRate(new BigDecimal("0.45"));
        b7.setQuickDeduction(new BigDecimal("181920"));
        brackets.add(b7);
    }

    private void setupMocksForCalculate() {
        when(runMapper.selectById(2001L)).thenReturn(draftRun);
        when(periodMapper.selectById(1001L)).thenReturn(openPeriod);
        when(employeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testEmployee));
        when(socialRateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rate);
        when(iitBracketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(brackets);
        // P1-DB1 FIX: batch preload uses selectList instead of selectOne
        when(compensationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(compensation));
        when(ledgerMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(detailMapper.insert(any(PyPayrollDetail.class))).thenReturn(1);
        when(runMapper.updateById(any(PyPayrollRun.class))).thenReturn(1);
    }

    @Test
    @DisplayName("Test 1: calculate with valid compensation -> correct gross, social, fund, iit, net")
    void testCalculateWithValidCompensation() {
        setupMocksForCalculate();

        List<Long> exceptions = payrollService.calculate(2001L);

        assertTrue(exceptions.isEmpty());

        // Verify detail was inserted
        ArgumentCaptor<PyPayrollDetail> detailCaptor = ArgumentCaptor.forClass(PyPayrollDetail.class);
        verify(detailMapper).insert(detailCaptor.capture());
        PyPayrollDetail detail = detailCaptor.getValue();

        // gross = 10000 + 2000 + 1000 + 500 = 13500
        assertEquals(0, new BigDecimal("13500.0000").compareTo(detail.getGrossPay()));
        // socialInsurance = 13500 * 10.5% = 1417.5
        assertEquals(0, new BigDecimal("1417.5000").compareTo(detail.getSocialInsurance()));
        // housingFund = 13500 * 12% = 1620
        assertEquals(0, new BigDecimal("1620.0000").compareTo(detail.getHousingFund()));

        // cumulativeTaxableIncome = 0 + 13500 - 0 - 1417.5 - 0 - 1620 - 5000*6 = 13500 - 1417.5 - 1620 - 30000 = -19537.5
        // Since < 0, iit = 0
        assertEquals(0, BigDecimal.ZERO.setScale(4).compareTo(detail.getIit()));

        // net = 13500 - 1417.5 - 1620 - 0 = 10462.5
        assertEquals(0, new BigDecimal("10462.5000").compareTo(detail.getNetPay()));

        assertFalse(detail.getIsException());

        // Verify run totals updated
        ArgumentCaptor<PyPayrollRun> runCaptor = ArgumentCaptor.forClass(PyPayrollRun.class);
        verify(runMapper).updateById(runCaptor.capture());
        PyPayrollRun updatedRun = runCaptor.getValue();
        assertEquals("CALCULATED", updatedRun.getStatus());
        assertEquals(1, updatedRun.getEmployeeCount());
    }

    @Test
    @DisplayName("Test 2: employee without compensation -> exception")
    void testCalculateWithoutCompensation() {
        when(runMapper.selectById(2001L)).thenReturn(draftRun);
        when(periodMapper.selectById(1001L)).thenReturn(openPeriod);
        when(employeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testEmployee));
        when(socialRateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rate);
        when(iitBracketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(brackets);
        // P1-DB1 FIX: batch preload returns empty for compensation -> no compensation found
        when(compensationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(ledgerMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(detailMapper.insert(any(PyPayrollDetail.class))).thenReturn(1);
        when(runMapper.updateById(any(PyPayrollRun.class))).thenReturn(1);

        List<Long> exceptions = payrollService.calculate(2001L);

        assertEquals(1, exceptions.size());
        assertEquals(50001L, exceptions.get(0));

        ArgumentCaptor<PyPayrollDetail> detailCaptor = ArgumentCaptor.forClass(PyPayrollDetail.class);
        verify(detailMapper).insert(detailCaptor.capture());
        assertTrue(detailCaptor.getValue().getIsException());
    }

    @Test
    @DisplayName("Test 3: lock run -> status=LOCKED, edits rejected")
    void testLockRun() {
        PyPayrollRun calculatedRun = new PyPayrollRun();
        calculatedRun.setId(2001L);
        calculatedRun.setStatus("CALCULATED");

        when(runMapper.selectById(2001L)).thenReturn(calculatedRun);
        when(runMapper.updateById(any(PyPayrollRun.class))).thenReturn(1);

        PyPayrollRun locked = payrollService.lockRun(2001L);
        assertEquals("LOCKED", locked.getStatus());

        // Verify cannot lock again
        when(runMapper.selectById(2001L)).thenReturn(locked);
        BizException ex = assertThrows(BizException.class,
                () -> payrollService.lockRun(2001L));
        assertEquals(BizCode.PAYROLL_RUN_LOCKED, ex.getCode());
    }

    @Test
    @DisplayName("Test 4: reverse run -> negative amounts, auto-locked")
    void testReverseRun() {
        PyPayrollRun lockedRun = new PyPayrollRun();
        lockedRun.setId(2001L);
        lockedRun.setPeriodId(1001L);
        lockedRun.setRunType("NORMAL");
        lockedRun.setStatus("LOCKED");
        lockedRun.setEmployeeCount(1);
        lockedRun.setTotalGross(new BigDecimal("13500.0000"));
        lockedRun.setTotalNet(new BigDecimal("10462.5000"));

        PyPayrollDetail originalDetail = new PyPayrollDetail();
        originalDetail.setRunId(2001L);
        originalDetail.setEmployeeId(50001L);
        originalDetail.setEmployeeName("Test User");
        originalDetail.setEmpNo("E001");
        originalDetail.setGrossPay(new BigDecimal("13500.0000"));
        originalDetail.setSocialInsurance(new BigDecimal("1417.5000"));
        originalDetail.setHousingFund(new BigDecimal("1620.0000"));
        originalDetail.setIit(new BigDecimal("0.0000"));
        originalDetail.setNetPay(new BigDecimal("10462.5000"));

        when(runMapper.selectById(2001L)).thenReturn(lockedRun);
        when(detailMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(originalDetail));
        when(runMapper.insert(any(PyPayrollRun.class))).thenReturn(1);
        when(detailMapper.insert(any(PyPayrollDetail.class))).thenReturn(1);

        PyPayrollRun reversal = payrollService.reverseRun(2001L);

        assertEquals("REVERSAL", reversal.getRunType());
        assertEquals("LOCKED", reversal.getStatus());
        assertEquals(2001L, reversal.getReverseOfRunId());

        // Verify reversal detail has negated amounts
        ArgumentCaptor<PyPayrollRun> runCaptor = ArgumentCaptor.forClass(PyPayrollRun.class);
        verify(runMapper).insert(runCaptor.capture());
        assertEquals(new BigDecimal("-13500.0000"), runCaptor.getValue().getTotalGross());
        assertEquals(new BigDecimal("-10462.5000"), runCaptor.getValue().getTotalNet());

        ArgumentCaptor<PyPayrollDetail> detailCaptor = ArgumentCaptor.forClass(PyPayrollDetail.class);
        verify(detailMapper).insert(detailCaptor.capture());
        PyPayrollDetail revDetail = detailCaptor.getValue();
        assertEquals(new BigDecimal("-13500.0000"), revDetail.getGrossPay());
        assertEquals(new BigDecimal("-1417.5000"), revDetail.getSocialInsurance());
        assertEquals(new BigDecimal("-1620.0000"), revDetail.getHousingFund());
        assertEquals(new BigDecimal("0.0000").setScale(4), revDetail.getIit());
        assertEquals(new BigDecimal("-10462.5000"), revDetail.getNetPay());
    }

    @Test
    @DisplayName("Test 5: duplicate normal run for same period -> PAYROLL_DUPLICATE_RUN")
    void testDuplicateNormalRun() {
        when(periodMapper.selectById(1001L)).thenReturn(openPeriod);
        when(runMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BizException ex = assertThrows(BizException.class,
                () -> payrollService.createRun(1001L));
        assertEquals(BizCode.PAYROLL_DUPLICATE_RUN, ex.getCode());
    }
}
