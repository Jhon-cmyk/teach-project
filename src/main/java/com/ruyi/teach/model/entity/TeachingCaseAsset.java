package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("teaching_case_asset")
public class TeachingCaseAsset {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("case_id")
    private Long caseId;

    @TableField("type")
    private String type;

    @TableField("url")
    private String url;

    @TableField("title")
    private String title;

    @TableField("caption")
    private String caption;

    @TableField("context")
    private String context;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("hash")
    private String hash;

    @TableField("width")
    private Integer width;

    @TableField("height")
    private Integer height;

    @TableField("source")
    private String source;

    @TableField("is_delete")
    private Integer isDelete;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
