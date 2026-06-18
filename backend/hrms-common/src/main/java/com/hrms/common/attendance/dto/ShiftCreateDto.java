package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating or updating a shift template.
 */
@Data
public class ShiftCreateDto {

    @NotBlank
    @Size(max = 50)
    private String shiftName;

    @NotBlank
    @Size(max = 5)
    private String startTime;

    @NotBlank
    @Size(max = 5)
    private String endTime;

    private Integer flexMinutes = 0;

    private Boolean isNightShift = false;

    @Size(max = 200)
    private String remark;
}
