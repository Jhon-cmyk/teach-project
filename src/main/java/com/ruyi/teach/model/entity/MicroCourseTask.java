package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("micro_course_task")
public class MicroCourseTask {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("status")
    private String status;

    @TableField("progress")
    private Integer progress;

    @TableField("title")
    private String title;

    @TableField("script_json")
    private String scriptJson;

    @TableField("video_url")
    private String videoUrl;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("subtitle_url")
    private String subtitleUrl;

    @TableField("audio_url")
    private String audioUrl;

    @TableField("duration_seconds")
    private Integer durationSeconds;

    @TableField("warnings_json")
    private String warningsJson;

    @TableField("render_stats_json")
    private String renderStatsJson;

    @TableField("params_json")
    private String paramsJson;

    @TableField("error_message")
    private String errorMessage;

    @TableField("course_id")
    private Long courseId;

    @TableField("chapter_id")
    private Long chapterId;

    @TableField("resource_id")
    private Long resourceId;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
