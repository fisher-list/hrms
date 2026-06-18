package com.hrms.common.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出差申请 DTO。
 */
@Data
public class BusinessTripCreateDto {

    /** 出差目的地 */
    @NotBlank
    @Size(max = 200)
    private String destination;

    /** 出差事由 */
    @NotBlank
    @Size(max = 1000)
    private String reason;

    /** 出差开始日期 */
    @NotNull
    private LocalDate startDate;

    /** 出差结束日期 */
    @NotNull
    private LocalDate endDate;

    /** 出差天数 */
    @NotNull
    private BigDecimal days;
}
