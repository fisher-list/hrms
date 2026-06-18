package com.hrms.common.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a handover item.
 */
@Data
public class HandoverItemCreateDto {

    @NotNull
    private Long formId;

    @NotBlank
    private String category;

    @NotBlank
    private String description;

    /** 交接接收人 */
    private String handoverTo;

    private String remark;
}
