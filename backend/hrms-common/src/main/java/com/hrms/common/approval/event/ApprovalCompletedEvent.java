package com.hrms.common.approval.event;

import lombok.Getter;

/**
 * Application event published when an approval instance reaches a terminal state.
 */
@Getter
public class ApprovalCompletedEvent {

    private final String businessType;
    private final String businessKey;
    private final String result;

    public ApprovalCompletedEvent(String businessType, String businessKey, String result) {
        this.businessType = businessType;
        this.businessKey = businessKey;
        this.result = result;
    }
}
