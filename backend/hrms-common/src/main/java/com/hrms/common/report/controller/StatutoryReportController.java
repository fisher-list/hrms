package com.hrms.common.report.controller;

import com.hrms.common.api.R;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.report.service.StatutoryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 法定报表导出控制器 —— 社保/个税/公积金申报表。
 */
@Tag(name = "StatutoryReport", description = "法定报表导出")
@RestController
@RequestMapping("/api/reports/statutory")
@RequiredArgsConstructor
public class StatutoryReportController {

    private final StatutoryReportService statutoryReportService;

    @Operation(summary = "导出社保申报表")
    @GetMapping("/social-insurance")
    @HasPermission("report:statutory:export")
    public void exportSocialInsurance(@RequestParam String periodMonth,
                                      HttpServletResponse response) throws IOException {
        byte[] data = statutoryReportService.exportSocialInsuranceReport(periodMonth);
        downloadExcel(response, "社保申报表_" + periodMonth + ".xlsx", data);
    }

    @Operation(summary = "导出个税申报表")
    @GetMapping("/iit")
    @HasPermission("report:statutory:export")
    public void exportIit(@RequestParam String periodMonth,
                          HttpServletResponse response) throws IOException {
        byte[] data = statutoryReportService.exportIitReport(periodMonth);
        downloadExcel(response, "个税申报表_" + periodMonth + ".xlsx", data);
    }

    @Operation(summary = "导出公积金申报表")
    @GetMapping("/housing-fund")
    @HasPermission("report:statutory:export")
    public void exportHousingFund(@RequestParam String periodMonth,
                                  HttpServletResponse response) throws IOException {
        byte[] data = statutoryReportService.exportHousingFundReport(periodMonth);
        downloadExcel(response, "公积金申报表_" + periodMonth + ".xlsx", data);
    }

    /**
     * 通用Excel下载响应处理。
     */
    private void downloadExcel(HttpServletResponse response, String fileName, byte[] data) throws IOException {
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLength(data.length);
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
        }
    }
}
