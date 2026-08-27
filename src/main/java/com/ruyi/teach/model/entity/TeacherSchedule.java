package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value = "teacher_schedule")
public class TeacherSchedule implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("course_name")
    private String courseName;

    @TableField("linked_course_id")
    private Long linkedCourseId;

    @TableField("class_name")
    private String className;

    @TableField("teaching_plan_id")
    private Long teachingPlanId;

    @TableField("week_start")
    private Integer weekStart;

    @TableField("week_end")
    private Integer weekEnd;

    @TableField("day_of_week")
    private Integer dayOfWeek;

    @TableField("start_period")
    private Integer startPeriod;

    @TableField("end_period")
    private Integer endPeriod;

    @TableField("semester_label")
    private String semesterLabel;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
