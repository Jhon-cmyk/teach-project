package com.ruyi.teach.model.dto.coursegraph;

import lombok.Data;

@Data
public class CourseGraphResourceBindRequest {
    private String nodeId;
    private Long resourceId;
    private String resourceType;
    private Integer relevanceScore;
    private String source;
}
