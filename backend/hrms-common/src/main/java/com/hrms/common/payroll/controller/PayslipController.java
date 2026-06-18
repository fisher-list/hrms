package com.hrms.common.payroll.controller;

import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.entity.PyPayrollDetail;
import com.hrms.common.payroll.service.PayrollService;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payslip", description = "Payslip / payroll detail queries")
@RestController
@RequestMapping("/api/payroll/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayrollService payrollService;

    @Operation(summary = "List current user's payslips")
    @GetMapping
    @HasPermission("py:payslip:list")
    public R<List<PyPayrollDetail>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(payrollService.listPayslips(requireEmployeeId(loginUser)));
    }

    @Operation(summary = "Get payslip for a specific run")
    @GetMapping("/{runId}")
    @HasPermission("py:payslip:list")
    public R<PyPayrollDetail> get(@PathVariable Long runId,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(payrollService.getPayslip(requireEmployeeId(loginUser), runId));
    }

    private Long requireEmployeeId(LoginUser loginUser) {
        Long employeeId = loginUser.getEmployeeId();
        if (employeeId == null) {
            throw new BizException(BizCode.FORBIDDEN, "当前账号未绑定员工档案，无法访问工资条");
        }
        return employeeId;
    }
}
