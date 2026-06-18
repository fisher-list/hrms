package com.hrms.common.org.controller;

import com.hrms.common.api.R;
import com.hrms.common.org.dto.PositionDto;
import com.hrms.common.org.entity.Position;
import com.hrms.common.org.service.PositionService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for position CRUD.
 */
@Tag(name = "Positions", description = "Position management")
@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @Operation(summary = "List positions")
    @GetMapping
    @HasPermission("org:view")
    public R<List<Position>> list(@RequestParam(required = false) Long deptId) {
        return R.ok(positionService.list(deptId));
    }

    @Operation(summary = "Get position by ID")
    @GetMapping("/{id}")
    @HasPermission("org:view")
    public R<Position> getById(@PathVariable Long id) {
        return R.ok(positionService.getById(id));
    }

    @Operation(summary = "Create a position")
    @PostMapping
    @HasPermission("org:create")
    public R<Position> create(@Valid @RequestBody PositionDto dto) {
        return R.ok(positionService.create(dto));
    }

    @Operation(summary = "Update a position")
    @PutMapping("/{id}")
    @HasPermission("org:edit")
    public R<Position> update(@PathVariable Long id, @Valid @RequestBody PositionDto dto) {
        return R.ok(positionService.update(id, dto));
    }

    @Operation(summary = "Delete a position")
    @DeleteMapping("/{id}")
    @HasPermission("org:delete")
    public R<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return R.ok();
    }
}
