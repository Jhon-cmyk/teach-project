package com.ruyi.teach.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public interface VisionProvider {

    List<RecognizedAnswer> recognizeHomeworkImages(VisionHomeworkRequest request);

    @Data
    class VisionHomeworkRequest {
        private Long assignmentId;
        private Long submissionId;
        private String paperContent;
        private List<ImageGroup> imageGroups = new ArrayList<>();
    }

    @Data
    class ImageGroup {
        private String questionNo;
        private List<String> imageUrls = new ArrayList<>();
    }

    @Data
    class RecognizedAnswer {
        private String questionNo;
        private String recognizedText;
        private Double confidence;
        private List<String> imageUrls = new ArrayList<>();
        private String rawJson;
    }
}
