package com.ruyi.teach.model.dto.coursegraph;

import lombok.Data;

import java.util.List;

@Data
public class CourseGraphPreferenceUpdateRequest {

    private List<String> focusedNodeIds;

    private List<String> recentVisitedNodeIds;

    private List<String> recentEditedNodeIds;
}
