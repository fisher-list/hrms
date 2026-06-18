package com.hrms.common.attendance.controller;

import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.SettlementCreateDto;
import com.hrms.common.attendance.entity.AtSettlementBatch;
import com.hrms.common.attendance.service.SettlementService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Settlement", description = "Year-end leave balance settlement")
@RestController
@RequestMapping("/api/attendance/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "Trigger year-end settlement")
    @PostMapping
    @HasPermission("at:settlement:run")
    public R<AtSettlementBatch> run(@Valid @RequestBody SettlementCreateDto dto) {
        return R.ok(settlementService.run(dto.getYear()));
    }

    @Operation(summary = "List settlement batches")
    @GetMapping
    @HasPermission("at:settlement:list")
    public R<List<AtSettlementBatch>> list() {
        return R.ok(settlementService.list());
    }

    @Operation(summary = "Get settlement batch detail")
    @GetMapping("/{id}")
    @HasPermission("at:settlement:list")
    public R<AtSettlementBatch> getById(@PathVariable Long id) {
        return R.ok(settlementService.getById(id));
    }
}
