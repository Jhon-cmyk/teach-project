package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("homework_monitor_report")
@Data
public class HomeworkMonitorReport implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private Long classId;

    /**
     * yyyy-MM-dd，可空
     */
    private String publishDate;

    /**
     * 筛选的练习题资源ID（ai_resource.id），可空；为空表示"全部练习题"
     */
    private Long quizResourceId;

    /**
     * 生成报告时的练习题标题快照，用于历史列表直接展示，避免每次回查 ai_resource
     */
    private String quizTitleSnapshot;

    private String reportTitle;

    private String reportMarkdown;

    private String assignmentIdsJson;

    private String summaryJson;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}