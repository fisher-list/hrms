package com.hrms.common.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.performance.dto.GoalCreateDto;
import com.hrms.common.performance.dto.ManagerReviewDto;
import com.hrms.common.performance.dto.SelfReviewDto;
import com.hrms.common.performance.entity.*;
import com.hrms.common.performance.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppraisalService (S13+S14).
 */
@ExtendWith(MockitoExtension.class)
class AppraisalServiceTest {

    @Mock
    private PfAppraisalMapper appraisalMapper;
    @Mock
    private PfGoalMapper goalMapper;
    @Mock
    private PfSelfReviewMapper selfReviewMapper;
    @Mock
    private PfManagerReviewMapper managerReviewMapper;
    @Mock
    private PfScoringItemMapper scoringItemMapper;

    private AppraisalService appraisalService;

    private PfAppraisal draftAppraisal;
    private List<PfScoringItem> scoringItems;

    @BeforeEach
    void setUp() {
        appraisalService = new AppraisalService(
                appraisalMapper, goalMapper, selfReviewMapper, managerReviewMapper, scoringItemMapper);

        draftAppraisal = new PfAppraisal();
        draftAppraisal.setId(1001L);
        draftAppraisal.setCycleId(501L);
        draftAppraisal.setTemplateId(901L);
        draftAppraisal.setEmployeeId(50001L);
        draftAppraisal.setStatus("DRAFT");

        scoringItems = new ArrayList<>();
        PfScoringItem item1 = new PfScoringItem();
        item1.setId(911L);
        item1.setTemplateId(901L);
        item1.setName("工作质量");
        item1.setWeight(40);
        item1.setMinScore(1);
        item1.setMaxScore(5);
        scoringItems.add(item1);

        PfScoringItem item2 = new PfScoringItem();
        item2.setId(912L);
        item2.setTemplateId(901L);
        item2.setName("团队协作");
        item2.setWeight(30);
        item2.setMinScore(1);
        item2.setMaxScore(5);
        scoringItems.add(item2);

        PfScoringItem item3 = new PfScoringItem();
        item3.setId(913L);
        item3.setTemplateId(901L);
        item3.setName("创新能力");
        item3.setWeight(30);
        item3.setMinScore(1);
        item3.setMaxScore(5);
        scoringItems.add(item3);
    }

