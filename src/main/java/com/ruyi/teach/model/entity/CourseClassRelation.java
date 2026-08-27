package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 课程-班级排课映射关系
 */
@TableName(value = "course_class_relation")
@Data
public class CourseClassRelation implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联 course 表的 id
     */
    @TableField("course_id")
    private Long courseId;

    /**
     * 关联 sys_class 表的 id
     */
    @TableField("class_id")
    private Long classId;

    /**
     * 排课时间
     */
    @TableField("create_time")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}