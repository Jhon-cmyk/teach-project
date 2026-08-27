package com.ruyi.teach.model.dto.learning;

import lombok.Data;

@Data
public class DailyRecommendationSubmitRequest {

    private Long courseId;

    private String goal;

    private String difficultyText;

    private String learningSituation;

    private String personalityType;

    private String universityName;

    private String developmentGoal;

    private Integer availableMinutes;

    private String preferredResourceType;

    /** Information collection path used for this recommendation. */
    private String collectionMode;

    /** Short, student-visible summary produced by the AI interview. */
    private String interviewSummary;
}
