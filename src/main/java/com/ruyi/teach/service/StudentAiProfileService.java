package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.LearningEventMapper;
import com.ruyi.teach.mapper.StudentKnowledgeMasteryMapper;
import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;
import com.ruyi.teach.model.dto.TutorChatRequest;
import com.ruyi.teach.model.entity.LearningEvent;
import com.ruyi.teach.model.entity.StudentKnowledgeMastery;
import com.ruyi.teach.model.entity.StudentLearningPreference;
import com.ruyi.teach.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 将数字人和 AI 助教提问沉淀到现有学习行为、长期学习画像中。
 */
@Service
public class StudentAiProfileService {

    private static final int MAX_RECENT_QUESTIONS = 12;
    private static final int MAX_STORED_QUESTION_LENGTH = 2000;
    private static final List<String> TOPIC_TERMS = List.of(
            "计算机网络", "操作系统", "数据结构", "软件工程", "人工智能",
            "循环链表", "双向链表", "单链表", "二叉树", "哈希表", "散列表",
            "时间复杂度", "空间复杂度", "数据库", "递归", "链表", "顺序表",
            "线性表", "队列", "栈", "数组", "排序", "查找", "图", "树",
            "Java", "Python", "MySQL", "Redis"
    );

    private final LearningEventMapper learningEventMapper;
    private final StudentLearningPreferenceMapper preferenceMapper;
    private final StudentKnowledgeMasteryMapper masteryMapper;
    private final StudentLearningContextService learningContextService;
    private final ObjectMapper objectMapper;

