package com.ruyi.teach.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class HomeworkTeacherMonitorReportRequest implements Serializable {

    /**
     * 可空；为空表示全部班级
     */
    private Long classId;

    /**
     * 可空；格式：yyyy-MM-dd
     */
    private String publishDate;

    /**
     * 可空；为空表示全部练习题（按 ai_resource.id 筛选）
     */
    private Long quizResourceId;

    /**
     * 可空；为空表示全部类型；"homework" / "exam" 表示仅筛选该类型
     */
    private String assignmentType;

    private static final long serialVersionUID = 1L;
}