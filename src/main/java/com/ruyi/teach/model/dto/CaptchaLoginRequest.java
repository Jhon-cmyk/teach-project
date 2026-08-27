package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CaptchaLoginRequest {
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 64, message = "账号长度必须在 4 到 64 位之间")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在 6 到 128 位之间")
    private String userPassword;

    @NotBlank(message = "验证码标识不能为空")
    @Size(max = 64, message = "验证码标识长度不能超过 64 位")
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 8, message = "验证码长度必须在 4 到 8 位之间")
    private String captchaCode;
}
