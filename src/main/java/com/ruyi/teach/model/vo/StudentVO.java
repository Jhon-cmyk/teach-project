package com.ruyi.teach.model.vo;

import lombok.Data;

@Data
public class StudentVO {
    private Long id;
    private String studentNo;   // 学号
    private String name;        // 姓名
    private String phone;       // 手机号
    private String userAvatar;  // 头像
}