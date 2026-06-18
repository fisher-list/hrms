package com.hrms.common.approval.service;

import com.hrms.common.approval.entity.ApprovalInstance;

/**
 * Strategy interface for resolving an approver rule to a concrete user ID.
 */
public interface ApproverResolver {

    /**
     * Resolve the approver rule to a concrete user ID.
     *
     * @param approverRule the rule string from the node definition
     * @param applicantId  the user who started the approval
     * @param instance     the current approval instance
     * @return assignee user id, or null if no eligible approver (triggers SUSPEND)
     */
    Long resolve(String approverRule, Long applicantId, ApprovalInstance instance);
}
