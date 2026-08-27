package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

import java.util.List;

@Data
public class CourseGraphResourceFocusVO {
    private String nodeId;
    private String nodeName;
    private String categoryName;
    private List<String> recommendedGraphTypes;
    private List<String> recommendedLibraryTypes;
    private String suggestedDirection;
    private Integer mappedResourceCount;
    private Integer matchedResourceCount;
    private Boolean fromMapping;
    private List<CourseGraphFocusedResourceItemVO> focusedResources;
}
