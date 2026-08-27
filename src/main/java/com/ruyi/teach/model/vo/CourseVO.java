package com.ruyi.teach.model.vo;

import com.ruyi.teach.model.entity.Course;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程视图对象（包含跨表查询的附加信息）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseVO extends Course {
    
    /**
     * 授课老师的头像 (来自 user 表连查)
     */
    private String teacherAvatar;
    
    // 如果以后需要返回排课时间、班级名称等，都可以加在这个类里
}