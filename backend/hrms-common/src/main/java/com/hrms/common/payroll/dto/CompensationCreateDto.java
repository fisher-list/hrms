package com.hrms.common.payroll.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a compensation master record.
 */
@Data
public class CompensationCreateDto {

    @NotNull
    private Long employeeId;

    private BigDecimal baseSalary;

    private BigDecimal positionSalary;

    private BigDecimal performanceBase;

    private BigDecimal allowance;

    @NotNull
    private LocalDate effectiveDate;
}
