package com.hrms.common.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建组织级绩效目标DTO。
 */
@Data
public class GoalPlanCreateDto {

    /** 关联绩效周期ID */
    @NotNull
    private Long cycleId;

    /** 目标层级：ORG / DEPT / PERSON */
    @NotBlank
    private String goalLevel;

    /** 所有者类型：ORG / DEPT / PERSON */
    @NotBlank
    private String ownerType;

    /** 所有者ID */
    @NotNull
    private Long ownerId;

    /** 目标列表 */
    @NotNull
    private List<GoalPlanItem> goals;

    @Data
    public static class GoalPlanItem {

        @NotBlank
        private String title;

        private String description;

        @NotNull
        private Integer weight;

        private BigDecimal targetValue;
    }
}
