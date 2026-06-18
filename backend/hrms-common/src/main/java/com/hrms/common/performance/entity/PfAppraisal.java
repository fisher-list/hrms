package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Performance appraisal for one employee in one cycle.
 * Status: DRAFT -> GOAL_SET -> GOAL_CONFIRMED -> SELF_REVIEWED -> MANAGER_REVIEWED -> COMPLETED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_appraisal")
public class PfAppraisal extends BaseEntity {

    private Long cycleId;

    private Long templateId;

    private Long employeeId;

    /** DRAFT / GOAL_SET / GOAL_CONFIRMED / SELF_REVIEWED / MANAGER_REVIEWED / COMPLETED */
    private String status;

    private BigDecimal selfScore;

    private BigDecimal managerScore;

    private BigDecimal finalScore;
}
