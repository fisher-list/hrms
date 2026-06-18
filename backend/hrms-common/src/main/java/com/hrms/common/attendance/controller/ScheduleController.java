package com.hrms.common.attendance.controller;

import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.ScheduleCreateDto;
import com.hrms.common.attendance.entity.AtSchedule;
import com.hrms.common.attendance.service.ScheduleService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Schedule", description = "Schedule assignment management")
@RestController
@RequestMapping("/api/attendance/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "Batch assign schedules")
    @PostMapping
    @HasPermission("at:schedule:edit")
    public R<List<AtSchedule>> create(@Valid @RequestBody ScheduleCreateDto dto) {
        return R.ok(scheduleService.create(dto));
    }

    @Operation(summary = "List schedules")
    @GetMapping
    @HasPermission("at:schedule:list")
    public R<List<AtSchedule>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return R.ok(scheduleService.list(employeeId, deptId, startDate, endDate));
    }
}