    @Test
    @DisplayName("Test 1: setGoals -> status GOAL_SET")
    void testSetGoals() {
        when(appraisalMapper.selectById(1001L)).thenReturn(draftAppraisal);
        when(goalMapper.insert(any(PfGoal.class))).thenReturn(1);
        when(appraisalMapper.updateById(any(PfAppraisal.class))).thenReturn(1);

        GoalCreateDto dto = new GoalCreateDto();
        GoalCreateDto.GoalItem g1 = new GoalCreateDto.GoalItem();
        g1.setDescription("完成项目A");
        g1.setWeight(60);
        GoalCreateDto.GoalItem g2 = new GoalCreateDto.GoalItem();
        g2.setDescription("学习新技术");
        g2.setWeight(40);
        dto.setGoals(List.of(g1, g2));

        List<PfGoal> goals = appraisalService.setGoals(1001L, dto);

        assertEquals(2, goals.size());
        verify(goalMapper, times(2)).insert(any(PfGoal.class));

        ArgumentCaptor<PfAppraisal> captor = ArgumentCaptor.forClass(PfAppraisal.class);
        verify(appraisalMapper).updateById(captor.capture());
        assertEquals("GOAL_SET", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Test 2: confirmGoals -> status GOAL_CONFIRMED")
    void testConfirmGoals() {
        PfAppraisal goalSetAppraisal = new PfAppraisal();
        goalSetAppraisal.setId(1001L);
        goalSetAppraisal.setTemplateId(901L);
        goalSetAppraisal.setStatus("GOAL_SET");

        when(appraisalMapper.selectById(1001L)).thenReturn(goalSetAppraisal);
        when(appraisalMapper.updateById(any(PfAppraisal.class))).thenReturn(1);

        PfGoal goal1 = new PfGoal();
        goal1.setId(2001L);
        goal1.setAppraisalId(1001L);
        goal1.setStatus("DRAFT");
        PfGoal goal2 = new PfGoal();
        goal2.setId(2002L);
        goal2.setAppraisalId(1001L);
        goal2.setStatus("DRAFT");

        when(goalMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(goal1, goal2));
        when(goalMapper.updateById(any(PfGoal.class))).thenReturn(1);

        appraisalService.confirmGoals(1001L);

        verify(goalMapper, times(2)).updateById(any(PfGoal.class));
        assertEquals("CONFIRMED", goal1.getStatus());
        assertEquals("CONFIRMED", goal2.getStatus());

        ArgumentCaptor<PfAppraisal> captor = ArgumentCaptor.forClass(PfAppraisal.class);
        verify(appraisalMapper).updateById(captor.capture());
        assertEquals("GOAL_CONFIRMED", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Test 3: submitSelfReview -> status SELF_REVIEWED, self_score computed")
    void testSubmitSelfReview() {
        PfAppraisal confirmedAppraisal = new PfAppraisal();
        confirmedAppraisal.setId(1001L);
        confirmedAppraisal.setTemplateId(901L);
        confirmedAppraisal.setStatus("GOAL_CONFIRMED");

        when(appraisalMapper.selectById(1001L)).thenReturn(confirmedAppraisal);
        when(scoringItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(scoringItems);
        when(selfReviewMapper.insert(any(PfSelfReview.class))).thenReturn(1);
        when(appraisalMapper.updateById(any(PfAppraisal.class))).thenReturn(1);

        SelfReviewDto dto = new SelfReviewDto();
        SelfReviewDto.ReviewItem r1 = new SelfReviewDto.ReviewItem();
        r1.setScoringItemId(911L);
        r1.setScore(4);
        r1.setComment("Good");
        SelfReviewDto.ReviewItem r2 = new SelfReviewDto.ReviewItem();
        r2.setScoringItemId(912L);
        r2.setScore(5);
        r2.setComment("Excellent");
        SelfReviewDto.ReviewItem r3 = new SelfReviewDto.ReviewItem();
        r3.setScoringItemId(913L);
        r3.setScore(3);
        r3.setComment("OK");
        dto.setReviews(List.of(r1, r2, r3));

        appraisalService.submitSelfReview(1001L, dto);

        verify(selfReviewMapper, times(3)).insert(any(PfSelfReview.class));

        ArgumentCaptor<PfAppraisal> captor = ArgumentCaptor.forClass(PfAppraisal.class);
        verify(appraisalMapper).updateById(captor.capture());
        PfAppraisal updated = captor.getValue();
        assertEquals("SELF_REVIEWED", updated.getStatus());
        // self_score = 4*40/100 + 5*30/100 + 3*30/100 = 1.60 + 1.50 + 0.90 = 4.00
        assertEquals(0, new BigDecimal("4.00").compareTo(updated.getSelfScore()));
    }

    @Test
    @DisplayName("Test 4: submitManagerReview -> status MANAGER_REVIEWED, manager_score computed")
    void testSubmitManagerReview() {
        PfAppraisal selfReviewedAppraisal = new PfAppraisal();
        selfReviewedAppraisal.setId(1001L);
        selfReviewedAppraisal.setTemplateId(901L);
        selfReviewedAppraisal.setStatus("SELF_REVIEWED");
        selfReviewedAppraisal.setSelfScore(new BigDecimal("4.00"));

        when(appraisalMapper.selectById(1001L)).thenReturn(selfReviewedAppraisal);
        when(scoringItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(scoringItems);
        when(managerReviewMapper.insert(any(PfManagerReview.class))).thenReturn(1);
        when(appraisalMapper.updateById(any(PfAppraisal.class))).thenReturn(1);

        ManagerReviewDto dto = new ManagerReviewDto();
        ManagerReviewDto.ReviewItem r1 = new ManagerReviewDto.ReviewItem();
        r1.setScoringItemId(911L);
        r1.setScore(3);
        r1.setComment("Average");
        ManagerReviewDto.ReviewItem r2 = new ManagerReviewDto.ReviewItem();
        r2.setScoringItemId(912L);
        r2.setScore(4);
        r2.setComment("Good");
        ManagerReviewDto.ReviewItem r3 = new ManagerReviewDto.ReviewItem();
        r3.setScoringItemId(913L);
        r3.setScore(2);
        r3.setComment("Needs improvement");
        dto.setReviews(List.of(r1, r2, r3));

        appraisalService.submitManagerReview(1001L, dto);

        verify(managerReviewMapper, times(3)).insert(any(PfManagerReview.class));

        ArgumentCaptor<PfAppraisal> captor = ArgumentCaptor.forClass(PfAppraisal.class);
        verify(appraisalMapper).updateById(captor.capture());
        PfAppraisal updated = captor.getValue();
        assertEquals("MANAGER_REVIEWED", updated.getStatus());
        // manager_score = 3*40/100 + 4*30/100 + 2*30/100 = 1.20 + 1.20 + 0.60 = 3.00
        assertEquals(0, new BigDecimal("3.00").compareTo(updated.getManagerScore()));
    }

    @Test
    @DisplayName("Test 5: finalize -> status COMPLETED, final_score = self*0.3 + manager*0.7")
    void testFinalize() {
        PfAppraisal managerReviewedAppraisal = new PfAppraisal();
        managerReviewedAppraisal.setId(1001L);
        managerReviewedAppraisal.setTemplateId(901L);
        managerReviewedAppraisal.setStatus("MANAGER_REVIEWED");
        managerReviewedAppraisal.setSelfScore(new BigDecimal("4.00"));
        managerReviewedAppraisal.setManagerScore(new BigDecimal("3.00"));

        when(appraisalMapper.selectById(1001L)).thenReturn(managerReviewedAppraisal);
        when(appraisalMapper.updateById(any(PfAppraisal.class))).thenReturn(1);

        PfAppraisal result = appraisalService.finalize(1001L);

        assertEquals("COMPLETED", result.getStatus());
        // final_score = 4.00 * 0.3 + 3.00 * 0.7 = 1.20 + 2.10 = 3.30
        assertEquals(0, new BigDecimal("3.30").compareTo(result.getFinalScore()));
    }
}
