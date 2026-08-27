package com.ruyi.teach.model.dto.video;

import lombok.Data;

import java.util.List;

@Data
public class VideoKnowledgeSegmentSaveRequest {

    private List<SegmentItem> segments;

    @Data
    public static class SegmentItem {
        private Long id;
        private Integer startSecond;
        private Integer endSecond;
        private String knowledgeName;
        private String description;
        private String difficulty;
        private Integer sortOrder;
    }
}
