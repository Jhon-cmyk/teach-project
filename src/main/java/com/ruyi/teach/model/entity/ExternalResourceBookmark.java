package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("external_resource_bookmark")
public class ExternalResourceBookmark {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("platform")
    private String platform;

    @TableField("external_id")
    private String externalId;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("cover")
    private String cover;

    @TableField("author")
    private String author;

    @TableField("url")
    private String url;

    @TableField("resource_type")
    private String resourceType;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("raw_json")
    private String rawJson;

    @TableField("is_delete")
    private Integer isDelete;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
