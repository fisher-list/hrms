package com.hrms.common.recruit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for creating a job requisition.
 */
@Data
public class RequisitionCreateDto {

    private Long positionId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Min(1)
    private Integer headcount;

    private LocalDate deadline;
}
