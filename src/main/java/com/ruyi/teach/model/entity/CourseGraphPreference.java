package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("course_graph_preference")
public class CourseGraphPreference implements Serializable {

    @TableId(value = "teacherId", type = IdType.INPUT)
    private Long teacherId;

    private String focusedNodeIds;

    private String recentVisitedNodeIds;

    private String recentEditedNodeIds;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
