package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

import java.util.List;

@Data
public class CourseGraphClassFocusVO {
    private String nodeId;
    private String nodeName;
    private String categoryName;
    private String summary;
    private String attentionLevel;
    private Integer relatedClassRecordCount;
    private String latestInsight;
    private List<String> observationPoints;
    private List<String> behaviorSignals;
    private List<String> recommendedFollowups;
}
