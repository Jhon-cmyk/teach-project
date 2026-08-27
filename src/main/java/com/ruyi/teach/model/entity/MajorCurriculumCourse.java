package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName("major_curriculum_course")
@Data
public class MajorCurriculumCourse implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String major;

    @TableField("semester_no")
    private Integer semesterNo;

    @TableField("course_id")
    private Long courseId;

    @TableField("course_name")
    private String courseName;

    @TableField("course_type")
    private String courseType;

    private BigDecimal credits;

    private Integer hours;

    @TableField("sort_order")
    private Integer sortOrder;

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
