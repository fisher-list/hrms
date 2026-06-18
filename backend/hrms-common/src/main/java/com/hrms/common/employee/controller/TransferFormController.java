package com.hrms.common.employee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.employee.dto.TransferFormCreateDto;
import com.hrms.common.employee.entity.HrTransferForm;
import com.hrms.common.employee.service.TransferFormService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "TransferForm", description = "Transfer form management")
@RestController
@RequestMapping("/api/hr/transfer-forms")
@RequiredArgsConstructor
public class TransferFormController {

    private final TransferFormService transferFormService;

    @Operation(summary = "Create transfer form")
    @PostMapping
    @HasPermission("hr:transfer:submit")
    public R<HrTransferForm> create(@Valid @RequestBody TransferFormCreateDto dto) {
        return R.ok(transferFormService.create(dto));
    }

    @Operation(summary = "Submit transfer form for approval")
    @PostMapping("/{id}/submit")
    @HasPermission("hr:transfer:submit")
    public R<Void> submit(@PathVariable Long id) {
        transferFormService.submit(id);
        return R.ok();
    }

    @Operation(summary = "List transfer forms")
    @GetMapping
    @HasPermission("hr:transfer:submit")
    public R<IPage<HrTransferForm>> list(
            @RequestParam(required = false) String status,
            Page<HrTransferForm> page) {
        return R.ok(transferFormService.list(status, page));
    }

    @Operation(summary = "Get transfer form detail")
    @GetMapping("/{id}")
    @HasPermission("hr:transfer:submit")
    public R<HrTransferForm> get(@PathVariable Long id) {
        return R.ok(transferFormService.getById(id));
    }
}
