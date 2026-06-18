package com.hrms.common.attendance.listener;

import com.hrms.common.approval.event.ApprovalCompletedEvent;
import com.hrms.common.attendance.service.LeaveRequestService;
import com.hrms.common.attendance.service.OvertimeRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for approval completion events and dispatches to the appropriate
 * leave/overtime service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceApprovalEventListener {

    private final LeaveRequestService leaveRequestService;
    private final OvertimeRequestService overtimeRequestService;

    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        String businessType = event.getBusinessType();

        // Only handle attendance-related approval types
        if (!"AT_LEAVE_REQUEST".equals(businessType) && !"AT_OVERTIME_REQUEST".equals(businessType)) {
            return;
        }

        Long formId = Long.parseLong(event.getBusinessKey());
        boolean approved = "APPROVED".equals(event.getResult());

        log.info("Attendance approval completed: type={}, formId={}, result={}",
                businessType, formId, event.getResult());

        if (approved) {
            switch (businessType) {
                case "AT_LEAVE_REQUEST" -> leaveRequestService.onApproved(formId);
                case "AT_OVERTIME_REQUEST" -> overtimeRequestService.onApproved(formId);
            }
        } else {
            switch (businessType) {
                case "AT_LEAVE_REQUEST" -> leaveRequestService.onRejected(formId);
                case "AT_OVERTIME_REQUEST" -> overtimeRequestService.onRejected(formId);
            }
        }
    }
}
