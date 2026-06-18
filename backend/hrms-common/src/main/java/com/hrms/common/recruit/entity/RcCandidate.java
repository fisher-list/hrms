package com.hrms.common.recruit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Candidate entity — tracks recruitment candidates.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rc_candidate")
public class RcCandidate extends BaseEntity {

    private String name;

    private String phone;

    private String email;

    /** AES-encrypted ID card number (base64). */
    private String idCardEnc;

    /** HMAC-SHA256 hash used only for duplicate checks. */
    private String idCardHash;

    private BigDecimal expectedSalary;

    /** NEW / INTERVIEW / OFFER / ACCEPTED / ONBOARDED */
    private String currentStatus;

    private Long jobRequisitionId;
}
