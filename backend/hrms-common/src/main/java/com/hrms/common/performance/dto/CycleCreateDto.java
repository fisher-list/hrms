package com.hrms.common.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating a performance cycle.
 */
@Data
public class CycleCreateDto {

    @NotBlank
    private String name;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    /** ALL or DEPT */
    private String scopeType;

    /** Comma-separated department IDs, required when scopeType=DEPT */
    private String scopeDepts;
}
