package com.ruyi.teach.model.dto.video;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VideoLearningEventBatchRequest {

    private Long sessionId;

    private List<EventItem> events;

    @Data
    public static class EventItem {
        private String eventType;
        private Long segmentId;
        private Integer fromSecond;
        private Integer toSecond;
        private Integer durationSecond;
        private BigDecimal playbackRate;
        private String extraJson;
    }
}
