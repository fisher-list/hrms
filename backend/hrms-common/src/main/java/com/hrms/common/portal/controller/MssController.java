package com.hrms.common.portal.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.approval.entity.ApprovalTask;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.attendance.entity.AtLeaveRequest;
import com.hrms.common.attendance.service.LeaveRequestService;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.service.HrEmployeeService;
import com.hrms.common.employee.dto.EmployeeListVo;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manager Self-Service (MSS) — team views and approval actions for managers.
 * Permissions: mss:* gate manager-only actions; data scope (OWN_DEPT/SUBORDINATE_TREE)
 * is enforced by @DataScope on the underlying services.
 */
@Tag(name = "MSS", description = "Manager Self-Service portal")
@RestController
@RequestMapping("/api/portal/mss")
@RequiredArgsConstructor
public class MssController {

    private final HrEmployeeService employeeService;
    private final LeaveRequestService leaveRequestService;
    private final ApprovalService approvalService;

    @Operation(summary = "List my team members (data scope filtered)")
    @GetMapping("/team")
    @HasPermission("mss:team:list")
    public R<IPage<EmployeeListVo>> team(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status,
                                          Page<HrEmployee> page) {
        return R.ok(employeeService.list(keyword, status, page));
    }

    @Operation(summary = "List team leave requests (data scope filtered)")
    @GetMapping("/team/leave-requests")
    @HasPermission("mss:leave:list")
    public R<IPage<AtLeaveRequest>> teamLeaveRequests(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status,
            Page<AtLeaveRequest> page) {
        return R.ok(leaveRequestService.list(employeeId, status, page));
    }

    @Operation(summary = "List my pending approval tasks")
    @GetMapping("/todo")
    @HasPermission("mss:todo:list")
    public R<List<ApprovalTask>> myTodo(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(approvalService.getTodoList(loginUser.getUid()));
    }

    @Operation(summary = "Approve a task")
    @PostMapping("/tasks/{taskId}/approve")
    @HasPermission("mss:approval:act")
    public R<Void> approve(@PathVariable Long taskId,
                           @RequestBody ApprovalActionDto dto,
                           @AuthenticationPrincipal LoginUser loginUser) {
        approvalService.approve(taskId, loginUser.getUid(), dto.getComment());
        return R.ok();
    }

    @Operation(summary = "Reject a task")
    @PostMapping("/tasks/{taskId}/reject")
    @HasPermission("mss:approval:act")
    public R<Void> reject(@PathVariable Long taskId,
                          @RequestBody ApprovalActionDto dto,
                          @AuthenticationPrincipal LoginUser loginUser) {
        approvalService.reject(taskId, loginUser.getUid(), dto.getComment());
        return R.ok();
    }

    @Data
    public static class ApprovalActionDto {
        @NotBlank
        private String comment;
    }
}
