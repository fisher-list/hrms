package com.hrms.common.attendance.controller;

import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.OvertimeToCompLeaveDto;
import com.hrms.common.attendance.entity.AtCompensatoryLeave;
import com.hrms.common.attendance.entity.AtCompensatoryLeaveLog;
import com.hrms.common.attendance.service.CompensatoryLeaveService;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 调休管理控制器。
 * 提供加班转调休、调休余额查询、调休变动日志查询等接口。
 */
@Tag(name = "CompensatoryLeave", description = "调休管理")
@RestController
@RequestMapping("/api/attendance/comp-leave")
@RequiredArgsConstructor
public class CompensatoryLeaveController {

    private final CompensatoryLeaveService compLeaveService;

    /**
     * 将已审批的加班申请转换为调休余额。
     */
    @Operation(summary = "加班转调休")
    @PostMapping("/convert")
    @HasPermission("at:comp:convert")
    public R<AtCompensatoryLeave> convertOvertime(
            @Valid @RequestBody OvertimeToCompLeaveDto dto,
            @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(compLeaveService.convertOvertime(dto, requireEmployeeId(loginUser)));
    }

    /**
     * 查询当前员工的调休余额（指定年度，默认当前年）。
     */
    @Operation(summary = "查询调休余额")
    @GetMapping("/balance")
    @HasPermission("at:comp:list")
    public R<AtCompensatoryLeave> getBalance(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) Integer year) {
        int queryYear = year != null ? year : LocalDate.now().getYear();
        return R.ok(compLeaveService.getBalance(requireEmployeeId(loginUser), queryYear));
    }

    /**
     * 查询当前员工所有年度的调休余额列表。
     */
    @Operation(summary = "调休余额历史列表")
    @GetMapping("/balances")
    @HasPermission("at:comp:list")
    public R<List<AtCompensatoryLeave>> listBalances(
            @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(compLeaveService.listBalances(requireEmployeeId(loginUser)));
    }

    /**
     * 查询调休余额变动日志。
     */
    @Operation(summary = "调休变动日志")
    @GetMapping("/logs/{compLeaveId}")
    @HasPermission("at:comp:list")
    public R<List<AtCompensatoryLeaveLog>> listLogs(@PathVariable Long compLeaveId) {
        return R.ok(compLeaveService.listLogs(compLeaveId));
    }

    private Long requireEmployeeId(LoginUser loginUser) {
        Long employeeId = loginUser.getEmployeeId();
        if (employeeId == null) {
            throw new BizException(BizCode.FORBIDDEN, "当前账号未绑定员工档案");
        }
        return employeeId;
    }
}
