package com.ruyi.teach.model.vo;

import lombok.Data;

@Data
public class VideoInterventionVO {

    private Boolean triggered;

    private String riskLevel;

    private Long segmentId;

    private String knowledgeName;

    private String behaviorSummary;

    private String suggestedPrompt;
}
