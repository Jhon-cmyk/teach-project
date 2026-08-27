package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField; // 👈 必须引入这个
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("text_course")
public class TextCourse {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    // 🔥 核心修改：明确指定数据库列名为 cover_img
    @TableField("cover_img")
    private String coverImg;

    private String description;

    // 如果 createTime 以后报错，也可以加上 @TableField("create_time")
    @TableField("create_time")
    private Date createTime;
}