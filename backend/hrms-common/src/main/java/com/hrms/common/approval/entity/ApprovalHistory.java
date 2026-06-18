package com.hrms.common.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Immutable audit trail entry for every action taken in an approval workflow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_history")
public class ApprovalHistory extends BaseEntity {

    /** FK to approval_instance.id */
    private Long instanceId;

    /** FK to approval_task.id (nullable for START action) */
    private Long taskId;

    /** Node sequence number */
    private Integer nodeSeq;

    /** User who performed the action */
    private Long actorId;

    /** START | APPROVE | REJECT | REVOKE | AUTO_REASSIGN | SUSPEND */
    private String action;

    /** Optional comment */
    private String comment;

    private LocalDateTime actedAt;
}
