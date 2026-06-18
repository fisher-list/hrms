package com.hrms.common.attendance.listener;

import com.hrms.common.approval.event.ApprovalCompletedEvent;
import com.hrms.common.attendance.service.BusinessTripService;
import com.hrms.common.attendance.service.LeaveRequestService;
import com.hrms.common.attendance.service.OvertimeRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听审批完成事件，分发到对应的考勤服务。
 * 支持请假、加班、出差三种审批类型。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceApprovalEventListener {

    private final LeaveRequestService leaveRequestService;
    private final OvertimeRequestService overtimeRequestService;
    private final BusinessTripService businessTripService;

    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        String businessType = event.getBusinessType();

        // 仅处理考勤相关的审批类型
        if (!"AT_LEAVE_REQUEST".equals(businessType)
                && !"AT_OVERTIME_REQUEST".equals(businessType)
                && !"AT_BUSINESS_TRIP".equals(businessType)) {
            return;
        }

        Long formId = Long.parseLong(event.getBusinessKey());
        boolean approved = "APPROVED".equals(event.getResult());

        log.info("考勤审批完成: type={}, formId={}, result={}",
                businessType, formId, event.getResult());

        if (approved) {
            switch (businessType) {
                case "AT_LEAVE_REQUEST" -> leaveRequestService.onApproved(formId);
                case "AT_OVERTIME_REQUEST" -> overtimeRequestService.onApproved(formId);
                case "AT_BUSINESS_TRIP" -> businessTripService.onApproved(formId);
            }
        } else {
            switch (businessType) {
                case "AT_LEAVE_REQUEST" -> leaveRequestService.onRejected(formId);
                case "AT_OVERTIME_REQUEST" -> overtimeRequestService.onRejected(formId);
                case "AT_BUSINESS_TRIP" -> businessTripService.onRejected(formId);
            }
        }
    }
}
