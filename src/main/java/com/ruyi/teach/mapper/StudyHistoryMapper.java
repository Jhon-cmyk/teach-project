package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.StudyHistory;
import com.ruyi.teach.model.vo.HistoryCourseVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface StudyHistoryMapper extends BaseMapper<StudyHistory> {

    /**
     * 高并发安全的记录更新：利用数据库唯一索引，存在则更新时间，不存在则插入
     */
    @Insert("INSERT INTO study_history (user_id, course_id, last_study_time) " +
            "VALUES (#{userId}, #{courseId}, #{lastStudyTime}) " +
            "ON DUPLICATE KEY UPDATE last_study_time = #{lastStudyTime}")
    void saveOrUpdateHistory(@Param("userId") Long userId,
                             @Param("courseId") Long courseId,
                             @Param("lastStudyTime") Date lastStudyTime);

    /**
     * 连表查询历史记录：直接查出课程信息，并严格按照最后学习时间倒序
     */
    @Select("SELECT c.id, c.name, c.description, c.coverImg, h.last_study_time AS updateTime " +
            "FROM study_history h " +
            "JOIN course c ON h.course_id = c.id " +
            "WHERE h.user_id = #{userId} AND c.isDelete = 0 " +
            "ORDER BY h.last_study_time DESC " +
            "LIMIT #{limit}")
    List<HistoryCourseVO> selectMyHistory(@Param("userId") Long userId, @Param("limit") int limit);
}