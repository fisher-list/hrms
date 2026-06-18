package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for manual time punch creation.
 */
@Data
public class TimePunchCreateDto {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate punchDate;

    @NotBlank
    @Size(max = 5)
    private String clockIn;

    @NotBlank
    @Size(max = 5)
    private String clockOut;
}
