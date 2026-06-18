package com.hrms.common.attendance.controller;

import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.LeaveQuotaGenerateDto;
import com.hrms.common.attendance.entity.AtLeaveQuotaRule;
import com.hrms.common.attendance.service.LeaveQuotaService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 缺勤配额管理接口。
 * <p>支持按司龄/职级规则自动生成年假配额，可单个或批量生成。</p>
 */
@Tag(name = "LeaveQuota", description = "缺勤配额自动生成管理")
@RestController
@RequestMapping("/api/attendance/leave-quota")
@RequiredArgsConstructor
public class LeaveQuotaController {

    private final LeaveQuotaService leaveQuotaService;

    // ==================== 规则管理 ====================

    @Operation(summary = "查询所有配额规则")
    @GetMapping("/rules")
    @HasPermission("at:quota:rule:list")
    public R<List<AtLeaveQuotaRule>> listRules() {
        return R.ok(leaveQuotaService.listRules());
    }

    @Operation(summary = "创建配额规则")
    @PostMapping("/rules")
    @HasPermission("at:quota:rule:edit")
    public R<AtLeaveQuotaRule> createRule(@Valid @RequestBody AtLeaveQuotaRule rule) {
        return R.ok(leaveQuotaService.createRule(rule));
    }

    @Operation(summary = "更新配额规则")
    @PutMapping("/rules/{id}")
    @HasPermission("at:quota:rule:edit")
    public R<AtLeaveQuotaRule> updateRule(@PathVariable Long id, @RequestBody AtLeaveQuotaRule rule) {
        return R.ok(leaveQuotaService.updateRule(id, rule));
    }

    @Operation(summary = "禁用配额规则")
    @DeleteMapping("/rules/{id}")
    @HasPermission("at:quota:rule:edit")
    public R<Void> disableRule(@PathVariable Long id) {
        leaveQuotaService.disableRule(id);
        return R.ok();
    }

    // ==================== 配额生成 ====================

    @Operation(summary = "为单个员工生成年假配额")
    @PostMapping("/generate")
    @HasPermission("at:quota:generate")
    public R<Map<String, Object>> generateForEmployee(@Valid @RequestBody LeaveQuotaGenerateDto dto) {
        BigDecimal quota = leaveQuotaService.generateQuotaForEmployee(dto.getEmployeeId(), dto.getYear());
        return R.ok(Map.of("employeeId", dto.getEmployeeId(), "year", dto.getYear(), "quotaDays", quota));
    }

    @Operation(summary = "批量为所有在职员工生成年假配额")
    @PostMapping("/generate-batch")
    @HasPermission("at:quota:generate")
    public R<Map<String, Object>> generateForAll(@RequestParam Integer year) {
        int count = leaveQuotaService.generateQuotaForAll(year);
        return R.ok(Map.of("year", year, "successCount", count));
    }
}
