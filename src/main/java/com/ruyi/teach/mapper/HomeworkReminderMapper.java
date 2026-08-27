package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.HomeworkReminder;
import org.apache.ibatis.annotations.*;

/**
 * 作业提醒 Mapper
 */
@Mapper
public interface HomeworkReminderMapper extends BaseMapper<HomeworkReminder> {

    /**
     * 查询学生最近一条新提醒。
     * 匹配条件：提醒的 class_id = 学生所在班级，或 class_id IS NULL（全体广播）。
     *
     * 注意：使用 @Results 显式指定列名 → 字段名映射，
     * 避免在未开启 map-underscore-to-camel-case 时 create_time 等字段映射为 null。
     */
    @Select("SELECT r.* FROM hw_reminder r " +
            "JOIN user u ON u.id = #{studentId} " +
            "WHERE r.is_delete = 0 " +
            "  AND r.create_time > #{since} " +
            "  AND (r.class_id IS NULL OR r.class_id = u.class_id) " +
            "ORDER BY r.create_time DESC " +
            "LIMIT 1")
    @Results(id = "homeworkReminderMap", value = {
            @Result(column = "id",          property = "id"),
            @Result(column = "teacher_id",  property = "teacherId"),
            @Result(column = "class_id",    property = "classId"),
            @Result(column = "message",     property = "message"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(column = "is_delete",   property = "isDelete")
    })
    HomeworkReminder selectLatestForStudent(@Param("studentId") Long studentId,
                                            @Param("since") java.util.Date since);
}