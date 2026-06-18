package com.hrms.common.payroll.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a payroll run.
 */
@Data
public class PayrollRunCreateDto {

    @NotNull
    private Long periodId;
}
