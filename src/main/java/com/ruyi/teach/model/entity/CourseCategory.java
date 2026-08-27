package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("course_category")
@Data
public class CourseCategory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("icon_url")
    private String iconUrl;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_enabled")
    private Integer isEnabled;

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