package com.hrms.common.employee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.audit.annotation.AuditLog;
import com.hrms.common.employee.dto.ContractRenewalCreateDto;
import com.hrms.common.employee.dto.ExpiringContractVo;
import com.hrms.common.employee.entity.HrContractRenewal;
import com.hrms.common.employee.service.ContractRenewalService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同续签控制器。
 * 提供即将到期合同预警、续签申请创建、提交审批等功能。
 */
@Tag(name = "ContractRenewal", description = "合同续签管理")
@RestController
@RequestMapping("/api/hr/contract-renewals")
@RequiredArgsConstructor
public class ContractRenewalController {

    private final ContractRenewalService contractRenewalService;

    /**
     * 获取即将到期的合同预警列表。
     * 前端根据alertLevel字段显示红色(URGENT)或黄色(WARNING)标记。
     */
    @Operation(summary = "即将到期合同预警列表")
    @GetMapping("/expiring")
    @HasPermission("hr:employee:list")
    public R<List<ExpiringContractVo>> listExpiringContracts(
            @RequestParam(defaultValue = "30") int daysAhead) {
        return R.ok(contractRenewalService.listExpiringContracts(daysAhead));
    }

    /**
     * 创建合同续签申请。
     */
    @Operation(summary = "创建合同续签申请")
    @PostMapping
    @HasPermission("hr:employee:edit")
    @AuditLog(action = "CREATE_CONTRACT_RENEWAL")
    public R<HrContractRenewal> create(@Valid @RequestBody ContractRenewalCreateDto dto) {
        return R.ok(contractRenewalService.create(dto));
    }

    /**
     * 提交续签申请进入审批流程。
     */
    @Operation(summary = "提交续签申请审批")
    @PostMapping("/{id}/submit")
    @HasPermission("hr:employee:edit")
    @AuditLog(action = "SUBMIT_CONTRACT_RENEWAL")
    public R<Void> submit(@PathVariable Long id) {
        contractRenewalService.submit(id);
        return R.ok();
    }

    /**
     * 分页查询续签申请列表。
     */
    @Operation(summary = "续签申请列表")
    @GetMapping
    @HasPermission("hr:employee:list")
    public R<IPage<HrContractRenewal>> list(
            @RequestParam(required = false) String status,
            Page<HrContractRenewal> page) {
        return R.ok(contractRenewalService.list(status, page));
    }

    /**
     * 获取续签申请详情。
     */
    @Operation(summary = "续签申请详情")
    @GetMapping("/{id}")
    @HasPermission("hr:employee:list")
    public R<HrContractRenewal> get(@PathVariable Long id) {
        return R.ok(contractRenewalService.getById(id));
    }
}
