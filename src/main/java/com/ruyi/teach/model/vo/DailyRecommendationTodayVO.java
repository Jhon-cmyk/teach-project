package com.ruyi.teach.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyRecommendationTodayVO {

    private Long sessionId;

    private LocalDate recommendDate;

    private String status;

    private Boolean shouldPrompt;

    private String promptType;

    private Long courseId;

    private String goal;

    private String difficultyText;

    private Integer availableMinutes;

    private String preferredResourceType;

    private List<StudentLearningProfileVO.RecommendationItem> recommendations;
}
