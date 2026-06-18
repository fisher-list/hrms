package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Leave type catalog entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_leave_type")
public class AtLeaveType extends BaseEntity {

    /** Leave type code: ANNUAL, SICK, PERSONAL, MARRIAGE, MATERNITY, PATERNITY, BEREAVEMENT, COMPOFF */
    private String code;

    /** Display name, e.g. "年假" */
    private String name;

    /** Whether this leave type is paid */
    private Boolean isPaid;

    /** Annual quota in days (or hours if balanceUnit=HOUR) */
    private BigDecimal annualQuota;

    /** Balance unit: DAY or HOUR */
    private String balanceUnit;

    /** Carry-over rule: NONE, CARRY_MAX5, EXPIRE_2M */
    private String carryOverRule;
}
