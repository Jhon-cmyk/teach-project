package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("course_graph_node_activity")
public class CourseGraphNodeActivity implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String nodeId;

    private Long teacherId;

    private String activityType;

    private Long activityId;

    private String activityTitle;

    private Integer weight;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
