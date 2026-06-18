package com.hrms.common.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.recruit.dto.EvaluationCreateDto;
import com.hrms.common.recruit.entity.RcEvaluation;
import com.hrms.common.recruit.mapper.RcEvaluationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Interview evaluation — append-only, no update allowed.
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final RcEvaluationMapper evaluationMapper;

    @Transactional
    public RcEvaluation create(EvaluationCreateDto dto) {
        RcEvaluation entity = new RcEvaluation();
        entity.setInterviewId(dto.getInterviewId());
        entity.setScore(dto.getScore());
        entity.setPassFlag(dto.getPassFlag());
        entity.setComment(dto.getComment());
        entity.setEvaluatorUserId(dto.getEvaluatorUserId());
        evaluationMapper.insert(entity);
        return entity;
    }

    public List<RcEvaluation> listByInterview(Long interviewId) {
        return evaluationMapper.selectList(
                new LambdaQueryWrapper<RcEvaluation>()
                        .eq(RcEvaluation::getInterviewId, interviewId)
                        .orderByDesc(RcEvaluation::getCreatedAt));
    }
}
