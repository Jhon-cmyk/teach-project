package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;
import java.util.List;

@Data
public class CourseGraphCommunityDeskFocusVO {
    private String nodeId;
    private String nodeName;
    private String categoryName;
    private String summary;
    private Integer pendingHomeworkCount;
    private Integer resolvedCount;
    private Integer featuredCount;
    private Integer candidateFeaturedCount;
    private Boolean shouldRecommendDesk;
    private List<CourseGraphCommunityPostItemVO> recentItems;
    private List<String> suggestedActions;
}
