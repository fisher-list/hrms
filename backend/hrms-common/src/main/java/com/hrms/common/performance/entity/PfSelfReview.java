package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Self-review entry: employee scores one scoring item.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_self_review")
public class PfSelfReview extends BaseEntity {

    private Long appraisalId;

    private Long scoringItemId;

    /** Score value within the scoring item's min-max range. */
    private Integer score;

    private String comment;
}
