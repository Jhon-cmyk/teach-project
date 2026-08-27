package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class HomeworkTeacherMonitorReportVO implements Serializable {

    private Long reportId;

    private String reportTitle;

    private Long classId;

    private String publishDate;

    /**
     * 报告生成时筛选的练习题资源ID，为空表示"全部练习题"
     */
    private Long quizResourceId;

    /**
     * 练习题标题快照，前端历史列表和报告头部直接展示用
     */
    private String quizTitle;

    private String reportMarkdown;

    private List<Long> assignmentIds;

    private Integer assignmentCount;

    private Integer studentTotal;

    private Integer completedCount;

    private Integer pendingCount;

    private Double overallCompletionRate;

    private Double overallAvgScore;

    private Integer lowScoreCount;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}