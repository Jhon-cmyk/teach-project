package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("coding_problem")
@Data
public class CodingProblem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private String languages;

    @TableField("time_limit_ms")
    private Integer timeLimitMs;

    @TableField("memory_limit_kb")
    private Integer memoryLimitKb;

    @TableField("graph_node_id")
    private Long graphNodeId;

    @TableField("course_id")
    private Long courseId;

    @TableField("semester_label")
    private String semesterLabel;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("is_public")
    private Integer isPublic;

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
