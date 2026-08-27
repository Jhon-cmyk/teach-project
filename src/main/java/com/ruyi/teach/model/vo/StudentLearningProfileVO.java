package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class StudentLearningProfileVO {

    private Integer days;

    private PreferenceSummary preference;

    private List<MasteryItem> weakPoints;

    private List<WrongQuestionItem> wrongQuestions;

    private List<RecommendationItem> recommendations;

    private List<InterventionAdvice> advices;

    private ProfileInsight insight;

    private List<ActionPlanItem> actionPlans;

    private List<EvidenceItem> evidenceItems;

    @Data
    public static class PreferenceSummary {
        private String dominantType;
        private String summary;
        private Integer videoScore;
        private Integer textScore;
        private Integer practiceScore;
        private Integer discussionScore;
        private Integer aiScore;
        private Integer resourceScore;
    }

    @Data
    public static class MasteryItem {
        private String knowledgeName;
        private Long courseId;
        private Long chapterId;
        private Integer masteryScore;
        private String status;
        private String evidenceSummary;
    }

    @Data
    public static class WrongQuestionItem {
        private Long submissionId;
        private Long assignmentId;
        private Long courseId;
        private Long chapterId;
        private Long detailId;
        private String assignmentTitle;
        private String questionNo;
        private String questionType;
        private String stemSnapshot;
        private String studentAnswer;
        private String aiComment;
        private String actionUrl;
        private String actionLabel;
    }

    @Data
    public static class RecommendationItem {
        private Long id;
        private Long courseId;
        private String courseName;
        private String coverImg;
        private Long resourceId;
        private String resourceType;
        private String resourceTitle;
        private String knowledgeName;
        private String recommendationReason;
        private String practiceSuggestion;
        private String recommendationSource;
        private String status;
        private String actionType;
        private String actionUrl;
        private String actionLabel;
        private String shortReason;
    }

    @Data
    public static class InterventionAdvice {
        private String title;
        private String body;
        private String tone;
    }

    @Data
    public static class ProfileInsight {
        private String title;
        private String body;
        private String riskLevel;
        private String riskLabel;
        private Integer overallScore;
        private Integer weakPointCount;
        private Integer wrongQuestionCount;
        private Integer confidence;
        private String confidenceLabel;
        private String trendLabel;
        private Integer recentActivityCount;
    }

    @Data
    public static class ActionPlanItem {
        private String title;
        private String target;
        private String reason;
        private String actionType;
        private String actionText;
        private String actionUrl;
        private Integer minutes;
        private Integer priority;
    }

    @Data
    public static class EvidenceItem {
        private String label;
        private String value;
        private String detail;
        private String tone;
    }
}
