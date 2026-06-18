package com.hrms.common.attendance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.LeaveRequestCreateDto;
import com.hrms.common.attendance.entity.AtLeaveRequest;
import com.hrms.common.attendance.service.LeaveRequestService;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "LeaveRequest", description = "Leave request management")
@RestController
@RequestMapping("/api/attendance/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @Operation(summary = "Create leave request")
    @PostMapping
    @HasPermission("at:leave:submit")
    public R<AtLeaveRequest> create(@Valid @RequestBody LeaveRequestCreateDto dto,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(leaveRequestService.create(dto, requireEmployeeId(loginUser)));
    }

    @Operation(summary = "Submit leave request for approval")
    @PostMapping("/{id}/submit")
    @HasPermission("at:leave:submit")
    public R<Void> submit(@PathVariable Long id) {
        leaveRequestService.submit(id);
        return R.ok();
    }

    @Operation(summary = "List leave requests")
    @GetMapping
    @HasPermission("at:leave:list")
    public R<IPage<AtLeaveRequest>> list(@RequestParam(required = false) Long employeeId,
                                         @RequestParam(required = false) String status,
                                         Page<AtLeaveRequest> page) {
        return R.ok(leaveRequestService.list(employeeId, status, page));
    }

    @Operation(summary = "Get leave request detail")
    @GetMapping("/{id}")
    @HasPermission("at:leave:list")
    public R<AtLeaveRequest> getById(@PathVariable Long id) {
        return R.ok(leaveRequestService.getById(id));
    }

    private Long requireEmployeeId(LoginUser loginUser) {
        Long employeeId = loginUser.getEmployeeId();
        if (employeeId == null) {
            throw new BizException(BizCode.FORBIDDEN, "当前账号未绑定员工档案，无法提交请假申请");
        }
        return employeeId;
    }
}
