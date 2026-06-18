package com.hrms.common.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for creating a termination form.
 */
@Data
public class TerminationFormCreateDto {

    @NotNull
    private Long employeeId;

    @NotBlank
    private String terminationType;

    private String reason;

    @NotNull
    private LocalDate lastWorkingDay;
}
