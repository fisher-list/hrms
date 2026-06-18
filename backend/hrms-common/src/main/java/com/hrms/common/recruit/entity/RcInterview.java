package com.hrms.common.recruit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Interview schedule entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rc_interview")
public class RcInterview extends BaseEntity {

    private Long candidateId;

    private Integer roundNo;

    private Long interviewerUserId;

    private LocalDateTime interviewTime;

    private String location;

    /** PENDING / PASS / FAIL */
    private String result;
}
