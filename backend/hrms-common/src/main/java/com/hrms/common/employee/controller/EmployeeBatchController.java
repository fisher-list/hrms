package com.hrms.common.employee.controller;

import com.hrms.common.api.R;
import com.hrms.common.audit.annotation.AuditLog;
import com.hrms.common.employee.dto.BatchImportResultVo;
import com.hrms.common.employee.service.EmployeeBatchService;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 员工批量导入导出控制器。
 * 导入：上传Excel文件，逐行校验并导入，返回结果反馈。
 * 导出：下载员工花名册Excel。
 */
@Tag(name = "EmployeeBatch", description = "员工批量导入导出")
@RestController
@RequestMapping("/api/hr/employees/batch")
@RequiredArgsConstructor
public class EmployeeBatchController {

    private final EmployeeBatchService employeeBatchService;

    /**
     * 批量导入员工信息。
     * 接收Excel文件，逐行解析校验，返回导入结果（含成功/失败统计及行级错误）。
     */
    @Operation(summary = "批量导入员工")
    @PostMapping("/import")
    @HasPermission("hr:employee:edit")
    @AuditLog(action = "BATCH_IMPORT_EMPLOYEE")
    public R<BatchImportResultVo> importEmployees(@RequestParam("file") MultipartFile file) {
        return R.ok(employeeBatchService.importFromExcel(file));
    }

    /**
     * 导出员工花名册。
     * 返回Excel文件下载流。
     */
    @Operation(summary = "导出员工花名册")
    @GetMapping("/export")
    @HasPermission("hr:employee:list")
    @AuditLog(action = "EXPORT_EMPLOYEE_ROSTER")
    public ResponseEntity<byte[]> exportEmployees(
            @RequestParam(required = false) String status) {
        byte[] data = employeeBatchService.exportToExcel(status);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "员工花名册.xlsx");
        headers.setContentLength(data.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}
