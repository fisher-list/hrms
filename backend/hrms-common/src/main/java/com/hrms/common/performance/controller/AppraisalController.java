package com.hrms.common.performance.controller;

import com.hrms.common.api.R;
import com.hrms.common.performance.dto.GoalCreateDto;
import com.hrms.common.performance.dto.ManagerReviewDto;
import com.hrms.common.performance.dto.SelfReviewDto;
import com.hrms.common.performance.entity.*;
import com.hrms.common.performance.service.AppraisalService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Appraisal", description = "Performance appraisal workflow")
@RestController
@RequestMapping("/api/performance/appraisals")
@RequiredArgsConstructor
public class AppraisalController {

    private final AppraisalService appraisalService;

    @Operation(summary = "List appraisals")
    @GetMapping
    @HasPermission("pf:appraisal:list")
    public R<List<PfAppraisal>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long cycleId) {
        return R.ok(appraisalService.list(employeeId, cycleId));
    }

    @Operation(summary = "Get appraisal detail with goals and reviews")
    @GetMapping("/{id}")
    @HasPermission("pf:appraisal:list")
    public R<Map<String, Object>> getById(@PathVariable Long id) {
        PfAppraisal appraisal = appraisalService.getById(id);
        List<PfGoal> goals = appraisalService.getGoals(id);
        List<PfSelfReview> selfReviews = appraisalService.getSelfReviews(id);
        List<PfManagerReview> managerReviews = appraisalService.getManagerReviews(id);

        Map<String, Object> result = new HashMap<>();
        result.put("appraisal", appraisal);
        result.put("goals", goals);
        result.put("selfReviews", selfReviews);
        result.put("managerReviews", managerReviews);
        return R.ok(result);
    }

    @Operation(summary = "Set goals on appraisal")
    @PostMapping("/{id}/goals")
    @HasPermission("pf:goal:edit")
    public R<List<PfGoal>> setGoals(@PathVariable Long id,
                                    @Valid @RequestBody GoalCreateDto dto) {
        return R.ok(appraisalService.setGoals(id, dto));
    }

    @Operation(summary = "Confirm goals")
    @PostMapping("/{id}/goals/confirm")
    @HasPermission("pf:goal:edit")
    public R<Void> confirmGoals(@PathVariable Long id) {
        appraisalService.confirmGoals(id);
        return R.ok(null);
    }

    @Operation(summary = "Submit self review")
    @PostMapping("/{id}/self-review")
    @HasPermission("pf:review:self")
    public R<Void> submitSelfReview(@PathVariable Long id,
                                    @Valid @RequestBody SelfReviewDto dto) {
        appraisalService.submitSelfReview(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "Submit manager review")
    @PostMapping("/{id}/manager-review")
    @HasPermission("pf:review:manager")
    public R<Void> submitManagerReview(@PathVariable Long id,
                                       @Valid @RequestBody ManagerReviewDto dto) {
        appraisalService.submitManagerReview(id, dto);
        return R.ok(null);
    }

    @Operation(summary = "Finalize appraisal")
    @PostMapping("/{id}/finalize")
    @HasPermission("pf:appraisal:finalize")
    public R<PfAppraisal> finalize(@PathVariable Long id) {
        return R.ok(appraisalService.finalize(id));
    }
}
