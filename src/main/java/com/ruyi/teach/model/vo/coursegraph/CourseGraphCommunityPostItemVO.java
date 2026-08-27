package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

@Data
public class CourseGraphCommunityPostItemVO {
    private Long id;
    private String title;
    private String courseName;
    private String postType;
    private String status;
    private Integer replyCount;
    private Integer viewCount;
    private String lastActiveTime;
    private String authorName;
    private Boolean isTeacherAnswered;
    private Boolean isFeatured;
}
