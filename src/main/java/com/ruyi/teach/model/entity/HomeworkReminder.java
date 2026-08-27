package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 作业提醒记录
 */
@TableName(value = "hw_reminder")
@Data
public class HomeworkReminder implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送提醒的教师ID */
    @TableField("teacher_id")
    private Long teacherId;

    /**
     * 目标班级ID，NULL 代表对该教师下所有班级广播
     */
    @TableField("class_id")
    private Long classId;

    /** 提醒内容 */
    private String message;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}