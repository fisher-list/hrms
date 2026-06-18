package com.hrms.common.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.recruit.dto.CandidateCreateDto;
import com.hrms.common.recruit.entity.RcCandidate;
import com.hrms.common.recruit.entity.RcCandidateStatusLog;
import com.hrms.common.recruit.mapper.RcCandidateMapper;
import com.hrms.common.recruit.mapper.RcCandidateStatusLogMapper;
import com.hrms.common.util.AesUtil;
import com.hrms.common.util.SensitiveHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Candidate CRUD with status tracking and duplicate ID card check.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final RcCandidateMapper candidateMapper;
    private final RcCandidateStatusLogMapper statusLogMapper;
    private final HrEmployeeMapper employeeMapper;

    @Transactional
    public RcCandidate create(CandidateCreateDto dto) {
        // Check duplicate ID card against hr_employee
        String idCard = resolveIdCard(dto);
        String idCardHash = SensitiveHashUtil.idCardHash(idCard);
        if (idCardHash != null) {
            Long count = employeeMapper.selectCount(
                    new LambdaQueryWrapper<HrEmployee>()
                            .eq(HrEmployee::getIdCardHash, idCardHash));
            if (count > 0) {
                throw new BizException(BizCode.CANDIDATE_DUPLICATE_ID_CARD,
                        "该身份证已存在员工档案，是否走重新雇佣流程");
            }
        }

        RcCandidate entity = new RcCandidate();
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setIdCardEnc(AesUtil.encrypt(idCard));
        entity.setIdCardHash(idCardHash);
        entity.setExpectedSalary(dto.getExpectedSalary());
        entity.setJobRequisitionId(dto.getJobRequisitionId());
        entity.setCurrentStatus("NEW");
        candidateMapper.insert(entity);
        return entity;
    }

    public RcCandidate getById(Long id) {
        RcCandidate candidate = candidateMapper.selectById(id);
        if (candidate == null) {
            throw new BizException(BizCode.BAD_REQUEST, "候选人不存在: " + id);
        }
        return candidate;
    }

    public IPage<RcCandidate> list(Page<RcCandidate> page) {
        return candidateMapper.selectPage(page,
                new LambdaQueryWrapper<RcCandidate>()
                        .orderByDesc(RcCandidate::getCreatedAt));
    }

    /**
     * Transition candidate status and log the change.
     */
    @Transactional
    public void changeStatus(Long candidateId, String newStatus, String remark) {
        RcCandidate candidate = getById(candidateId);
        String oldStatus = candidate.getCurrentStatus();
        if (oldStatus.equals(newStatus)) {
            return;
        }
        candidate.setCurrentStatus(newStatus);
        candidateMapper.updateById(candidate);

        // Log status change
        RcCandidateStatusLog statusLog = new RcCandidateStatusLog();
        statusLog.setCandidateId(candidateId);
        statusLog.setOldStatus(oldStatus);
        statusLog.setNewStatus(newStatus);
        statusLog.setRemark(remark);
        statusLogMapper.insert(statusLog);
        log.info("Candidate {} status changed: {} -> {}", candidateId, oldStatus, newStatus);
    }

    private String resolveIdCard(CandidateCreateDto dto) {
        if (dto.getIdCard() != null && !dto.getIdCard().isBlank()) {
            return dto.getIdCard();
        }
        if (dto.getIdCardEnc() != null && !dto.getIdCardEnc().isBlank()) {
            return dto.getIdCardEnc();
        }
        return null;
    }
}
