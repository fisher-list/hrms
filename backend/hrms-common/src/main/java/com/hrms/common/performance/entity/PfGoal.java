package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Goal item within a performance appraisal.
 * Status: DRAFT / CONFIRMED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_goal")
public class PfGoal extends BaseEntity {

    private Long appraisalId;

    private String description;

    /** Weight percentage (should sum to 100 across all goals in an appraisal). */
    private Integer weight;

    /** DRAFT / CONFIRMED */
    private String status;

    private Integer sortOrder;
}
