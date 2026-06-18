package com.hrms.common.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.R;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.rbac.annotation.HasPermission;
import com.hrms.common.report.dto.*;
import com.hrms.common.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表控制器
 * 提供HR仪表盘、人事花名册、考勤月报、薪资汇总等报表API
 */
@Tag(name = "Report", description = "HR报表管理")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ==================== HR仪表盘 ====================

    /**
     * 获取HR仪表盘概览数据
     * 包含在职人数、离职率、招聘进度、考勤异常率、薪资总额等关键指标
     */
    @Operation(summary = "获取HR仪表盘概览数据")
    @GetMapping("/dashboard")
    @HasPermission("hr:dashboard:view")
    public R<DashboardVo> getDashboard() {
        return R.ok(reportService.getDashboard());
    }

    // ==================== 人事花名册报表 ====================

    /**
     * 人事花名册查询
     * 支持自定义列、筛选条件、排序、分页
     */
    @Operation(summary = "人事花名册查询")
    @PostMapping("/roster")
    @HasPermission("hr:roster:view")
    public R<IPage<RosterRowVo>> getRoster(@RequestBody(required = false) RosterQueryDto query,
                                            Page<HrEmployee> page) {
        if (query == null) {
            query = new RosterQueryDto();
        }
        if (page == null) {
            page = new Page<>(1, 20);
        }
        return R.ok(reportService.getRoster(query, page));
    }

    // ==================== 考勤月报 ====================

    /**
     * 考勤月报 - 按部门维度
     * 统计月度出勤率、迟到、早退、加班、请假等指标
     */
    @Operation(summary = "考勤月报-按部门维度")
    @GetMapping("/attendance-monthly/dept")
    @HasPermission("at:report:view")
    public R<List<AttendanceMonthlyVo>> getAttendanceMonthlyByDept(
            @RequestParam String month) {
        return R.ok(reportService.getAttendanceMonthlyByDept(month));
    }

    /**
     * 考勤月报 - 按个人维度
     * 统计月度出勤率、迟到、早退、加班、请假等指标
     */
    @Operation(summary = "考勤月报-按个人维度")
    @GetMapping("/attendance-monthly/employee")
    @HasPermission("at:report:view")
    public R<List<AttendanceMonthlyVo>> getAttendanceMonthlyByEmployee(
            @RequestParam String month,
            @RequestParam(required = false) Long deptId) {
        return R.ok(reportService.getAttendanceMonthlyByEmployee(month, deptId));
    }

    // ==================== 薪资汇总报表 ====================

    /**
     * 薪资汇总报表 - 按部门维度
     * 部门薪资对比、薪资结构分析
     */
    @Operation(summary = "薪资汇总报表-按部门维度")
    @GetMapping("/payroll-summary/dept")
    @HasPermission("py:report:view")
    public R<List<PayrollSummaryVo>> getPayrollSummaryByDept(
            @RequestParam String month) {
        return R.ok(reportService.getPayrollSummaryByDept(month));
    }

    /**
     * 薪资汇总报表 - 年度薪资趋势
     * 返回每个月各部门的薪资汇总，用于年度趋势分析
     */
    @Operation(summary = "薪资汇总报表-年度薪资趋势")
    @GetMapping("/payroll-summary/trend")
    @HasPermission("py:report:view")
    public R<List<PayrollSummaryVo>> getPayrollYearlyTrend(
            @RequestParam Integer year) {
        return R.ok(reportService.getPayrollYearlyTrend(year));
    }
}
