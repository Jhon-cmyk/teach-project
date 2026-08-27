package com.ruyi.teach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the transport-format parsing used by homework submissions.
 * Domain-specific paper/answer matching remains in the homework service for now.
 */
@Component
public class AnswerParser {

    private static final Logger log = LoggerFactory.getLogger(AnswerParser.class);

    private final ObjectMapper objectMapper;

    public AnswerParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean hasImagePayload(HomeworkSubmitRequest request) {
        if (request == null) {
            return false;
        }
        if (request.getWholePaperImageUrls() != null
                && request.getWholePaperImageUrls().stream().anyMatch(StringUtils::isNotBlank)) {
            return true;
        }
        if (request.getQuestionImageItems() == null) {
            return false;
        }
        Set<String> allowedQuestionNos = parseImageAnswerQuestionNos(request.getStudentAnswerJson());
        return request.getQuestionImageItems().stream()
                .anyMatch(item -> item != null
                        && allowedQuestionNos.contains(StringUtils.trimToEmpty(item.getQuestionNo()))
                        && item.getImageUrls() != null
                        && item.getImageUrls().stream().anyMatch(StringUtils::isNotBlank));
    }

    public Set<String> parseImageAnswerQuestionNos(String studentAnswerJson) {
        if (StringUtils.isBlank(studentAnswerJson)) {
            return Collections.emptySet();
        }
        try {
            JsonNode root = objectMapper.readTree(studentAnswerJson);
            if (!root.isArray()) {
                return Collections.emptySet();
            }
            Set<String> result = new HashSet<>();
            for (JsonNode item : root) {
                String type = item.path("type").asText("");
                String questionNo = item.path("imageKey").asText(item.path("num").asText(""));
                if (StringUtils.isNotBlank(questionNo) && ("fill".equals(type) || "text".equals(type))) {
                    result.add(questionNo.trim());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析图片题号失败，忽略逐题图片", e);
            return Collections.emptySet();
        }
    }

    public List<Map<String, Object>> prepareAnswerItems(String studentAnswerJson,
                                                        Map<String, List<String>> imageUrlsByQuestion) {
        List<Map<String, Object>> answers = new ArrayList<>();
        if (StringUtils.isNotBlank(studentAnswerJson)) {
            try {
                List<Map<String, Object>> parsed = objectMapper.readValue(
                        studentAnswerJson,
                        new TypeReference<List<Map<String, Object>>>() {
                        }
                );
                if (parsed != null) {
                    for (Map<String, Object> item : parsed) {
                        answers.add(new LinkedHashMap<>(item));
                    }
                }
            } catch (Exception e) {
                Map<String, Object> fallback = new LinkedHashMap<>();
                fallback.put("num", "1");
                fallback.put("type", "text");
                fallback.put("stem", "全文作答");
                fallback.put("answer", studentAnswerJson);
                answers.add(fallback);
            }
        }

        if (imageUrlsByQuestion != null && !imageUrlsByQuestion.isEmpty()) {
            Set<String> matchedKeys = new HashSet<>();
            for (Map<String, Object> item : answers) {
                String key = answerQuestionKey(item);
                List<String> urls = imageUrlsByQuestion.get(key);
                if (urls == null) {
                    urls = imageUrlsByQuestion.get(
                            StringUtils.trimToEmpty(String.valueOf(item.getOrDefault("num", "")))
                    );
                }
                if (urls != null && !urls.isEmpty()) {
                    item.put("imageUrls", urls);
                    matchedKeys.add(key);
                }
            }
            for (Map.Entry<String, List<String>> entry : imageUrlsByQuestion.entrySet()) {
                if (matchedKeys.contains(entry.getKey())) {
                    continue;
                }
                Map<String, Object> imageOnly = new LinkedHashMap<>();
                imageOnly.put("num", entry.getKey());
                imageOnly.put("type", "text");
                imageOnly.put("stem", "图片作答");
                imageOnly.put("answer", "");
                imageOnly.put("imageUrls", entry.getValue());
                answers.add(imageOnly);
            }
        }
        return answers;
    }

    public boolean hasTextAnswer(Object answer) {
        if (answer == null) {
            return false;
        }
        if (answer instanceof Collection<?>) {
            return !((Collection<?>) answer).isEmpty();
        }
        return StringUtils.isNotBlank(String.valueOf(answer));
    }

    public boolean hasImageUrls(Object imageUrls) {
        if (imageUrls instanceof Collection<?>) {
            return !((Collection<?>) imageUrls).isEmpty();
        }
        return imageUrls != null && StringUtils.isNotBlank(String.valueOf(imageUrls));
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("答案数据无法序列化", e);
        }
    }

    public List<HomeworkSubmissionDetail> parseDetails(Long submissionId,
                                                       String studentAnswerJson,
                                                       String aiResponse) {
        if (StringUtils.isBlank(studentAnswerJson)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> answerList;
        try {
            answerList = objectMapper.readValue(
                    studentAnswerJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    }
            );
        } catch (Exception e) {
            HomeworkSubmissionDetail fallback = new HomeworkSubmissionDetail();
            fallback.setSubmissionId(submissionId);
            fallback.setQuestionNo("1");
            fallback.setQuestionType("text");
            fallback.setStudentAnswer(studentAnswerJson);
            return List.of(fallback);
        }

        Map<String, String> aiComments = extractPerQuestionComments(aiResponse);
        List<HomeworkSubmissionDetail> details = new ArrayList<>();
        for (Map<String, Object> item : answerList) {
            HomeworkSubmissionDetail detail = new HomeworkSubmissionDetail();
            detail.setSubmissionId(submissionId);
            String num = String.valueOf(item.getOrDefault("num", ""));
            detail.setQuestionNo(num);
            detail.setQuestionType(String.valueOf(item.getOrDefault("type", "")));
            detail.setStemSnapshot(String.valueOf(item.getOrDefault("stem", "")));
            detail.setStudentAnswer(String.valueOf(item.getOrDefault("answer", "")));
            setJsonValue(detail::setImageUrlsJson, item.get("imageUrls"));
            setStringValue(detail::setRecognizedText, item.get("recognizedText"));
            setDoubleValue(detail::setVisionConfidence, item.get("visionConfidence"));
            setStringValue(detail::setStandardAnswer, item.get("standardAnswer"));
            setIntegerValue(detail::setFullScore, item.get("fullScore"));
            setIntegerValue(detail::setScore, item.get("score"));
            setIntegerValue(detail::setAiSuggestedScore, item.get("aiSuggestedScore"));
            setIntegerValue(detail::setIsCorrect, item.get("isCorrect"));
            setStringValue(detail::setAiComment, item.get("aiComment"));
            if (detail.getScore() != null && detail.getIsCorrect() == null) {
                detail.setIsCorrect(detail.getScore() > 0 ? 1 : 0);
            }

            String comment = aiComments.get(num);
            if (comment != null) {
                detail.setAiComment(comment);
                extractDetailScore(comment, detail);
            }
            details.add(detail);
        }
        return details;
    }

    private String answerQuestionKey(Map<String, Object> item) {
        if (item == null) {
            return "";
        }
        Object imageKey = item.get("imageKey");
        if (imageKey != null && StringUtils.isNotBlank(String.valueOf(imageKey))) {
            return String.valueOf(imageKey).trim();
        }
        Object num = item.get("num");
        return num == null ? "" : String.valueOf(num).trim();
    }

    private Map<String, String> extractPerQuestionComments(String aiResponse) {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isBlank(aiResponse)) {
            return result;
        }
        Pattern pattern = Pattern.compile(
                "(?:(?:\\*\\*)?第\\s*(\\d+)\\s*题(?:\\*\\*)?[：:]?)([\\s\\S]*?)(?=(?:\\*\\*)?第\\s*\\d+\\s*题|## |$)"
        );
        Matcher matcher = pattern.matcher(aiResponse);
        while (matcher.find()) {
            String comment = matcher.group(2).trim();
            if (comment.length() > 500) {
                comment = comment.substring(0, 500) + "...";
            }
            result.put(matcher.group(1), comment);
        }
        return result;
    }

