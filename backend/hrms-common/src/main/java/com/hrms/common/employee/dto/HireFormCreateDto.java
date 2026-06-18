package com.hrms.common.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for creating a hire form.
 */
@Data
public class HireFormCreateDto {

    @NotNull
    private Long positionId;

    @NotNull
    private Long deptId;

    @NotNull
    private LocalDate hireDate;

    @NotBlank
    private String accountName;

    /** JSON snapshot of the employee data. */
    @NotBlank
    private String employeeSnapshot;
}
