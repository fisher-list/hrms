package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Year-end settlement batch entity.
 * Tracks settlement runs per year with aggregate statistics.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_settlement_batch")
public class AtSettlementBatch extends BaseEntity {

    /** Settlement year, e.g. 2026 */
    private Integer settlementYear;

    /** Batch status: PENDING / RUNNING / COMPLETED */
    private String status;

    /** Number of employees processed */
    private Integer processedCount;

    /** Total carryover days across all employees */
    private BigDecimal totalCarryoverDays;

    /** Total expired/forfeited days across all employees */
    private BigDecimal totalExpiredDays;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
