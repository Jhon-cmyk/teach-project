package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

import java.util.List;

@Data
public class CourseGraphPreferenceVO {

    private List<String> focusedNodeIds;

    private List<String> recentVisitedNodeIds;

    private List<String> recentEditedNodeIds;
}
