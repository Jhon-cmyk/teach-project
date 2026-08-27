package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("platform_banner")
@Data
public class PlatformBanner implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    @TableField("image_url")
    private String imageUrl;

    @TableField("target_url")
    private String targetUrl;

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