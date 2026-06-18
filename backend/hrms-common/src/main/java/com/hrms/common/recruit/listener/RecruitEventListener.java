package com.hrms.common.recruit.listener;

import com.hrms.common.approval.event.ApprovalCompletedEvent;
import com.hrms.common.recruit.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for approval completion events on RC_OFFER_APPROVAL and
 * delegates to OfferService for status transitions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitEventListener {

    private final OfferService offerService;

    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        if (!"RC_OFFER_APPROVAL".equals(event.getBusinessType())) {
            return;
        }
        log.info("Offer approval completed: key={}, result={}",
                event.getBusinessKey(), event.getResult());
        if ("APPROVED".equals(event.getResult())) {
            offerService.onApproved(event.getBusinessKey());
        } else if ("REJECTED".equals(event.getResult())) {
            offerService.onRejected(event.getBusinessKey());
        }
    }
}
