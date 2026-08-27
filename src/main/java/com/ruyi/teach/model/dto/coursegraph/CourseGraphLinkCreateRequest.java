package com.ruyi.teach.model.dto.coursegraph;

import lombok.Data;

@Data
public class CourseGraphLinkCreateRequest {

    private String source;

    private String target;

    private String relationType;

    private String description;
}
