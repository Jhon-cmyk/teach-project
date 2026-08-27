package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("fatigue_record")
public class FatigueRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("course_id")
    private Long courseId;

    @TableField("chapter_id")
    private Long chapterId;

    /** 记录日期（按天聚合） */
    @TableField("record_date")
    private LocalDate recordDate;

    @TableField("yawn_count")
    private Integer yawnCount;

    @TableField("fatigue_count")
    private Integer fatigueCount;

    @TableField("no_face_count")
    private Integer noFaceCount;

    @TableField("normal_count")
    private Integer normalCount;

    @TableField("total_detections")
    private Integer totalDetections;

    /** 摄像头开启累计秒数 */
    @TableField("monitor_seconds")
    private Integer monitorSeconds;

    /** 疲劳事件时间线JSON */
    @TableField("events")
    private String events;

    /** EAR采样序列JSON */
    @TableField("ear_samples")
    private String earSamples;

    /** MAR采样序列JSON */
    @TableField("mar_samples")
    private String marSamples;

    @TableField("last_status")
    private String lastStatus;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
