package com.hrms.common.employee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.employee.dto.HireFormCreateDto;
import com.hrms.common.employee.entity.HrHireForm;
import com.hrms.common.employee.service.HireFormService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "HireForm", description = "Hire form management")
@RestController
@RequestMapping("/api/hr/hire-forms")
@RequiredArgsConstructor
public class HireFormController {

    private final HireFormService hireFormService;

    @Operation(summary = "Create hire form")
    @PostMapping
    @HasPermission("hr:hire:submit")
    public R<HrHireForm> create(@Valid @RequestBody HireFormCreateDto dto) {
        return R.ok(hireFormService.create(dto));
    }

    @Operation(summary = "Submit hire form for approval")
    @PostMapping("/{id}/submit")
    @HasPermission("hr:hire:submit")
    public R<Void> submit(@PathVariable Long id) {
        hireFormService.submit(id);
        return R.ok();
    }

    @Operation(summary = "List hire forms")
    @GetMapping
    @HasPermission("hr:hire:submit")
    public R<IPage<HrHireForm>> list(
            @RequestParam(required = false) String status,
            Page<HrHireForm> page) {
        return R.ok(hireFormService.list(status, page));
    }

    @Operation(summary = "Get hire form detail")
    @GetMapping("/{id}")
    @HasPermission("hr:hire:submit")
    public R<HrHireForm> get(@PathVariable Long id) {
        return R.ok(hireFormService.getById(id));
    }
}
