package com.hrms.common.rbac.controller;

import com.hrms.common.api.R;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.rbac.dto.RoleDto;
import com.hrms.common.rbac.entity.SysRole;
import com.hrms.common.rbac.service.SysRoleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CRUD endpoints for RBAC roles.
 */
@Tag(name = "Roles", description = "RBAC role management")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService roleService;

    @Operation(summary = "List all roles")
    @GetMapping
    @HasPermission("role:view")
    public R<List<SysRole>> list() {
        return R.ok(roleService.listAll());
    }

    @Operation(summary = "Get role by ID")
    @GetMapping("/{id}")
    @HasPermission("role:view")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @Operation(summary = "Create a new role")
    @PostMapping
    @HasPermission("role:create")
    public R<SysRole> create(@Valid @RequestBody RoleDto dto) {
        return R.ok(roleService.create(dto));
    }

    @Operation(summary = "Update an existing role")
    @PutMapping("/{id}")
    @HasPermission("role:edit")
    public R<SysRole> update(@PathVariable Long id, @Valid @RequestBody RoleDto dto) {
        return R.ok(roleService.update(id, dto));
    }

    @Operation(summary = "Delete a role")
    @DeleteMapping("/{id}")
    @HasPermission("role:delete")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }
}
