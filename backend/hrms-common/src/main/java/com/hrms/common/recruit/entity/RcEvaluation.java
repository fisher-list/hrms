package com.hrms.common.recruit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Interview evaluation entity — append-only, never updated.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rc_evaluation")
public class RcEvaluation extends BaseEntity {

    private Long interviewId;

    /** Score 1-5 */
    private Integer score;

    private Boolean passFlag;

    private String comment;

    private Long evaluatorUserId;
}
