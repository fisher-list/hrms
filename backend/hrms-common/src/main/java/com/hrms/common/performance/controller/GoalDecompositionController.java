package com.hrms.common.performance.controller;

import com.hrms.common.api.R;
import com.hrms.common.performance.dto.GoalCascadeVo;
import com.hrms.common.performance.dto.GoalDecompositionDto;
import com.hrms.common.performance.dto.GoalPlanCreateDto;
import com.hrms.common.performance.entity.PfGoalPlan;
import com.hrms.common.performance.service.GoalDecompositionService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 绩效目标分解控制器 —— 组织→部门→个人目标层层分解对齐。
 */
@Tag(name = "GoalDecomposition", description = "绩效目标分解联动")
@RestController
@RequestMapping("/api/performance/goal-plans")
@RequiredArgsConstructor
public class GoalDecompositionController {

    private final GoalDecompositionService goalDecompositionService;

    @Operation(summary = "创建顶层目标")
    @PostMapping
    @HasPermission("pf:goal:edit")
    public R<List<PfGoalPlan>> createGoals(@Valid @RequestBody GoalPlanCreateDto dto) {
        return R.ok(goalDecompositionService.createGoals(dto));
    }

    @Operation(summary = "分解目标（上级→下级子目标）")
    @PostMapping("/decompose")
    @HasPermission("pf:goal:edit")
    public R<List<PfGoalPlan>> decompose(@Valid @RequestBody GoalDecompositionDto dto) {
        return R.ok(goalDecompositionService.decompose(dto));
    }

    @Operation(summary = "确认目标")
    @PostMapping("/{id}/confirm")
    @HasPermission("pf:goal:edit")
    public R<Void> confirm(@PathVariable Long id) {
        goalDecompositionService.confirm(id);
        return R.ok(null);
    }

    @Operation(summary = "获取目标树（按周期）")
    @GetMapping("/tree")
    @HasPermission("pf:goal:list")
    public R<List<GoalCascadeVo>> getGoalTree(@RequestParam Long cycleId) {
        return R.ok(goalDecompositionService.getGoalTree(cycleId));
    }

    @Operation(summary = "获取目标的所有下级子目标")
    @GetMapping("/{id}/descendants")
    @HasPermission("pf:goal:list")
    public R<List<PfGoalPlan>> getDescendants(@PathVariable Long id) {
        return R.ok(goalDecompositionService.getDescendants(id));
    }

    @Operation(summary = "更新目标实际完成值")
    @PutMapping("/{id}/actual-value")
    @HasPermission("pf:goal:edit")
    public R<Void> updateActualValue(@PathVariable Long id, @RequestParam BigDecimal actualValue) {
        goalDecompositionService.updateActualValue(id, actualValue);
        return R.ok(null);
    }
}
