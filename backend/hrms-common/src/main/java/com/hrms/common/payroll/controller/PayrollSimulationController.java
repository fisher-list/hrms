package com.hrms.common.payroll.controller;

import com.hrms.common.api.R;
import com.hrms.common.payroll.dto.SimulationCompareVo;
import com.hrms.common.payroll.dto.SimulationCreateDto;
import com.hrms.common.payroll.entity.PyPayrollSimulation;
import com.hrms.common.payroll.entity.PySimulationDetail;
import com.hrms.common.payroll.service.PayrollSimulationService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工资模拟运行控制器。
 * 用于试算工资，不影响正式数据。支持调整参数后模拟，并与正式结果对比。
 */
@Tag(name = "PayrollSimulation", description = "工资模拟运行管理")
@RestController
@RequestMapping("/api/payroll/simulation")
@RequiredArgsConstructor
public class PayrollSimulationController {

    private final PayrollSimulationService simulationService;

    /**
     * 创建并执行工资模拟运行。
     * 可覆盖社保/公积金比例参数，不影响正式数据。
     */
    @Operation(summary = "创建工资模拟运行")
    @PostMapping
    @HasPermission("py:simulation:create")
    public R<PyPayrollSimulation> createSimulation(@Valid @RequestBody SimulationCreateDto dto) {
        return R.ok(simulationService.createSimulation(dto));
    }

    /**
     * 将模拟结果与正式运行结果对比。
     */
    @Operation(summary = "模拟与正式运行对比")
    @GetMapping("/{simulationId}/compare/{formalRunId}")
    @HasPermission("py:simulation:compare")
    public R<List<SimulationCompareVo>> compare(
            @PathVariable Long simulationId,
            @PathVariable Long formalRunId) {
        return R.ok(simulationService.compareWithFormal(simulationId, formalRunId));
    }

    /**
     * 查询模拟运行列表。
     */
    @Operation(summary = "查询模拟运行列表")
    @GetMapping
    @HasPermission("py:simulation:list")
    public R<List<PyPayrollSimulation>> listSimulations(
            @RequestParam(required = false) Long periodId) {
        return R.ok(simulationService.listSimulations(periodId));
    }

    /**
     * 获取模拟运行详情。
     */
    @Operation(summary = "获取模拟运行详情")
    @GetMapping("/{id}")
    @HasPermission("py:simulation:list")
    public R<PyPayrollSimulation> getSimulation(@PathVariable Long id) {
        return R.ok(simulationService.getSimulation(id));
    }

    /**
     * 获取模拟运行的员工明细。
     */
    @Operation(summary = "获取模拟运行明细")
    @GetMapping("/{id}/details")
    @HasPermission("py:simulation:list")
    public R<List<PySimulationDetail>> getSimulationDetails(@PathVariable Long id) {
        return R.ok(simulationService.getSimulationDetails(id));
    }

    /**
     * 删除模拟运行及其明细。
     */
    @Operation(summary = "删除模拟运行")
    @DeleteMapping("/{id}")
    @HasPermission("py:simulation:delete")
    public R<Void> deleteSimulation(@PathVariable Long id) {
        simulationService.deleteSimulation(id);
        return R.ok();
    }
}
