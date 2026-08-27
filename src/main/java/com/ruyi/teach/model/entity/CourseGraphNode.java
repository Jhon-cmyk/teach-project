package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("course_graph_node")
public class CourseGraphNode implements Serializable {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    private String parentId;

    private String name;

    private String category;

    private Integer symbolSize;

    private String description;

    private String difficulty;

    private String importance;

    private Integer estimatedHours;

    private Integer teachingWeek;

    private String commonMistakes;

    private String teachingTips;

    private Integer resourceCount;

    private Integer exerciseCount;

    private Integer isCore;

    private Integer isKeyPoint;

    private String resourceSummary;

    private String resourceTypes;

    private String learnUrl;

    private String learningContent;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
