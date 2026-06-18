package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Leave balance change log entity.
 * Uses limited audit fields (no soft delete, no version).
 */
@Data
@TableName("at_leave_balance_log")
public class AtLeaveBalanceLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long balanceId;

    /** Change type: ADJUST / DEDUCT / REFUND / SETTLE */
    private String changeType;

    /** Change value (positive = add, negative = deduct) */
    private BigDecimal changeValue;

    /** Related leave request ID (nullable) */
    private Long relatedRequestId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
