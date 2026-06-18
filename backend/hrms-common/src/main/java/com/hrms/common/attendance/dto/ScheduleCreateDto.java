package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for batch schedule assignment.
 */
@Data
public class ScheduleCreateDto {

    @NotEmpty
    private List<Long> employeeIds;

    @NotNull
    private Long shiftId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
