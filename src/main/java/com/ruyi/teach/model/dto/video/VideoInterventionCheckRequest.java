package com.ruyi.teach.model.dto.video;

import lombok.Data;

@Data
public class VideoInterventionCheckRequest {

    private Long sessionId;

    private Long segmentId;

    private String latestEventType;
}
