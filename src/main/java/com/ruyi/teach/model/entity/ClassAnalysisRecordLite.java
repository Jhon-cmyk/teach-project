package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("class_analysis_record")
public class ClassAnalysisRecordLite implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("audio_url")
    private String audioUrl;

    @TableField("plan_text")
    private String planText;

    @TableField("transcript_json")
    private String transcriptJson;

    @TableField("ai_report")
    private String aiReport;

    private String status;

    @TableField("create_time")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
