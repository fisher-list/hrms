package com.hrms.common.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.performance.dto.GoalCascadeVo;
import com.hrms.common.performance.dto.GoalDecompositionDto;
import com.hrms.common.performance.dto.GoalPlanCreateDto;
import com.hrms.common.performance.entity.PfGoalPlan;
import com.hrms.common.performance.mapper.PfGoalPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 绩效目标分解服务 —— 支持组织→部门→个人目标层层分解对齐。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalDecompositionService {

    private final PfGoalPlanMapper goalPlanMapper;

    /**
     * 创建顶层目标（组织级/部门级/个人级）。
     */
    @Transactional
    public List<PfGoalPlan> createGoals(GoalPlanCreateDto dto) {
        List<PfGoalPlan> result = new ArrayList<>();
        for (int i = 0; i < dto.getGoals().size(); i++) {
            GoalPlanCreateDto.GoalPlanItem item = dto.getGoals().get(i);
            PfGoalPlan plan = new PfGoalPlan();
            plan.setCycleId(dto.getCycleId());
            plan.setGoalLevel(dto.getGoalLevel());
            plan.setOwnerType(dto.getOwnerType());
            plan.setOwnerId(dto.getOwnerId());
            plan.setTitle(item.getTitle());
            plan.setDescription(item.getDescription());
            plan.setWeight(item.getWeight());
            plan.setTargetValue(item.getTargetValue());
            plan.setStatus("DRAFT");
            plan.setSortOrder(i + 1);
            goalPlanMapper.insert(plan);
            result.add(plan);
        }
        log.info("创建{}级目标: 周期={}, 所有者={}, 数量={}", dto.getGoalLevel(), dto.getCycleId(), dto.getOwnerId(), result.size());
        return result;
    }

    /**
     * 将上级目标分解为下级子目标。
     * <p>
     * 校验规则：
     * 1. 父目标必须存在
     * 2. 子目标权重之和必须等于父目标权重
     * </p>
     */
    @Transactional
    public List<PfGoalPlan> decompose(GoalDecompositionDto dto) {
        // 查找父目标
        PfGoalPlan parent = goalPlanMapper.selectById(dto.getParentGoalId());
        if (parent == null) {
            throw new BizException(BizCode.APPRAISAL_NOT_FOUND, "父目标不存在: " + dto.getParentGoalId());
        }

        // 校验子目标权重之和
        int totalWeight = dto.getSubGoals().stream()
                .mapToInt(GoalDecompositionDto.SubGoalItem::getWeight)
                .sum();
        if (totalWeight != parent.getWeight()) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "子目标权重之和(" + totalWeight + ")必须等于父目标权重(" + parent.getWeight() + ")");
        }

        // 确定下级层级
        String childLevel = determineChildLevel(parent.getGoalLevel());

        List<PfGoalPlan> children = new ArrayList<>();
        for (int i = 0; i < dto.getSubGoals().size(); i++) {
            GoalDecompositionDto.SubGoalItem item = dto.getSubGoals().get(i);
            PfGoalPlan child = new PfGoalPlan();
            child.setCycleId(parent.getCycleId());
            child.setParentId(parent.getId());
            child.setGoalLevel(childLevel);
            child.setOwnerType(item.getOwnerType());
            child.setOwnerId(item.getOwnerId());
            child.setTitle(item.getTitle());
            child.setDescription(item.getDescription());
            child.setWeight(item.getWeight());
            child.setTargetValue(item.getTargetValue());
            child.setStatus("DRAFT");
            child.setSortOrder(i + 1);
            goalPlanMapper.insert(child);
            children.add(child);
        }

        log.info("目标分解: 父目标={}, 分解为{}个子目标", parent.getId(), children.size());
        return children;
    }

    /**
     * 确认目标。
     */
    @Transactional
    public void confirm(Long goalId) {
        PfGoalPlan plan = goalPlanMapper.selectById(goalId);
        if (plan == null) {
            throw new BizException(BizCode.APPRAISAL_NOT_FOUND, "目标不存在: " + goalId);
        }
        if (!"DRAFT".equals(plan.getStatus())) {
            throw new BizException(BizCode.APPRAISAL_INVALID_STATE, "当前状态不允许确认: " + plan.getStatus());
        }
        plan.setStatus("CONFIRMED");
        goalPlanMapper.updateById(plan);
    }

    /**
     * 获取指定周期下的目标树（按层级和父ID构建树形结构）。
     */
    public List<GoalCascadeVo> getGoalTree(Long cycleId) {
        List<PfGoalPlan> all = goalPlanMapper.selectList(
                new LambdaQueryWrapper<PfGoalPlan>()
                        .eq(PfGoalPlan::getCycleId, cycleId)
                        .orderByAsc(PfGoalPlan::getGoalLevel)
                        .orderByAsc(PfGoalPlan::getSortOrder));

        // 转换为VO
        List<GoalCascadeVo> voList = all.stream().map(this::toVo).collect(Collectors.toList());

        // 构建树形结构
        Map<Long, GoalCascadeVo> idMap = voList.stream()
                .collect(Collectors.toMap(GoalCascadeVo::getId, v -> v));

        List<GoalCascadeVo> roots = new ArrayList<>();
        for (GoalCascadeVo vo : voList) {
            if (vo.getParentId() == null) {
                roots.add(vo);
            } else {
                GoalCascadeVo parent = idMap.get(vo.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }
        return roots;
    }

    /**
     * 获取指定目标及其所有下级子目标（平铺列表）。
     */
    public List<PfGoalPlan> getDescendants(Long goalId) {
        List<PfGoalPlan> result = new ArrayList<>();
        collectDescendants(goalId, result);
        return result;
    }

    /**
     * 递归收集所有下级目标。
     */
    private void collectDescendants(Long parentId, List<PfGoalPlan> result) {
        List<PfGoalPlan> children = goalPlanMapper.selectList(
                new LambdaQueryWrapper<PfGoalPlan>()
                        .eq(PfGoalPlan::getParentId, parentId)
                        .orderByAsc(PfGoalPlan::getSortOrder));
        for (PfGoalPlan child : children) {
            result.add(child);
            collectDescendants(child.getId(), result);
        }
    }

    /**
     * 更新目标实际完成值。
     */
    @Transactional
    public void updateActualValue(Long goalId, java.math.BigDecimal actualValue) {
        PfGoalPlan plan = goalPlanMapper.selectById(goalId);
        if (plan == null) {
            throw new BizException(BizCode.APPRAISAL_NOT_FOUND, "目标不存在: " + goalId);
        }
        plan.setActualValue(actualValue);
        plan.setStatus("COMPLETED");
        goalPlanMapper.updateById(plan);
    }

    /**
     * 根据父目标层级确定子目标层级。
     */
    private String determineChildLevel(String parentLevel) {
        return switch (parentLevel) {
            case "ORG" -> "DEPT";
            case "DEPT" -> "PERSON";
            default -> throw new BizException(BizCode.BAD_REQUEST, "个人目标不能再分解");
        };
    }

    private GoalCascadeVo toVo(PfGoalPlan plan) {
        GoalCascadeVo vo = new GoalCascadeVo();
        vo.setId(plan.getId());
        vo.setParentId(plan.getParentId());
        vo.setCycleId(plan.getCycleId());
        vo.setGoalLevel(plan.getGoalLevel());
        vo.setOwnerType(plan.getOwnerType());
        vo.setOwnerId(plan.getOwnerId());
        vo.setTitle(plan.getTitle());
        vo.setDescription(plan.getDescription());
        vo.setWeight(plan.getWeight());
        vo.setTargetValue(plan.getTargetValue());
        vo.setActualValue(plan.getActualValue());
        vo.setStatus(plan.getStatus());
        return vo;
    }
}
