package com.hrms.common.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO for creating a payroll period.
 */
@Data
public class PayrollPeriodCreateDto {

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Period month must be in YYYY-MM format")
    private String periodMonth;
}
