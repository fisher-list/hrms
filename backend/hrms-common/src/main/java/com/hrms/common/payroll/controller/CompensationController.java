package com.hrms.common.payroll.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.payroll.dto.CompensationCreateDto;
import com.hrms.common.payroll.entity.PyCompensationMaster;
import com.hrms.common.payroll.service.CompensationService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Compensation", description = "Compensation master management")
@RestController
@RequestMapping("/api/payroll/compensations")
@RequiredArgsConstructor
public class CompensationController {

    private final CompensationService compensationService;

    @Operation(summary = "Create compensation record")
    @PostMapping
    @HasPermission("py:compensation:edit")
    public R<PyCompensationMaster> create(@Valid @RequestBody CompensationCreateDto dto) {
        return R.ok(compensationService.create(dto));
    }

    @Operation(summary = "List all compensations")
    @GetMapping
    @HasPermission("py:compensation:list")
    public R<IPage<PyCompensationMaster>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(compensationService.list(new Page<>(current, size)));
    }

    @Operation(summary = "Get latest compensation for an employee")
    @GetMapping("/{employeeId}")
    @HasPermission("py:compensation:list")
    public R<PyCompensationMaster> getByEmployee(@PathVariable Long employeeId) {
        return R.ok(compensationService.getByEmployee(employeeId));
    }
}
