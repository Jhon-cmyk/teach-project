package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdatePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    @Size(max = 128, message = "旧密码长度不能超过 128 位")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 128, message = "新密码长度必须在 6 到 128 位之间")
    private String newPassword;
}
