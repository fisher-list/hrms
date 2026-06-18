package com.hrms.common.recruit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Offer entity — tracks offers made to candidates.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rc_offer")
public class RcOffer extends BaseEntity {

    private Long candidateId;

    private Long jobRequisitionId;

    private BigDecimal salary;

    private LocalDate onboardDate;

    /** DRAFT / SENT / ACCEPTED / DECLINED / EXPIRED */
    private String status;

    private Long approvalInstanceId;
}
