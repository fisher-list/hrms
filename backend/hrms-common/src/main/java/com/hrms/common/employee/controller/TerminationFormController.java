package com.hrms.common.employee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.employee.dto.TerminationFormCreateDto;
import com.hrms.common.employee.entity.HrTerminationForm;
import com.hrms.common.employee.service.TerminationFormService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "TerminationForm", description = "Termination form management")
@RestController
@RequestMapping("/api/hr/termination-forms")
@RequiredArgsConstructor
public class TerminationFormController {

    private final TerminationFormService terminationFormService;

    @Operation(summary = "Create termination form")
    @PostMapping
    @HasPermission("hr:termination:submit")
    public R<HrTerminationForm> create(@Valid @RequestBody TerminationFormCreateDto dto) {
        return R.ok(terminationFormService.create(dto));
    }

    @Operation(summary = "Submit termination form for approval")
    @PostMapping("/{id}/submit")
    @HasPermission("hr:termination:submit")
    public R<Void> submit(@PathVariable Long id) {
        terminationFormService.submit(id);
        return R.ok();
    }

    @Operation(summary = "List termination forms")
    @GetMapping
    @HasPermission("hr:termination:submit")
    public R<IPage<HrTerminationForm>> list(
            @RequestParam(required = false) String status,
            Page<HrTerminationForm> page) {
        return R.ok(terminationFormService.list(status, page));
    }

    @Operation(summary = "Get termination form detail")
    @GetMapping("/{id}")
    @HasPermission("hr:termination:submit")
    public R<HrTerminationForm> get(@PathVariable Long id) {
        return R.ok(terminationFormService.getById(id));
    }
}
