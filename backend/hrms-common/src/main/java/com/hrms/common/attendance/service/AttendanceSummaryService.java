package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.attendance.entity.AtDailySummary;
import com.hrms.common.attendance.entity.AtSchedule;
import com.hrms.common.attendance.entity.AtShift;
import com.hrms.common.attendance.entity.AtTimePunch;
import com.hrms.common.attendance.mapper.AtDailySummaryMapper;
import com.hrms.common.attendance.mapper.AtScheduleMapper;
import com.hrms.common.attendance.mapper.AtShiftMapper;
import com.hrms.common.attendance.mapper.AtTimePunchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for attendance summary generation and querying.
 */
@Service
@RequiredArgsConstructor
public class AttendanceSummaryService {

    private final AtScheduleMapper scheduleMapper;
    private final AtShiftMapper shiftMapper;
    private final AtTimePunchMapper punchMapper;
    private final AtDailySummaryMapper summaryMapper;

    /**
     * Generate attendance summaries for a date range.
     * For each (employee, date) with a schedule, match with punch records and compute summary.
     */
    @Transactional
    public List<AtDailySummary> generate(LocalDate startDate, LocalDate endDate) {
        // Fetch all schedules in range
        List<AtSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<AtSchedule>()
                        .ge(AtSchedule::getScheduleDate, startDate)
                        .le(AtSchedule::getScheduleDate, endDate)
                        .orderByAsc(AtSchedule::getEmployeeId)
                        .orderByAsc(AtSchedule::getScheduleDate));

        for (AtSchedule schedule : schedules) {
            AtShift shift = shiftMapper.selectById(schedule.getShiftId());
            if (shift == null) continue;

            // Find punch record for this employee/date
            AtTimePunch punch = punchMapper.selectOne(
                    new LambdaQueryWrapper<AtTimePunch>()
                            .eq(AtTimePunch::getEmployeeId, schedule.getEmployeeId())
                            .eq(AtTimePunch::getPunchDate, schedule.getScheduleDate()));

            AtDailySummary summary = computeSummary(schedule, shift, punch);

            // Upsert: check if summary already exists
            AtDailySummary existing = summaryMapper.selectOne(
                    new LambdaQueryWrapper<AtDailySummary>()
                            .eq(AtDailySummary::getEmployeeId, schedule.getEmployeeId())
                            .eq(AtDailySummary::getSummaryDate, schedule.getScheduleDate()));

            if (existing != null) {
                existing.setScheduled(summary.getScheduled());
                existing.setAttended(summary.getAttended());
                existing.setLateMinutes(summary.getLateMinutes());
                existing.setEarlyLeaveMinutes(summary.getEarlyLeaveMinutes());
                existing.setAbsent(summary.getAbsent());
                existing.setOvertimeHours(summary.getOvertimeHours());
                summaryMapper.updateById(existing);
            } else {
                summaryMapper.insert(summary);
            }
        }

        return list(null, startDate, endDate);
    }

    /**
     * Compute summary from schedule, shift, and punch data.
     */
    AtDailySummary computeSummary(AtSchedule schedule, AtShift shift, AtTimePunch punch) {
        AtDailySummary summary = new AtDailySummary();
        summary.setEmployeeId(schedule.getEmployeeId());
        summary.setSummaryDate(schedule.getScheduleDate());
        summary.setScheduled(true);
        summary.setLateMinutes(0);
        summary.setEarlyLeaveMinutes(0);
        summary.setAbsent(false);
        summary.setAttended(false);
        summary.setOvertimeHours(BigDecimal.ZERO);

        LocalTime shiftStart = LocalTime.parse(shift.getStartTime());
        LocalTime shiftEnd = LocalTime.parse(shift.getEndTime());
        int flex = shift.getFlexMinutes() != null ? shift.getFlexMinutes() : 0;

        if (punch == null || punch.getClockIn() == null || punch.getClockOut() == null) {
            summary.setAbsent(true);
            return summary;
        }

        summary.setAttended(true);

        LocalTime clockIn = LocalTime.parse(punch.getClockIn());
        LocalTime clockOut = LocalTime.parse(punch.getClockOut());

        // late_minutes = max(0, (clock_in - shift_start) in minutes - flex)
        long lateRaw = ChronoUnit.MINUTES.between(shiftStart, clockIn);
        int lateMinutes = (int) Math.max(0, lateRaw - flex);
        summary.setLateMinutes(lateMinutes);

        // early_leave_minutes = max(0, (shift_end - clock_out) in minutes - flex)
        long earlyRaw = ChronoUnit.MINUTES.between(clockOut, shiftEnd);
        int earlyMinutes = (int) Math.max(0, earlyRaw - flex);
        summary.setEarlyLeaveMinutes(earlyMinutes);

        // overtime_hours = max(0, (clock_out - shift_end) in minutes / 60), round to 0.5
        long overtimeRaw = ChronoUnit.MINUTES.between(shiftEnd, clockOut);
        if (overtimeRaw > 0) {
            double overtimeHours = overtimeRaw / 60.0;
            // Round to nearest 0.5
            BigDecimal overtime = BigDecimal.valueOf(Math.floor(overtimeHours * 2) / 2.0)
                    .setScale(1, RoundingMode.HALF_UP);
            summary.setOvertimeHours(overtime);
        }

        return summary;
    }

    /**
     * List summaries with optional filters.
     */
    public List<AtDailySummary> list(Long employeeId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AtDailySummary> qw = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            qw.eq(AtDailySummary::getEmployeeId, employeeId);
        }
        if (startDate != null) {
            qw.ge(AtDailySummary::getSummaryDate, startDate);
        }
        if (endDate != null) {
            qw.le(AtDailySummary::getSummaryDate, endDate);
        }
        qw.orderByAsc(AtDailySummary::getSummaryDate);
        return summaryMapper.selectList(qw);
    }
}
