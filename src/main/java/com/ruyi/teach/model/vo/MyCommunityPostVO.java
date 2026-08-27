package com.ruyi.teach.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class MyCommunityPostVO implements Serializable {

    private Long id;

    private String title;

    private Long courseId;

    private String courseName;

    /**
     * discussion / homework
     */
    private String postType;

    /**
     * open / resolved
     */
    private String status;

    private Integer replyCount;

    private Integer viewCount;

    private String lastActiveTime;

    private Long lastActiveTimestamp;

    @JsonProperty("isTeacherAnswered")
    private Boolean teacherAnswered;

    private static final long serialVersionUID = 1L;
}