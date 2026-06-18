package com.hrms.common.recruit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Candidate status change log — not extending BaseEntity since it has
 * a minimal column set (no version, no soft delete).
 */
@Data
@TableName("rc_candidate_status_log")
public class RcCandidateStatusLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long candidateId;

    private String oldStatus;

    private String newStatus;

    private String remark;

    private LocalDateTime createdAt;

    private Long createdBy;

    private Long tenantId;
}
