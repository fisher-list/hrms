package com.hrms.common.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出差申请单实体。
 * 包含出差目的地、事由、天数等信息，支持审批流程。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("at_business_trip")
public class AtBusinessTrip extends BaseEntity {

    /** 员工ID */
    private Long employeeId;

    /** 出差目的地 */
    private String destination;

    /** 出差事由 */
    private String reason;

    /** 出差开始日期 */
    private LocalDate startDate;

    /** 出差结束日期 */
    private LocalDate endDate;

    /** 出差天数 */
    private BigDecimal days;

    /**
     * 状态：
     * PENDING   - 草稿/待提交
     * SUBMITTED - 已提交审批
     * APPROVED  - 已批准
     * REJECTED  - 已驳回
     * CANCELLED - 已取消
     */
    private String status;

    /** 关联的审批实例ID */
    private Long approvalInstanceId;
}
