package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("points_record")
public class PointsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 🌟 核心修复：显式指定数据库中的字段名是 user_id
    @TableField("user_id")
    private Long userId;

    // 这两个名字一样，可以不加注解
    private String type;
    private Integer points;
    private String description;

    // 🌟 核心修复：显式指定数据库中的字段名是 create_time
    @TableField("create_time")
    private Date createTime;
}