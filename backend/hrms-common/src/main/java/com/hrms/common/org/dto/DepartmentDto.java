package com.hrms.common.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating / updating a department.
 */
@Data
public class DepartmentDto {

    @NotNull(message = "companyId is required")
    private Long companyId;

    private Long parentId;

    @NotBlank(message = "name is required")
    @Size(max = 128, message = "name max length 128")
    private String name;

    /** Optional. Auto-generated as DPT-{companyId}-{nanoid} when missing. */
    @Size(max = 64, message = "code max length 64")
    private String code;

    private Long headId;

    private Integer sortOrder;
}
