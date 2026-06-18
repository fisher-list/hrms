package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.attendance.dto.ScheduleCreateDto;
import com.hrms.common.attendance.entity.AtSchedule;
import com.hrms.common.attendance.mapper.AtScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for schedule assignment.
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final AtScheduleMapper scheduleMapper;

    /**
     * Assign shifts to employees for a date range. Uses saveOrUpdate (upsert) semantics:
     * if a schedule for (employee, date) already exists, update it; otherwise insert.
     */
    @Transactional
    public List<AtSchedule> create(ScheduleCreateDto dto) {
        List<AtSchedule> result = new ArrayList<>();

        for (Long employeeId : dto.getEmployeeIds()) {
            for (LocalDate date = dto.getStartDate(); !date.isAfter(dto.getEndDate()); date = date.plusDays(1)) {
                // Check for existing schedule on this date for this employee
                AtSchedule existing = scheduleMapper.selectOne(
                        new LambdaQueryWrapper<AtSchedule>()
                                .eq(AtSchedule::getEmployeeId, employeeId)
                                .eq(AtSchedule::getScheduleDate, date));

                if (existing != null) {
                    // Update existing
                    existing.setShiftId(dto.getShiftId());
                    scheduleMapper.updateById(existing);
                    result.add(existing);
                } else {
                    // Insert new
                    AtSchedule schedule = new AtSchedule();
                    schedule.setEmployeeId(employeeId);
                    schedule.setScheduleDate(date);
                    schedule.setShiftId(dto.getShiftId());
                    scheduleMapper.insert(schedule);
                    result.add(schedule);
                }
            }
        }
        return result;
    }

    /**
     * List schedules with optional filters.
     */
    public List<AtSchedule> list(Long employeeId, Long deptId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AtSchedule> qw = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            qw.eq(AtSchedule::getEmployeeId, employeeId);
        }
        if (startDate != null) {
            qw.ge(AtSchedule::getScheduleDate, startDate);
        }
        if (endDate != null) {
            qw.le(AtSchedule::getScheduleDate, endDate);
        }
        qw.orderByAsc(AtSchedule::getScheduleDate);
        // Note: deptId filtering requires joining with hr_employee, handled via custom SQL if needed
        return scheduleMapper.selectList(qw);
    }
}
