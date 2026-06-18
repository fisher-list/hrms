package com.hrms.common.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * A running (or completed) instance of an approval workflow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_instance")
public class ApprovalInstance extends BaseEntity {

    /** FK to approval_definition.id */
    private Long definitionId;

    /** Business type tag */
    private String businessType;

    /** Business key referencing the domain entity (e.g. leave request id) */
    private String businessKey;

    /** User who initiated the approval */
    private Long applicantId;

    /** Sequence number of the current node awaiting action */
    private Integer currentNodeSeq;

    /** PENDING | APPROVED | REJECTED | REVOKED | SUSPENDED */
    private String status;

    /** JSON payload carried through the approval */
    private String payload;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
