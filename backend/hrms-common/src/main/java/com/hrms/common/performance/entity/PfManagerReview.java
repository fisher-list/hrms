package com.hrms.common.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Manager review entry: manager scores one scoring item.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pf_manager_review")
public class PfManagerReview extends BaseEntity {

    private Long appraisalId;

    private Long scoringItemId;

    /** Score value within the scoring item's min-max range. */
    private Integer score;

    private String comment;
}
