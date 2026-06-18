package com.hrms.common.payroll.controller;

import com.hrms.common.api.R;
import com.hrms.common.payroll.dto.PayrollPeriodCreateDto;
import com.hrms.common.payroll.entity.PyPayrollPeriod;
import com.hrms.common.payroll.service.PayrollPeriodService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "PayrollPeriod", description = "Payroll period management")
@RestController
@RequestMapping("/api/payroll/periods")
@RequiredArgsConstructor
public class PayrollPeriodController {

    private final PayrollPeriodService periodService;

    @Operation(summary = "Create payroll period")
    @PostMapping
    @HasPermission("py:period:edit")
    public R<PyPayrollPeriod> create(@Valid @RequestBody PayrollPeriodCreateDto dto) {
        return R.ok(periodService.create(dto));
    }

    @Operation(summary = "List payroll periods")
    @GetMapping
    @HasPermission("py:period:list")
    public R<List<PyPayrollPeriod>> list() {
        return R.ok(periodService.list());
    }
}
