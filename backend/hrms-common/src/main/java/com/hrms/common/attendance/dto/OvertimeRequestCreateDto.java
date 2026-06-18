package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating an overtime request.
 */
@Data
public class OvertimeRequestCreateDto {

    @NotNull
    private LocalDate overtimeDate;

    @NotNull
    private BigDecimal hours;

    @Size(max = 500)
    private String reason;
}
