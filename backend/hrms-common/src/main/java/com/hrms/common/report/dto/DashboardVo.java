package com.hrms.common.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * HR仪表盘概览数据VO
 * 包含在职人数、离职率、招聘进度、考勤异常率、薪资总额等关键指标
 */
@Data
public class DashboardVo {

    // ==================== 人员指标 ====================
    /** 在职人数 */
    private Long activeCount;
    /** 本月新入职人数 */
    private Long newHireThisMonth;
    /** 本月离职人数 */
    private Long terminatedThisMonth;
    /** 年度累计离职人数 */
    private Long terminatedThisYear;
    /** 年度离职率 (离职人数 / (期初人数+新入职)) */
    private BigDecimal turnoverRate;

    // ==================== 招聘指标 ====================
    /** 招聘中的需求数 */
    private Long openRequisitionCount;
    /** 招聘中的候选人总数 */
    private Long candidateCount;
    /** 已发Offer数 */
    private Long offerCount;

    // ==================== 考勤指标 ====================
    /** 当月考勤异常率 (异常人次 / 应出勤人次) */
    private BigDecimal attendanceAnomalyRate;
    /** 当月请假人次 */
    private Long leaveRequestCount;
    /** 当月加班人次 */
    private Long overtimeRequestCount;

    // ==================== 薪资指标 ====================
    /** 最近一个已锁定批次的薪资总额(gross) */
    private BigDecimal latestPayrollGross;
    /** 最近一个已锁定批次的实发总额(net) */
    private BigDecimal latestPayrollNet;
    /** 最近一个已锁定批次的员工数 */
    private Integer latestPayrollEmployeeCount;
    /** 最近一个已锁定批次的月份 */
    private String latestPayrollMonth;
}
