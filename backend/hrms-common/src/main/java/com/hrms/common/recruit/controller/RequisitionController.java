package com.hrms.common.recruit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.recruit.dto.RequisitionCreateDto;
import com.hrms.common.recruit.entity.RcJobRequisition;
import com.hrms.common.recruit.service.RequisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Requisition", description = "Job requisition management")
@RestController
@RequestMapping("/api/recruit/requisitions")
@RequiredArgsConstructor
public class RequisitionController {

    private final RequisitionService requisitionService;

    @Operation(summary = "Create job requisition")
    @PostMapping
    @HasPermission("rc:requisition:edit")
    public R<RcJobRequisition> create(@Valid @RequestBody RequisitionCreateDto dto) {
        return R.ok(requisitionService.create(dto));
    }

    @Operation(summary = "Get requisition by ID")
    @GetMapping("/{id}")
    @HasPermission("rc:requisition:list")
    public R<RcJobRequisition> getById(@PathVariable Long id) {
        return R.ok(requisitionService.getById(id));
    }

    @Operation(summary = "List all requisitions")
    @GetMapping
    @HasPermission("rc:requisition:list")
    public R<IPage<RcJobRequisition>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(requisitionService.list(new Page<>(current, size)));
    }
}
