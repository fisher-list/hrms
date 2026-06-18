package com.hrms.common.payroll.controller;

import com.hrms.common.api.R;
import com.hrms.common.payroll.dto.BankPaymentCreateDto;
import com.hrms.common.payroll.entity.PyBankPaymentBatch;
import com.hrms.common.payroll.entity.PyBankPaymentDetail;
import com.hrms.common.payroll.service.BankPaymentService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 银行代发控制器。
 * 根据锁定的薪酬运行批次生成银行代发文件，支持通用CSV/工行/建行等格式。
 */
@Tag(name = "BankPayment", description = "银行代发管理")
@RestController
@RequestMapping("/api/payroll/bank-payment")
@RequiredArgsConstructor
public class BankPaymentController {

    private final BankPaymentService bankPaymentService;

    /**
     * 创建银行代发批次。
     * 根据锁定的薪酬运行生成代发明细。
     */
    @Operation(summary = "创建银行代发批次")
    @PostMapping
    @HasPermission("py:bank:create")
    public R<PyBankPaymentBatch> createBatch(@Valid @RequestBody BankPaymentCreateDto dto) {
        return R.ok(bankPaymentService.createBatch(dto));
    }

    /**
     * 生成银行代发文件内容。
     * 根据银行类型生成对应格式的文件内容。
     */
    @Operation(summary = "生成银行代发文件")
    @GetMapping("/{batchId}/file")
    @HasPermission("py:bank:export")
    public R<String> generateFile(@PathVariable Long batchId) {
        return R.ok(bankPaymentService.generateFileContent(batchId));
    }

    /**
     * 查询代发批次列表。
     */
    @Operation(summary = "查询代发批次列表")
    @GetMapping
    @HasPermission("py:bank:list")
    public R<List<PyBankPaymentBatch>> listBatches(
            @RequestParam(required = false) Long runId) {
        return R.ok(bankPaymentService.listBatches(runId));
    }

    /**
     * 查询代发批次明细。
     */
    @Operation(summary = "查询代发批次明细")
    @GetMapping("/{batchId}/details")
    @HasPermission("py:bank:list")
    public R<List<PyBankPaymentDetail>> getBatchDetails(@PathVariable Long batchId) {
        return R.ok(bankPaymentService.getBatchDetails(batchId));
    }

    /**
     * 标记批次为已导出。
     */
    @Operation(summary = "标记批次已导出")
    @PostMapping("/{batchId}/export")
    @HasPermission("py:bank:export")
    public R<PyBankPaymentBatch> markExported(@PathVariable Long batchId) {
        return R.ok(bankPaymentService.markExported(batchId));
    }

    /**
     * 标记批次为已确认（银行已处理）。
     */
    @Operation(summary = "标记批次已确认")
    @PostMapping("/{batchId}/confirm")
    @HasPermission("py:bank:confirm")
    public R<PyBankPaymentBatch> markConfirmed(@PathVariable Long batchId) {
        return R.ok(bankPaymentService.markConfirmed(batchId));
    }
}
