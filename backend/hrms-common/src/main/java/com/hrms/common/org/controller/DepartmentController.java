package com.hrms.common.org.controller;

import com.hrms.common.api.R;
import com.hrms.common.org.dto.DepartmentDto;
import com.hrms.common.org.entity.Department;
import com.hrms.common.org.service.DepartmentService;
import com.hrms.common.org.vo.DepartmentTreeVo;
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
 * Controller for department CRUD and tree.
 */
@Tag(name = "Departments", description = "Department management")
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Get department tree for a company")
    @GetMapping("/tree")
    @HasPermission("org:view")
    public R<List<DepartmentTreeVo>> tree(@RequestParam Long companyId) {
        return R.ok(departmentService.tree(companyId));
    }

    @Operation(summary = "Create a department")
    @PostMapping
    @HasPermission("org:create")
    public R<Department> create(@Valid @RequestBody DepartmentDto dto) {
        return R.ok(departmentService.create(dto));
    }

    @Operation(summary = "Update a department")
    @PutMapping("/{id}")
    @HasPermission("org:edit")
    public R<Department> update(@PathVariable Long id, @Valid @RequestBody DepartmentDto dto) {
        return R.ok(departmentService.update(id, dto));
    }

    @Operation(summary = "Move a department to a new parent")
    @PutMapping("/{id}/move")
    @HasPermission("org:edit")
    public R<Department> move(@PathVariable Long id, @RequestParam Long newParentId) {
        return R.ok(departmentService.move(id, newParentId));
    }

    @Operation(summary = "Delete a department (soft delete)")
    @DeleteMapping("/{id}")
    @HasPermission("org:delete")
    public R<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return R.ok();
    }
}
