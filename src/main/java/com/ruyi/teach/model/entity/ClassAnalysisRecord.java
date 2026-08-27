package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("class_analysis_record")
public class ClassAnalysisRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("audio_url")
    private String audioUrl;

    @TableField("plan_text")
    private String planText;

    @TableField("plan_resource_id")
    private Long planResourceId;

    @TableField("plan_title_snapshot")
    private String planTitleSnapshot;

    @TableField("transcript_json")
    private String transcriptJson;

    @TableField("ai_report")
    private String aiReport;

    @TableField("status")
    private String status;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}