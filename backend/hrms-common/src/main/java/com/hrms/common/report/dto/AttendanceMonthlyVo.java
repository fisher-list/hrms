package com.hrms.common.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 考勤月报VO
 * 支持按部门/个人维度统计月度出勤情况
 */
@Data
public class AttendanceMonthlyVo {

    /** 维度标识: 部门ID 或 员工ID */
    private Long dimensionId;

    /** 维度名称: 部门名称 或 员工姓名 */
    private String dimensionName;

    /** 员工工号(个人维度时有值) */
    private String empNo;

    /** 月份, 格式: YYYY-MM */
    private String month;

    /** 应出勤天数 */
    private Integer scheduledDays;

    /** 实际出勤天数 */
    private Integer attendedDays;

    /** 出勤率 */
    private BigDecimal attendanceRate;

    /** 迟到次数 */
    private Integer lateCount;

    /** 早退次数 */
    private Integer earlyLeaveCount;

    /** 旷工天数 */
    private Integer absentDays;

    /** 加班总时长(小时) */
    private BigDecimal overtimeHours;

    /** 请假天数 */
    private BigDecimal leaveDays;
}
