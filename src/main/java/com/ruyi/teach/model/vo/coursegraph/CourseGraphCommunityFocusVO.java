package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;
import java.util.List;

@Data
public class CourseGraphCommunityFocusVO {
    private String nodeId;
    private String nodeName;
    private String categoryName;
    private String summary;
    private String hotLevel;
    private Integer discussionCount;
    private Integer homeworkCount;
    private Integer featuredCount;
    private Integer pendingHomeworkCount;
    private Integer recentActiveCount;
    private Boolean shouldGoDesk;
    private List<CourseGraphCommunityPostItemVO> recentItems;
    private List<CourseGraphCommunityPostItemVO> featuredItems;
    private List<String> suggestedActions;
}
