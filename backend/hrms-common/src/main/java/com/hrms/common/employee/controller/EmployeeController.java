package com.hrms.common.employee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.audit.annotation.AuditLog;
import com.hrms.common.api.R;
import com.hrms.common.employee.dto.EmployeeCreateDto;
import com.hrms.common.employee.dto.EmployeeDetailVo;
import com.hrms.common.employee.dto.EmployeeListVo;
import com.hrms.common.employee.dto.EmployeeUpdateDto;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.service.HrEmployeeService;
import com.hrms.common.rbac.annotation.DataScope;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Employee", description = "Employee management")
@RestController
@RequestMapping("/api/hr/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final HrEmployeeService employeeService;

    @Operation(summary = "List employees with pagination")
    @GetMapping
    @HasPermission("hr:employee:list")
    @DataScope(deptField = "dept_id", employeeField = "id")
    public R<IPage<EmployeeListVo>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Page<HrEmployee> page) {
        // Defensive: if the request did not supply pageNum/pageSize, Spring's
        // DataBinder may not know how to materialise the generic Page<HrEmployee>
        // and the parameter will be null, which then NPEs inside MyBatis-Plus.
        // Fall back to a sane default consistent with the other controllers.
        if (page == null) {
            page = new Page<>(1, 10);
        }
        return R.ok(employeeService.list(keyword, status, page));
    }

    @Operation(summary = "Get employee detail")
    @GetMapping("/{id}")
    @HasPermission("hr:employee:list")
    public R<EmployeeDetailVo> get(@PathVariable Long id,
                                   @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(employeeService.getDetailByIdForUser(id, loginUser.getUid(), loginUser.getEmployeeId()));
    }

    @Operation(summary = "Create employee")
    @PostMapping
    @HasPermission("hr:employee:edit")
    @AuditLog(action = "CREATE_EMPLOYEE")
    public R<HrEmployee> create(@Valid @RequestBody EmployeeCreateDto dto) {
        return R.ok(employeeService.create(dto));
    }

    @Operation(summary = "Update employee")
    @PutMapping("/{id}")
    @HasPermission("hr:employee:edit")
    @AuditLog(action = "UPDATE_EMPLOYEE")
    public R<HrEmployee> update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateDto dto) {
        return R.ok(employeeService.update(id, dto));
    }

    @Operation(summary = "Terminate employee")
    @PutMapping("/{id}/termination")
    @HasPermission("hr:employee:edit")
    @AuditLog(action = "TERMINATE_EMPLOYEE")
    public R<Void> terminate(@PathVariable Long id) {
        employeeService.terminate(id);
        return R.ok();
    }
}
