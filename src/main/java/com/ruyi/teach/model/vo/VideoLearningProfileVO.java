package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class VideoLearningProfileVO {

    private Integer days;

    private String conclusion;

    private Integer totalEvents;

    private Integer weakPointCount;

    private Integer totalRewatchCount;

    private Integer totalSkipCount;

    private Integer totalPauseSeconds;

    private Integer totalInterventionCount;

    private Integer highSpeedEventCount;

    private LatestIntervention latestIntervention;

    private List<WeakPoint> weakPoints;

    @Data
    public static class LatestIntervention {
        private String eventTime;
        private String courseName;
        private String chapterTitle;
        private String knowledgeName;
    }

    @Data
    public static class WeakPoint {
        private Long segmentId;
        private String courseName;
        private String chapterTitle;
        private String knowledgeName;
        private String difficulty;
        private Integer rewatchCount;
        private Integer pauseSeconds;
        private Integer skipCount;
        private Integer interventionCount;
        private String conclusion;
        private List<BehaviorDetail> behaviorDetails;
    }

    @Data
    public static class BehaviorDetail {
        private String eventType;
        private String label;
        private String timeRange;
        private Integer fromSecond;
        private Integer toSecond;
        private Integer durationSecond;
        private String eventTime;
    }
}
