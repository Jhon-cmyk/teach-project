package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class StudentTrajectoryVO {

    private Long classId;
    private String className;

    private Long studentId;
    private String studentNo;
    private String studentName;

    private Summary summary;
    private Fatigue fatigue;
    private List<UnfinishedHomeworkItem> unfinishedHomeworkList;

    @Data
    public static class Summary {
        /**
         * 累计学习秒数（由 fatigue_record.monitor_seconds 汇总）
         */
        private Integer totalStudySeconds;

        /**
         * 友好展示文本：如 5小时20分钟
         */
        private String totalStudyDurationText;

        /**
         * 正式作业总数
         */
        private Integer totalHomeworkCount;

        /**
         * 已完成作业数
         */
        private Integer completedHomeworkCount;

        /**
         * 未完成作业数
         */
        private Integer unfinishedHomeworkCount;

        /**
         * 完成率（0-100）
         */
        private Integer completionRate;

        /**
         * 是否存在未完成作业
         */
        private Boolean hasUnfinishedHomework;
    }

    @Data
    public static class Fatigue {
        /**
         * 最近一次疲劳记录日期
         */
        private String latestRecordDate;

        private Integer fatigueCount;
        private Integer yawnCount;
        private Integer noFaceCount;
        private Integer normalCount;
        private Integer totalDetections;
        private Integer monitorSeconds;

        /**
         * 原始状态值：normal / fatigue / yawn / no_face
         */
        private String lastStatus;

        /**
         * 中文展示
         */
        private String lastStatusText;

        /**
         * 综合疲劳结论
         */
        private String fatigueLevelText;
    }

    @Data
    public static class UnfinishedHomeworkItem {
        private Long assignmentId;
        private String title;
        private Date deadline;
        private Integer questionCount;
        private Integer totalScore;
        private String assignmentType;
    }
}