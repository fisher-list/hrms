package com.hrms.common.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating / updating a position.
 */
@Data
public class PositionDto {

    @NotNull(message = "deptId is required")
    private Long deptId;

    @NotNull(message = "jobId is required")
    private Long jobId;

    @NotBlank(message = "name is required")
    @Size(max = 128, message = "name max length 128")
    private String name;

    @Size(max = 64, message = "code max length 64")
    private String code;

    private Integer headcount;
}
