package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("course_mindmap")
public class CourseMindmap implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("course_id")
    private Long courseId;

    private String title;

    @TableField("mindmap_json")
    private String mindmapJson;

    @TableField("source_hash")
    private String sourceHash;

    /**
     * ready / fallback
     */
    private String status;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;

    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;
}