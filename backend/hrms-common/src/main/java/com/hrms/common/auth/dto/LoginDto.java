package com.hrms.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDto {

    @NotBlank(message = "username must not be blank")
    @Size(max = 64)
    private String username;

    @NotBlank(message = "password must not be blank")
    @Size(min = 1, max = 128)
    private String password;
}
