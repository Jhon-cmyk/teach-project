package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(max = 50, message = "姓名长度不能超过 50 位")
    private String userName;

    @Size(max = 4000, message = "个人简介长度不能超过 4000 位")
    private String userProfile;

    @Size(max = 2048, message = "头像地址长度不能超过 2048 位")
    private String userAvatar;
}
