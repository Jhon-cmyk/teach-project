package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "user")
@Data
public class User implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String userAccount;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String userPassword;

    private String userName;

    private String userAvatar;

    private String userRole;

    @TableField("teacher_title")
    private String teacherTitle;

    @TableField("teacher_register_code")
    private String teacherRegisterCode;

    /**
     * 个性签名
     */
    @TableField("user_profile")
    private String userProfile;

    private Integer points;

    @TableField("class_id")
    private Long classId; // 关联 sys_class 表的 id (仅学生有意义)

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
