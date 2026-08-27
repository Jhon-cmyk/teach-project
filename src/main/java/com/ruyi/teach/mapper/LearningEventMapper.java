package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.LearningEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface LearningEventMapper extends BaseMapper<LearningEvent> {

    @Select("""
            SELECT studyDate, SUM(totalSeconds) AS totalSeconds
            FROM (
                SELECT DATE(eventTime) AS studyDate,
                       SUM(GREATEST(COALESCE(durationSecond, 0), 0)) AS totalSeconds
                FROM learning_event
                WHERE studentId = #{studentId}
                  AND isDelete = 0
                  AND eventTime >= #{startTime}
                  AND eventTime < #{endTimeExclusive}
                  AND eventType NOT IN ('video_watch', 'video_pause', 'video_rewatch')
                GROUP BY DATE(eventTime)
                UNION ALL
                SELECT DATE(event_time) AS studyDate,
                       SUM(GREATEST(COALESCE(duration_second, 0), 0)) AS totalSeconds
                FROM video_learning_event
                WHERE student_id = #{studentId}
                  AND event_time >= #{startTime}
                  AND event_time < #{endTimeExclusive}
                GROUP BY DATE(event_time)
            ) daily
            GROUP BY studyDate
            ORDER BY studyDate
            """)
    List<Map<String, Object>> sumDailyLearningSeconds(@Param("studentId") Long studentId,
                                                      @Param("startTime") Date startTime,
                                                      @Param("endTimeExclusive") Date endTimeExclusive);
}
