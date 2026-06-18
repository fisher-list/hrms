package com.hrms.common.attendance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.AnomalyHandleDto;
import com.hrms.common.attendance.entity.AtAttendanceAnomaly;
import com.hrms.common.attendance.service.AttendanceAnomalyService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考勤异常管理控制器。
 * 提供异常检测、列表查询、异常处理等接口。
 */
@Tag(name = "AttendanceAnomaly", description = "考勤异常管理")
@RestController
@RequestMapping("/api/attendance/anomalies")
@RequiredArgsConstructor
public class AttendanceAnomalyController {

    private final AttendanceAnomalyService anomalyService;

    /**
     * 检测指定日期范围内的考勤异常。
     * 根据排班+打卡数据自动识别迟到/早退/旷工/漏打卡。
     */
    @Operation(summary = "检测考勤异常")
    @PostMapping("/detect")
    @HasPermission("at:anomaly:detect")
    public R<List<AtAttendanceAnomaly>> detect(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(anomalyService.detect(startDate, endDate));
    }

    /**
     * 查询异常列表，支持多条件筛选和分页。
     */
    @Operation(summary = "异常列表查询")
    @GetMapping
    @HasPermission("at:anomaly:list")
    public R<IPage<AtAttendanceAnomaly>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String anomalyType,
            @RequestParam(required = false) String status,
            Page<AtAttendanceAnomaly> page) {
        return R.ok(anomalyService.list(employeeId, startDate, endDate, anomalyType, status, page));
    }

    /**
     * 处理异常记录（标记为已处理或已忽略）。
     */
    @Operation(summary = "处理考勤异常")
    @PostMapping("/handle")
    @HasPermission("at:anomaly:handle")
    public R<Void> handle(@Valid @RequestBody AnomalyHandleDto dto) {
        anomalyService.handle(dto);
        return R.ok();
    }
}
