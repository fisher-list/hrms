package com.hrms.common.performance.controller;

import com.hrms.common.api.R;
import com.hrms.common.performance.dto.SalaryAdjustmentVo;
import com.hrms.common.performance.entity.PfSalaryAdjustment;
import com.hrms.common.performance.service.PerformanceSalaryLinkService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 绩效结果联动薪酬调整控制器。
 */
@Tag(name = "PerformanceSalaryLink", description = "绩效结果联动薪酬调整")
@RestController
@RequestMapping("/api/performance/salary-adjustments")
@RequiredArgsConstructor
public class PerformanceSalaryLinkController {

    private final PerformanceSalaryLinkService performanceSalaryLinkService;

    @Operation(summary = "为单个考核生成调薪建议")
    @PostMapping("/generate/{appraisalId}")
    @HasPermission("pf:appraisal:finalize")
    public R<PfSalaryAdjustment> generate(@PathVariable Long appraisalId) {
        return R.ok(performanceSalaryLinkService.generateAdjustment(appraisalId));
    }

    @Operation(summary = "批量为周期内所有已完成考核生成调薪建议")
    @PostMapping("/batch-generate")
    @HasPermission("pf:appraisal:finalize")
    public R<List<PfSalaryAdjustment>> batchGenerate(@RequestParam Long cycleId) {
        return R.ok(performanceSalaryLinkService.batchGenerateForCycle(cycleId));
    }

    @Operation(summary = "查询调薪建议列表")
    @GetMapping
    @HasPermission("pf:appraisal:list")
    public R<List<SalaryAdjustmentVo>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long cycleId,
            @RequestParam(required = false) String status) {
        return R.ok(performanceSalaryLinkService.list(employeeId, cycleId, status));
    }

    @Operation(summary = "审批通过调薪建议")
    @PostMapping("/{id}/approve")
    @HasPermission("pf:appraisal:finalize")
    public R<Void> approve(@PathVariable Long id) {
        performanceSalaryLinkService.approve(id);
        return R.ok(null);
    }

    @Operation(summary = "拒绝调薪建议")
    @PostMapping("/{id}/reject")
    @HasPermission("pf:appraisal:finalize")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        performanceSalaryLinkService.reject(id, reason);
        return R.ok(null);
    }

    @Operation(summary = "执行已审批的调薪")
    @PostMapping("/{id}/execute")
    @HasPermission("pf:appraisal:finalize")
    public R<Void> execute(@PathVariable Long id) {
        performanceSalaryLinkService.execute(id);
        return R.ok(null);
    }
}
