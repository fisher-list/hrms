package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.attendance.entity.AtTimePunch;
import com.hrms.common.attendance.mapper.AtTimePunchMapper;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the TimePunch CSV import logic.
 */
@ExtendWith(MockitoExtension.class)
class TimePunchServiceTest {

    @Mock
    private AtTimePunchMapper punchMapper;

    @Mock
    private HrEmployeeMapper employeeMapper;

    @InjectMocks
    private TimePunchService timePunchService;

    private HrEmployee testEmployee;

    @BeforeEach
    void setUp() {
        testEmployee = new HrEmployee();
        testEmployee.setId(1L);
        testEmployee.setEmpNo("E000001");
    }

    private InputStream csvStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test 1: importCsv with valid rows -> all inserted")
    void testImportValidRows() {
        String csv = "emp_no,date,clock_in,clock_out\n" +
                "E000001,2026-06-15,09:00,18:00\n" +
                "E000001,2026-06-16,08:55,18:10\n";

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEmployee);
        when(punchMapper.insert(any(AtTimePunch.class))).thenReturn(1);

        Map<String, Object> result = timePunchService.importCsv(csvStream(csv));

        assertEquals(2, result.get("successCount"));
        assertTrue(((java.util.List<?>) result.get("errors")).isEmpty());
        verify(punchMapper, times(2)).insert(any(AtTimePunch.class));
    }

    @Test
    @DisplayName("Test 2: importCsv with invalid emp_no -> error returned, nothing inserted")
    void testImportInvalidEmpNo() {
        String csv = "emp_no,date,clock_in,clock_out\n" +
                "E999999,2026-06-15,09:00,18:00\n";

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Map<String, Object> result = timePunchService.importCsv(csvStream(csv));

        assertEquals(0, result.get("successCount"));
        java.util.List<?> errors = (java.util.List<?>) result.get("errors");
        assertFalse(errors.isEmpty());
        verify(punchMapper, never()).insert(any(AtTimePunch.class));
    }

    @Test
    @DisplayName("Test 3: importCsv with clock_in > clock_out -> error returned")
    void testImportClockInAfterClockOut() {
        String csv = "emp_no,date,clock_in,clock_out\n" +
                "E000001,2026-06-15,18:00,09:00\n";

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEmployee);

        Map<String, Object> result = timePunchService.importCsv(csvStream(csv));

        assertEquals(0, result.get("successCount"));
        java.util.List<?> errors = (java.util.List<?>) result.get("errors");
        assertFalse(errors.isEmpty());
        verify(punchMapper, never()).insert(any(AtTimePunch.class));
    }
}
