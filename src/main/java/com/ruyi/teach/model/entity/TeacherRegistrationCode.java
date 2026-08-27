package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("teacher_registration_code")
public class TeacherRegistrationCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("register_code")
    private String registerCode;

    @TableField("teacher_name")
    private String teacherName;

    @TableField("teacher_title")
    private String teacherTitle;

    @TableField("status")
    private String status;

    @TableField("used_by")
    private Long usedBy;

    @TableField("used_time")
    private Date usedTime;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;
}
