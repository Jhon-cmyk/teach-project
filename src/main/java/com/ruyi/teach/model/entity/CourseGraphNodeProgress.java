package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("course_graph_node_progress")
public class CourseGraphNodeProgress implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private String nodeId;

    private Integer completionRate;

    private Integer masteryRate;

    private Integer studyMinutes;

    private Date lastStudyTime;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
