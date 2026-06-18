package com.hrms.common.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * An individual approval task assigned to a specific approver at a given node.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_task")
public class ApprovalTask extends BaseEntity {

    /** FK to approval_instance.id */
    private Long instanceId;

    /** Node sequence number in the workflow */
    private Integer nodeSeq;

    /** The user assigned to approve this task */
    private Long assigneeId;

    /** PENDING | APPROVED | REJECTED | REVOKED */
    private String status;

    private LocalDateTime assignedAt;

    private LocalDateTime actedAt;
}
