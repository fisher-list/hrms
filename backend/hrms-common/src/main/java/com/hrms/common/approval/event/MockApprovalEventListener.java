package com.hrms.common.approval.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mock listener that logs approval completion events.
 * Replace with real business logic (e.g. update leave status) in later sprints.
 */
@Slf4j
@Component
public class MockApprovalEventListener {

    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        log.info("Approval completed: businessType={}, businessKey={}, result={}",
                event.getBusinessType(), event.getBusinessKey(), event.getResult());
    }
}
