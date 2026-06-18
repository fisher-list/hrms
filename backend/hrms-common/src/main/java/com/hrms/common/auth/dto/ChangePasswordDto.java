package com.hrms.common.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDto {

    @NotBlank(message = "旧密码不能为空")
    @Size(max = 128)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 128)
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(max = 128)
    private String confirmPassword;
}
