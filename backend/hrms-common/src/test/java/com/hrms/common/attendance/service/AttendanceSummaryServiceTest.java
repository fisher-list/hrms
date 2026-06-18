package com.hrms.common.attendance.service;

import com.hrms.common.attendance.entity.AtDailySummary;
import com.hrms.common.attendance.entity.AtSchedule;
import com.hrms.common.attendance.entity.AtShift;
import com.hrms.common.attendance.entity.AtTimePunch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the attendance summary computation logic.
 */
class AttendanceSummaryServiceTest {

    private AttendanceSummaryService service;

    private AtShift shift;

    @BeforeEach
    void setUp() {
        // We only need the computeSummary method which is package-private;
        // create service with null mappers since we won't hit DB
        service = new AttendanceSummaryService(null, null, null, null);

        shift = new AtShift();
        shift.setShiftName("朝九晚六");
        shift.setStartTime("09:00");
        shift.setEndTime("18:00");
        shift.setFlexMinutes(10);
        shift.setIsNightShift(false);
    }

    private AtSchedule buildSchedule(long employeeId, LocalDate date) {
        AtSchedule schedule = new AtSchedule();
        schedule.setEmployeeId(employeeId);
        schedule.setScheduleDate(date);
        schedule.setShiftId(1L);
        return schedule;
    }

    private AtTimePunch buildPunch(long employeeId, LocalDate date, String clockIn, String clockOut) {
        AtTimePunch punch = new AtTimePunch();
        punch.setEmployeeId(employeeId);
        punch.setPunchDate(date);
        punch.setClockIn(clockIn);
        punch.setClockOut(clockOut);
        punch.setSource("MANUAL");
        return punch;
    }

    @Test
    @DisplayName("Test 1: on-time attendance - late_minutes=0, early_leave_minutes=0")
    void testOnTimeAttendance() {
        AtSchedule schedule = buildSchedule(1L, LocalDate.of(2026, 6, 15));
        AtTimePunch punch = buildPunch(1L, LocalDate.of(2026, 6, 15), "09:00", "18:00");

        AtDailySummary summary = service.computeSummary(schedule, shift, punch);

        assertTrue(summary.getScheduled());
        assertTrue(summary.getAttended());
        assertFalse(summary.getAbsent());
        assertEquals(0, summary.getLateMinutes());
        assertEquals(0, summary.getEarlyLeaveMinutes());
        assertEquals(BigDecimal.ZERO, summary.getOvertimeHours());
    }

    @Test
    @DisplayName("Test 2: late arrival - clock_in 09:15 with flex 10min -> late_minutes=5")
    void testLateArrival() {
        AtSchedule schedule = buildSchedule(1L, LocalDate.of(2026, 6, 15));
        AtTimePunch punch = buildPunch(1L, LocalDate.of(2026, 6, 15), "09:15", "18:00");

        AtDailySummary summary = service.computeSummary(schedule, shift, punch);

        assertTrue(summary.getAttended());
        assertFalse(summary.getAbsent());
        // 09:15 - 09:00 = 15 minutes, flex=10, late = 15 - 10 = 5
        assertEquals(5, summary.getLateMinutes());
        assertEquals(0, summary.getEarlyLeaveMinutes());
    }

    @Test
    @DisplayName("Test 3: early leave - clock_out 17:30 with flex 10min -> early_leave_minutes=20")
    void testEarlyLeave() {
        AtSchedule schedule = buildSchedule(1L, LocalDate.of(2026, 6, 15));
        AtTimePunch punch = buildPunch(1L, LocalDate.of(2026, 6, 15), "09:00", "17:30");

        AtDailySummary summary = service.computeSummary(schedule, shift, punch);

        assertTrue(summary.getAttended());
        assertFalse(summary.getAbsent());
        assertEquals(0, summary.getLateMinutes());
        // 18:00 - 17:30 = 30 minutes, flex=10, early = 30 - 10 = 20
        assertEquals(20, summary.getEarlyLeaveMinutes());
    }

    @Test
    @DisplayName("Test 4: absent - scheduled but no punch -> absent=true")
    void testAbsent() {
        AtSchedule schedule = buildSchedule(1L, LocalDate.of(2026, 6, 15));

        AtDailySummary summary = service.computeSummary(schedule, shift, null);

        assertTrue(summary.getScheduled());
        assertFalse(summary.getAttended());
        assertTrue(summary.getAbsent());
        assertEquals(0, summary.getLateMinutes());
        assertEquals(0, summary.getEarlyLeaveMinutes());
        assertEquals(BigDecimal.ZERO, summary.getOvertimeHours());
    }

    @Test
    @DisplayName("Test 5: overtime - clock_out 20:00 with shift end 18:00 -> overtime_hours=2.0")
    void testOvertime() {
        AtSchedule schedule = buildSchedule(1L, LocalDate.of(2026, 6, 15));
        AtTimePunch punch = buildPunch(1L, LocalDate.of(2026, 6, 15), "09:00", "20:00");

        AtDailySummary summary = service.computeSummary(schedule, shift, punch);

        assertTrue(summary.getAttended());
        assertFalse(summary.getAbsent());
        assertEquals(0, summary.getLateMinutes());
        assertEquals(0, summary.getEarlyLeaveMinutes());
        // 20:00 - 18:00 = 120 minutes = 2.0 hours
        assertEquals(new BigDecimal("2.0"), summary.getOvertimeHours());
    }
}
