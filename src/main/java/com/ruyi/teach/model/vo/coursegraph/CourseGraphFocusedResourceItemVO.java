package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

@Data
public class CourseGraphFocusedResourceItemVO {
    private Long id;
    private String title;
    private String type;
    private String typeText;
    private Integer isPublished;
    private Integer matchScore;
    private String matchReason;
    private Boolean isMapped;
}
