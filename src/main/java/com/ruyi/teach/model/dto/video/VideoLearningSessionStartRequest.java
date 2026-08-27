package com.ruyi.teach.model.dto.video;

import lombok.Data;

@Data
public class VideoLearningSessionStartRequest {

    private Long courseId;

    private Long chapterId;
}
