package com.hrms.common.employee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.audit.annotation.AuditLog;
import com.hrms.common.employee.dto.ProbationConversionCreateDto;
import com.hrms.common.employee.dto.ProbationEmployeeVo;
import com.hrms.common.employee.entity.HrProbationConversion;
import com.hrms.common.employee.service.ProbationConversionService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 试用期转正控制器。
 * 提供试用期员工预警列表、转正申请创建、提交审批等功能。
 */
@Tag(name = "ProbationConversion", description = "试用期转正管理")
@RestController
@RequestMapping("/api/hr/probation-conversions")
@RequiredArgsConstructor
public class ProbationConversionController {

    private final ProbationConversionService probationConversionService;

    /**
     * 获取即将到期试用期员工预警列表。
     * 前端根据alertLevel字段显示红色(URGENT)或黄色(WARNING)标记。
     */
    @Operation(summary = "即将到期试用期员工列表")
    @GetMapping("/expiring")
    @HasPermission("hr:employee:list")
    public R<List<ProbationEmployeeVo>> listExpiringProbations(
            @RequestParam(defaultValue = "30") int daysAhead) {
        return R.ok(probationConversionService.listExpiringProbations(daysAhead));
    }

    /**
     * 创建试用期转正申请。
     */
    @Operation(summary = "创建转正申请")
    @PostMapping
    @HasPermission("hr:employee:edit")
    @AuditLog(action = "CREATE_PROBATION_CONVERSION")
    public R<HrProbationConversion> create(@Valid @RequestBody ProbationConversionCreateDto dto) {
        return R.ok(probationConversionService.create(dto));
    }

    /**
     * 提交转正申请进入审批流程。
     */
    @Operation(summary = "提交转正申请审批")
    @PostMapping("/{id}/submit")
    @HasPermission("hr:employee:edit")
    @AuditLog(action = "SUBMIT_PROBATION_CONVERSION")
    public R<Void> submit(@PathVariable Long id) {
        probationConversionService.submit(id);
        return R.ok();
    }

    /**
     * 分页查询转正申请列表。
     */
    @Operation(summary = "转正申请列表")
    @GetMapping
    @HasPermission("hr:employee:list")
    public R<IPage<HrProbationConversion>> list(
            @RequestParam(required = false) String status,
            Page<HrProbationConversion> page) {
        return R.ok(probationConversionService.list(status, page));
    }

    /**
     * 获取转正申请详情。
     */
    @Operation(summary = "转正申请详情")
    @GetMapping("/{id}")
    @HasPermission("hr:employee:list")
    public R<HrProbationConversion> get(@PathVariable Long id) {
        return R.ok(probationConversionService.getById(id));
    }
}
