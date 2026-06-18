package com.hrms.common.recruit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for creating a candidate.
 */
@Data
public class CandidateCreateDto {

    @NotBlank
    private String name;

    private String phone;

    private String email;

    private String idCard;

    /**
     * @deprecated Kept for legacy frontend payloads. The value is treated as plaintext ID card input.
     */
    @Deprecated
    private String idCardEnc;

    private BigDecimal expectedSalary;

    private Long jobRequisitionId;
}
