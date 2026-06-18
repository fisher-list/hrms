package com.hrms.common.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 绩效目标分解DTO —— 将上级目标分解为下级子目标。
 */
@Data
public class GoalDecompositionDto {

    /** 父目标ID */
    @NotNull
    private Long parentGoalId;

    /** 下级子目标列表 */
    @NotNull
    private List<SubGoalItem> subGoals;

    @Data
    public static class SubGoalItem {

        /** 子目标标题 */
        @NotBlank
        private String title;

        /** 子目标描述 */
        private String description;

        /** 权重百分比 */
        @NotNull
        private Integer weight;

        /** 目标值 */
        private BigDecimal targetValue;

        /** 下级所有者类型：DEPT / PERSON */
        @NotBlank
        private String ownerType;

        /** 下级所有者ID */
        @NotNull
        private Long ownerId;
    }
}
