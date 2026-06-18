package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Scoring item within a performance template.
 * Weight is a percentage (0-100), score range is min_score..max_score.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_scoring_item")
public class PfScoringItem extends BaseEntity {

    private Long templateId;

    private String name;

    /** Weight percentage (must sum to 100 across all items in a template). */
    private Integer weight;

    /** Minimum score value, default 1. */
    private Integer minScore;

    /** Maximum score value, default 5. */
    private Integer maxScore;

    private Integer sortOrder;
}
