package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionDetailMapper;
import com.ruyi.teach.mapper.LearningEventMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.mapper.StudentKnowledgeMasteryMapper;
import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;
import com.ruyi.teach.mapper.StudentResourceRecommendationMapper;
import com.ruyi.teach.mapper.TextCourseMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.LearningEvent;
import com.ruyi.teach.model.entity.StudentKnowledgeMastery;
import com.ruyi.teach.model.entity.StudentLearningPreference;
import com.ruyi.teach.model.entity.StudentResourceRecommendation;
import com.ruyi.teach.model.entity.TextCourse;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.StudentLearningProfileVO;
import com.ruyi.teach.service.HomeworkSubmissionService;
import com.ruyi.teach.service.StudentProfileService;
import com.ruyi.teach.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StudentProfileServiceImpl implements StudentProfileService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private LearningEventMapper learningEventMapper;

    @Resource
    private StudentLearningPreferenceMapper preferenceMapper;

    @Resource
    private StudentKnowledgeMasteryMapper masteryMapper;

    @Resource
    private StudentResourceRecommendationMapper recommendationMapper;

    @Resource
    private HomeworkSubmissionService homeworkSubmissionService;

    @Resource
    private HomeworkSubmissionDetailMapper detailMapper;

    @Resource
    private HomeworkAssignmentMapper assignmentMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private TextCourseMapper textCourseMapper;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentLearningProfileVO getStudentLearningProfile(Long classId, Long studentId, Integer days, User viewer) {
        User student = requireViewAccess(classId, studentId, viewer);
        return buildProfile(student, days, null, null, viewer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentLearningProfileVO getSelfLearningProfile(Integer days, Long courseId, Long chapterId, User student) {
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (!"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可查看自己的学习诊断");
        }
        return buildProfile(student, days, courseId, chapterId, student);
    }

    private StudentLearningProfileVO buildProfile(User student, Integer days, Long courseId, Long chapterId, User viewer) {
        int safeDays = days == null || days <= 0 ? 7 : Math.min(days, 30);
        Date after = new Date(System.currentTimeMillis() - safeDays * 24L * 60L * 60L * 1000L);

        List<LearningEvent> events = listRecentEvents(student.getId(), after, courseId, chapterId);
        StudentLearningPreference preference = refreshPreference(student.getId(), courseId, events);
        refreshMastery(student, events, after);

        List<StudentKnowledgeMastery> masteryRecords = listMasteryRecords(student.getId(), courseId, chapterId);
        List<StudentLearningProfileVO.MasteryItem> weakPoints = listWeakPoints(student.getId(), courseId, chapterId);
        List<StudentLearningProfileVO.WrongQuestionItem> wrongQuestions = listWrongQuestions(student.getId(), after, courseId);
        List<StudentLearningProfileVO.RecommendationItem> recommendations = listRecommendations(student.getId(), courseId, chapterId);

        StudentLearningProfileVO vo = new StudentLearningProfileVO();
        vo.setDays(safeDays);
        vo.setPreference(toPreferenceSummary(preference));
        vo.setWeakPoints(weakPoints);
        vo.setWrongQuestions(wrongQuestions);
        vo.setRecommendations(recommendations);
        vo.setAdvices(buildAdvices(vo));
        enrichInsight(vo, events, masteryRecords);
        return vo;
    }

    private User requireViewAccess(Long classId, Long studentId, User viewer) {
        if (viewer == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (classId == null || studentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "classId 和 studentId 不能为空");
        }
        User student = userService.getById(studentId);
        if (student == null || !"student".equals(student.getUserRole()) || !Objects.equals(student.getClassId(), classId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学生不存在或不属于该班级");
        }
        if ("admin".equals(viewer.getUserRole())) {
            return student;
        }
        if ("teacher".equals(viewer.getUserRole())) {
            return student;
        }
        if ("student".equals(viewer.getUserRole()) && Objects.equals(viewer.getId(), studentId)) {
            return student;
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该学生画像");
    }

    private List<LearningEvent> listRecentEvents(Long studentId, Date after) {
        return listRecentEvents(studentId, after, null, null);
    }

    private List<LearningEvent> listRecentEvents(Long studentId, Date after, Long courseId, Long chapterId) {
        LambdaQueryWrapper<LearningEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningEvent::getStudentId, studentId)
                .eq(LearningEvent::getIsDelete, 0)
                .ge(LearningEvent::getEventTime, after);
        if (courseId != null) {
            wrapper.eq(LearningEvent::getCourseId, courseId);
        }
        if (chapterId != null) {
            wrapper.eq(LearningEvent::getChapterId, chapterId);
        }
        wrapper.orderByDesc(LearningEvent::getEventTime);
        return learningEventMapper.selectList(wrapper);
    }

    private StudentLearningPreference refreshPreference(Long studentId, Long courseId, List<LearningEvent> events) {
        ScoreBox score = new ScoreBox();
        for (LearningEvent event : events) {
            int weight = Math.max(1, defaultZero(event.getDurationSecond()) / 20 + 1);
            String type = StringUtils.defaultString(event.getEventType());
            String resourceType = StringUtils.defaultString(event.getResourceType()).toLowerCase();
            if (type.startsWith("video_") || "video".equals(resourceType)) {
                score.video += weight;
            } else if (type.startsWith("practice_") || "quiz".equals(resourceType) || "practice".equals(resourceType)) {
                score.practice += weight + 1;
            } else if (type.startsWith("comment_") || "discussion".equals(resourceType)) {
                score.discussion += weight;
            } else if ("ai_question".equals(type)) {
                score.ai += weight + 2;
            } else if ("text".equals(resourceType) || "article".equals(resourceType) || "plan".equals(resourceType)) {
                score.text += weight;
            } else if ("resource_click".equals(type)) {
                score.resource += weight;
            }
        }

        String dominant = resolveDominant(score);
        Date now = new Date();
        StudentLearningPreference preference = findPreference(studentId, courseId);
        if (preference == null) {
            preference = new StudentLearningPreference();
            preference.setStudentId(studentId);
            preference.setCourseId(courseId);
            preference.setCreateTime(now);
        }
        preference.setDominantType(dominant);
        preference.setVideoScore(score.video);
        preference.setTextScore(score.text);
        preference.setPracticeScore(score.practice);
        preference.setDiscussionScore(score.discussion);
        preference.setAiScore(score.ai);
        preference.setResourceScore(score.resource);
        preference.setSummary(buildPreferenceSummary(dominant, score));
        preference.setUpdateTime(now);
        if (preference.getId() == null) {
            preferenceMapper.insert(preference);
        } else {
            preferenceMapper.updateById(preference);
        }
        return preference;
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

    private String resolveDominant(ScoreBox score) {
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("video", score.video);
        values.put("practice", score.practice);
        values.put("discussion", score.discussion);
        values.put("ai", score.ai);
        values.put("text", score.text);
        values.put("resource", score.resource);
        return values.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse("balanced");
    }

    private String buildPreferenceSummary(String dominant, ScoreBox score) {
        return switch (dominant) {
            case "video" -> "更偏好视频讲解，可优先推送短视频和时间轴知识点复盘。";
            case "practice" -> "更偏好通过练习巩固，适合推送基础题、变式题和错题回练。";
            case "discussion" -> "更偏好评论和讨论区互动，适合推送带问答解析的资源。";
            case "ai" -> "经常向 AI 助教提问，适合用分步提示和自检问题干预。";
            case "text" -> "更偏好文字/图文资料，适合推送讲义、笔记和例题解析。";
            case "resource" -> "资源探索较活跃，可推送结构化资源清单。";
            default -> "暂无明显单一偏好，建议混合推送视频、文字和练习资源。";
        };
    }

    private void refreshMastery(User student, List<LearningEvent> events, Date after) {
        Map<String, MasteryEvidence> evidenceMap = new LinkedHashMap<>();
        for (LearningEvent event : events) {
            String knowledge = normalizeKnowledge(event.getKnowledgeName(), event.getChapterId());
            if (knowledge == null) {
                continue;
            }
            MasteryEvidence evidence = evidenceMap.computeIfAbsent(knowledge, k -> new MasteryEvidence());
            evidence.courseId = event.getCourseId();
            evidence.chapterId = event.getChapterId();
            evidence.knowledgeName = knowledge;
            evidence.lastTime = later(evidence.lastTime, event.getEventTime());
            if ("practice_submit".equals(event.getEventType())) {
                evidence.practiceCount++;
                if (event.getCorrect() != null && event.getCorrect() == 1) {
                    evidence.correctCount++;
                }
                if (event.getScore() != null) {
                    evidence.scoreTotal += event.getScore().intValue();
                    evidence.scoreCount++;
                }
            } else if ("video_pause".equals(event.getEventType())) {
                evidence.videoRisk += defaultZero(event.getDurationSecond()) >= 60 ? 2 : 1;
            } else if ("video_rewatch".equals(event.getEventType())) {
                evidence.videoRisk += 2;
            } else if ("ai_question".equals(event.getEventType())) {
                evidence.aiQuestions++;
            } else if ("wrong_question_review".equals(event.getEventType())) {
                evidence.reviewCount++;
            }
        }

        List<HomeworkSubmission> submissions = listRecentSubmissions(student.getId(), after);
        for (HomeworkSubmission submission : submissions) {
            HomeworkAssignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
            String knowledge = normalizeKnowledge(
                    assignment == null ? null : StringUtils.defaultIfBlank(assignment.getChapterTitleSnapshot(), assignment.getTitle()),
                    assignment == null ? null : assignment.getChapterId()
            );
            if (knowledge == null) {
                continue;
            }
            MasteryEvidence evidence = evidenceMap.computeIfAbsent(knowledge, k -> new MasteryEvidence());
            evidence.courseId = submission.getCourseId();
            evidence.chapterId = assignment == null ? null : assignment.getChapterId();
            evidence.knowledgeName = knowledge;
            evidence.lastTime = later(evidence.lastTime, submission.getSubmitTime());
            evidence.practiceCount += Math.max(1, defaultZero(submission.getCorrectCount()) + defaultZero(submission.getWrongCount()));
            evidence.correctCount += defaultZero(submission.getCorrectCount());
            evidence.wrongCount += defaultZero(submission.getWrongCount());
            if (submission.getTotalScore() != null) {
                evidence.scoreTotal += submission.getTotalScore();
                evidence.scoreCount++;
            }
        }

        for (MasteryEvidence evidence : evidenceMap.values()) {
            upsertMastery(student.getId(), evidence);
        }
    }

    private List<HomeworkSubmission> listRecentSubmissions(Long studentId, Date after) {
        LambdaQueryWrapper<HomeworkSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkSubmission::getStudentId, studentId)
                .eq(HomeworkSubmission::getIsDelete, 0)
                .ge(HomeworkSubmission::getSubmitTime, after)
                .orderByDesc(HomeworkSubmission::getSubmitTime);
        return homeworkSubmissionService.list(wrapper);
    }

    private void upsertMastery(Long studentId, MasteryEvidence evidence) {
        int score = evidence.resolveScore();
        String status = score >= 80 ? "mastered" : score >= 60 ? "partial" : "not_mastered";
        String summary = evidence.summary(score);

        LambdaQueryWrapper<StudentKnowledgeMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentKnowledgeMastery::getStudentId, studentId)
                .eq(StudentKnowledgeMastery::getKnowledgeName, evidence.knowledgeName)
                .eq(StudentKnowledgeMastery::getIsDelete, 0)
                .last("limit 1");
        StudentKnowledgeMastery mastery = masteryMapper.selectOne(wrapper);
        Date now = new Date();
        if (mastery == null) {
            mastery = new StudentKnowledgeMastery();
            mastery.setStudentId(studentId);
            mastery.setKnowledgeName(evidence.knowledgeName);
            mastery.setCreateTime(now);
            mastery.setIsDelete(0);
        }
        mastery.setCourseId(evidence.courseId);
        mastery.setChapterId(evidence.chapterId);
        mastery.setMasteryScore(score);
        mastery.setStatus(status);
        mastery.setEvidenceSummary(summary);
        mastery.setLastEvidenceTime(evidence.lastTime == null ? now : evidence.lastTime);
        mastery.setUpdateTime(now);
        if (mastery.getId() == null) {
            masteryMapper.insert(mastery);
        } else {
            masteryMapper.updateById(mastery);
        }
    }

    private List<StudentKnowledgeMastery> listMasteryRecords(Long studentId, Long courseId, Long chapterId) {
        LambdaQueryWrapper<StudentKnowledgeMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentKnowledgeMastery::getStudentId, studentId)
                .eq(StudentKnowledgeMastery::getIsDelete, 0);
        if (courseId != null) {
            wrapper.eq(StudentKnowledgeMastery::getCourseId, courseId);
        }
        if (chapterId != null) {
            wrapper.eq(StudentKnowledgeMastery::getChapterId, chapterId);
        }
        wrapper.orderByAsc(StudentKnowledgeMastery::getMasteryScore).last("limit 30");
        return masteryMapper.selectList(wrapper);
    }

    private List<StudentLearningProfileVO.MasteryItem> listWeakPoints(Long studentId, Long courseId, Long chapterId) {
        LambdaQueryWrapper<StudentKnowledgeMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentKnowledgeMastery::getStudentId, studentId)
                .eq(StudentKnowledgeMastery::getIsDelete, 0);
        if (courseId != null) {
            wrapper.eq(StudentKnowledgeMastery::getCourseId, courseId);
        }
        if (chapterId != null) {
            wrapper.eq(StudentKnowledgeMastery::getChapterId, chapterId);
        }
        wrapper.orderByAsc(StudentKnowledgeMastery::getMasteryScore).last("limit 8");
        return masteryMapper.selectList(wrapper).stream()
                .filter(item -> defaultZero(item.getMasteryScore()) < 80)
                .map(item -> {
                    StudentLearningProfileVO.MasteryItem vo = new StudentLearningProfileVO.MasteryItem();
                    vo.setKnowledgeName(item.getKnowledgeName());
                    vo.setCourseId(item.getCourseId());
                    vo.setChapterId(item.getChapterId());
                    vo.setMasteryScore(item.getMasteryScore());
                    vo.setStatus(item.getStatus());
                    vo.setEvidenceSummary(item.getEvidenceSummary());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<StudentLearningProfileVO.WrongQuestionItem> listWrongQuestions(Long studentId, Date after, Long courseId) {
        List<Long> submissionIds = listRecentSubmissions(studentId, after).stream()
                .filter(item -> courseId == null || Objects.equals(item.getCourseId(), courseId))
                .map(HomeworkSubmission::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (submissionIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<HomeworkSubmissionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(HomeworkSubmissionDetail::getSubmissionId, submissionIds)
                .eq(HomeworkSubmissionDetail::getIsCorrect, 0)
                .orderByDesc(HomeworkSubmissionDetail::getCreateTime)
                .last("limit 8");
        return detailMapper.selectList(wrapper).stream()
                .map(detail -> {
                    HomeworkSubmission submission = homeworkSubmissionService.getById(detail.getSubmissionId());
                    HomeworkAssignment assignment = submission == null ? null : assignmentMapper.selectById(submission.getAssignmentId());
                    StudentLearningProfileVO.WrongQuestionItem vo = new StudentLearningProfileVO.WrongQuestionItem();
                    Long assignmentId = assignment == null ? (submission == null ? null : submission.getAssignmentId()) : assignment.getId();
                    Long courseIdValue = submission == null ? null : submission.getCourseId();
                    if (courseIdValue == null && assignment != null) {
                        courseIdValue = assignment.getCourseId();
                    }
                    vo.setSubmissionId(detail.getSubmissionId());
                    vo.setAssignmentId(assignmentId);
                    vo.setCourseId(courseIdValue);
                    vo.setChapterId(assignment == null ? null : assignment.getChapterId());
                    vo.setDetailId(detail.getId());
                    vo.setAssignmentTitle(assignment == null ? "未命名练习" : assignment.getTitle());
                    vo.setQuestionNo(detail.getQuestionNo());
                    vo.setQuestionType(detail.getQuestionType());
                    vo.setStemSnapshot(detail.getStemSnapshot());
                    vo.setStudentAnswer(detail.getStudentAnswer());
                    vo.setAiComment(detail.getAiComment());
                    if (assignmentId != null && detail.getSubmissionId() != null) {
                        vo.setActionUrl("/student/homework/" + assignmentId
                                + "?submissionId=" + detail.getSubmissionId()
                                + "&questionNo=" + encode(StringUtils.defaultString(detail.getQuestionNo()))
                                + "&mode=report");
                        vo.setActionLabel("回看第 " + StringUtils.defaultIfBlank(detail.getQuestionNo(), "?") + " 题");
                    } else {
                        vo.setActionLabel("暂无入口");
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<StudentLearningProfileVO.RecommendationItem> listRecommendations(Long studentId, Long courseId, Long chapterId) {
        LambdaQueryWrapper<StudentResourceRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentResourceRecommendation::getStudentId, studentId)
                .eq(StudentResourceRecommendation::getIsDelete, 0);
        if (courseId != null) {
            wrapper.eq(StudentResourceRecommendation::getCourseId, courseId);
        }
        if (chapterId != null) {
            wrapper.eq(StudentResourceRecommendation::getChapterId, chapterId);
        }
        wrapper.orderByDesc(StudentResourceRecommendation::getCreateTime).last("limit 8");
        return recommendationMapper.selectList(wrapper).stream()
                .map(this::toRecommendationVO)
                .collect(Collectors.toList());
    }

    private StudentLearningProfileVO.RecommendationItem toRecommendationVO(StudentResourceRecommendation item) {
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
        fillRecommendationAction(vo, item);
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

    private void fillRecommendationAction(StudentLearningProfileVO.RecommendationItem vo, StudentResourceRecommendation item) {
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
            vo.setActionUrl("/student/search?keyword=" + encode(StringUtils.defaultString(keyword)));
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

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void enrichInsight(StudentLearningProfileVO profile,
                               List<LearningEvent> events,
                               List<StudentKnowledgeMastery> masteryRecords) {
        int overallScore = resolveOverallScore(masteryRecords, profile.getWeakPoints());
        int weakCount = profile.getWeakPoints() == null ? 0 : profile.getWeakPoints().size();
        int wrongCount = profile.getWrongQuestions() == null ? 0 : profile.getWrongQuestions().size();
        int eventCount = events == null ? 0 : events.size();
        int confidence = resolveConfidence(eventCount, masteryRecords.size(), wrongCount);
        String riskLevel = resolveRiskLevel(overallScore, weakCount, wrongCount);

        StudentLearningProfileVO.ProfileInsight insight = new StudentLearningProfileVO.ProfileInsight();
        insight.setOverallScore(overallScore);
        insight.setWeakPointCount(weakCount);
        insight.setWrongQuestionCount(wrongCount);
        insight.setRecentActivityCount(eventCount);
        insight.setConfidence(confidence);
        insight.setConfidenceLabel(confidence >= 75 ? "证据充分" : confidence >= 45 ? "证据中等" : "证据偏少");
        insight.setRiskLevel(riskLevel);
        insight.setRiskLabel(resolveRiskLabel(riskLevel));
        insight.setTrendLabel(resolveTrendLabel(events));
        insight.setTitle(buildInsightTitle(riskLevel, profile));
        insight.setBody(buildInsightBody(overallScore, weakCount, wrongCount, confidence, profile));
        profile.setInsight(insight);
        profile.setActionPlans(buildActionPlans(profile));
        profile.setEvidenceItems(buildEvidenceItems(profile, eventCount, masteryRecords.size(), confidence));
    }

    private int resolveOverallScore(List<StudentKnowledgeMastery> masteryRecords,
                                    List<StudentLearningProfileVO.MasteryItem> weakPoints) {
        if (masteryRecords != null && !masteryRecords.isEmpty()) {
            int total = masteryRecords.stream()
                    .map(StudentKnowledgeMastery::getMasteryScore)
                    .mapToInt(this::defaultZero)
                    .sum();
            return clamp(Math.round(total * 1f / masteryRecords.size()));
        }
        if (weakPoints != null && !weakPoints.isEmpty()) {
            int total = weakPoints.stream()
                    .map(StudentLearningProfileVO.MasteryItem::getMasteryScore)
                    .mapToInt(this::defaultZero)
                    .sum();
            return clamp(Math.round(total * 1f / weakPoints.size()));
        }
        return 72;
    }

    private int resolveConfidence(int eventCount, int masteryCount, int wrongCount) {
        int score = Math.min(45, eventCount * 5)
                + Math.min(35, masteryCount * 7)
                + Math.min(20, wrongCount * 5);
        return clamp(score);
    }

    private String resolveRiskLevel(int overallScore, int weakCount, int wrongCount) {
        if (overallScore < 55 || wrongCount >= 5 || weakCount >= 5) {
            return "high";
        }
        if (overallScore < 75 || wrongCount >= 2 || weakCount >= 2) {
            return "medium";
        }
        return "low";
    }

    private String resolveRiskLabel(String riskLevel) {
        return switch (riskLevel) {
            case "high" -> "需要优先补基础";
            case "medium" -> "适合集中巩固";
            default -> "整体较稳定";
        };
    }

    private String resolveTrendLabel(List<LearningEvent> events) {
        if (events == null || events.isEmpty()) {
            return "暂无近期趋势";
        }
        long now = System.currentTimeMillis();
        long sevenDays = 7L * 24L * 60L * 60L * 1000L;
        long recentStart = now - sevenDays;
        long previousStart = now - sevenDays * 2;
        int recent = 0;
        int previous = 0;
        for (LearningEvent event : events) {
            Date eventTime = event.getEventTime();
            if (eventTime == null) {
                continue;
            }
            long time = eventTime.getTime();
            if (time >= recentStart) {
                recent++;
            } else if (time >= previousStart) {
                previous++;
            }
        }
        if (recent >= previous + 3) {
            return "近 7 天更活跃";
        }
        if (previous >= recent + 3) {
            return "近 7 天有放缓";
        }
        return "近 7 天较平稳";
    }

    private String buildInsightTitle(String riskLevel, StudentLearningProfileVO profile) {
        List<StudentLearningProfileVO.MasteryItem> weakPoints = profile.getWeakPoints();
        String firstWeak = weakPoints == null || weakPoints.isEmpty()
                ? ""
                : StringUtils.defaultString(weakPoints.get(0).getKnowledgeName());
        if ("high".equals(riskLevel)) {
            return StringUtils.isBlank(firstWeak) ? "先稳住基础，再推进新内容" : "先补 " + firstWeak + "，再推进新内容";
        }
        if ("medium".equals(riskLevel)) {
            return StringUtils.isBlank(firstWeak) ? "问题集中，适合短周期巩固" : firstWeak + " 是当前最该处理的点";
        }
        return "学习状态比较稳定，继续保持节奏";
    }

    private String buildInsightBody(int overallScore,
                                    int weakCount,
                                    int wrongCount,
                                    int confidence,
                                    StudentLearningProfileVO profile) {
        String dominant = profile.getPreference() == null ? "balanced" : StringUtils.defaultString(profile.getPreference().getDominantType());
        String preference = switch (dominant) {
            case "video" -> "视频讲解";
            case "practice" -> "练习巩固";
            case "text" -> "图文材料";
            case "discussion" -> "问答讨论";
            case "ai" -> "AI 分步提示";
            default -> "混合学习";
        };
        return "综合掌握度约 " + overallScore + "%，识别到 " + weakCount + " 个优先关注点、"
                + wrongCount + " 条错题信号。当前更适合用“" + preference + " + 小题回练”的方式推进。"
                + (confidence < 45 ? "不过现有行为证据还偏少，建议先完成一次练习后再复查画像。" : "");
    }

    private List<StudentLearningProfileVO.ActionPlanItem> buildActionPlans(StudentLearningProfileVO profile) {
        List<StudentLearningProfileVO.ActionPlanItem> plans = new ArrayList<>();
        List<StudentLearningProfileVO.MasteryItem> weakPoints = profile.getWeakPoints() == null
                ? List.of()
                : profile.getWeakPoints();
        int priority = 1;
        for (StudentLearningProfileVO.MasteryItem weakPoint : weakPoints.stream().limit(3).collect(Collectors.toList())) {
            StudentLearningProfileVO.ActionPlanItem plan = new StudentLearningProfileVO.ActionPlanItem();
            plan.setPriority(priority++);
            plan.setTarget(weakPoint.getKnowledgeName());
            plan.setTitle("补强 " + StringUtils.defaultIfBlank(weakPoint.getKnowledgeName(), "薄弱知识点"));
            plan.setReason(StringUtils.defaultIfBlank(weakPoint.getEvidenceSummary(), "该知识点近期证据显示掌握不稳。"));
            plan.setMinutes(resolvePlanMinutes(weakPoint.getMasteryScore()));
            plan.setActionType("student_practice");
            if (weakPoint.getCourseId() != null) {
                StringBuilder url = new StringBuilder("/learn/").append(weakPoint.getCourseId()).append("?");
                if (weakPoint.getChapterId() != null) {
                    url.append("chapterId=").append(weakPoint.getChapterId()).append("&");
                }
                url.append("from=diagnosis");
                plan.setActionText("开始专项练习");
                plan.setActionUrl(url.toString());
            } else {
                plan.setActionText("开始专项练习");
                plan.setActionUrl("/student/search?keyword="
                        + encode(StringUtils.defaultString(weakPoint.getKnowledgeName())));
            }
            plans.add(plan);
        }
        if (plans.isEmpty() && profile.getWrongQuestions() != null && !profile.getWrongQuestions().isEmpty()) {
            StudentLearningProfileVO.WrongQuestionItem wrong = profile.getWrongQuestions().get(0);
            StudentLearningProfileVO.ActionPlanItem plan = new StudentLearningProfileVO.ActionPlanItem();
            plan.setPriority(1);
            plan.setTitle("先回看最近错题");
            plan.setTarget(StringUtils.defaultIfBlank(wrong.getAssignmentTitle(), "错题复盘"));
            plan.setReason(StringUtils.defaultIfBlank(wrong.getAiComment(), "错题是当前最直接的诊断证据。"));
            plan.setMinutes(12);
            plan.setActionType("wrong_question");
            plan.setActionText(StringUtils.defaultIfBlank(wrong.getActionLabel(), "回看错题"));
            plan.setActionUrl(wrong.getActionUrl());
            plans.add(plan);
        }
        if (plans.isEmpty()) {
            StudentLearningProfileVO.ActionPlanItem plan = new StudentLearningProfileVO.ActionPlanItem();
            plan.setPriority(1);
            plan.setTitle("完成一次诊断练习");
            plan.setTarget("补充画像证据");
            plan.setReason("当前证据不足，先通过练习让系统判断薄弱点。");
            plan.setMinutes(15);
            plan.setActionType("practice");
            plan.setActionText("去练习");
            plan.setActionUrl("/student/coding");
            plans.add(plan);
        }
        return plans;
    }

    private int resolvePlanMinutes(Integer masteryScore) {
        int score = defaultZero(masteryScore);
        if (score < 50) {
            return 25;
        }
        if (score < 70) {
            return 18;
        }
        return 12;
    }

    private List<StudentLearningProfileVO.EvidenceItem> buildEvidenceItems(StudentLearningProfileVO profile,
                                                                           int eventCount,
                                                                           int masteryCount,
                                                                           int confidence) {
        List<StudentLearningProfileVO.EvidenceItem> items = new ArrayList<>();
        items.add(evidence("行为记录", String.valueOf(eventCount), "最近 " + defaultZero(profile.getDays()) + " 天用于判断学习节奏", eventCount > 0 ? "primary" : "muted"));
        items.add(evidence("掌握度样本", String.valueOf(masteryCount), "来自作业、练习、视频行为和 AI 提问", masteryCount > 0 ? "success" : "muted"));
        items.add(evidence("错题信号", String.valueOf(profile.getWrongQuestions() == null ? 0 : profile.getWrongQuestions().size()), "错题会提高补强优先级", "warning"));
        items.add(evidence("诊断置信度", confidence + "%", confidence >= 75 ? "证据充分，可以直接按计划推进" : "建议继续补充练习证据", confidence >= 75 ? "success" : "warning"));
        return items;
    }

    private StudentLearningProfileVO.EvidenceItem evidence(String label, String value, String detail, String tone) {
        StudentLearningProfileVO.EvidenceItem item = new StudentLearningProfileVO.EvidenceItem();
        item.setLabel(label);
        item.setValue(value);
        item.setDetail(detail);
        item.setTone(tone);
        return item;
    }

    private int clamp(float value) {
        return Math.max(0, Math.min(100, Math.round(value)));
    }

    private List<StudentLearningProfileVO.InterventionAdvice> buildAdvices(StudentLearningProfileVO profile) {
        List<StudentLearningProfileVO.InterventionAdvice> result = new ArrayList<>();
        if (profile.getWeakPoints() != null && !profile.getWeakPoints().isEmpty()) {
            StudentLearningProfileVO.MasteryItem first = profile.getWeakPoints().get(0);
            result.add(advice("优先补弱知识点",
                    "先跟进“" + first.getKnowledgeName() + "”，建议安排一次短讲解和 3 道同类变式题。",
                    "danger"));
        }
        if (profile.getWrongQuestions() != null && !profile.getWrongQuestions().isEmpty()) {
            result.add(advice("组织错题回练",
                    "该学生近期仍有错题，建议要求先看解析，再完成相似题回练并复查掌握度。",
                    "warning"));
        }
        String dominant = profile.getPreference() == null ? "" : profile.getPreference().getDominantType();
        if ("video".equals(dominant)) {
            result.add(advice("按视频偏好推荐", "资源推送优先选择短视频讲解，再配 1 组基础练习。", "primary"));
        } else if ("text".equals(dominant)) {
            result.add(advice("按文字偏好推荐", "资源推送优先选择图文笔记、例题解析和知识点清单。", "primary"));
        } else if ("discussion".equals(dominant)) {
            result.add(advice("利用互动偏好", "可引导学生在评论区复述错因，教师再做一次点评确认。", "primary"));
        }
        if (result.isEmpty()) {
            result.add(advice("保持常规观察", "暂无明显风险，建议在下一次练习后复查掌握度变化。", "success"));
        }
        return result.stream().limit(4).collect(Collectors.toList());
    }

    private StudentLearningProfileVO.InterventionAdvice advice(String title, String body, String tone) {
        StudentLearningProfileVO.InterventionAdvice vo = new StudentLearningProfileVO.InterventionAdvice();
        vo.setTitle(title);
        vo.setBody(body);
        vo.setTone(tone);
        return vo;
    }

    private StudentLearningProfileVO.PreferenceSummary toPreferenceSummary(StudentLearningPreference preference) {
        StudentLearningProfileVO.PreferenceSummary vo = new StudentLearningProfileVO.PreferenceSummary();
        vo.setDominantType(preference.getDominantType());
        vo.setSummary(preference.getSummary());
        vo.setVideoScore(preference.getVideoScore());
        vo.setTextScore(preference.getTextScore());
        vo.setPracticeScore(preference.getPracticeScore());
        vo.setDiscussionScore(preference.getDiscussionScore());
        vo.setAiScore(preference.getAiScore());
        vo.setResourceScore(preference.getResourceScore());
        return vo;
    }

    private String normalizeKnowledge(String knowledgeName, Long chapterId) {
        String value = StringUtils.trimToEmpty(knowledgeName);
        if (value.length() > 80) {
            value = value.substring(0, 80);
        }
        if (StringUtils.isNotBlank(value)) {
            return value;
        }
        return chapterId == null ? null : "章节 " + chapterId;
    }

    private Date later(Date a, Date b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.after(b) ? a : b;
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static class ScoreBox {
        private int video;
        private int text;
        private int practice;
        private int discussion;
        private int ai;
        private int resource;
    }

    private static class MasteryEvidence {
        private Long courseId;
        private Long chapterId;
        private String knowledgeName;
        private Date lastTime;
        private int practiceCount;
        private int correctCount;
        private int wrongCount;
        private int videoRisk;
        private int aiQuestions;
        private int reviewCount;
        private int scoreTotal;
        private int scoreCount;

        private int resolveScore() {
            int base = 72;
            if (practiceCount > 0) {
                base = Math.round(correctCount * 100f / Math.max(1, practiceCount));
            }
            if (scoreCount > 0) {
                base = Math.max(base, Math.min(100, Math.round(scoreTotal * 1f / scoreCount)));
            }
            base -= Math.min(25, wrongCount * 8);
            base -= Math.min(18, videoRisk * 4);
            base -= Math.min(10, aiQuestions * 2);
            base += Math.min(12, reviewCount * 4);
            return Math.max(0, Math.min(100, base));
        }

        private String summary(int score) {
            List<String> parts = new ArrayList<>();
            if (practiceCount > 0) {
                parts.add("练习正确 " + correctCount + "/" + practiceCount);
            }
            if (wrongCount > 0) {
                parts.add("错题 " + wrongCount + " 道");
            }
            if (videoRisk > 0) {
                parts.add("视频卡点 " + videoRisk + " 次");
            }
            if (aiQuestions > 0) {
                parts.add("AI 提问 " + aiQuestions + " 次");
            }
            if (reviewCount > 0) {
                parts.add("已回看/回练 " + reviewCount + " 次");
            }
            if (parts.isEmpty()) {
                parts.add("行为证据较少，暂按默认掌握度评估");
            }
            parts.add("掌握度 " + score + "%");
            return String.join("；", parts);
        }
    }
}
