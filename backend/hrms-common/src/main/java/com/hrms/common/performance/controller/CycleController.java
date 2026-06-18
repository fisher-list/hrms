package com.hrms.common.performance.controller;

import com.hrms.common.api.R;
import com.hrms.common.performance.dto.CycleCreateDto;
import com.hrms.common.performance.entity.PfCycle;
import com.hrms.common.performance.service.CycleService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "PerformanceCycle", description = "Performance cycle management")
@RestController
@RequestMapping("/api/performance/cycles")
@RequiredArgsConstructor
public class CycleController {

    private final CycleService cycleService;

    @Operation(summary = "Create performance cycle")
    @PostMapping
    @HasPermission("pf:cycle:edit")
    public R<PfCycle> create(@Valid @RequestBody CycleCreateDto dto) {
        return R.ok(cycleService.create(dto));
    }

    @Operation(summary = "Activate cycle")
    @PostMapping("/{id}/activate")
    @HasPermission("pf:cycle:edit")
    public R<PfCycle> activate(@PathVariable Long id) {
        return R.ok(cycleService.activate(id));
    }

    @Operation(summary = "Close cycle")
    @PostMapping("/{id}/close")
    @HasPermission("pf:cycle:edit")
    public R<PfCycle> close(@PathVariable Long id) {
        return R.ok(cycleService.close(id));
    }

    @Operation(summary = "List cycles")
    @GetMapping
    @HasPermission("pf:cycle:list")
    public R<List<PfCycle>> list() {
        return R.ok(cycleService.list());
    }
}
