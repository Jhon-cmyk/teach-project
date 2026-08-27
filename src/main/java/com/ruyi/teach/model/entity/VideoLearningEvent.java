package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "video_learning_event")
@Data
public class VideoLearningEvent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("student_id")
    private Long studentId;

    @TableField("course_id")
    private Long courseId;

    @TableField("chapter_id")
    private Long chapterId;

    @TableField("segment_id")
    private Long segmentId;

    @TableField("event_type")
    private String eventType;

    @TableField("from_second")
    private Integer fromSecond;

    @TableField("to_second")
    private Integer toSecond;

    @TableField("duration_second")
    private Integer durationSecond;

    @TableField("playback_rate")
    private BigDecimal playbackRate;

    @TableField("event_time")
    private Date eventTime;

    @TableField("extra_json")
    private String extraJson;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
