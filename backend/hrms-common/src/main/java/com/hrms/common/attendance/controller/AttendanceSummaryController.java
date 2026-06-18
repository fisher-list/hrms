package com.hrms.common.attendance.controller;

import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.SummaryGenerateDto;
import com.hrms.common.attendance.entity.AtDailySummary;
import com.hrms.common.attendance.service.AttendanceSummaryService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "AttendanceSummary", description = "Attendance summary management")
@RestController
@RequestMapping("/api/attendance/summaries")
@RequiredArgsConstructor
public class AttendanceSummaryController {

    private final AttendanceSummaryService summaryService;

    @Operation(summary = "Generate attendance summaries")
    @PostMapping("/generate")
    @HasPermission("at:summary:generate")
    public R<List<AtDailySummary>> generate(@Valid @RequestBody SummaryGenerateDto dto) {
        return R.ok(summaryService.generate(dto.getStartDate(), dto.getEndDate()));
    }

    @Operation(summary = "List attendance summaries")
    @GetMapping
    @HasPermission("at:summary:list")
    public R<List<AtDailySummary>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return R.ok(summaryService.list(employeeId, startDate, endDate));
    }
}
