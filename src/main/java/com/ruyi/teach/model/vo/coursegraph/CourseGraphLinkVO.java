package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

@Data
public class CourseGraphLinkVO {

    private Long id;

    private String source;

    private String target;

    private String relationType;

    private String description;
}
