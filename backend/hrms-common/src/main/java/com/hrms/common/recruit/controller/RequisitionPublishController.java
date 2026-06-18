package com.hrms.common.recruit.controller;

import com.hrms.common.api.R;
import com.hrms.common.recruit.dto.RequisitionPublishDto;
import com.hrms.common.recruit.entity.RcRequisitionPublish;
import com.hrms.common.recruit.service.RequisitionPublishService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 职位一键发布管理接口。
 * <p>支持将一个职位需求同时发布到多个招聘渠道。</p>
 */
@Tag(name = "RequisitionPublish", description = "职位一键发布管理")
@RestController
@RequestMapping("/api/recruit/requisitions")
@RequiredArgsConstructor
public class RequisitionPublishController {

    private final RequisitionPublishService publishService;

    @Operation(summary = "一键发布职位到多个渠道")
    @PostMapping("/publish")
    @HasPermission("rc:requisition:publish")
    public R<List<RcRequisitionPublish>> publish(@Valid @RequestBody RequisitionPublishDto dto) {
        return R.ok(publishService.publishToMultipleChannels(dto));
    }

    @Operation(summary = "查询职位需求的发布记录")
    @GetMapping("/{requisitionId}/publishes")
    @HasPermission("rc:requisition:list")
    public R<List<RcRequisitionPublish>> listPublishes(@PathVariable Long requisitionId) {
        return R.ok(publishService.listByRequisition(requisitionId));
    }

    @Operation(summary = "关闭某渠道的发布")
    @PostMapping("/publishes/{publishId}/close")
    @HasPermission("rc:requisition:publish")
    public R<Void> closePublish(@PathVariable Long publishId) {
        publishService.closePublish(publishId);
        return R.ok();
    }

    @Operation(summary = "获取支持的发布渠道列表")
    @GetMapping("/publish-channels")
    @HasPermission("rc:requisition:list")
    public R<List<Map<String, String>>> getSupportedChannels() {
        return R.ok(publishService.getSupportedChannels());
    }
}
