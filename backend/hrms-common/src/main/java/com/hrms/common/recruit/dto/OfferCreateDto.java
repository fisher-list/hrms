package com.hrms.common.recruit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating an offer.
 */
@Data
public class OfferCreateDto {

    @NotNull
    private Long candidateId;

    private Long jobRequisitionId;

    @NotNull
    private BigDecimal salary;

    @NotNull
    private LocalDate onboardDate;
}
