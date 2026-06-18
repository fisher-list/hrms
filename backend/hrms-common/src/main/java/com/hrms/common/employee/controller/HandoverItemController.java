package com.hrms.common.employee.controller;

import com.hrms.common.api.R;
import com.hrms.common.employee.dto.HandoverItemCreateDto;
import com.hrms.common.employee.entity.HrHandoverItem;
import com.hrms.common.employee.service.HandoverItemService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "HandoverItem", description = "Termination handover item management")
@RestController
@RequestMapping("/api/hr/handover-items")
@RequiredArgsConstructor
public class HandoverItemController {

    private final HandoverItemService handoverItemService;

    @Operation(summary = "Create handover item")
    @PostMapping
    @HasPermission("hr:termination:submit")
    public R<HrHandoverItem> create(@Valid @RequestBody HandoverItemCreateDto dto) {
        return R.ok(handoverItemService.create(dto));
    }

    @Operation(summary = "List handover items by form ID")
    @GetMapping
    @HasPermission("hr:termination:submit")
    public R<List<HrHandoverItem>> list(@RequestParam Long formId) {
        return R.ok(handoverItemService.listByFormId(formId));
    }

    @Operation(summary = "Update handover item status")
    @PutMapping("/{id}/status")
    @HasPermission("hr:termination:submit")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        handoverItemService.updateStatus(id, status);
        return R.ok();
    }
}
