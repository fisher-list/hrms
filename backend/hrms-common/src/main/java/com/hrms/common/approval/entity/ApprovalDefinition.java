package com.hrms.common.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Approval definition defining the workflow template (nodes as JSON).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_definition")
public class ApprovalDefinition extends BaseEntity {

    /** Unique code, e.g. MOCK_LEAVE */
    private String code;

    /** Human-readable name */
    private String name;

    /** Business type tag, e.g. LEAVE */
    private String businessType;

    /** JSON array of approval nodes */
    private String nodes;

    /** Whether this definition is active */
    private Boolean enabled;
}
