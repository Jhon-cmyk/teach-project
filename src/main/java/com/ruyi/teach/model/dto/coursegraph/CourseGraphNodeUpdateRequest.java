package com.ruyi.teach.model.dto.coursegraph;

import lombok.Data;

import java.util.List;

@Data
public class CourseGraphNodeUpdateRequest {

    private String id;

    private String name;

    private String category;

    private Integer symbolSize;

    private String parentId;

    private String learnUrl;

    private String learningContent;

    private String description;

    private String difficulty;

    private String importance;

    private Integer estimatedHours;

    private Integer teachingWeek;

    private List<String> commonMistakes;

    private List<String> teachingTips;

    private Boolean isCore;

    private Boolean isKeyPoint;
}
