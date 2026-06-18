package com.hrms.common.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for creating / updating a job.
 */
@Data
public class JobDto {

    @NotBlank(message = "code is required")
    @Size(max = 64, message = "code max length 64")
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 128, message = "name max length 128")
    private String name;

    @NotBlank(message = "sequence is required")
    @Size(max = 16, message = "sequence max length 16")
    private String sequence;

    @NotNull(message = "grade is required")
    private Integer grade;

    private BigDecimal minSalary;

    private BigDecimal maxSalary;
}
