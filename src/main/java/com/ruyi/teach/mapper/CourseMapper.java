package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.vo.CourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 学生查询分配给自己班级的专属课程
     */
    @Select("SELECT c.*, c.face_detection_required AS faceDetectionRequired, t.user_avatar AS teacherAvatar " +
            "FROM user u " +
            "JOIN course_class_relation ccr ON u.class_id = ccr.class_id " +
            "JOIN course c ON ccr.course_id = c.id " +
            "LEFT JOIN user t ON c.teacherId = t.id " +
            "WHERE u.id = #{studentId} AND c.isDelete = 0")
    List<CourseVO> selectStudentCourses(@Param("studentId") Long studentId);

    /**
     * 查询课程已绑定的班级ID列表
     */
    @Select("SELECT class_id FROM course_class_relation WHERE course_id = #{courseId} ORDER BY id ASC")
    List<Long> selectClassIdsByCourseId(@Param("courseId") Long courseId);
}
