package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a leave request.
 */
@Data
public class LeaveRequestCreateDto {

    @NotNull
    private Long leaveTypeId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    /** Total leave days, supports 0.5 increments */
    @NotNull
    private BigDecimal days;

    @Size(max = 500)
    private String reason;
}
