package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_resource")
public class AiResource {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    /**
     * 资源类型：plan / quiz / anim / analysis ...
     */
    @TableField("type")
    private String type;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("params_json")
    private String paramsJson;

    @TableField("is_published")
    private Integer isPublished;

    @TableField("is_delete")
    private Integer isDelete;

    /**
     * 新增：来源资源ID（检索中心里的原始资源ID）
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 新增：来源资源类型（video / plan / quiz / anim）
     */
    @TableField("source_type")
    private String sourceType;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}