    public StudentAiProfileService(LearningEventMapper learningEventMapper,
                                   StudentLearningPreferenceMapper preferenceMapper,
                                   StudentKnowledgeMasteryMapper masteryMapper,
                                   StudentLearningContextService learningContextService,
                                   ObjectMapper objectMapper) {
        this.learningEventMapper = learningEventMapper;
        this.preferenceMapper = preferenceMapper;
        this.masteryMapper = masteryMapper;
        this.learningContextService = learningContextService;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordQuestion(User student, TutorChatRequest request) {
        recordQuestion(student, request, Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordQuestion(User student,
                               TutorChatRequest request,
                               Map<String, Object> trustedMetadata) {
        requireStudent(student);
        if (request == null || StringUtils.isBlank(request.getMessage())) {
            return;
        }

        String question = limit(StringUtils.normalizeSpace(request.getMessage()), MAX_STORED_QUESTION_LENGTH);
        Map<String, Object> context = request.getContext() == null ? Map.of() : request.getContext();
        Long courseId = request.getCourseId() == null ? readLong(context.get("courseId")) : request.getCourseId();
        Long chapterId = readLong(context.get("chapterId"));
        String source = StringUtils.defaultIfBlank(request.getSource(), "ai_assistant");
        String knowledgeName = resolveKnowledgeName(question, context);
        Date now = new Date();

        LearningEvent event = new LearningEvent();
        event.setStudentId(student.getId());
        event.setClassId(student.getClassId());
        event.setCourseId(courseId);
        event.setChapterId(chapterId);
        event.setResourceType(source);
        event.setKnowledgeName(knowledgeName);
        event.setEventType("ai_question");
        event.setDurationSecond(0);
        event.setExtraJson(buildEventExtraJson(question, request, context, trustedMetadata));
        event.setEventTime(now);
        event.setCreateTime(now);
        event.setIsDelete(0);
        learningEventMapper.insert(event);

        StudentLearningPreference preference = learningContextService.getOrCreateGeneralPreference(student.getId());
        ObjectNode profile = readProfile(preference.getAiProfileJson());
        updateCount(profile.withObject("/topicCounts"), knowledgeName);
        updateCount(profile.withObject("/sourceCounts"), source);
        updateRecentQuestions(profile, question, knowledgeName, source, courseId, chapterId, now);
        profile.put("version", 1);

        preference.setAiQuestionCount(defaultZero(preference.getAiQuestionCount()) + 1);
        preference.setAiProfileJson(writeJson(profile));
        preference.setAiProfileSummary(buildSummary(profile));
        preference.setLastAiQuestionTime(now);
        preference.setUpdateTime(now);
        preferenceMapper.updateById(preference);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildTutorProfileContext(User student) {
        requireStudent(student);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentName", StringUtils.defaultIfBlank(student.getUserName(), "同学"));

        StudentLearningPreference preference = learningContextService.findGeneralPreference(student.getId());
        if (preference != null) {
            putIfNotBlank(result, "universityName", preference.getUniversityName());
            putIfNotBlank(result, "developmentGoal", preference.getDevelopmentGoal());
            putIfNotBlank(result, "learningPreference", preference.getDominantType());
            putIfNotBlank(result, "learningProfileSummary", preference.getSummary());
            putIfNotBlank(result, "aiQuestionProfile", preference.getAiProfileSummary());
            result.put("aiQuestionCount", defaultZero(preference.getAiQuestionCount()));
        }

        List<StudentKnowledgeMastery> weakPoints = masteryMapper.selectList(
                new LambdaQueryWrapper<StudentKnowledgeMastery>()
                        .eq(StudentKnowledgeMastery::getStudentId, student.getId())
                        .eq(StudentKnowledgeMastery::getIsDelete, 0)
                        .orderByAsc(StudentKnowledgeMastery::getMasteryScore)
                        .last("limit 5")
        );
        if (!weakPoints.isEmpty()) {
            result.put("weakKnowledgePoints", weakPoints.stream().map(item -> Map.of(
                    "knowledgeName", StringUtils.defaultIfBlank(item.getKnowledgeName(), "相关知识点"),
                    "masteryScore", defaultZero(item.getMasteryScore()),
                    "evidence", StringUtils.defaultString(item.getEvidenceSummary())
            )).toList());
        }
        return result;
    }

    public String buildWelcomeText(User user) {
        String name = user == null ? "" : StringUtils.normalizeSpace(StringUtils.defaultString(user.getUserName()));
        String salutation = StringUtils.isBlank(name) ? "同学" : name + (name.endsWith("同学") ? "" : "同学");
        return salutation + "您好呀，欢迎使用智慧教学平台呀，有什么可以帮您？";
    }

    private ObjectNode readProfile(String json) {
        if (StringUtils.isBlank(json)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node instanceof ObjectNode objectNode ? objectNode : objectMapper.createObjectNode();
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private void updateCount(ObjectNode counts, String key) {
        String safeKey = StringUtils.defaultIfBlank(key, "综合学习问题");
        counts.put(safeKey, counts.path(safeKey).asInt(0) + 1);
    }

    private void updateRecentQuestions(ObjectNode profile,
                                       String question,
                                       String knowledgeName,
                                       String source,
                                       Long courseId,
                                       Long chapterId,
                                       Date askedAt) {
        ArrayNode oldQuestions = profile.path("recentQuestions") instanceof ArrayNode array
                ? array : objectMapper.createArrayNode();
        ArrayNode recentQuestions = objectMapper.createArrayNode();
        ObjectNode current = objectMapper.createObjectNode();
        current.put("question", question);
        current.put("knowledgeName", knowledgeName);
        current.put("source", source);
        if (courseId != null) current.put("courseId", courseId);
        if (chapterId != null) current.put("chapterId", chapterId);
        current.put("askedAt", askedAt.getTime());
        recentQuestions.add(current);
        for (JsonNode oldQuestion : oldQuestions) {
            if (recentQuestions.size() >= MAX_RECENT_QUESTIONS) break;
            recentQuestions.add(oldQuestion);
        }
        profile.set("recentQuestions", recentQuestions);
    }

    private String buildSummary(ObjectNode profile) {
        List<Map.Entry<String, Integer>> topics = new ArrayList<>();
        profile.path("topicCounts").fields().forEachRemaining(entry ->
                topics.add(Map.entry(entry.getKey(), entry.getValue().asInt(0))));
        topics.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()));
        if (topics.isEmpty()) {
            return "";
        }
        String joined = topics.stream().limit(5)
                .map(entry -> entry.getKey() + "（" + entry.getValue() + "次）")
                .reduce((left, right) -> left + "、" + right)
                .orElse("");
        return limit("近期通过数字人或 AI 助教主要询问：" + joined + "。", 1000);
    }

    private String resolveKnowledgeName(String question, Map<String, Object> context) {
        for (String key : List.of("chapterTitle", "problemTitle")) {
            String value = text(context.get(key));
            if (StringUtils.isNotBlank(value)) {
                return limit(value, 120);
            }
        }
        String lowerQuestion = StringUtils.defaultString(question).toLowerCase(Locale.ROOT);
        for (String term : TOPIC_TERMS) {
            if (lowerQuestion.contains(term.toLowerCase(Locale.ROOT))) {
                return term;
            }
        }
        String courseName = text(context.get("courseName"));
        return StringUtils.isBlank(courseName) ? "综合学习问题" : limit(courseName, 120);
    }

    private String buildEventExtraJson(String question,
                                       TutorChatRequest request,
                                       Map<String, Object> context,
                                       Map<String, Object> trustedMetadata) {
        ObjectNode extra = objectMapper.createObjectNode();
        extra.put("question", question);
        extra.put("mode", StringUtils.defaultIfBlank(request.getMode(), "explain"));
        extra.put("source", StringUtils.defaultIfBlank(request.getSource(), "ai_assistant"));
        putJsonText(extra, "pageTitle", context.get("pageTitle"));
        putJsonText(extra, "courseName", context.get("courseName"));
        putJsonText(extra, "chapterTitle", context.get("chapterTitle"));
        if (trustedMetadata != null && !trustedMetadata.isEmpty()) {
            trustedMetadata.forEach((key, value) -> {
                if (StringUtils.isNotBlank(key) && value != null) {
                    extra.set(key, objectMapper.valueToTree(value));
                }
            });
        }
        return writeJson(extra);
    }

    private void putJsonText(ObjectNode target, String field, Object value) {
        String text = text(value);
        if (StringUtils.isNotBlank(text)) target.put(field, limit(text, 200));
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) target.put(key, value);
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存 AI 学习画像失败");
        }
    }

    private Long readLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try {
            String text = StringUtils.trimToEmpty(String.valueOf(value));
            return text.isEmpty() ? null : Long.parseLong(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String text(Object value) {
        return value == null ? "" : StringUtils.normalizeSpace(String.valueOf(value));
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String limit(String value, int maxLength) {
        String safe = StringUtils.defaultString(value);
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private void requireStudent(User student) {
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (!"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可使用学习助教画像");
        }
    }
}
