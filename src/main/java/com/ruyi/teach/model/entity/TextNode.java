package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("text_node")
public class TextNode {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 数据库列名为 course_id
    @TableField("course_id")
    private Long courseId;

    private String title;

    private String content;

    // 数据库列名为 sort_order
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private Date createTime;
}