package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.mapper.LearningEventMapper;
import com.ruyi.teach.mapper.StudentKnowledgeMasteryMapper;
import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;
import com.ruyi.teach.mapper.StudentResourceRecommendationMapper;
import com.ruyi.teach.mapper.TextCourseMapper;
import com.ruyi.teach.model.dto.learning.RecommendationGenerateRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.LearningEvent;
import com.ruyi.teach.model.entity.StudentKnowledgeMastery;
import com.ruyi.teach.model.entity.StudentLearningPreference;
import com.ruyi.teach.model.entity.StudentResourceRecommendation;
import com.ruyi.teach.model.entity.TextCourse;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.RecommendationGenerateVO;
import com.ruyi.teach.model.vo.StudentLearningProfileVO;
import com.ruyi.teach.service.RecommendationService;
import com.ruyi.teach.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private UserService userService;

    @Resource
    private StudentKnowledgeMasteryMapper masteryMapper;

    @Resource
    private StudentLearningPreferenceMapper preferenceMapper;

    @Resource
    private StudentResourceRecommendationMapper recommendationMapper;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private LearningEventMapper learningEventMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private TextCourseMapper textCourseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecommendationGenerateVO generateRecommendations(RecommendationGenerateRequest request, User viewer) {
        if (viewer == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (request == null || request.getStudentId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "studentId 不能为空");
        }
        User student = userService.getById(request.getStudentId());
        if (student == null || !"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学生不存在");
        }
        if (!"admin".equals(viewer.getUserRole())
                && !"teacher".equals(viewer.getUserRole())
                && !Objects.equals(viewer.getId(), request.getStudentId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权生成该学生推荐");
        }

        StudentLearningPreference preference = findPreference(request.getStudentId(), request.getCourseId());
        List<StudentKnowledgeMastery> weakPoints = listWeakPoints(request);
        List<AiResource> resources = listCandidateResources(preference);
        List<StudentResourceRecommendation> generated = new ArrayList<>();
        cleanupMismatchedRecommendations(request, resources, weakPoints);

        Date now = new Date();
        for (StudentKnowledgeMastery weakPoint : weakPoints) {
            AiResource resource = pickResource(resources, weakPoint, preference, generated);
            StudentResourceRecommendation existing = findExisting(request.getStudentId(),
                    resource == null ? null : resource.getId(),
                    weakPoint.getKnowledgeName());
            StudentResourceRecommendation item = existing == null ? new StudentResourceRecommendation() : existing;
            item.setStudentId(request.getStudentId());
            item.setCourseId(weakPoint.getCourseId() == null ? request.getCourseId() : weakPoint.getCourseId());
            item.setChapterId(weakPoint.getChapterId());
            item.setResourceId(resource == null ? null : resource.getId());
            item.setResourceType(resource == null
                    ? "review_task"
                    : StringUtils.defaultIfBlank(resource.getType(), resource.getSourceType()));
            item.setResourceTitle(resource == null ? buildFallbackTaskTitle(weakPoint) : resource.getTitle());
            item.setKnowledgeName(weakPoint.getKnowledgeName());
            item.setRecommendationReason(buildReason(weakPoint, preference, resource));
            item.setPracticeSuggestion(buildPracticeSuggestion(weakPoint, preference));
            item.setRecommendationSource(StringUtils.defaultIfBlank(item.getRecommendationSource(), "profile"));
            item.setStatus(StringUtils.defaultIfBlank(item.getStatus(), "pending"));
            item.setUpdateTime(now);
            item.setIsDelete(0);
            if (item.getId() == null) {
                item.setCreateTime(now);
                recommendationMapper.insert(item);
            } else {
                recommendationMapper.updateById(item);
            }
            generated.add(item);
            if (generated.size() >= 5) {
                break;
            }
        }

        RecommendationGenerateVO vo = new RecommendationGenerateVO();
        vo.setRecommendations(generated.stream().map(this::toVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRecommendationStatus(Long recommendationId, String status, User student) {
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (!"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可操作自己的补强任务");
        }
        if (recommendationId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "recommendationId 不能为空");
        }
        if (!"completed".equals(status) && !"pending".equals(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务状态不合法");
        }

        StudentResourceRecommendation recommendation = recommendationMapper.selectById(recommendationId);
        if (recommendation == null || defaultZero(recommendation.getIsDelete()) == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "补强任务不存在");
        }
        if (!Objects.equals(recommendation.getStudentId(), student.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该补强任务");
        }

        recommendation.setStatus(status);
        recommendation.setUpdateTime(new Date());
        recommendationMapper.updateById(recommendation);

        LearningEvent event = new LearningEvent();
        event.setStudentId(student.getId());
        event.setClassId(student.getClassId());
        event.setCourseId(recommendation.getCourseId());
        event.setChapterId(recommendation.getChapterId());
        event.setResourceId(recommendation.getResourceId());
        event.setResourceType(recommendation.getResourceType());
        event.setKnowledgeName(recommendation.getKnowledgeName());
        event.setEventType("completed".equals(status) ? "wrong_question_review" : "resource_click");
        event.setExtraJson("{\"recommendationId\":" + recommendation.getId() + ",\"status\":\"" + status + "\"}");
        event.setEventTime(new Date());
        event.setCreateTime(new Date());
        event.setIsDelete(0);
        learningEventMapper.insert(event);
        if ("completed".equals(status)) {
            improveMasteryAfterRecommendationCompleted(recommendation);
        }
        return true;
    }

    private void improveMasteryAfterRecommendationCompleted(StudentResourceRecommendation recommendation) {
        if (recommendation == null || StringUtils.isBlank(recommendation.getKnowledgeName())) {
            return;
        }
        LambdaQueryWrapper<StudentKnowledgeMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentKnowledgeMastery::getStudentId, recommendation.getStudentId())
                .eq(StudentKnowledgeMastery::getKnowledgeName, recommendation.getKnowledgeName())
                .eq(StudentKnowledgeMastery::getIsDelete, 0);
        if (recommendation.getCourseId() == null) {
            wrapper.isNull(StudentKnowledgeMastery::getCourseId);
        } else {
            wrapper.eq(StudentKnowledgeMastery::getCourseId, recommendation.getCourseId());
        }
        if (recommendation.getChapterId() == null) {
            wrapper.isNull(StudentKnowledgeMastery::getChapterId);
        } else {
            wrapper.eq(StudentKnowledgeMastery::getChapterId, recommendation.getChapterId());
        }
        wrapper.last("limit 1");
        StudentKnowledgeMastery mastery = masteryMapper.selectOne(wrapper);
        if (mastery == null) {
            return;
        }
        int nextScore = Math.min(85, defaultZero(mastery.getMasteryScore()) + 8);
        mastery.setMasteryScore(nextScore);
        mastery.setStatus(nextScore >= 80 ? "mastered" : nextScore >= 60 ? "partial" : "not_mastered");
        mastery.setEvidenceSummary(StringUtils.defaultIfBlank(mastery.getEvidenceSummary(), "已完成一次推荐资源学习")
                + "；已完成推荐资源“" + StringUtils.defaultIfBlank(recommendation.getResourceTitle(), recommendation.getKnowledgeName()) + "”。");
        mastery.setLastEvidenceTime(new Date());
        mastery.setUpdateTime(new Date());
        masteryMapper.updateById(mastery);
    }

    private StudentLearningPreference findPreference(Long studentId, Long courseId) {
        LambdaQueryWrapper<StudentLearningPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentLearningPreference::getStudentId, studentId);
        if (courseId == null) {
            wrapper.isNull(StudentLearningPreference::getCourseId);
        } else {
            wrapper.eq(StudentLearningPreference::getCourseId, courseId);
        }
        wrapper.last("limit 1");
        return preferenceMapper.selectOne(wrapper);
    }

    private List<StudentKnowledgeMastery> listWeakPoints(RecommendationGenerateRequest request) {
        LambdaQueryWrapper<StudentKnowledgeMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentKnowledgeMastery::getStudentId, request.getStudentId())
                .eq(StudentKnowledgeMastery::getIsDelete, 0);
        if (request.getCourseId() != null) {
            wrapper.and(w -> w.eq(StudentKnowledgeMastery::getCourseId, request.getCourseId()).or().isNull(StudentKnowledgeMastery::getCourseId));
        }
        if (request.getChapterId() != null) {
            wrapper.eq(StudentKnowledgeMastery::getChapterId, request.getChapterId());
        }
        wrapper.orderByAsc(StudentKnowledgeMastery::getMasteryScore).last("limit 10");
        return masteryMapper.selectList(wrapper).stream()
                .filter(item -> item.getMasteryScore() == null || item.getMasteryScore() < 80)
                .collect(Collectors.toList());
    }

    private List<AiResource> listCandidateResources(StudentLearningPreference preference) {
        LambdaQueryWrapper<AiResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiResource::getIsDelete, 0)
                .and(w -> w.eq(AiResource::getIsPublished, 1).or().isNull(AiResource::getIsPublished))
                .orderByDesc(AiResource::getCreateTime)
                .last("limit 80");
        List<AiResource> resources = aiResourceMapper.selectList(wrapper);
        String preferred = preference == null ? "" : preference.getDominantType();
        resources.sort(Comparator.comparingInt((AiResource resource) -> preferenceScore(resource, preferred)).reversed());
        return resources;
    }

    private int preferenceScore(AiResource resource, String preferred) {
        String type = StringUtils.defaultIfBlank(resource.getType(), resource.getSourceType()).toLowerCase();
        return switch (preferred) {
            case "practice" -> "quiz".equals(type) || "practice".equals(type) ? 10 : 0;
            case "text" -> "plan".equals(type) || "analysis".equals(type) ? 10 : 0;
            case "video" -> "video".equals(type) || "micro_video".equals(type) ? 10 : 0;
            case "ai" -> "analysis".equals(type) ? 8 : 0;
            default -> 0;
        };
    }

    private AiResource pickResource(List<AiResource> resources,
                                    StudentKnowledgeMastery weakPoint,
                                    StudentLearningPreference preference,
                                    List<StudentResourceRecommendation> generated) {
        String knowledge = StringUtils.defaultString(weakPoint.getKnowledgeName());
        List<Long> usedIds = generated.stream().map(StudentResourceRecommendation::getResourceId).collect(Collectors.toList());
        return resources.stream()
                .filter(resource -> resource.getId() != null && !usedIds.contains(resource.getId()))
                .map(resource -> new ResourceCandidate(resource, knowledgeMatchScore(resource, knowledge)))
                .filter(candidate -> candidate.score > 0)
                .sorted(Comparator
                        .comparingInt((ResourceCandidate candidate) -> candidate.score * 100
                                + preferenceScore(candidate.resource, preference == null ? "" : preference.getDominantType()))
                        .reversed())
                .map(candidate -> candidate.resource)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesKnowledge(AiResource resource, String knowledge) {
        return knowledgeMatchScore(resource, knowledge) > 0;
    }

    private int knowledgeMatchScore(AiResource resource, String knowledge) {
        if (StringUtils.isBlank(knowledge)) {
            return 1;
        }
        String normalizedKnowledge = normalizeSearchText(knowledge);
        String normalizedTitle = normalizeSearchText(resource.getTitle());
        String normalizedHaystack = normalizeSearchText(resource.getTitle()) + " "
                + StringUtils.defaultString(resource.getContent()) + " "
                + StringUtils.defaultString(resource.getParamsJson());
        normalizedHaystack = normalizeSearchText(normalizedHaystack);

        if (StringUtils.isBlank(normalizedKnowledge)) {
            return 1;
        }
        if (normalizedHaystack.contains(normalizedKnowledge)) {
            return 5;
        }
        if (StringUtils.isNotBlank(normalizedTitle)
                && normalizedKnowledge.length() >= normalizedTitle.length()
                && normalizedTitle.length() >= 4
                && normalizedKnowledge.contains(normalizedTitle)) {
            return 4;
        }

        Set<String> tokens = knowledgeTokens(normalizedKnowledge);
        if (tokens.isEmpty()) {
            return 0;
        }
        int score = 0;
        for (String token : tokens) {
            if (normalizedTitle.contains(token)) {
                score += 2;
            } else if (normalizedHaystack.contains(token)) {
                score += 1;
            }
        }
        return score;
    }

    private String normalizeSearchText(String value) {
        return StringUtils.defaultString(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。、“”‘’（）《》【】：；！？·-]+", "");
    }

    private Set<String> knowledgeTokens(String normalizedKnowledge) {
        Set<String> tokens = new LinkedHashSet<>();
        java.util.regex.Matcher asciiMatcher = java.util.regex.Pattern
                .compile("[a-z0-9]{2,}")
                .matcher(normalizedKnowledge);
        while (asciiMatcher.find()) {
            tokens.add(asciiMatcher.group());
        }
        if (normalizedKnowledge.length() >= 4) {
            tokens.add(normalizedKnowledge);
        }
        return tokens;
    }

    private void cleanupMismatchedRecommendations(RecommendationGenerateRequest request,
                                                  List<AiResource> resources,
                                                  List<StudentKnowledgeMastery> weakPoints) {
        if (weakPoints.isEmpty()) {
            return;
        }
        Set<String> weakNames = weakPoints.stream()
                .map(StudentKnowledgeMastery::getKnowledgeName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (weakNames.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<StudentResourceRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentResourceRecommendation::getStudentId, request.getStudentId())
                .eq(StudentResourceRecommendation::getIsDelete, 0)
                .in(StudentResourceRecommendation::getKnowledgeName, weakNames);
        if (request.getCourseId() != null) {
            wrapper.eq(StudentResourceRecommendation::getCourseId, request.getCourseId());
        }
        if (request.getChapterId() != null) {
            wrapper.eq(StudentResourceRecommendation::getChapterId, request.getChapterId());
        }

        List<StudentResourceRecommendation> existing = recommendationMapper.selectList(wrapper);
        if (existing.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (StudentResourceRecommendation item : existing) {
            if (item.getResourceId() == null) {
                continue;
            }
            AiResource resource = resources.stream()
                    .filter(candidate -> Objects.equals(candidate.getId(), item.getResourceId()))
                    .findFirst()
                    .orElseGet(() -> aiResourceMapper.selectById(item.getResourceId()));
            if (resource == null || !matchesKnowledge(resource, item.getKnowledgeName())) {
                item.setIsDelete(1);
                item.setUpdateTime(now);
                recommendationMapper.updateById(item);
            }
        }
    }

    private StudentResourceRecommendation findExisting(Long studentId, Long resourceId, String knowledgeName) {
        LambdaQueryWrapper<StudentResourceRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentResourceRecommendation::getStudentId, studentId)
                .eq(StudentResourceRecommendation::getKnowledgeName, knowledgeName)
                .eq(StudentResourceRecommendation::getIsDelete, 0)
                .last("limit 1");
        if (resourceId == null) {
            wrapper.isNull(StudentResourceRecommendation::getResourceId);
        } else {
            wrapper.eq(StudentResourceRecommendation::getResourceId, resourceId);
        }
        return recommendationMapper.selectOne(wrapper);
    }

    private String buildReason(StudentKnowledgeMastery weakPoint, StudentLearningPreference preference, AiResource resource) {
        String dominant = preference == null ? "balanced" : StringUtils.defaultIfBlank(preference.getDominantType(), "balanced");
        String resourceText = resource == null ? "暂未找到完全匹配的资源，先安排一项复习任务。" : "已匹配到相关资源。";
        return "薄弱知识点“" + weakPoint.getKnowledgeName() + "”当前掌握度 "
                + (weakPoint.getMasteryScore() == null ? "--" : weakPoint.getMasteryScore())
                + "%，结合学生偏好“" + dominant + "”。" + resourceText;
    }

    private String buildFallbackTaskTitle(StudentKnowledgeMastery weakPoint) {
        return "复习任务：" + StringUtils.defaultIfBlank(weakPoint.getKnowledgeName(), "当前薄弱知识点");
    }

    private String buildPracticeSuggestion(StudentKnowledgeMastery weakPoint, StudentLearningPreference preference) {
        String dominant = preference == null ? "" : preference.getDominantType();
        if ("video".equals(dominant)) {
            return "先看 5-10 分钟讲解视频，再完成 3 道基础变式题。";
        }
        if ("text".equals(dominant)) {
            return "先阅读知识点笔记和例题解析，再整理一条错因说明。";
        }
        if ("discussion".equals(dominant)) {
            return "先查看讨论区问答，再用自己的话复述解题关键。";
        }
        return "完成资源学习后进行一次错题回练，并复查该知识点掌握度。";
    }

    private StudentLearningProfileVO.RecommendationItem toVO(StudentResourceRecommendation item) {
        StudentLearningProfileVO.RecommendationItem vo = new StudentLearningProfileVO.RecommendationItem();
        vo.setId(item.getId());
        vo.setCourseId(item.getCourseId());
        vo.setResourceId(item.getResourceId());
        vo.setResourceType(item.getResourceType());
        vo.setResourceTitle(item.getResourceTitle());
        vo.setKnowledgeName(item.getKnowledgeName());
        vo.setRecommendationReason(item.getRecommendationReason());
        vo.setPracticeSuggestion(item.getPracticeSuggestion());
        vo.setRecommendationSource(item.getRecommendationSource());
        vo.setStatus(item.getStatus());
        fillResourceDisplay(vo, item);
        fillAction(vo, item);
        return vo;
    }

    private void fillResourceDisplay(StudentLearningProfileVO.RecommendationItem vo, StudentResourceRecommendation item) {
        String type = StringUtils.defaultString(item.getResourceType()).toLowerCase();
        if (("text".equals(type) || "tutorial".equals(type)) && item.getResourceId() != null) {
            TextCourse textCourse = textCourseMapper.selectById(item.getResourceId());
            if (textCourse != null) {
                vo.setCourseName(textCourse.getName());
                vo.setCoverImg(textCourse.getCoverImg());
                vo.setResourceTitle(StringUtils.defaultIfBlank(textCourse.getName(), vo.getResourceTitle()));
            }
            return;
        }

        if (isAiResourceType(type) && item.getResourceId() != null) {
            AiResource resource = aiResourceMapper.selectById(item.getResourceId());
            if (resource != null) {
                vo.setCourseName(resource.getTitle());
                vo.setCoverImg(readJsonText(resource.getParamsJson(), "coverUrl"));
                vo.setResourceTitle(StringUtils.defaultIfBlank(resource.getTitle(), vo.getResourceTitle()));
            }
            return;
        }

        Long courseId = item.getCourseId();
        if (courseId == null && item.getResourceId() != null && type.contains("video")) {
            courseId = item.getResourceId();
        }
        if (courseId == null) {
            return;
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return;
        }
        vo.setCourseId(course.getId());
        vo.setCourseName(course.getName());
        vo.setCoverImg(course.getCoverImg());
        vo.setResourceTitle(StringUtils.defaultIfBlank(course.getName(), vo.getResourceTitle()));
    }

    private boolean isAiResourceType(String type) {
        return "micro_video".equals(type)
                || "quiz".equals(type)
                || "anim".equals(type)
                || "plan".equals(type)
                || "review_task".equals(type);
    }

    private String readJsonText(String json, String field) {
        if (StringUtils.isBlank(json)) {
            return "";
        }
        try {
            return OBJECT_MAPPER.readTree(json).path(field).asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private void fillAction(StudentLearningProfileVO.RecommendationItem vo, StudentResourceRecommendation item) {
        String type = StringUtils.defaultString(item.getResourceType()).toLowerCase();
        if (("text".equals(type) || "tutorial".equals(type)) && item.getResourceId() != null) {
            vo.setActionType("tutorial_read");
            vo.setActionUrl("/student/tutorial/" + item.getResourceId() + "/read");
            vo.setActionLabel("阅读教程");
            vo.setShortReason(buildShortReason(item));
            return;
        }
        if (item.getCourseId() != null) {
            StringBuilder url = new StringBuilder("/learn/").append(item.getCourseId()).append("?");
            if (item.getChapterId() != null) {
                url.append("chapterId=").append(item.getChapterId()).append("&");
            }
            url.append("from=diagnosis");
            if (item.getId() != null) {
                url.append("&taskId=").append(item.getId());
            }
            vo.setActionType("course_learn");
            vo.setActionUrl(url.toString());
            vo.setActionLabel(resolveActionLabel(item.getResourceType()));
        } else {
            String keyword = StringUtils.defaultIfBlank(item.getKnowledgeName(), item.getResourceTitle());
            vo.setActionType("search");
            vo.setActionUrl("/student/search?keyword=" + URLEncoder.encode(StringUtils.defaultString(keyword), StandardCharsets.UTF_8));
            vo.setActionLabel("找相关课");
        }
        vo.setShortReason(buildShortReason(item));
    }

    private String resolveActionLabel(String resourceType) {
        String type = StringUtils.defaultString(resourceType).toLowerCase();
        if (type.contains("text") || type.contains("tutorial")) {
            return "阅读教程";
        }
        if (type.contains("practice") || type.contains("quiz") || type.contains("review")) {
            return "去练习";
        }
        if (type.contains("discussion")) {
            return "去看问答";
        }
        return "去完成";
    }

    private String buildShortReason(StudentResourceRecommendation item) {
        String type = StringUtils.defaultString(item.getResourceType()).toLowerCase();
        String reason = StringUtils.defaultString(item.getRecommendationReason());
        if (reason.contains("错") || reason.toLowerCase().contains("wrong")) {
            return "最近这类题错得多";
        }
        if (type.contains("video")) {
            return "先看短讲解，再做几道题";
        }
        if (type.contains("discussion")) {
            return "先看问答，把关键点说清楚";
        }
        if (type.contains("review")) {
            return "先做一次小复习";
        }
        if (item.getChapterId() != null) {
            return "这章需要再补一小步";
        }
        return "先把这个知识点补稳";
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static class ResourceCandidate {
        private final AiResource resource;
        private final int score;

        private ResourceCandidate(AiResource resource, int score) {
            this.resource = resource;
            this.score = score;
        }
    }
}
