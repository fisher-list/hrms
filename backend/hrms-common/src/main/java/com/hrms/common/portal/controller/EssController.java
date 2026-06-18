package com.hrms.common.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.hrms.common.approval.entity.ApprovalTask;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.attendance.entity.AtLeaveBalance;
import com.hrms.common.attendance.entity.AtLeaveRequest;
import com.hrms.common.attendance.service.LeaveBalanceService;
import com.hrms.common.attendance.service.LeaveRequestService;
import com.hrms.common.employee.service.HrEmployeeService;
import com.hrms.common.payroll.entity.PyPayrollDetail;
import com.hrms.common.payroll.service.PayrollService;
import com.hrms.common.security.LoginUser;
import com.hrms.common.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Employee Self-Service (ESS) — aggregates self-only views.
 * All employee-data endpoints scope by the authenticated principal's bound employee id.
 */
@Tag(name = "ESS", description = "Employee Self-Service portal")
@RestController
@RequestMapping("/api/portal/ess")
@RequiredArgsConstructor
public class EssController {

    private final HrEmployeeService employeeService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveRequestService leaveRequestService;
    private final ApprovalService approvalService;
    private final PayrollService payrollService;

    @Operation(summary = "Get my profile (masked)")
    @GetMapping("/me")
    public R<Map<String, Object>> me(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(employeeService.getById(requireEmployeeId(loginUser)));
    }

    @Operation(summary = "Get my leave balances for given year")
    @GetMapping("/leave-balances")
    public R<List<AtLeaveBalance>> myLeaveBalances(
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal LoginUser loginUser) {
        int y = year != null ? year : java.time.Year.now().getValue();
        return R.ok(leaveBalanceService.getBalances(requireEmployeeId(loginUser), y));
    }

    @Operation(summary = "List my leave requests")
    @GetMapping("/leave-requests")
    public R<com.baomidou.mybatisplus.core.metadata.IPage<AtLeaveRequest>> myLeaveRequests(
            @RequestParam(required = false) String status,
            Page<AtLeaveRequest> page,
            @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(leaveRequestService.list(requireEmployeeId(loginUser), status, page));
    }

    @Operation(summary = "List my approval to-do tasks")
    @GetMapping("/todo")
    public R<List<ApprovalTask>> myTodo(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(approvalService.getTodoList(loginUser.getUid()));
    }

    @Operation(summary = "List my payslips (history)")
    @GetMapping("/payslips")
    public R<List<PyPayrollDetail>> myPayslips(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(payrollService.listPayslips(requireEmployeeId(loginUser)));
    }

    @Operation(summary = "Get a specific payslip")
    @GetMapping("/payslips/{runId}")
    public R<PyPayrollDetail> myPayslip(@PathVariable Long runId,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(payrollService.getPayslip(requireEmployeeId(loginUser), runId));
    }

    private Long requireEmployeeId(LoginUser loginUser) {
        Long employeeId = loginUser.getEmployeeId();
        if (employeeId == null) {
            throw new BizException(BizCode.FORBIDDEN, "当前账号未绑定员工档案，无法访问员工自助数据");
        }
        return employeeId;
    }
}
