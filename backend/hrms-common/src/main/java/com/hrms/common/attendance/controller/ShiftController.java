package com.hrms.common.attendance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.ShiftCreateDto;
import com.hrms.common.attendance.entity.AtShift;
import com.hrms.common.attendance.service.ShiftService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shift", description = "Shift template management")
@RestController
@RequestMapping("/api/attendance/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @Operation(summary = "Create shift")
    @PostMapping
    @HasPermission("at:shift:edit")
    public R<AtShift> create(@Valid @RequestBody ShiftCreateDto dto) {
        return R.ok(shiftService.create(dto));
    }

    @Operation(summary = "List shifts")
    @GetMapping
    @HasPermission("at:shift:list")
    public R<IPage<AtShift>> list(Page<AtShift> page) {
        return R.ok(shiftService.list(page));
    }

    @Operation(summary = "Update shift")
    @PutMapping("/{id}")
    @HasPermission("at:shift:edit")
    public R<AtShift> update(@PathVariable Long id, @Valid @RequestBody ShiftCreateDto dto) {
        return R.ok(shiftService.update(id, dto));
    }

    @Operation(summary = "Delete shift")
    @DeleteMapping("/{id}")
    @HasPermission("at:shift:edit")
    public R<Void> delete(@PathVariable Long id) {
        shiftService.delete(id);
        return R.ok();
    }
}
