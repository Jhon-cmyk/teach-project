package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.SysClass;
// 注意这里要引入你第一步写的 StudentVO
import com.ruyi.teach.model.vo.StudentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysClassMapper extends BaseMapper<SysClass> {

    /**
     * 老师查询自己某门课程分配给了哪些班级
     */
    @Select("SELECT sc.* " +
            "FROM course c " +
            "JOIN course_class_relation ccr ON c.id = ccr.course_id " +
            "JOIN sys_class sc ON ccr.class_id = sc.id " +
            "WHERE c.teacherId = #{teacherId} AND c.id = #{courseId} AND c.isDelete = 0")
    List<SysClass> selectClassesByTeacherCourse(@Param("teacherId") Long teacherId, @Param("courseId") Long courseId);

    /**
     * 👇 新增：查询指定班级下的所有学生
     * 使用 userAccount 作为学号，userName 作为姓名
     */
    @Select("SELECT id, " +
            "userAccount AS studentNo, " +
            "userName AS name, " +
            "userAvatar AS userAvatar, " +
            "'' AS phone " +
            "FROM `user` " +
            "WHERE class_id = #{classId} " +
            "AND userRole = 'student' " +
            "AND isDelete = 0")
    List<StudentVO> getStudentsByClassId(@Param("classId") Long classId);

    /**
     * 查询所有班级，并统计每个班级下的学生人数
     */
    @Select("SELECT c.*, " +
            "(SELECT COUNT(*) FROM `user` u WHERE u.class_id = c.id AND u.userRole = 'student' AND u.isDelete = 0) AS studentCount " +
            "FROM sys_class c")
    List<SysClass> selectAllClassesWithStudentCount();

    /**
     * 查询教师实际任教的班级（课程排课、教务课程分配或教师课表）。
     * 历史作业不作为授权来源，避免一次错误发布永久扩大教师的班级权限。
     */
    @Select("SELECT DISTINCT sc.*, " +
            "(SELECT COUNT(*) FROM `user` u WHERE u.class_id = sc.id AND u.userRole = 'student' AND u.isDelete = 0) AS studentCount " +
            "FROM sys_class sc WHERE sc.id IN (" +
            "  SELECT ccr.class_id FROM course_class_relation ccr " +
            "  JOIN course c ON ccr.course_id = c.id " +
            "  WHERE c.teacherId = #{teacherId} AND c.isDelete = 0" +
            "  UNION " +
            "  SELECT ccr2.class_id FROM teacher_course_assignment tca " +
            "  JOIN course_class_relation ccr2 ON ccr2.course_id = tca.course_id " +
            "  JOIN course c2 ON c2.id = tca.course_id " +
            "  WHERE tca.teacher_id = #{teacherId} AND tca.is_delete = 0 AND c2.isDelete = 0" +
            "  UNION " +
            "  SELECT sc2.id FROM teacher_schedule ts " +
            "  JOIN sys_class sc2 ON sc2.name = ts.class_name " +
            "  WHERE ts.teacher_id = #{teacherId} AND ts.is_delete = 0" +
            ") ORDER BY sc.id")
    List<SysClass> selectMyClasses(@Param("teacherId") Long teacherId);

    /**
     * 查询教师当前授课班级，仅以教师课表为准。
     */
    @Select("SELECT sc.*, " +
            "(SELECT COUNT(*) FROM `user` u WHERE u.class_id = sc.id AND u.userRole = 'student' AND u.isDelete = 0) AS studentCount " +
            "FROM sys_class sc " +
            "JOIN ( " +
            "  SELECT DISTINCT ts.class_name FROM teacher_schedule ts " +
            "  WHERE ts.teacher_id = #{teacherId} " +
            "    AND ts.is_delete = 0 " +
            "    AND (#{semesterLabel} IS NULL OR #{semesterLabel} = '' OR ts.semester_label = #{semesterLabel}) " +
            ") assigned ON assigned.class_name = sc.name " +
            "ORDER BY sc.id")
    List<SysClass> selectTeachingClassesBySchedule(@Param("teacherId") Long teacherId,
                                                   @Param("semesterLabel") String semesterLabel);
}
