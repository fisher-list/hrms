package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for triggering a year-end settlement.
 */
@Data
public class SettlementCreateDto {

    @NotNull(message = "year is required")
    private Integer year;
}
