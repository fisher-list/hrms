package com.hrms.common.payroll.controller;

import com.hrms.common.api.R;
import com.hrms.common.payroll.dto.YearEndBonusCreateDto;
import com.hrms.common.payroll.entity.PyYearEndBonus;
import com.hrms.common.payroll.service.YearEndBonusService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 年终奖控制器。
 * 支持年终奖计算（单独计税/并入综合所得/十三薪）和查询。
 */
@Tag(name = "YearEndBonus", description = "年终奖管理")
@RestController
@RequestMapping("/api/payroll/year-end-bonus")
@RequiredArgsConstructor
public class YearEndBonusController {

    private final YearEndBonusService yearEndBonusService;

    /**
     * 计算年终奖（单个或批量）。
     */
    @Operation(summary = "计算年终奖")
    @PostMapping("/calculate")
    @HasPermission("py:bonus:calculate")
    public R<List<PyYearEndBonus>> calculate(@Valid @RequestBody YearEndBonusCreateDto dto) {
        return R.ok(yearEndBonusService.calculate(dto));
    }

    /**
     * 查询指定年度的年终奖记录。
     */
    @Operation(summary = "查询年度年终奖记录")
    @GetMapping
    @HasPermission("py:bonus:list")
    public R<List<PyYearEndBonus>> listByYear(@RequestParam Integer bonusYear) {
        return R.ok(yearEndBonusService.listByYear(bonusYear));
    }

    /**
     * 查询指定员工的年终奖记录。
     */
    @Operation(summary = "查询员工年终奖记录")
    @GetMapping("/employee/{employeeId}")
    @HasPermission("py:bonus:list")
    public R<List<PyYearEndBonus>> listByEmployee(@PathVariable Long employeeId) {
        return R.ok(yearEndBonusService.listByEmployee(employeeId));
    }

    /**
     * 标记年终奖为已发放。
     */
    @Operation(summary = "标记年终奖已发放")
    @PostMapping("/{id}/pay")
    @HasPermission("py:bonus:pay")
    public R<PyYearEndBonus> markPaid(@PathVariable Long id) {
        return R.ok(yearEndBonusService.markPaid(id));
    }
}
