package com.ruyi.teach.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 64, message = "账号长度必须在 4 到 64 位之间")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在 6 到 128 位之间")
    private String userPassword;

    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;

    @Size(max = 50, message = "姓名长度不能超过 50 位")
    private String userName;

    @Pattern(regexp = "^(student|teacher)?$", message = "用户角色只能是 student 或 teacher")
    private String userRole;

    @Size(max = 64, message = "教师注册号长度不能超过 64 位")
    private String teacherRegisterCode;

    @AssertTrue(message = "两次输入的密码不一致")
    @JsonIgnore
    public boolean isPasswordConfirmed() {
        return userPassword != null && userPassword.equals(checkPassword);
    }
}
