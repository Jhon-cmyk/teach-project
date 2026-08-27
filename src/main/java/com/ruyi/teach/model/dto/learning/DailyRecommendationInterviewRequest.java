package com.ruyi.teach.model.dto.learning;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DailyRecommendationInterviewRequest {

    private List<Message> messages = new ArrayList<>();

    private DailyRecommendationSubmitRequest profile;

    private List<CourseOption> courses = new ArrayList<>();

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class CourseOption {
        private Long id;
        private String name;
    }
}
