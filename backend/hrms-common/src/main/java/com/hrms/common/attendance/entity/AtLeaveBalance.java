package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Leave balance entity: tracks remaining quota per employee per leave type per year.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_leave_balance")
public class AtLeaveBalance extends BaseEntity {

    private Long employeeId;

    private Long leaveTypeId;

    /** Balance year, e.g. 2026. `year` is a SQL reserved word in H2/PostgreSQL, so we quote it explicitly. */
    @TableField(value = "`year`")
    private Integer year;

    /** Total quota allocated for this year */
    private BigDecimal quota;

    /** Days already used */
    private BigDecimal used;

    /** Remaining = quota - used */
    private BigDecimal remaining;
}
