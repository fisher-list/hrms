package com.hrms.common.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating / updating a role.
 */
@Data
public class RoleDto {

    @NotBlank(message = "code is required")
    @Size(max = 64, message = "code max length 64")
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 64, message = "name max length 64")
    private String name;

    @Size(max = 256, message = "description max length 256")
    private String description;

    private Boolean enabled;

    /** Data scope: ALL, OWN_DEPT, SUBORDINATE_TREE, SELF_ONLY */
    private String dataScope;
}
