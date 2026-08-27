package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

import java.util.List;

@Data
public class CourseGraphNodeVO {

    private String id;
    private String parentId;
    private String name;
    private String category;
    private Integer symbolSize;
    private String description;
    private String learnUrl;
    private String learningContent;
    private String difficulty;
    private String importance;
    private Integer estimatedHours;
    private Integer teachingWeek;
    private List<String> commonMistakes;
    private List<String> teachingTips;
    private Integer resourceCount;
    private Integer exerciseCount;
    private Boolean isCore;
    private Boolean isKeyPoint;
    private String resourceSummary;
    private List<String> resourceTypes;
    private String analysisHeatLevel;
    private String weaknessLevel;
    private Boolean recommendedForVisual;
    private Boolean recommendedForAnalysis;
    private String analysisSummary;
    private String communityHotLevel;
    private Integer pendingCommunityCount;
    private Integer featuredCommunityCount;
    private Boolean recommendedForCommunityDesk;
    private String communitySummary;
}
