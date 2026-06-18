package com.hrms.common.employee.listener;

import com.hrms.common.approval.event.ApprovalCompletedEvent;
import com.hrms.common.employee.service.ContractRenewalService;
import com.hrms.common.employee.service.HireFormService;
import com.hrms.common.employee.service.ProbationConversionService;
import com.hrms.common.employee.service.TerminationFormService;
import com.hrms.common.employee.service.TransferFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听审批完成事件，分发到对应的表单服务处理。
 * 支持：入职申请、调动申请、离职申请、合同续签、试用期转正。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalEventListener {

    private final HireFormService hireFormService;
    private final TransferFormService transferFormService;
    private final TerminationFormService terminationFormService;
    private final ContractRenewalService contractRenewalService;
    private final ProbationConversionService probationConversionService;

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
                case "HR_CONTRACT_RENEWAL" -> contractRenewalService.onApproved(formId);
                case "HR_PROBATION_CONVERSION" -> probationConversionService.onApproved(formId);
                default -> log.debug("Ignoring approval event for type: {}", event.getBusinessType());
            }
        } else {
            switch (event.getBusinessType()) {
                case "HR_HIRE_FORM" -> hireFormService.onRejected(formId);
                case "HR_TRANSFER_FORM" -> transferFormService.onRejected(formId);
                case "HR_TERMINATION_FORM" -> terminationFormService.onRejected(formId);
                case "HR_CONTRACT_RENEWAL" -> contractRenewalService.onRejected(formId);
                case "HR_PROBATION_CONVERSION" -> probationConversionService.onRejected(formId);
                default -> log.debug("Ignoring approval event for type: {}", event.getBusinessType());
            }
        }
    }
}
