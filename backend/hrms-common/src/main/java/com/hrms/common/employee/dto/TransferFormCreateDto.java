package com.hrms.common.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for creating a transfer form.
 */
@Data
public class TransferFormCreateDto {

    @NotNull
    private Long employeeId;

    private Long toDeptId;

    private Long toPositionId;

    @NotBlank
    private String transferType;

    private LocalDate effectiveDate;

    private String reason;
}
