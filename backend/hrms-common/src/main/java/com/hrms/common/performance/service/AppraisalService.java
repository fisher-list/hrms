package com.hrms.common.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.performance.dto.GoalCreateDto;
import com.hrms.common.performance.dto.ManagerReviewDto;
import com.hrms.common.performance.dto.SelfReviewDto;
import com.hrms.common.performance.entity.*;
import com.hrms.common.performance.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Core appraisal workflow service.
 * State machine: DRAFT -> GOAL_SET -> GOAL_CONFIRMED -> SELF_REVIEWED -> MANAGER_REVIEWED -> COMPLETED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppraisalService {

    private final PfAppraisalMapper appraisalMapper;
    private final PfGoalMapper goalMapper;
    private final PfSelfReviewMapper selfReviewMapper;
    private final PfManagerReviewMapper managerReviewMapper;
    private final PfScoringItemMapper scoringItemMapper;

    /**
     * List appraisals, optionally filtered by employee or cycle.
     */
    public List<PfAppraisal> list(Long employeeId, Long cycleId) {
        LambdaQueryWrapper<PfAppraisal> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(PfAppraisal::getEmployeeId, employeeId);
        }
        if (cycleId != null) {
            wrapper.eq(PfAppraisal::getCycleId, cycleId);
        }
        wrapper.orderByDesc(PfAppraisal::getCreatedAt);
        return appraisalMapper.selectList(wrapper);
    }

    /**
     * Get appraisal by ID with goals, self reviews, and manager reviews.
     */
    public PfAppraisal getById(Long id) {
        return appraisalMapper.selectById(id);
    }

    /**
     * Get goals for an appraisal.
     */
    public List<PfGoal> getGoals(Long appraisalId) {
        return goalMapper.selectList(
                new LambdaQueryWrapper<PfGoal>()
                        .eq(PfGoal::getAppraisalId, appraisalId)
                        .orderByAsc(PfGoal::getSortOrder));
    }

    /**
     * Get self reviews for an appraisal.
     */
    public List<PfSelfReview> getSelfReviews(Long appraisalId) {
        return selfReviewMapper.selectList(
                new LambdaQueryWrapper<PfSelfReview>()
                        .eq(PfSelfReview::getAppraisalId, appraisalId));
    }

    /**
     * Get manager reviews for an appraisal.
     */
    public List<PfManagerReview> getManagerReviews(Long appraisalId) {
        return managerReviewMapper.selectList(
                new LambdaQueryWrapper<PfManagerReview>()
                        .eq(PfManagerReview::getAppraisalId, appraisalId));
    }

    /**
     * Set goals on an appraisal. Transitions DRAFT -> GOAL_SET.
     */
    @Transactional
    public List<PfGoal> setGoals(Long appraisalId, GoalCreateDto dto) {
        PfAppraisal appraisal = requireAppraisal(appraisalId);
        assertState(appraisal, "DRAFT");

        List<PfGoal> goals = new ArrayList<>();
        for (int i = 0; i < dto.getGoals().size(); i++) {
            GoalCreateDto.GoalItem item = dto.getGoals().get(i);
            PfGoal goal = new PfGoal();
            goal.setAppraisalId(appraisalId);
            goal.setDescription(item.getDescription());
            goal.setWeight(item.getWeight());
            goal.setStatus("DRAFT");
            goal.setSortOrder(i + 1);
            goalMapper.insert(goal);
            goals.add(goal);
        }

        appraisal.setStatus("GOAL_SET");
        appraisalMapper.updateById(appraisal);
        return goals;
    }

    /**
     * Confirm goals. Transitions GOAL_SET -> GOAL_CONFIRMED.
     */
    @Transactional
    public void confirmGoals(Long appraisalId) {
        PfAppraisal appraisal = requireAppraisal(appraisalId);
        assertState(appraisal, "GOAL_SET");

        List<PfGoal> goals = getGoals(appraisalId);
        for (PfGoal goal : goals) {
            goal.setStatus("CONFIRMED");
            goalMapper.updateById(goal);
        }

        appraisal.setStatus("GOAL_CONFIRMED");
        appraisalMapper.updateById(appraisal);
    }

    /**
     * Submit self-review. Transitions GOAL_CONFIRMED -> SELF_REVIEWED.
     * Computes self_score = sum(item.score * item.weight / 100).
     */
    @Transactional
    public void submitSelfReview(Long appraisalId, SelfReviewDto dto) {
        PfAppraisal appraisal = requireAppraisal(appraisalId);
        assertState(appraisal, "GOAL_CONFIRMED");

        List<PfScoringItem> scoringItems = scoringItemMapper.selectList(
                new LambdaQueryWrapper<PfScoringItem>()
                        .eq(PfScoringItem::getTemplateId, appraisal.getTemplateId()));

        BigDecimal selfScore = BigDecimal.ZERO;
        for (SelfReviewDto.ReviewItem reviewItem : dto.getReviews()) {
            PfScoringItem scoringItem = scoringItems.stream()
                    .filter(si -> si.getId().equals(reviewItem.getScoringItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(BizCode.BAD_REQUEST,
                            "Scoring item not found: " + reviewItem.getScoringItemId()));

            if (reviewItem.getScore() < scoringItem.getMinScore()
                    || reviewItem.getScore() > scoringItem.getMaxScore()) {
                throw new BizException(BizCode.BAD_REQUEST,
                        "Score " + reviewItem.getScore() + " out of range ["
                                + scoringItem.getMinScore() + ", " + scoringItem.getMaxScore() + "]");
            }

            PfSelfReview selfReview = new PfSelfReview();
            selfReview.setAppraisalId(appraisalId);
            selfReview.setScoringItemId(reviewItem.getScoringItemId());
            selfReview.setScore(reviewItem.getScore());
            selfReview.setComment(reviewItem.getComment());
            selfReviewMapper.insert(selfReview);

            selfScore = selfScore.add(
                    BigDecimal.valueOf(reviewItem.getScore())
                            .multiply(BigDecimal.valueOf(scoringItem.getWeight()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        appraisal.setSelfScore(selfScore);
        appraisal.setStatus("SELF_REVIEWED");
        appraisalMapper.updateById(appraisal);
    }

    /**
     * Submit manager review. Transitions SELF_REVIEWED -> MANAGER_REVIEWED.
     * Computes manager_score = sum(item.score * item.weight / 100).
     */
    @Transactional
    public void submitManagerReview(Long appraisalId, ManagerReviewDto dto) {
        PfAppraisal appraisal = requireAppraisal(appraisalId);
        assertState(appraisal, "SELF_REVIEWED");

        List<PfScoringItem> scoringItems = scoringItemMapper.selectList(
                new LambdaQueryWrapper<PfScoringItem>()
                        .eq(PfScoringItem::getTemplateId, appraisal.getTemplateId()));

        BigDecimal managerScore = BigDecimal.ZERO;
        for (ManagerReviewDto.ReviewItem reviewItem : dto.getReviews()) {
            PfScoringItem scoringItem = scoringItems.stream()
                    .filter(si -> si.getId().equals(reviewItem.getScoringItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(BizCode.BAD_REQUEST,
                            "Scoring item not found: " + reviewItem.getScoringItemId()));

            if (reviewItem.getScore() < scoringItem.getMinScore()
                    || reviewItem.getScore() > scoringItem.getMaxScore()) {
                throw new BizException(BizCode.BAD_REQUEST,
                        "Score " + reviewItem.getScore() + " out of range ["
                                + scoringItem.getMinScore() + ", " + scoringItem.getMaxScore() + "]");
            }

            PfManagerReview managerReview = new PfManagerReview();
            managerReview.setAppraisalId(appraisalId);
            managerReview.setScoringItemId(reviewItem.getScoringItemId());
            managerReview.setScore(reviewItem.getScore());
            managerReview.setComment(reviewItem.getComment());
            managerReviewMapper.insert(managerReview);

            managerScore = managerScore.add(
                    BigDecimal.valueOf(reviewItem.getScore())
                            .multiply(BigDecimal.valueOf(scoringItem.getWeight()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        appraisal.setManagerScore(managerScore);
        appraisal.setStatus("MANAGER_REVIEWED");
        appraisalMapper.updateById(appraisal);
    }

    /**
     * Finalize appraisal. Transitions MANAGER_REVIEWED -> COMPLETED.
     * Computes final_score = self_score * 0.3 + manager_score * 0.7.
     */
    @Transactional
    public PfAppraisal finalize(Long appraisalId) {
        PfAppraisal appraisal = requireAppraisal(appraisalId);
        assertState(appraisal, "MANAGER_REVIEWED");

        BigDecimal finalScore = appraisal.getSelfScore()
                .multiply(new BigDecimal("0.30"))
                .add(appraisal.getManagerScore()
                        .multiply(new BigDecimal("0.70")))
                .setScale(2, RoundingMode.HALF_UP);

        appraisal.setFinalScore(finalScore);
        appraisal.setStatus("COMPLETED");
        appraisalMapper.updateById(appraisal);
        return appraisal;
    }

    private PfAppraisal requireAppraisal(Long id) {
        PfAppraisal appraisal = appraisalMapper.selectById(id);
        if (appraisal == null) {
            throw new BizException(BizCode.APPRAISAL_NOT_FOUND, "Appraisal not found: " + id);
        }
        return appraisal;
    }

    private void assertState(PfAppraisal appraisal, String expected) {
        if (!expected.equals(appraisal.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE,
                    "Expected status " + expected + ", got: " + appraisal.getStatus());
        }
    }
}
