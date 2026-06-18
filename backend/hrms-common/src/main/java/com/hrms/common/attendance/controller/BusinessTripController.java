package com.hrms.common.attendance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.BusinessTripCreateDto;
import com.hrms.common.attendance.entity.AtBusinessTrip;
import com.hrms.common.attendance.service.BusinessTripService;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 出差管理控制器。
 * 提供出差申请、提交审批、查询列表等接口。
 */
@Tag(name = "BusinessTrip", description = "出差管理")
@RestController
@RequestMapping("/api/attendance/business-trips")
@RequiredArgsConstructor
public class BusinessTripController {

    private final BusinessTripService tripService;

    /**
     * 创建出差申请单。
     */
    @Operation(summary = "创建出差申请")
    @PostMapping
    @HasPermission("at:trip:submit")
    public R<AtBusinessTrip> create(@Valid @RequestBody BusinessTripCreateDto dto,
                                     @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(tripService.create(dto, requireEmployeeId(loginUser)));
    }

    /**
     * 提交出差申请进行审批。
     */
    @Operation(summary = "提交出差审批")
    @PostMapping("/{id}/submit")
    @HasPermission("at:trip:submit")
    public R<Void> submit(@PathVariable Long id) {
        tripService.submit(id);
        return R.ok();
    }

    /**
     * 查询出差申请列表。
     */
    @Operation(summary = "出差申请列表")
    @GetMapping
    @HasPermission("at:trip:list")
    public R<IPage<AtBusinessTrip>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status,
            Page<AtBusinessTrip> page) {
        return R.ok(tripService.list(employeeId, status, page));
    }

    /**
     * 获取出差申请详情。
     */
    @Operation(summary = "出差申请详情")
    @GetMapping("/{id}")
    @HasPermission("at:trip:list")
    public R<AtBusinessTrip> getById(@PathVariable Long id) {
        return R.ok(tripService.getById(id));
    }

    private Long requireEmployeeId(LoginUser loginUser) {
        Long employeeId = loginUser.getEmployeeId();
        if (employeeId == null) {
            throw new BizException(BizCode.FORBIDDEN, "当前账号未绑定员工档案，无法提交出差申请");
        }
        return employeeId;
    }
}