    private void extractDetailScore(String comment, HomeworkSubmissionDetail detail) {
        Matcher matcher = Pattern.compile("(?:得|扣后|该题)\\s*(\\d+)\\s*分").matcher(comment);
        if (matcher.find()) {
            int score = Integer.parseInt(matcher.group(1));
            detail.setScore(score);
            detail.setIsCorrect(score > 0 ? 1 : 0);
        } else if (comment.contains("正确") || comment.contains("满分")) {
            detail.setIsCorrect(1);
        } else if (comment.contains("错误") || comment.contains("0分") || comment.contains("零分")) {
            detail.setIsCorrect(0);
            detail.setScore(0);
        }
    }

    private void setJsonValue(java.util.function.Consumer<String> setter, Object value) {
        if (value != null) {
            setter.accept(toJson(value));
        }
    }

    private void setStringValue(java.util.function.Consumer<String> setter, Object value) {
        if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
            setter.accept(String.valueOf(value));
        }
    }

    private void setIntegerValue(java.util.function.Consumer<Integer> setter, Object value) {
        if (value == null) {
            return;
        }
        try {
            setter.accept(Integer.parseInt(String.valueOf(value)));
        } catch (NumberFormatException ignore) {
            // A malformed optional score must not discard the rest of the answer.
        }
    }

    private void setDoubleValue(java.util.function.Consumer<Double> setter, Object value) {
        if (value == null) {
            return;
        }
        try {
            setter.accept(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException ignore) {
            // A malformed optional confidence must not discard the rest of the answer.
        }
    }
}
