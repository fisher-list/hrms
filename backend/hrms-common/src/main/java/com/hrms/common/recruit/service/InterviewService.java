package com.hrms.common.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.recruit.dto.InterviewCreateDto;
import com.hrms.common.recruit.entity.RcInterview;
import com.hrms.common.recruit.mapper.RcInterviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Interview scheduling and result recording.
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final RcInterviewMapper interviewMapper;

    @Transactional
    public RcInterview create(InterviewCreateDto dto) {
        RcInterview entity = new RcInterview();
        entity.setCandidateId(dto.getCandidateId());
        entity.setRoundNo(dto.getRoundNo());
        entity.setInterviewerUserId(dto.getInterviewerUserId());
        entity.setInterviewTime(dto.getInterviewTime());
        entity.setLocation(dto.getLocation());
        entity.setResult("PENDING");
        interviewMapper.insert(entity);
        return entity;
    }

    public List<RcInterview> listByCandidate(Long candidateId) {
        return interviewMapper.selectList(
                new LambdaQueryWrapper<RcInterview>()
                        .eq(RcInterview::getCandidateId, candidateId)
                        .orderByAsc(RcInterview::getRoundNo));
    }

    /**
     * Record interview result (PASS or FAIL).
     */
    @Transactional
    public void recordResult(Long interviewId, String result) {
        if (!"PASS".equals(result) && !"FAIL".equals(result)) {
            throw new BizException(BizCode.BAD_REQUEST, "面试结果只能是 PASS 或 FAIL");
        }
        RcInterview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new BizException(BizCode.BAD_REQUEST, "面试安排不存在: " + interviewId);
        }
        interview.setResult(result);
        interviewMapper.updateById(interview);
    }
}
