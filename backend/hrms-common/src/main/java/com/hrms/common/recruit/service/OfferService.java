package com.hrms.common.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Offer lifecycle management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {

    private static final String APPROVAL_DEF = "RC_OFFER_APPROVAL";
    private static final String BUSINESS_TYPE = "RC_OFFER_APPROVAL";

    private final RcOfferMapper offerMapper;
    private final RcCandidateMapper candidateMapper;
    private final CandidateService candidateService;
    private final RequisitionService requisitionService;
    private final ApprovalService approvalService;
    private final HrEmployeeMapper employeeMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a DRAFT offer for a candidate.
     */
    @Transactional
    public RcOffer create(OfferCreateDto dto) {
        RcCandidate candidate = candidateMapper.selectById(dto.getCandidateId());
        if (candidate == null) {
            throw new BizException(BizCode.BAD_REQUEST, "候选人不存在: " + dto.getCandidateId());
        }

        RcOffer offer = new RcOffer();
        offer.setCandidateId(dto.getCandidateId());
        offer.setJobRequisitionId(dto.getJobRequisitionId());
        offer.setSalary(dto.getSalary());
        offer.setOnboardDate(dto.getOnboardDate());
        offer.setStatus("DRAFT");
        offerMapper.insert(offer);

        // Update candidate status to OFFER
        candidateService.changeStatus(candidate.getId(), "OFFER", "创建Offer");

        return offer;
    }

    /**
     * Submit offer for approval.
     */
    @Transactional
    public void submit(Long offerId, Long applicantId) {
        RcOffer offer = getOfferById(offerId);
        if (!"DRAFT".equals(offer.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "只有 DRAFT 状态的 Offer 可以提交审批");
        }

        Long instanceId = approvalService.start(
                APPROVAL_DEF,
                String.valueOf(offerId),
                BUSINESS_TYPE,
                null,
                applicantId);
        offer.setApprovalInstanceId(instanceId);
        offer.setStatus("SENT");
        offerMapper.updateById(offer);
    }

    /**
     * Mark offer as accepted — creates employee draft and user account.
     */
    @Transactional
    public void accept(Long offerId) {
        RcOffer offer = getOfferById(offerId);
        if (!"SENT".equals(offer.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "只有 SENT 状态的 Offer 可以接受");
        }

        offer.setStatus("ACCEPTED");
        offerMapper.updateById(offer);

        // Create employee draft (PENDING_HIRE)
        RcCandidate candidate = candidateMapper.selectById(offer.getCandidateId());
        HrEmployee employee = new HrEmployee();
        employee.setName(candidate.getName());
        employee.setIdCardEnc(candidate.getIdCardEnc());
        employee.setIdCardHash(candidate.getIdCardHash());
        employee.setEmail(candidate.getEmail());
        employee.setPhoneEnc(candidate.getPhone());
        employee.setPositionId(offer.getJobRequisitionId());
        employee.setHireDate(offer.getOnboardDate());
        employee.setStatus("PENDING_HIRE");
        employeeMapper.insert(employee);

        // Create user account
        SysUser user = new SysUser();
        user.setUsername("E" + employee.getId());
        user.setNickname(candidate.getName());
        user.setEmail(candidate.getEmail());
        user.setPhone(candidate.getPhone());
        user.setEmployeeId(employee.getId());
        user.setStatus("ACTIVE");
        // Generate random temporary password, BCrypt-hash it for admin handoff
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setPasswordChangedAt(null); // force password change on first login
        userMapper.insert(user);

        // Update candidate status
        candidateService.changeStatus(candidate.getId(), "ACCEPTED", "候选人接受Offer");

        // Check auto-close requisition
        if (offer.getJobRequisitionId() != null) {
            requisitionService.checkAndAutoClose(offer.getJobRequisitionId());
        }

        log.info("Offer {} accepted, employee {} created, user '{}', account created (please notify the new employee to change password on first login)",
                offerId, employee.getId(), user.getUsername());
    }

    /**
     * Mark offer as declined.
     */
    @Transactional
    public void decline(Long offerId) {
        RcOffer offer = getOfferById(offerId);
        if (!"SENT".equals(offer.getStatus())) {
            throw new BizException(BizCode.BAD_REQUEST, "只有 SENT 状态的 Offer 可以拒绝");
        }

        offer.setStatus("DECLINED");
        offerMapper.updateById(offer);

        candidateService.changeStatus(offer.getCandidateId(), "NEW", "候选人拒绝Offer");
    }

    /**
     * Called when approval is completed with APPROVED result.
     */
    @Transactional
    public void onApproved(String businessKey) {
        Long offerId = Long.parseLong(businessKey);
        RcOffer offer = getOfferById(offerId);
        if ("SENT".equals(offer.getStatus())) {
            // Offer was already moved to SENT on submit; approval completes it
            log.info("Offer {} approval completed (already SENT)", offerId);
        }
    }

    /**
     * Called when approval is completed with REJECTED result.
     */
    @Transactional
    public void onRejected(String businessKey) {
        Long offerId = Long.parseLong(businessKey);
        RcOffer offer = getOfferById(offerId);
        if (!"DRAFT".equals(offer.getStatus()) && !"DECLINED".equals(offer.getStatus())) {
            offer.setStatus("DECLINED");
            offerMapper.updateById(offer);
            candidateService.changeStatus(offer.getCandidateId(), "NEW", "Offer审批被拒绝");
            log.info("Offer {} rejected by approval", offerId);
        }
    }

    public RcOffer getById(Long id) {
        return getOfferById(id);
    }

    private RcOffer getOfferById(Long id) {
        RcOffer offer = offerMapper.selectById(id);
        if (offer == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Offer不存在: " + id);
        }
        return offer;
    }

    /**
     * Generate a random 12-char alphanumeric temporary password (upper+lower+digits).
     * Guaranteed to contain at least one uppercase, one lowercase, and one digit.
     */
    static String generateTempPassword() {
        final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String LOWER = "abcdefghijklmnopqrstuvwxyz";
        final String DIGITS = "0123456789";
        final String ALL = UPPER + LOWER + DIGITS;
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        // Ensure at least one char from each category
        sb.append(UPPER.charAt(random.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        for (int i = 3; i < 12; i++) {
            sb.append(ALL.charAt(random.nextInt(ALL.length())));
        }
        // Shuffle to avoid predictable positions (Fisher-Yates)
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }
}
