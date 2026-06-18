package com.hrms.common.attendance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.OvertimeRequestCreateDto;
import com.hrms.common.attendance.entity.AtOvertimeRequest;
import com.hrms.common.attendance.service.OvertimeRequestService;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OvertimeRequest", description = "Overtime request management")
@RestController
@RequestMapping("/api/attendance/overtime-requests")
@RequiredArgsConstructor
public class OvertimeRequestController {

    private final OvertimeRequestService overtimeRequestService;

    @Operation(summary = "Create overtime request")
    @PostMapping
    @HasPermission("at:overtime:submit")
    public R<AtOvertimeRequest> create(@Valid @RequestBody OvertimeRequestCreateDto dto,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(overtimeRequestService.create(dto, requireEmployeeId(loginUser)));
    }

    @Operation(summary = "Submit overtime request for approval")
    @PostMapping("/{id}/submit")
    @HasPermission("at:overtime:submit")
    public R<Void> submit(@PathVariable Long id) {
        overtimeRequestService.submit(id);
        return R.ok();
    }

    @Operation(summary = "List overtime requests")
    @GetMapping
    @HasPermission("at:overtime:list")
    public R<IPage<AtOvertimeRequest>> list(@RequestParam(required = false) Long employeeId,
                                            @RequestParam(required = false) String status,
                                            Page<AtOvertimeRequest> page) {
        return R.ok(overtimeRequestService.list(employeeId, status, page));
    }

    private Long requireEmployeeId(LoginUser loginUser) {
        Long employeeId = loginUser.getEmployeeId();
        if (employeeId == null) {
            throw new BizException(BizCode.FORBIDDEN, "当前账号未绑定员工档案，无法提交加班申请");
        }
        return employeeId;
    }
}
