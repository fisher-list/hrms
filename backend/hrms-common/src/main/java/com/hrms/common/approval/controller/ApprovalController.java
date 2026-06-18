package com.hrms.common.approval.controller;

import com.hrms.common.audit.annotation.AuditLog;
import com.hrms.common.api.R;
import com.hrms.common.approval.dto.CommentDto;
import com.hrms.common.approval.entity.ApprovalHistory;
import com.hrms.common.approval.entity.ApprovalTask;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing the approval workflow endpoints.
 */
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * Get pending approval tasks for the current user.
     */
    @GetMapping("/todo")
    @HasPermission("approval:view")
    public R<List<ApprovalTask>> getTodoList(@AuthenticationPrincipal LoginUser loginUser) {
        List<ApprovalTask> tasks = approvalService.getTodoList(loginUser.getUid());
        return R.ok(tasks);
    }

    /**
     * Approve a pending task.
     */
    @PostMapping("/tasks/{id}/approve")
    @HasPermission("approval:approve")
    @AuditLog(action = "APPROVE")
    public R<Void> approve(@PathVariable("id") Long taskId,
                           @AuthenticationPrincipal LoginUser loginUser,
                           @RequestBody(required = false) CommentDto body) {
        String comment = body != null ? body.getComment() : null;
        approvalService.approve(taskId, loginUser.getUid(), comment);
        return R.ok();
    }

    /**
     * Reject a pending task.
     */
    @PostMapping("/tasks/{id}/reject")
    @HasPermission("approval:approve")
    @AuditLog(action = "REJECT")
    public R<Void> reject(@PathVariable("id") Long taskId,
                          @AuthenticationPrincipal LoginUser loginUser,
                          @RequestBody(required = false) CommentDto body) {
        String comment = body != null ? body.getComment() : null;
        approvalService.reject(taskId, loginUser.getUid(), comment);
        return R.ok();
    }

    /**
     * Revoke a previously approved task (roll back to previous node).
     */
    @PostMapping("/tasks/{id}/revoke")
    @HasPermission("approval:approve")
    @AuditLog(action = "REVOKE_APPROVAL")
    public R<Void> revoke(@PathVariable("id") Long taskId,
                          @AuthenticationPrincipal LoginUser loginUser) {
        approvalService.revoke(taskId, loginUser.getUid());
        return R.ok();
    }

    /**
     * Get the full approval history for an instance.
     */
    @GetMapping("/instances/{id}/history")
    @HasPermission("approval:view")
    public R<List<ApprovalHistory>> getHistory(@PathVariable("id") Long instanceId) {
        List<ApprovalHistory> history = approvalService.getHistory(instanceId);
        return R.ok(history);
    }
}
