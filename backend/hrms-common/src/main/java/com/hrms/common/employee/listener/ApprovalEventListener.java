package com.hrms.common.employee.listener;

import com.hrms.common.approval.event.ApprovalCompletedEvent;
import com.hrms.common.employee.service.HireFormService;
import com.hrms.common.employee.service.TerminationFormService;
import com.hrms.common.employee.service.TransferFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for approval completion events and dispatches to the appropriate form service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalEventListener {

    private final HireFormService hireFormService;
    private final TransferFormService transferFormService;
    private final TerminationFormService terminationFormService;

    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        // businessKey is the form ID (passed as form.getId().toString() in submit())
        Long formId = Long.parseLong(event.getBusinessKey());
        boolean approved = "APPROVED".equals(event.getResult());

        log.info("Approval completed: type={}, formId={}, result={}",
                event.getBusinessType(), formId, event.getResult());

        if (approved) {
            switch (event.getBusinessType()) {
                case "HR_HIRE_FORM" -> hireFormService.onApproved(formId);
                case "HR_TRANSFER_FORM" -> transferFormService.onApproved(formId);
                case "HR_TERMINATION_FORM" -> terminationFormService.onApproved(formId);
                default -> log.debug("Ignoring approval event for type: {}", event.getBusinessType());
            }
        } else {
            switch (event.getBusinessType()) {
                case "HR_HIRE_FORM" -> hireFormService.onRejected(formId);
                case "HR_TRANSFER_FORM" -> transferFormService.onRejected(formId);
                case "HR_TERMINATION_FORM" -> terminationFormService.onRejected(formId);
                default -> log.debug("Ignoring approval event for type: {}", event.getBusinessType());
            }
        }
    }
}
