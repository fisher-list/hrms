package com.hrms.common.performance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 绩效目标树形视图VO。
 */
@Data
public class GoalCascadeVo {

    private Long id;
    private Long parentId;
    private Long cycleId;
    private String goalLevel;
    private String ownerType;
    private Long ownerId;
    private String title;
    private String description;
    private Integer weight;
    private BigDecimal targetValue;
    private BigDecimal actualValue;
    private String status;

    /** 子目标列表（递归树形结构） */
    private List<GoalCascadeVo> children;
}
