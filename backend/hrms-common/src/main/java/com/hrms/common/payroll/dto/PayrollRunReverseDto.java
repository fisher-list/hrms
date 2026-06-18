package com.hrms.common.payroll.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for reversing a payroll run.
 */
@Data
public class PayrollRunReverseDto {

    @NotNull
    private Long runId;
}
