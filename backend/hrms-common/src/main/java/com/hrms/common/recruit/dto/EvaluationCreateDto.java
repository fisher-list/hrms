package com.hrms.common.recruit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating an interview evaluation (append-only).
 */
@Data
public class EvaluationCreateDto {

    @NotNull
    private Long interviewId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    @NotNull
    private Boolean passFlag;

    private String comment;

    private Long evaluatorUserId;
}
