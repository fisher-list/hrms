package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for triggering attendance summary generation.
 */
@Data
public class SummaryGenerateDto {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
