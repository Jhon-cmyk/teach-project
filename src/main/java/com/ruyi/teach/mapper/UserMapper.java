package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据班级 ID 查询该班级的所有学生
     */
    @Select("SELECT id, userAccount, userName, user_avatar AS userAvatar, user_profile AS userProfile " +
            "FROM user " +
            "WHERE class_id = #{classId} AND userRole = 'student' AND isDelete = 0")
    List<User> selectStudentsByClassId(@Param("classId") Long classId);

    /**
     * 统计某位老师带的所有学生总数（去重）
     */
    @Select("SELECT COUNT(DISTINCT u.id) FROM user u " +
            "JOIN course_class_relation ccr ON u.class_id = ccr.class_id " +
            "JOIN course c ON ccr.course_id = c.id " +
            "WHERE c.teacherId = #{teacherId} AND u.userRole = 'student' AND u.isDelete = 0")
    long countStudentsByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 查询某位老师带的所有学生ID列表（去重）
     */
    @Select("SELECT DISTINCT u.id FROM user u " +
            "JOIN course_class_relation ccr ON u.class_id = ccr.class_id " +
            "JOIN course c ON ccr.course_id = c.id " +
            "WHERE c.teacherId = #{teacherId} AND u.userRole = 'student' AND u.isDelete = 0")
    List<Long> listStudentIdsByTeacherId(@Param("teacherId") Long teacherId);
}