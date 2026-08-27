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

    // 数据库列名为 user_id
    @TableField("user_id")
    private Long userId;

    // 这两个名字一样，可以不加注解
    private String type;
    private Integer points;
    private String description;

    // 数据库列名为 create_time
    @TableField("create_time")
    private Date createTime;
}