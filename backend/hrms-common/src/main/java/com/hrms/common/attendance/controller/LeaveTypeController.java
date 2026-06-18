package com.hrms.common.attendance.controller;

import com.hrms.common.api.R;
import com.hrms.common.attendance.entity.AtLeaveType;
import com.hrms.common.attendance.service.LeaveTypeService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "LeaveType", description = "Leave type management")
@RestController
@RequestMapping("/api/attendance/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @Operation(summary = "List leave types")
    @GetMapping
    @HasPermission("at:leave-type:list")
    public R<List<AtLeaveType>> list() {
        return R.ok(leaveTypeService.list());
    }

    @Operation(summary = "Create leave type")
    @PostMapping
    @HasPermission("at:leave-type:edit")
    public R<AtLeaveType> create(@Valid @RequestBody AtLeaveType dto) {
        return R.ok(leaveTypeService.create(dto));
    }
}
