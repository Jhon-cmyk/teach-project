package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("course_graph_resource_link")
public class CourseGraphResourceLink implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("node_id")
    private String nodeId;

    @TableField("resource_id")
    private Long resourceId;

    @TableField("resource_type")
    private String resourceType;

    @TableField("relevance_score")
    private Integer relevanceScore;

    private String source;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
