package com.hrms.common.recruit.service;

import com.hrms.common.api.BizCode;
import com.hrms.common.approval.service.ApprovalService;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.recruit.dto.OfferCreateDto;
import com.hrms.common.recruit.entity.RcCandidate;
import com.hrms.common.recruit.entity.RcOffer;
import com.hrms.common.recruit.mapper.RcCandidateMapper;
import com.hrms.common.recruit.mapper.RcOfferMapper;
import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OfferService (S12).
 */
@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock private RcOfferMapper offerMapper;
    @Mock private RcCandidateMapper candidateMapper;
    @Mock private com.hrms.common.recruit.mapper.RcCandidateStatusLogMapper statusLogMapper;
    @Mock private com.hrms.common.recruit.mapper.RcJobRequisitionMapper requisitionMapper;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    private RecordingCandidateService candidateService;
    private RecordingRequisitionService requisitionService;
    private RecordingApprovalService approvalService;
    private OfferService offerService;

    private RcCandidate candidate;

    @BeforeEach
    void setUp() {
        candidateService = new RecordingCandidateService(candidateMapper, statusLogMapper, employeeMapper);
        requisitionService = new RecordingRequisitionService(requisitionMapper, offerMapper);
        approvalService = new RecordingApprovalService();
        offerService = new OfferService(
                offerMapper, candidateMapper, candidateService, requisitionService,
                approvalService, employeeMapper, userMapper, passwordEncoder);

        candidate = new RcCandidate();
        candidate.setId(7001L);
        candidate.setName("张三");
        candidate.setPhone("13800000000");
        candidate.setEmail("zhang@example.com");
        candidate.setIdCardEnc("ENC_ID_CARD");
        candidate.setCurrentStatus("INTERVIEW");
        candidate.setJobRequisitionId(8001L);
    }

    @Test
    @DisplayName("Test 1: create -> DRAFT offer + candidate status to OFFER")
    void testCreate() {
        when(candidateMapper.selectById(7001L)).thenReturn(candidate);
        when(offerMapper.insert(any(RcOffer.class))).thenReturn(1);

        OfferCreateDto dto = new OfferCreateDto();
        dto.setCandidateId(7001L);
        dto.setJobRequisitionId(8001L);
        dto.setSalary(new BigDecimal("15000.00"));
        dto.setOnboardDate(LocalDate.of(2026, 7, 1));

        RcOffer offer = offerService.create(dto);

        assertEquals("DRAFT", offer.getStatus());
        assertEquals(0, new BigDecimal("15000.00").compareTo(offer.getSalary()));
        verify(offerMapper).insert(any(RcOffer.class));
        assertEquals(1, candidateService.calls.size());
        assertEquals("7001|OFFER|创建Offer", candidateService.calls.get(0));
    }

    @Test
    @DisplayName("Test 2: create -> throws when candidate missing")
    void testCreateMissingCandidate() {
        when(candidateMapper.selectById(7001L)).thenReturn(null);

        OfferCreateDto dto = new OfferCreateDto();
        dto.setCandidateId(7001L);
        dto.setSalary(new BigDecimal("15000.00"));
        dto.setOnboardDate(LocalDate.of(2026, 7, 1));

        BizException ex = assertThrows(BizException.class, () -> offerService.create(dto));
        assertEquals(BizCode.BAD_REQUEST, ex.getCode());
        verify(offerMapper, never()).insert(any());
        assertTrue(candidateService.calls.isEmpty());
    }

    @Test
    @DisplayName("Test 3: submit -> status SENT and approval started")
    void testSubmit() {
        RcOffer draft = newOffer(9001L, "DRAFT");
        when(offerMapper.selectById(9001L)).thenReturn(draft);
        when(offerMapper.updateById(any(RcOffer.class))).thenReturn(1);
        approvalService.nextInstanceId = 55555L;

        offerService.submit(9001L, 123L);

        ArgumentCaptor<RcOffer> captor = ArgumentCaptor.forClass(RcOffer.class);
        verify(offerMapper).updateById(captor.capture());
        RcOffer saved = captor.getValue();
        assertEquals("SENT", saved.getStatus());
        assertEquals(55555L, saved.getApprovalInstanceId());
        assertEquals(1, approvalService.startCalls.size());
        assertEquals("RC_OFFER_APPROVAL|9001|RC_OFFER_APPROVAL|null|123",
                approvalService.startCalls.get(0));
    }

    @Test
    @DisplayName("Test 4: submit -> rejects non-DRAFT")
    void testSubmitNonDraft() {
        RcOffer sent = newOffer(9001L, "SENT");
        when(offerMapper.selectById(9001L)).thenReturn(sent);

        BizException ex = assertThrows(BizException.class, () -> offerService.submit(9001L, 123L));
        assertEquals(BizCode.BAD_REQUEST, ex.getCode());
        assertTrue(approvalService.startCalls.isEmpty());
    }

    @Test
    @DisplayName("Test 5: accept -> creates employee draft + sys user, candidate ACCEPTED, requisition checked")
    void testAccept() {
        RcOffer sent = newOffer(9001L, "SENT");
        sent.setCandidateId(7001L);
        sent.setJobRequisitionId(8001L);
        sent.setOnboardDate(LocalDate.of(2026, 7, 1));

        when(offerMapper.selectById(9001L)).thenReturn(sent);
        when(candidateMapper.selectById(7001L)).thenReturn(candidate);
        when(offerMapper.updateById(any(RcOffer.class))).thenReturn(1);
        when(employeeMapper.insert(any(HrEmployee.class))).thenAnswer(inv -> {
            HrEmployee e = inv.getArgument(0);
            e.setId(60001L);
            return 1;
        });
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        offerService.accept(9001L);

        // Offer becomes ACCEPTED
        ArgumentCaptor<RcOffer> offerCap = ArgumentCaptor.forClass(RcOffer.class);
        verify(offerMapper).updateById(offerCap.capture());
        assertEquals("ACCEPTED", offerCap.getValue().getStatus());

        // Employee created in PENDING_HIRE
        ArgumentCaptor<HrEmployee> empCap = ArgumentCaptor.forClass(HrEmployee.class);
        verify(employeeMapper).insert(empCap.capture());
        HrEmployee emp = empCap.getValue();
        assertEquals("张三", emp.getName());
        assertEquals("PENDING_HIRE", emp.getStatus());
        assertEquals(LocalDate.of(2026, 7, 1), emp.getHireDate());

        // SysUser created with username = E + employeeId
        ArgumentCaptor<SysUser> userCap = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(userCap.capture());
        assertEquals("E60001", userCap.getValue().getUsername());
        assertEquals("ACTIVE", userCap.getValue().getStatus());

        // Candidate moves to ACCEPTED, requisition auto-close checked
        assertTrue(candidateService.calls.contains("7001|ACCEPTED|候选人接受Offer"));
        assertEquals(java.util.List.of(8001L), requisitionService.closedCalls);
    }

    @Test
    @DisplayName("Test 6: accept -> rejects non-SENT")
    void testAcceptNonSent() {
        RcOffer draft = newOffer(9001L, "DRAFT");
        when(offerMapper.selectById(9001L)).thenReturn(draft);

        BizException ex = assertThrows(BizException.class, () -> offerService.accept(9001L));
        assertEquals(BizCode.BAD_REQUEST, ex.getCode());
        verify(employeeMapper, never()).insert(any());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("Test 7: decline -> status DECLINED + candidate back to NEW")
    void testDecline() {
        RcOffer sent = newOffer(9001L, "SENT");
        sent.setCandidateId(7001L);
        when(offerMapper.selectById(9001L)).thenReturn(sent);
        when(offerMapper.updateById(any(RcOffer.class))).thenReturn(1);

        offerService.decline(9001L);

        ArgumentCaptor<RcOffer> captor = ArgumentCaptor.forClass(RcOffer.class);
        verify(offerMapper).updateById(captor.capture());
        assertEquals("DECLINED", captor.getValue().getStatus());
        assertEquals(java.util.List.of("7001|NEW|候选人拒绝Offer"), candidateService.calls);
    }

    @Test
    @DisplayName("Test 8: onRejected -> status DECLINED via approval")
    void testOnRejected() {
        RcOffer sent = newOffer(9001L, "SENT");
        sent.setCandidateId(7001L);
        when(offerMapper.selectById(9001L)).thenReturn(sent);
        when(offerMapper.updateById(any(RcOffer.class))).thenReturn(1);

        offerService.onRejected("9001");

        ArgumentCaptor<RcOffer> captor = ArgumentCaptor.forClass(RcOffer.class);
        verify(offerMapper).updateById(captor.capture());
        assertEquals("DECLINED", captor.getValue().getStatus());
        assertEquals(java.util.List.of("7001|NEW|Offer审批被拒绝"), candidateService.calls);
    }

    @Test
    @DisplayName("Test 9: getById -> throws when not found")
    void testGetByIdMissing() {
        when(offerMapper.selectById(9999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> offerService.getById(9999L));
        assertEquals(BizCode.BAD_REQUEST, ex.getCode());
    }

    private RcOffer newOffer(Long id, String status) {
        RcOffer offer = new RcOffer();
        offer.setId(id);
        offer.setStatus(status);
        offer.setSalary(new BigDecimal("15000.00"));
        offer.setOnboardDate(LocalDate.of(2026, 7, 1));
        return offer;
    }

    /** Stub CandidateService capturing changeStatus calls — avoids mocking concrete class on JDK 25. */
    static class RecordingCandidateService extends CandidateService {
        final java.util.List<String> calls = new java.util.ArrayList<>();
        RecordingCandidateService(RcCandidateMapper m,
                                  com.hrms.common.recruit.mapper.RcCandidateStatusLogMapper l,
                                  HrEmployeeMapper e) { super(m, l, e); }
        @Override public void changeStatus(Long id, String status, String remark) {
            calls.add(id + "|" + status + "|" + remark);
        }
    }

    /** Stub RequisitionService capturing checkAndAutoClose calls. */
    static class RecordingRequisitionService extends RequisitionService {
        final java.util.List<Long> closedCalls = new java.util.ArrayList<>();
        RecordingRequisitionService(com.hrms.common.recruit.mapper.RcJobRequisitionMapper m,
                                    RcOfferMapper o) { super(m, o); }
        @Override public void checkAndAutoClose(Long requisitionId) { closedCalls.add(requisitionId); }
    }

    /** Stub ApprovalService — bypasses parent's heavy deps via no-arg ctor would fail; use null mappers. */
    static class RecordingApprovalService extends ApprovalService {
        final java.util.List<String> startCalls = new java.util.ArrayList<>();
        long nextInstanceId = 1L;
        RecordingApprovalService() {
            super(null, null, null, null, null, null, null, java.time.Clock.systemDefaultZone());
        }
        @Override
        public Long start(String definitionCode, String businessKey, String businessType,
                          String formData, Long applicantId) {
            startCalls.add(definitionCode + "|" + businessKey + "|" + businessType + "|"
                    + formData + "|" + applicantId);
            return nextInstanceId;
        }
    }
}
