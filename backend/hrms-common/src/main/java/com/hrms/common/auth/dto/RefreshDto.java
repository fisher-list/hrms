package com.hrms.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshDto {

    @NotBlank(message = "refreshToken must not be blank")
    private String refreshToken;
}
