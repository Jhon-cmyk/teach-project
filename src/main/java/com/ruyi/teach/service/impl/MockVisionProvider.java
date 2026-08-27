package com.ruyi.teach.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.service.VisionProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "homework.vision.provider", havingValue = "mock", matchIfMissing = true)
public class MockVisionProvider implements VisionProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public List<RecognizedAnswer> recognizeHomeworkImages(VisionHomeworkRequest request) {
        List<RecognizedAnswer> result = new ArrayList<>();
        if (request == null || request.getImageGroups() == null) {
            return result;
        }

        int index = 1;
        for (ImageGroup group : request.getImageGroups()) {
            RecognizedAnswer answer = new RecognizedAnswer();
            String questionNo = StringUtils.defaultIfBlank(group.getQuestionNo(), String.valueOf(index));
            answer.setQuestionNo(questionNo);
            answer.setImageUrls(group.getImageUrls() == null ? new ArrayList<>() : new ArrayList<>(group.getImageUrls()));
            answer.setConfidence(0.60D);
            answer.setRecognizedText(buildMockText(questionNo, answer.getImageUrls()));
            try {
                answer.setRawJson(OBJECT_MAPPER.writeValueAsString(answer));
            } catch (Exception ignore) {
                answer.setRawJson("{}");
            }
            result.add(answer);
            index++;
        }
        return result;
    }

    private String buildMockText(String questionNo, List<String> imageUrls) {
        return "Mock vision result for question " + questionNo
                + ". Image count: " + (imageUrls == null ? 0 : imageUrls.size())
                + ". Please replace homework.vision.provider with a real vision provider in production.";
    }
}
