package com.ruyi.teach.model.dto.learning;

import lombok.Data;

@Data
public class RecommendationGenerateRequest {

    private Long studentId;

    private Long courseId;

    private Long chapterId;
}
