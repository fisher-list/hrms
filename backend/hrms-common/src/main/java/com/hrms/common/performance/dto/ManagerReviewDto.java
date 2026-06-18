package com.hrms.common.performance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * DTO for submitting manager review scores.
 */
@Data
public class ManagerReviewDto {

    @NotEmpty
    @Valid
    private List<ReviewItem> reviews;

    @Data
    public static class ReviewItem {

        @NotNull
        private Long scoringItemId;

        @NotNull
        private Integer score;

        private String comment;
    }
}
