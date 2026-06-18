package com.hrms.common.performance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * DTO for setting goals on an appraisal.
 */
@Data
public class GoalCreateDto {

    @NotEmpty
    @Valid
    private List<GoalItem> goals;

    @Data
    public static class GoalItem {

        @NotNull
        private String description;

        @NotNull
        private Integer weight;
    }
}
