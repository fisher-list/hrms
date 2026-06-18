package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Daily attendance summary entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_daily_summary")
public class AtDailySummary extends BaseEntity {

    private Long employeeId;

    private LocalDate summaryDate;

    /** Whether employee was scheduled on this date */
    private Boolean scheduled;

    /** Whether employee actually attended */
    private Boolean attended;

    /** Minutes late (past shift start + flex) */
    private Integer lateMinutes;

    /** Minutes of early leave (before shift end - flex) */
    private Integer earlyLeaveMinutes;

    /** Whether employee was absent (scheduled but no punch) */
    private Boolean absent;

    /** Overtime hours, rounded to 0.5 */
    private BigDecimal overtimeHours;
}
