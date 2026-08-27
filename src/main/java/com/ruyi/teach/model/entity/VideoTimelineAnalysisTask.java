package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "video_timeline_analysis_task")
@Data
public class VideoTimelineAnalysisTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("course_id")
    private Long courseId;

    @TableField("chapter_id")
    private Long chapterId;

    @TableField("teacher_id")
    private Long teacherId;

    private String status;

    @TableField("source_video_url")
    private String sourceVideoUrl;

    @TableField("transcript_json")
    private String transcriptJson;

    @TableField("result_json")
    private String resultJson;

    @TableField("error_message")
    private String errorMessage;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("started_at")
    private Date startedAt;

    @TableField("finished_at")
    private Date finishedAt;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
