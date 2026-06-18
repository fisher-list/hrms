package com.hrms.common.attendance.controller;

import com.hrms.common.api.R;
import com.hrms.common.attendance.entity.AtLeaveBalance;
import com.hrms.common.attendance.service.LeaveBalanceService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "LeaveBalance", description = "Leave balance query")
@RestController
@RequestMapping("/api/attendance/leave-balances")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @Operation(summary = "List leave balances for an employee")
    @GetMapping
    @HasPermission("at:leave-balance:list")
    public R<List<AtLeaveBalance>> list(@RequestParam Long employeeId,
                                        @RequestParam(required = false, defaultValue = "2026") Integer year) {
        return R.ok(leaveBalanceService.getBalances(employeeId, year));
    }
}
