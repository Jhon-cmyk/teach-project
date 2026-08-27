package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("teacher_course_assignment")
@Data
public class TeacherCourseAssignment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("course_id")
    private Long courseId;

    private String semester;

    @TableField("assigned_by")
    private Long assignedBy;

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
