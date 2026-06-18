package com.hrms.common.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating / updating a company.
 */
@Data
public class CompanyDto {

    @NotBlank(message = "code is required")
    @Size(max = 64, message = "code max length 64")
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 128, message = "name max length 128")
    private String name;

    @Size(max = 64, message = "legalRepresentative max length 64")
    private String legalRepresentative;

    @Size(max = 256, message = "address max length 256")
    private String address;

    @Size(max = 32, message = "phone max length 32")
    private String phone;

    @Size(max = 128, message = "email max length 128")
    private String email;
}
