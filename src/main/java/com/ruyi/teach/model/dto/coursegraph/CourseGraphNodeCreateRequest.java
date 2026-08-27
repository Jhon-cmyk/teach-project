package com.ruyi.teach.model.dto.coursegraph;

import lombok.Data;

@Data
public class CourseGraphNodeCreateRequest {

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

    private Boolean isCore;

    private Boolean isKeyPoint;
}
