package com.ruyi.teach.model.vo;

import com.ruyi.teach.model.entity.VideoKnowledgeSegment;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class VideoTimelineAnalysisTaskVO {

    private Long taskId;

    private Long chapterId;

    private String status;

    private String errorMessage;

    private Date createTime;

    private Date startedAt;

    private Date finishedAt;

    private List<VideoKnowledgeSegment> segments = new ArrayList<>();
}
