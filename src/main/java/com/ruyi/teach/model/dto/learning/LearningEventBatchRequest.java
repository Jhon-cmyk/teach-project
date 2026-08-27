package com.ruyi.teach.model.dto.learning;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LearningEventBatchRequest {

    private List<EventItem> events;

    @Data
    public static class EventItem {
        private Long courseId;
        private Long chapterId;
        private Long resourceId;
        private String resourceType;
        private String knowledgeName;
        private String eventType;
        private Integer durationSecond;
        private BigDecimal score;
        private Integer correct;
        private String extraJson;
    }
}
