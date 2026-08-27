package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data // Lombok 注解，自动生成 get/set 方法
@TableName("course_comment") // 指定对应的数据库表名
public class CourseComment {

    @TableId(type = IdType.AUTO) // 指定主键且为自增
    private Long id;

    @TableField("course_id")
    private Long courseId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("user_avatar")
    private String userAvatar;

    @TableField("content")
    private String content;

    @TableField("likes")
    private Integer likes;

    @TableField("create_time")
    private LocalDateTime createTime;
}