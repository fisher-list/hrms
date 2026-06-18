package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 调休余额实体。
 * 记录员工的加班转调休余额，支持多倍率转换。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_compensatory_leave")
public class AtCompensatoryLeave extends BaseEntity {

    /** 员工ID */
    private Long employeeId;

    /** 年度，如 2026 */
    private Integer year;

    /** 总额度（天），由加班转调休累计 */
    private BigDecimal totalQuota;

    /** 已使用天数 */
    private BigDecimal used;

    /** 剩余可用天数 */
    private BigDecimal remaining;
}
