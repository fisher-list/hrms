package com.hrms.common.attendance.controller;

import com.hrms.common.api.BizCode;
import com.hrms.common.api.R;
import com.hrms.common.attendance.dto.TimePunchCreateDto;
import com.hrms.common.attendance.entity.AtTimePunch;
import com.hrms.common.attendance.service.TimePunchService;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "TimePunch", description = "Time punch management")
@RestController
@RequestMapping("/api/attendance/punches")
@RequiredArgsConstructor
public class TimePunchController {

    private static final long MAX_CSV_BYTES = 2 * 1024 * 1024;

    private final TimePunchService timePunchService;

    @Operation(summary = "Manual punch record")
    @PostMapping
    @HasPermission("at:punch:list")
    public R<AtTimePunch> create(@Valid @RequestBody TimePunchCreateDto dto) {
        return R.ok(timePunchService.create(dto));
    }

    @Operation(summary = "Import punch records from CSV")
    @PostMapping("/import")
    @HasPermission("at:punch:import")
    public R<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        validateCsvFile(file);
        try {
            return R.ok(timePunchService.importCsv(file.getInputStream()));
        } catch (IOException e) {
            throw new BizException(BizCode.BAD_REQUEST, "CSV 文件读取失败");
        }
    }

    @Operation(summary = "List punch records")
    @GetMapping
    @HasPermission("at:punch:list")
    public R<List<AtTimePunch>> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return R.ok(timePunchService.list(employeeId, startDate, endDate));
    }

    private void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(BizCode.BAD_REQUEST, "CSV 文件不能为空");
        }
        if (file.getSize() > MAX_CSV_BYTES) {
            throw new BizException(BizCode.BAD_REQUEST, "CSV 文件不能超过 2MB");
        }
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        boolean csvName = filename != null && filename.toLowerCase().endsWith(".csv");
        boolean csvType = contentType == null
                || "text/csv".equalsIgnoreCase(contentType)
                || "application/csv".equalsIgnoreCase(contentType)
                || "text/plain".equalsIgnoreCase(contentType)
                || "application/vnd.ms-excel".equalsIgnoreCase(contentType);
        if (!csvName && !csvType) {
            throw new BizException(BizCode.BAD_REQUEST, "仅支持 CSV 文件");
        }
    }
}
