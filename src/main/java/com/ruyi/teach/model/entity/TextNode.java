package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField; // 👈 必须引入
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("text_node")
public class TextNode {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 🔥 核心修改：明确映射 course_id
    @TableField("course_id")
    private Long courseId;

    private String title;

    private String content;

    // 🔥 核心修改：明确映射 sort_order
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private Date createTime;
}