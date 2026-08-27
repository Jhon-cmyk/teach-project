package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseChapterMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionDetailMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionMapper;
import com.ruyi.teach.mapper.LearningEventMapper;
import com.ruyi.teach.mapper.StudentDailyRecommendationSessionMapper;
import com.ruyi.teach.mapper.StudentKnowledgeMasteryMapper;
import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;
import com.ruyi.teach.mapper.StudentResourceRecommendationMapper;
import com.ruyi.teach.mapper.TextCourseMapper;
import com.ruyi.teach.mapper.VideoKnowledgeSegmentMapper;
import com.ruyi.teach.mapper.VideoLearningEventMapper;
import com.ruyi.teach.model.dto.learning.DailyRecommendationSubmitRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.LearningEvent;
import com.ruyi.teach.model.entity.StudentDailyRecommendationSession;
import com.ruyi.teach.model.entity.StudentKnowledgeMastery;
import com.ruyi.teach.model.entity.StudentLearningPreference;
import com.ruyi.teach.model.entity.StudentResourceRecommendation;
import com.ruyi.teach.model.entity.TextCourse;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.VideoKnowledgeSegment;
import com.ruyi.teach.model.entity.VideoLearningEvent;
import com.ruyi.teach.model.vo.DailyRecommendationTodayVO;
import com.ruyi.teach.model.vo.StudentLearningProfileVO;
import com.ruyi.teach.service.DailyRecommendationService;
import com.ruyi.teach.service.StudentLearningContextService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DailyRecommendationServiceImpl implements DailyRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(DailyRecommendationServiceImpl.class);

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_DISMISSED = "dismissed";
    private static final String STATUS_COMPLETED = "completed";
    private static final String PROMPT_ONBOARDING_ASSESSMENT = "onboarding_assessment";
    private static final String PROMPT_DAILY_REVIEW = "daily_review";
    private static final String PROMPT_PROFILE_ENRICHMENT = "profile_enrichment";
    private static final String SOURCE_DAILY_SURVEY = "daily_survey";
    private static final String SOURCE_VIDEO_BEHAVIOR = "video_behavior";
    private static final String SOURCE_EXAM_BEHAVIOR = "exam_behavior";
    private static final String SOURCE_HOMEWORK_BEHAVIOR = "homework_behavior";
    private static final String SOURCE_LEARNING_HISTORY = "learning_history";
    private static final String SOURCE_AI_TUTOR = "ai_tutor";
    private static final int MIN_DAILY_RECOMMENDATION_COUNT = 8;
    private static final Set<String> TODAY_RECOMMENDATION_SOURCES = Set.of(
            SOURCE_DAILY_SURVEY, SOURCE_VIDEO_BEHAVIOR, SOURCE_EXAM_BEHAVIOR,
            SOURCE_HOMEWORK_BEHAVIOR, SOURCE_LEARNING_HISTORY, SOURCE_AI_TUTOR
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private StudentDailyRecommendationSessionMapper sessionMapper;

    @Resource
    private StudentResourceRecommendationMapper recommendationMapper;

    @Resource
    private StudentKnowledgeMasteryMapper masteryMapper;

    @Resource
    private StudentLearningPreferenceMapper preferenceMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private TextCourseMapper textCourseMapper;

    @Resource
    private VideoLearningEventMapper videoLearningEventMapper;

    @Resource
    private VideoKnowledgeSegmentMapper videoKnowledgeSegmentMapper;

    @Resource
    private HomeworkSubmissionMapper homeworkSubmissionMapper;

    @Resource
    private HomeworkSubmissionDetailMapper homeworkSubmissionDetailMapper;

    @Resource
    private HomeworkAssignmentMapper homeworkAssignmentMapper;

    @Resource
    private CourseChapterMapper courseChapterMapper;

    @Resource
    private LearningEventMapper learningEventMapper;

    @Resource
    private StudentLearningContextService studentLearningContextService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyRecommendationTodayVO getToday(User student) {
        requireStudent(student);
        StudentDailyRecommendationSession session = ensureTodaySession(student.getId());
        boolean newStudent = isNewStudentWithoutAssessment(student);
        boolean needsContext = !newStudent && needsProfileEnrichment(student.getId());
        if (!newStudent) {
            ensureTodayBehaviorRecommendations(session, student);
        }
        boolean shouldPrompt = STATUS_PENDING.equals(session.getStatus())
                || (needsContext && !STATUS_DISMISSED.equals(session.getStatus()));
        return toTodayVO(session, session.getStatus(), shouldPrompt,
                resolvePromptType(newStudent, needsContext));
    }

    @Override
    @Transactional(readOnly = true)
    public DailyRecommendationTodayVO getTodayCached(User student) {
        requireStudent(student);
        StudentDailyRecommendationSession session = findTodaySession(student.getId());
        boolean newStudent = isNewStudentWithoutAssessment(student);
        boolean needsContext = !newStudent && needsProfileEnrichment(student.getId());
        if (session == null) {
            return toTodayVO(null, STATUS_PENDING, true,
                    resolvePromptType(newStudent, needsContext));
        }
        boolean shouldPrompt = STATUS_PENDING.equals(session.getStatus())
                || (needsContext && !STATUS_DISMISSED.equals(session.getStatus()));
        return toTodayVO(session, session.getStatus(), shouldPrompt,
                resolvePromptType(newStudent, needsContext));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyRecommendationTodayVO dismissToday(User student) {
        requireStudent(student);
        StudentDailyRecommendationSession session = ensureTodaySession(student.getId());
        session.setStatus(STATUS_DISMISSED);
        session.setUpdateTime(new Date());
        sessionMapper.updateById(session);
        return toTodayVO(session, STATUS_DISMISSED, false, PROMPT_DAILY_REVIEW);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyRecommendationTodayVO submitToday(DailyRecommendationSubmitRequest request, User student) {
        requireStudent(student);
        DailyRecommendationSubmitRequest safeRequest = request == null ? new DailyRecommendationSubmitRequest() : request;
        validateLearningContext(safeRequest);
        StudentDailyRecommendationSession session = ensureTodaySession(student.getId());
        fillSession(session, safeRequest);
        if (session.getId() == null) {
            sessionMapper.insert(session);
        } else {
            sessionMapper.updateById(session);
        }
        markProfileCompleted(student.getId(), safeRequest);

        cleanupTodayDailyRecommendations(student.getId());
        List<StudentResourceRecommendation> recommendations = generateDailyRecommendations(session, student);
        for (StudentResourceRecommendation recommendation : recommendations) {
            recommendationMapper.insert(recommendation);
        }
        return toTodayVO(session, STATUS_COMPLETED, false, PROMPT_DAILY_REVIEW);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyRecommendationTodayVO refreshToday(User student) {
        requireStudent(student);
        StudentDailyRecommendationSession session = ensureTodaySession(student.getId());
        syncAssessmentMastery(student.getId());
        Set<String> retainedKeys = cleanupPendingBehaviorRecommendations(student.getId());
        List<StudentResourceRecommendation> recommendations = generateBehaviorRecommendations(session, student, retainedKeys);
        for (StudentResourceRecommendation recommendation : recommendations) {
            recommendationMapper.insert(recommendation);
        }
        return toTodayVO(session, session.getStatus(), false, PROMPT_DAILY_REVIEW);
    }

    private void requireStudent(User student) {
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (!"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可使用今日推荐");
        }
    }

    private StudentDailyRecommendationSession findTodaySession(Long studentId) {
        LambdaQueryWrapper<StudentDailyRecommendationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentDailyRecommendationSession::getStudentId, studentId)
                .eq(StudentDailyRecommendationSession::getRecommendDate, LocalDate.now())
                .eq(StudentDailyRecommendationSession::getIsDelete, 0)
                .last("limit 1");
        return sessionMapper.selectOne(wrapper);
    }

    private StudentDailyRecommendationSession ensureTodaySession(Long studentId) {
        StudentDailyRecommendationSession session = findTodaySession(studentId);
        if (session != null) {
            return session;
        }
        Date now = new Date();
        session = new StudentDailyRecommendationSession();
        session.setStudentId(studentId);
        session.setRecommendDate(LocalDate.now());
        session.setStatus(STATUS_PENDING);
        session.setPreferredResourceType("balanced");
        session.setCreateTime(now);
        session.setUpdateTime(now);
        session.setIsDelete(0);
        sessionMapper.insert(session);
        return session;
    }

    private void fillSession(StudentDailyRecommendationSession session, DailyRecommendationSubmitRequest request) {
        Date now = new Date();
        session.setStatus(STATUS_COMPLETED);
        session.setCourseId(request.getCourseId());
        session.setGoal(limit(request.getGoal(), 80));
        session.setDifficultyText(limit(request.getDifficultyText(), 500));
        session.setAvailableMinutes(request.getAvailableMinutes() == null ? null : Math.max(0, request.getAvailableMinutes()));
        session.setPreferredResourceType(resolvePreferredType(request.getPreferredResourceType()));
        session.setAnswersJson(toAnswersJson(request));
        session.setUpdateTime(now);
        if (session.getCreateTime() == null) {
            session.setCreateTime(now);
        }
        session.setIsDelete(0);
    }

    private String resolvePreferredType(String value) {
        String type = StringUtils.defaultString(value).trim().toLowerCase(Locale.ROOT);
        if ("video".equals(type) || "text".equals(type)) {
            return type;
        }
        return "balanced";
    }

    private String toAnswersJson(DailyRecommendationSubmitRequest request) {
        try {
            Map<String, Object> answers = new LinkedHashMap<>();
            answers.put("courseId", request.getCourseId() == null ? "" : request.getCourseId());
            answers.put("goal", StringUtils.defaultString(request.getGoal()));
            answers.put("difficultyText", StringUtils.defaultString(request.getDifficultyText()));
            answers.put("learningSituation", StringUtils.defaultString(request.getLearningSituation()));
            answers.put("personalityType", StringUtils.defaultString(request.getPersonalityType()));
            answers.put("universityName", studentLearningContextService.normalizeUniversityName(request.getUniversityName()));
            answers.put("developmentGoal", studentLearningContextService.normalizeDevelopmentGoal(request.getDevelopmentGoal()));
            answers.put("availableMinutes", request.getAvailableMinutes() == null ? "" : request.getAvailableMinutes());
            answers.put("preferredResourceType", resolvePreferredType(request.getPreferredResourceType()));
            answers.put("collectionMode", StringUtils.defaultIfBlank(request.getCollectionMode(), "questionnaire"));
            answers.put("interviewSummary", StringUtils.defaultString(request.getInterviewSummary()));
            return OBJECT_MAPPER.writeValueAsString(answers);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private void ensureTodayBehaviorRecommendations(StudentDailyRecommendationSession session, User student) {
        if (!listTodayRecommendations(session).isEmpty()) {
            return;
        }
        syncAssessmentMastery(student.getId());
        Set<String> retainedKeys = cleanupPendingBehaviorRecommendations(student.getId());
        List<StudentResourceRecommendation> recommendations = generateBehaviorRecommendations(session, student, retainedKeys);
        for (StudentResourceRecommendation recommendation : recommendations) {
            recommendationMapper.insert(recommendation);
        }
    }

    private boolean isNewStudentWithoutAssessment(User student) {
        if (hasCompletedProfile(student.getId())) {
            return false;
        }
        boolean hasHistory = hasAnyLearningHistory(student);
        if (hasHistory) {
            markProfileCompleted(student.getId(), null);
        }
        return !hasHistory;
    }

    private boolean hasCompletedProfile(Long studentId) {
        LambdaQueryWrapper<StudentLearningPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentLearningPreference::getStudentId, studentId)
                .eq(StudentLearningPreference::getProfileCompleted, 1)
                .last("limit 1");
        return preferenceMapper.selectOne(wrapper) != null;
    }

    private boolean needsProfileEnrichment(Long studentId) {
        return !studentLearningContextService.isComplete(
                studentLearningContextService.findGeneralPreference(studentId));
    }

    private String resolvePromptType(boolean newStudent, boolean needsContext) {
        if (newStudent) {
            return PROMPT_ONBOARDING_ASSESSMENT;
        }
        return needsContext ? PROMPT_PROFILE_ENRICHMENT : PROMPT_DAILY_REVIEW;
    }

    private void validateLearningContext(DailyRecommendationSubmitRequest request) {
        request.setUniversityName(studentLearningContextService.normalizeUniversityName(request.getUniversityName()));
        request.setDevelopmentGoal(studentLearningContextService.normalizeDevelopmentGoal(request.getDevelopmentGoal()));
        if (StringUtils.isBlank(request.getUniversityName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择或填写所在大学");
        }
        if (StringUtils.isBlank(request.getDevelopmentGoal())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择发展目标");
        }
    }

    private boolean hasAnyLearningHistory(User student) {
        Long studentId = student.getId();
        Long learningCount = learningEventMapper.selectCount(new LambdaQueryWrapper<LearningEvent>()
                .eq(LearningEvent::getStudentId, studentId)
                .eq(LearningEvent::getIsDelete, 0));
        if (learningCount != null && learningCount > 0) {
            return true;
        }
        Long videoCount = videoLearningEventMapper.selectCount(new LambdaQueryWrapper<VideoLearningEvent>()
                .eq(VideoLearningEvent::getStudentId, studentId));
        if (videoCount != null && videoCount > 0) {
            return true;
        }
        Long submissionCount = homeworkSubmissionMapper.selectCount(new LambdaQueryWrapper<HomeworkSubmission>()
                .eq(HomeworkSubmission::getStudentId, studentId)
                .eq(HomeworkSubmission::getIsDelete, 0));
        return submissionCount != null && submissionCount > 0;
    }

    private void markProfileCompleted(Long studentId, DailyRecommendationSubmitRequest request) {
        LambdaQueryWrapper<StudentLearningPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentLearningPreference::getStudentId, studentId)
                .isNull(StudentLearningPreference::getCourseId)
                .last("limit 1");
        StudentLearningPreference preference = preferenceMapper.selectOne(wrapper);
        Date now = new Date();
        if (preference == null) {
            preference = new StudentLearningPreference();
            preference.setStudentId(studentId);
            preference.setDominantType(resolvePreferredType(request == null ? null : request.getPreferredResourceType()));
            preference.setVideoScore(0);
            preference.setTextScore(0);
            preference.setPracticeScore(0);
            preference.setDiscussionScore(0);
            preference.setAiScore(0);
            preference.setResourceScore(0);
            preference.setCreateTime(now);
        }
        if (request != null) {
            preference.setDominantType(resolvePreferredType(request.getPreferredResourceType()));
            preference.setPersonalityType(limit(request.getPersonalityType(), 40));
            preference.setUniversityName(studentLearningContextService.normalizeUniversityName(request.getUniversityName()));
            preference.setDevelopmentGoal(studentLearningContextService.normalizeDevelopmentGoal(request.getDevelopmentGoal()));
            preference.setAssessmentJson(toAnswersJson(request));
            preference.setSummary(limit(StringUtils.defaultIfBlank(request.getLearningSituation(), request.getDifficultyText()), 500));
        }
        preference.setProfileCompleted(1);
        preference.setUpdateTime(now);
        if (preference.getId() == null) {
            preferenceMapper.insert(preference);
        } else {
            preferenceMapper.updateById(preference);
        }
    }

    private void cleanupTodayDailyRecommendations(Long studentId) {
        Date start = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        LambdaQueryWrapper<StudentResourceRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentResourceRecommendation::getStudentId, studentId)
                .eq(StudentResourceRecommendation::getRecommendationSource, SOURCE_DAILY_SURVEY)
                .eq(StudentResourceRecommendation::getIsDelete, 0)
                .ge(StudentResourceRecommendation::getCreateTime, start);
        List<StudentResourceRecommendation> existing = recommendationMapper.selectList(wrapper);
        Date now = new Date();
        for (StudentResourceRecommendation item : existing) {
            item.setIsDelete(1);
            item.setUpdateTime(now);
            recommendationMapper.updateById(item);
        }
    }

    private Set<String> cleanupPendingBehaviorRecommendations(Long studentId) {
        Date start = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        LambdaQueryWrapper<StudentResourceRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentResourceRecommendation::getStudentId, studentId)
                .in(StudentResourceRecommendation::getRecommendationSource,
                        SOURCE_VIDEO_BEHAVIOR, SOURCE_EXAM_BEHAVIOR, SOURCE_HOMEWORK_BEHAVIOR,
                        SOURCE_LEARNING_HISTORY, SOURCE_AI_TUTOR)
                .eq(StudentResourceRecommendation::getIsDelete, 0)
                .ge(StudentResourceRecommendation::getCreateTime, start);
        List<StudentResourceRecommendation> existing = recommendationMapper.selectList(wrapper);
        Set<String> retainedKeys = new HashSet<>();
        Date now = new Date();
        for (StudentResourceRecommendation item : existing) {
            if ("completed".equals(item.getStatus())) {
                retainedKeys.add(recommendationKey(item.getResourceType(), item.getResourceId(), item.getCourseId(), item.getKnowledgeName()));
                continue;
            }
            item.setIsDelete(1);
            item.setUpdateTime(now);
            recommendationMapper.updateById(item);
        }
        return retainedKeys;
    }

    private List<StudentResourceRecommendation> generateDailyRecommendations(StudentDailyRecommendationSession session, User student) {
        Set<String> demandKeywords = buildDemandKeywords(session);
        Set<String> explicitTopicFocus = explicitTopicFocus(session, demandKeywords);
        Set<String> keywords = buildKeywords(session, student.getId());
        StudentLearningPreference preference = studentLearningContextService.findGeneralPreference(student.getId());
        String developmentGoal = preference == null ? ""
                : studentLearningContextService.normalizeDevelopmentGoal(preference.getDevelopmentGoal());
        List<ResourceCandidate> candidates = new ArrayList<>();
        candidates.addAll(videoCourseCandidates(session, keywords));
        candidates.addAll(selectedCourseChapterCandidates(session, keywords));
        candidates.addAll(microVideoCandidates(session, keywords));
        candidates.addAll(textCourseCandidates(session, keywords));

        for (ResourceCandidate candidate : candidates) {
            candidate.demandScore = score(candidate.title, candidate.description, demandKeywords);
            candidate.matchedDemandKeyword = bestMatchedKeyword(candidate.title, candidate.description, demandKeywords);
            candidate.score += candidate.demandScore * 2;
            candidate.directionScore = developmentDirectionScore(
                    candidate.title, candidate.description, developmentGoal);
            candidate.developmentGoal = candidate.directionScore > 0 ? developmentGoal : "";
            candidate.score += candidate.directionScore;
            candidate.topicMatched = matchesTopicFocus(candidate.title, candidate.description, explicitTopicFocus);
        }

        List<ResourceCandidate> demandMatched = candidates.stream()
                .filter(candidate -> candidate.demandScore > 0)
                .sorted(Comparator.comparingInt((ResourceCandidate candidate) -> candidate.score).reversed())
                .collect(Collectors.toList());

        List<ResourceCandidate> supplemental = candidates.stream()
                .filter(candidate -> candidate.demandScore <= 0)
                .filter(candidate -> candidate.keywordScore > 0
                        || candidate.directionScore > 0
                        || Objects.equals(session.getCourseId(), candidate.courseId))
                .sorted(Comparator.comparingInt((ResourceCandidate candidate) -> candidate.score).reversed())
                .collect(Collectors.toList());

        List<ResourceCandidate> sorted;
        if (!explicitTopicFocus.isEmpty()) {
            // 学生明确说出学科或知识点时，只在同主题资源中排序。
            // 宁可少给，也不要用历史课程中的 Java、Python 等无关资源凑满五条。
            sorted = candidates.stream()
                    .filter(candidate -> candidate.topicMatched)
                    .sorted(Comparator.comparingInt((ResourceCandidate candidate) -> candidate.score).reversed())
                    .collect(Collectors.toList());
        } else if (!demandKeywords.isEmpty()) {
            sorted = new ArrayList<>(demandMatched);
            sorted.addAll(supplemental);
        } else {
            sorted = candidates.stream()
                    .filter(candidate -> candidate.keywordScore > 0
                            || candidate.directionScore > 0
                            || Objects.equals(session.getCourseId(), candidate.courseId))
                    .sorted(Comparator.comparingInt((ResourceCandidate candidate) -> candidate.score).reversed())
                    .collect(Collectors.toList());
        }

        List<StudentResourceRecommendation> result = new ArrayList<>();
        Set<String> usedResourceKeys = new HashSet<>();
        Set<String> usedTitles = new HashSet<>();
        for (ResourceCandidate candidate : sorted) {
            String resourceKey = candidateIdentity(candidate);
            String titleKey = normalize(candidate.title);
            if (!usedResourceKeys.add(resourceKey)
                    || (StringUtils.isNotBlank(titleKey) && !usedTitles.add(titleKey))) {
                continue;
            }
            result.add(toRecommendation(session, candidate));
            if (result.size() >= MIN_DAILY_RECOMMENDATION_COUNT) {
                break;
            }
        }
        String fallbackKeyword = recommendationTopic(session, explicitTopicFocus,
                demandKeywords.isEmpty() ? keywords : demandKeywords);
        ensureDailyRecommendationMinimum(result, session, fallbackKeyword);
        return result;
    }

    private String candidateIdentity(ResourceCandidate candidate) {
        String type = StringUtils.defaultString(candidate.resourceType).toLowerCase(Locale.ROOT);
        if (candidate.courseId != null && type.contains("video")) {
            return "course:" + candidate.courseId;
        }
        if (candidate.courseId != null && candidate.chapterId != null) {
            return "course:" + candidate.courseId + ":chapter:" + candidate.chapterId;
        }
        if (candidate.resourceId != null) {
            return type + ":" + candidate.resourceId;
        }
        return "title:" + normalize(candidate.title);
    }

    private void ensureDailyRecommendationMinimum(
            List<StudentResourceRecommendation> recommendations,
            StudentDailyRecommendationSession session,
            String fallbackKeyword) {
        while (recommendations.size() < MIN_DAILY_RECOMMENDATION_COUNT) {
            recommendations.add(fallbackRecommendation(session, fallbackKeyword, recommendations.size()));
        }
    }

    private List<StudentResourceRecommendation> generateBehaviorRecommendations(
            StudentDailyRecommendationSession session, User student, Set<String> retainedKeys) {
        List<BehaviorSignal> signals = new ArrayList<>();
        try {
            signals.addAll(aiTutorQuestionSignals(student.getId()));
        } catch (RuntimeException e) {
            log.warn("Skip AI tutor question signals for student {}", student.getId(), e);
        }
        try {
            signals.addAll(learningHistorySignals(student.getId()));
        } catch (RuntimeException e) {
            log.warn("Skip learning history signals for student {}", student.getId(), e);
        }
        try {
            signals.addAll(videoBehaviorSignals(student.getId()));
        } catch (RuntimeException e) {
            log.warn("Skip video behavior signals for student {}", student.getId(), e);
        }
        try {
            signals.addAll(assessmentBehaviorSignals(student.getId()));
        } catch (RuntimeException e) {
            log.warn("Skip assessment behavior signals for student {}", student.getId(), e);
        }
        signals.sort(Comparator.comparingInt((BehaviorSignal signal) -> signal.weight).reversed());

        List<StudentResourceRecommendation> result = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>(retainedKeys == null ? Set.of() : retainedKeys);
        for (StudentResourceRecommendation item : pendingHomeworkRecommendations(session, student, usedKeys)) {
            result.add(item);
            if (result.size() >= 5) {
                return result;
            }
        }
        for (BehaviorSignal signal : signals) {
            List<ResourceCandidate> candidates = resourceCandidatesForSignal(session, signal);
            for (ResourceCandidate candidate : candidates) {
                String key = recommendationKey(candidate.resourceType, candidate.resourceId, candidate.courseId, signal.knowledgeName);
                if (!usedKeys.add(key)) {
                    continue;
                }
                result.add(toBehaviorRecommendation(session, signal, candidate));
                break;
            }
            if (result.size() >= 5) {
                break;
            }
        }
        return result;
    }

    private List<BehaviorSignal> aiTutorQuestionSignals(Long studentId) {
        Date after = new Date(System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L);
        LambdaQueryWrapper<LearningEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LearningEvent::getStudentId, studentId)
                .eq(LearningEvent::getEventType, "ai_question")
                .eq(LearningEvent::getIsDelete, 0)
                .ge(LearningEvent::getEventTime, after)
                .orderByDesc(LearningEvent::getEventTime)
                .last("limit 100");
        List<LearningEvent> events = learningEventMapper.selectList(wrapper);
        if (events.isEmpty()) {
            return List.of();
        }

        Map<String, AiQuestionStats> statsMap = new LinkedHashMap<>();
        for (LearningEvent event : events) {
            String knowledgeName = StringUtils.defaultIfBlank(event.getKnowledgeName(), "综合学习问题");
            String key = (event.getCourseId() == null ? "" : event.getCourseId()) + ":"
                    + (event.getChapterId() == null ? "" : event.getChapterId()) + ":"
                    + normalize(knowledgeName);
            AiQuestionStats stats = statsMap.computeIfAbsent(key, ignored -> new AiQuestionStats());
            stats.courseId = event.getCourseId();
            stats.chapterId = event.getChapterId();
            stats.knowledgeName = knowledgeName;
            stats.count++;
            stats.lastTime = later(stats.lastTime, event.getEventTime());
            String question = readJsonText(event.getExtraJson(), "question");
            if (StringUtils.isNotBlank(question) && stats.questions.size() < 4) {
                stats.questions.add(limit(question, 160));
            }
        }

        Map<Long, Course> courses = loadCourses(statsMap.values().stream()
                .map(stats -> stats.courseId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, CourseChapter> chapters = loadChapters(statsMap.values().stream()
                .map(stats -> stats.chapterId).filter(Objects::nonNull).collect(Collectors.toSet()));
        long now = System.currentTimeMillis();
        return statsMap.values().stream()
                .map(stats -> {
                    BehaviorSignal signal = new BehaviorSignal();
                    signal.source = SOURCE_AI_TUTOR;
                    signal.courseId = stats.courseId;
                    signal.chapterId = stats.chapterId;
                    Course course = nullableMapGet(courses, stats.courseId);
                    CourseChapter chapter = nullableMapGet(chapters, stats.chapterId);
                    signal.courseName = course == null ? "" : course.getName();
                    signal.chapterTitle = chapter == null ? "" : chapter.getTitle();
                    signal.knowledgeName = stats.knowledgeName;
                    signal.keywordText = String.join(" ", stats.questions);
                    long ageDays = stats.lastTime == null ? 30L
                            : Math.max(0L, (now - stats.lastTime.getTime()) / (24L * 60L * 60L * 1000L));
                    signal.weight = 135 + Math.min(60, stats.count * 12) + (int) Math.max(0L, 30L - ageDays);
                    signal.reason = "你最近通过数字人或 AI 助教"
                            + (stats.count > 1 ? "多次" : "")
                            + "询问了“" + stats.knowledgeName + "”，系统优先补充相关讲解和练习资源。";
                    signal.suggestion = "先学习推荐内容，再用一道相关练习验证是否真正理解；后续提问会继续更新你的学习画像。";
                    return signal;
                })
                .sorted(Comparator.comparingInt((BehaviorSignal signal) -> signal.weight).reversed())
                .limit(4)
                .collect(Collectors.toList());
    }

    private List<StudentResourceRecommendation> pendingHomeworkRecommendations(
            StudentDailyRecommendationSession session, User student, Set<String> usedKeys) {
        if (student.getClassId() == null) {
            return List.of();
        }
        Date now = new Date();
        Date nearDeadline = new Date(now.getTime() + 3L * 24L * 60L * 60L * 1000L);
        LambdaQueryWrapper<HomeworkAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkAssignment::getClassId, student.getClassId())
                .eq(HomeworkAssignment::getStatus, "published")
                .eq(HomeworkAssignment::getIsDelete, 0)
                .isNotNull(HomeworkAssignment::getDeadline)
                .le(HomeworkAssignment::getDeadline, nearDeadline)
                .orderByAsc(HomeworkAssignment::getDeadline)
                .last("limit 12");
        List<HomeworkAssignment> assignments = homeworkAssignmentMapper.selectList(wrapper);
        if (assignments.isEmpty()) {
            return List.of();
        }

        Set<Long> assignmentIds = assignments.stream().map(HomeworkAssignment::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<HomeworkSubmission> submissionWrapper = new LambdaQueryWrapper<>();
        submissionWrapper.eq(HomeworkSubmission::getStudentId, student.getId())
                .eq(HomeworkSubmission::getIsDelete, 0)
                .in(HomeworkSubmission::getAssignmentId, assignmentIds)
                .in(HomeworkSubmission::getSubmitStatus, "submitted", "judging", "completed");
        Set<Long> finishedAssignmentIds = homeworkSubmissionMapper.selectList(submissionWrapper).stream()
                .map(HomeworkSubmission::getAssignmentId)
                .collect(Collectors.toSet());
        Map<Long, Course> courseMap = loadCourses(assignments.stream().map(HomeworkAssignment::getCourseId).collect(Collectors.toSet()));

        List<StudentResourceRecommendation> result = new ArrayList<>();
        for (HomeworkAssignment assignment : assignments) {
            if (finishedAssignmentIds.contains(assignment.getId())) {
                continue;
            }
            String key = recommendationKey("homework_assignment", assignment.getId(), assignment.getCourseId(), assignment.getTitle());
            if (!usedKeys.add(key)) {
                continue;
            }
            result.add(toPendingHomeworkRecommendation(
                    session, assignment, nullableMapGet(courseMap, assignment.getCourseId()), now));
            if (result.size() >= 3) {
                break;
            }
        }
        return result;
    }

    private List<BehaviorSignal> learningHistorySignals(Long studentId) {
        LocalDate today = LocalDate.now();
        Date start = Date.from(today.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<Map<String, Object>> dailyRows = learningEventMapper.sumDailyLearningSeconds(studentId, start, end);
        long totalSeconds = dailyRows.stream()
                .mapToLong(row -> readLongValue(row.get("totalSeconds")))
                .sum();
        long yesterdaySeconds = dailyRows.stream()
                .filter(row -> today.minusDays(1).toString().equals(String.valueOf(row.get("studyDate"))))
                .mapToLong(row -> readLongValue(row.get("totalSeconds")))
                .findFirst()
                .orElse(0L);
        if (yesterdaySeconds >= 1800 && totalSeconds >= 7L * 1200L) {
            return List.of();
        }

        LambdaQueryWrapper<StudentKnowledgeMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentKnowledgeMastery::getStudentId, studentId)
                .eq(StudentKnowledgeMastery::getIsDelete, 0)
                .orderByAsc(StudentKnowledgeMastery::getMasteryScore)
                .last("limit 3");
        List<StudentKnowledgeMastery> weakPoints = masteryMapper.selectList(wrapper);
        if (weakPoints.isEmpty()) {
            BehaviorSignal signal = new BehaviorSignal();
            signal.source = SOURCE_LEARNING_HISTORY;
            signal.knowledgeName = "学习节奏";
            signal.keywordText = "复习 巩固 基础";
            signal.weight = 90;
            signal.reason = "昨天学习记录较少，今天先安排一份基础复习资源，帮助恢复学习节奏。";
            signal.suggestion = "先投入 15-25 分钟完成一段短学习，再根据掌握情况继续练习。";
            return List.of(signal);
        }
        return weakPoints.stream().map(item -> {
            BehaviorSignal signal = new BehaviorSignal();
            signal.source = SOURCE_LEARNING_HISTORY;
            signal.courseId = item.getCourseId();
            signal.chapterId = item.getChapterId();
            signal.knowledgeName = item.getKnowledgeName();
            signal.keywordText = item.getEvidenceSummary();
            signal.weight = 100 + Math.max(0, 80 - defaultZero(item.getMasteryScore()));
            signal.reason = "结合昨天和近 7 天学习时长，系统优先安排薄弱点“"
                    + StringUtils.defaultIfBlank(item.getKnowledgeName(), "相关知识点")
                    + "”的补学资源。";
            signal.suggestion = "先看讲解资源，再完成配套练习，避免只浏览不巩固。";
            return signal;
        }).collect(Collectors.toList());
    }

    private List<ResourceCandidate> resourceCandidatesForSignal(StudentDailyRecommendationSession session, BehaviorSignal signal) {
        Set<String> keywords = new LinkedHashSet<>();
        addKeyword(keywords, signal.knowledgeName);
        addKeyword(keywords, signal.keywordText);
        addKeyword(keywords, signal.courseName);
        addKeyword(keywords, signal.chapterTitle);
        if (keywords.isEmpty()) {
            keywords.add("学习重点");
        }
        List<ResourceCandidate> candidates = new ArrayList<>();
        candidates.addAll(courseChapterCandidatesForSignal(signal, keywords));
        candidates.addAll(videoCourseCandidates(session, keywords));
        candidates.addAll(microVideoCandidates(session, keywords));
        candidates.addAll(textCourseCandidates(session, keywords));
        for (ResourceCandidate candidate : candidates) {
            candidate.score += signal.weight;
            if (signal.courseId != null && Objects.equals(signal.courseId, candidate.courseId)) {
                candidate.score += 80;
            }
            if (signal.chapterId != null && Objects.equals(signal.chapterId, candidate.chapterId)) {
                candidate.score += 45;
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparingInt((ResourceCandidate candidate) -> candidate.score).reversed())
                .collect(Collectors.toList());
    }

    private List<ResourceCandidate> courseChapterCandidatesForSignal(BehaviorSignal signal, Set<String> keywords) {
        if (signal.courseId == null) {
            return List.of();
        }
        Course course = courseMapper.selectById(signal.courseId);
        if (course == null || defaultZero(course.getIsDelete()) == 1) {
            return List.of();
        }
        LambdaQueryWrapper<CourseChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseChapter::getCourseId, signal.courseId)
                .eq(CourseChapter::getIsDelete, 0);
        if (signal.chapterId != null) {
            wrapper.eq(CourseChapter::getId, signal.chapterId);
        }
        wrapper.orderByAsc(CourseChapter::getSortOrder)
                .orderByAsc(CourseChapter::getId)
                .last("limit 12");
        return courseChapterMapper.selectList(wrapper).stream().map(chapter -> {
            ResourceCandidate candidate = new ResourceCandidate();
            candidate.resourceId = chapter.getId();
            candidate.courseId = signal.courseId;
            candidate.chapterId = chapter.getId();
            candidate.resourceType = "course_chapter_video";
            candidate.title = StringUtils.defaultIfBlank(course.getName(), "视频课程")
                    + " · " + StringUtils.defaultIfBlank(chapter.getTitle(), "课程章节");
            candidate.description = StringUtils.defaultString(course.getDescription())
                    + " " + StringUtils.defaultString(chapter.getTitle());
            candidate.keywordScore = score(candidate.title, candidate.description, keywords);
            candidate.score = candidate.keywordScore + 90;
            return candidate;
        }).collect(Collectors.toList());
    }

    private List<BehaviorSignal> videoBehaviorSignals(Long studentId) {
        Date after = new Date(System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L);
        LambdaQueryWrapper<VideoLearningEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoLearningEvent::getStudentId, studentId)
                .ge(VideoLearningEvent::getEventTime, after)
                .in(VideoLearningEvent::getEventType, "seek_backward", "pause", "intervention_shown")
                .orderByDesc(VideoLearningEvent::getEventTime)
                .last("limit 300");
        List<VideoLearningEvent> events = videoLearningEventMapper.selectList(wrapper);
        if (events.isEmpty()) {
            return List.of();
        }

        Map<String, VideoSignalStats> statsMap = new HashMap<>();
        for (VideoLearningEvent event : events) {
            String key = event.getCourseId() + ":" + event.getChapterId() + ":" + event.getSegmentId();
            VideoSignalStats stats = statsMap.computeIfAbsent(key, ignored -> {
                VideoSignalStats item = new VideoSignalStats();
                item.courseId = event.getCourseId();
                item.chapterId = event.getChapterId();
                item.segmentId = event.getSegmentId();
                return item;
            });
            if ("seek_backward".equals(event.getEventType())) {
                stats.rewatchCount++;
            } else if ("pause".equals(event.getEventType())) {
                stats.pauseSeconds += defaultZero(event.getDurationSecond());
            } else if ("intervention_shown".equals(event.getEventType())) {
                stats.interventionCount++;
            }
        }

        Map<Long, Course> courseMap = loadCourses(statsMap.values().stream().map(stats -> stats.courseId).collect(Collectors.toSet()));
        Map<Long, CourseChapter> chapterMap = loadChapters(statsMap.values().stream().map(stats -> stats.chapterId).collect(Collectors.toSet()));
        Map<Long, VideoKnowledgeSegment> segmentMap = loadSegments(statsMap.values().stream().map(stats -> stats.segmentId).collect(Collectors.toSet()));
        return statsMap.values().stream()
                .filter(stats -> stats.rewatchCount >= 2 || stats.pauseSeconds >= 60 || stats.interventionCount > 0)
                .map(stats -> {
                    Course course = nullableMapGet(courseMap, stats.courseId);
                    CourseChapter chapter = nullableMapGet(chapterMap, stats.chapterId);
                    VideoKnowledgeSegment segment = nullableMapGet(segmentMap, stats.segmentId);
                    BehaviorSignal signal = new BehaviorSignal();
                    signal.source = SOURCE_VIDEO_BEHAVIOR;
                    signal.courseId = stats.courseId;
                    signal.chapterId = stats.chapterId;
                    signal.courseName = course == null ? "" : course.getName();
                    signal.chapterTitle = chapter == null ? "" : chapter.getTitle();
                    signal.knowledgeName = StringUtils.defaultIfBlank(
                            segment == null ? null : segment.getKnowledgeName(),
                            StringUtils.defaultIfBlank(signal.chapterTitle, signal.courseName));
                    signal.keywordText = String.join(" ",
                            StringUtils.defaultString(signal.courseName),
                            StringUtils.defaultString(signal.chapterTitle),
                            StringUtils.defaultString(signal.knowledgeName),
                            segment == null ? "" : StringUtils.defaultString(segment.getDescription()));
                    signal.weight = 110 + stats.rewatchCount * 20 + stats.pauseSeconds / 8 + stats.interventionCount * 35;
                    signal.reason = "你最近在"
                            + displayScope(signal)
                            + buildVideoBehaviorText(stats)
                            + "，系统优先为你补充相关讲解资源。";
                    signal.suggestion = "先用 15-25 分钟复看推荐资源，再回到原章节把卡住的位置顺一遍。";
                    return signal;
                })
                .sorted(Comparator.comparingInt((BehaviorSignal signal) -> signal.weight).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<BehaviorSignal> assessmentBehaviorSignals(Long studentId) {
        Date after = new Date(System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L);
        LambdaQueryWrapper<HomeworkSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkSubmission::getStudentId, studentId)
                .eq(HomeworkSubmission::getIsDelete, 0)
                .ge(HomeworkSubmission::getSubmitTime, after)
                .orderByDesc(HomeworkSubmission::getSubmitTime)
                .last("limit 30");
        List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectList(wrapper);
        if (submissions.isEmpty()) {
            return List.of();
        }

        Map<Long, HomeworkAssignment> assignmentMap = loadAssignments(
                submissions.stream().map(HomeworkSubmission::getAssignmentId).collect(Collectors.toSet()));
        Map<Long, List<HomeworkSubmissionDetail>> detailMap = loadSubmissionDetails(
                submissions.stream().map(HomeworkSubmission::getId).collect(Collectors.toSet()));
        Map<Long, Course> courseMap = loadCourses(submissions.stream().map(HomeworkSubmission::getCourseId).collect(Collectors.toSet()));
        Map<Long, CourseChapter> chapterMap = loadChapters(assignmentMap.values().stream().map(HomeworkAssignment::getChapterId).collect(Collectors.toSet()));
        Map<Long, AiResource> quizResourceMap = loadAiResources(
                assignmentMap.values().stream().map(HomeworkAssignment::getQuizResourceId).collect(Collectors.toSet()));

        List<BehaviorSignal> signals = new ArrayList<>();
        for (HomeworkSubmission submission : submissions) {
            HomeworkAssignment assignment = nullableMapGet(assignmentMap, submission.getAssignmentId());
            String assignmentType = assignment == null ? "" : StringUtils.defaultString(assignment.getAssignmentType());
            boolean exam = "exam".equals(assignmentType);
            boolean homework = "homework".equals(assignmentType) || "chapter_practice".equals(assignmentType);
            if (!exam && !homework) {
                continue;
            }
            List<HomeworkSubmissionDetail> weakDetails = detailMap.getOrDefault(submission.getId(), List.of()).stream()
                    .filter(this::isWeakSubmissionDetail)
                    .collect(Collectors.toList());
            int wrongCount = defaultZero(submission.getWrongCount());
            boolean lowScore = isLowScoreSubmission(submission, assignment);
            if (wrongCount <= 0 && weakDetails.isEmpty() && !lowScore) {
                continue;
            }

            Long courseId = submission.getCourseId();
            Long chapterId = assignment == null ? null : assignment.getChapterId();
            Course course = nullableMapGet(courseMap, courseId);
            CourseChapter chapter = nullableMapGet(chapterMap, chapterId);
            AiResource quizResource = assignment == null
                    ? null
                    : nullableMapGet(quizResourceMap, assignment.getQuizResourceId());
            HomeworkSubmissionDetail firstWeakDetail = weakDetails.isEmpty() ? null : weakDetails.get(0);
            List<HomeworkSubmissionDetail> keywordDetails = weakDetails.stream().limit(3).collect(Collectors.toList());
            int evidenceCount = Math.max(wrongCount, weakDetails.size());
            BehaviorSignal signal = new BehaviorSignal();
            signal.source = exam ? SOURCE_EXAM_BEHAVIOR : SOURCE_HOMEWORK_BEHAVIOR;
            signal.courseId = courseId;
            signal.chapterId = chapterId;
            signal.courseName = course == null ? "" : course.getName();
            signal.chapterTitle = StringUtils.defaultIfBlank(
                    chapter == null ? null : chapter.getTitle(),
                    assignment == null ? "" : assignment.getChapterTitleSnapshot());
            signal.knowledgeName = resolveAssessmentKnowledge(assignment, firstWeakDetail, signal.chapterTitle, quizResource);
            signal.keywordText = buildAssessmentKeywordText(assignment, keywordDetails, signal.courseName, signal.chapterTitle, quizResource);
            signal.weight = (exam ? 125 : 95)
                    + wrongCount * (exam ? 18 : 12)
                    + weakDetails.size() * (exam ? 14 : 10)
                    + (lowScore ? (exam ? 35 : 20) : 0);
            signal.reason = (exam ? "最近考试" : "最近作业")
                    + "中，" + displayScope(signal)
                    + "相关题目出现失分或错题，系统为你匹配讲解型资源。";
            signal.reason = buildAssessmentReason(exam, signal, evidenceCount, lowScore);
            signal.suggestion = exam
                    ? "先看推荐资源梳理失分点，再回看考试报告里的错题解析。"
                    : "先补推荐资源中的关键讲解，再回到作业错题做一次订正。";
            signals.add(signal);
        }
        return signals.stream()
                .sorted(Comparator.comparingInt((BehaviorSignal signal) -> signal.weight).reversed())
                .limit(6)
                .collect(Collectors.toList());
    }

    private void syncAssessmentMastery(Long studentId) {
        Date after = new Date(System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L);
        LambdaQueryWrapper<HomeworkSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkSubmission::getStudentId, studentId)
                .eq(HomeworkSubmission::getIsDelete, 0)
                .ge(HomeworkSubmission::getSubmitTime, after)
                .orderByDesc(HomeworkSubmission::getSubmitTime)
                .last("limit 40");
        List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectList(wrapper);
        if (submissions.isEmpty()) {
            return;
        }

        Map<Long, HomeworkAssignment> assignmentMap = loadAssignments(
                submissions.stream().map(HomeworkSubmission::getAssignmentId).collect(Collectors.toSet()));
        Map<Long, List<HomeworkSubmissionDetail>> detailMap = loadSubmissionDetails(
                submissions.stream().map(HomeworkSubmission::getId).collect(Collectors.toSet()));
        Map<Long, CourseChapter> chapterMap = loadChapters(
                assignmentMap.values().stream().map(HomeworkAssignment::getChapterId).collect(Collectors.toSet()));
        Map<Long, AiResource> quizResourceMap = loadAiResources(
                assignmentMap.values().stream().map(HomeworkAssignment::getQuizResourceId).collect(Collectors.toSet()));

        Map<String, AssessmentMasteryEvidence> evidenceMap = new HashMap<>();
        for (HomeworkSubmission submission : submissions) {
            HomeworkAssignment assignment = nullableMapGet(assignmentMap, submission.getAssignmentId());
            String assignmentType = assignment == null ? "" : StringUtils.defaultString(assignment.getAssignmentType());
            boolean exam = "exam".equals(assignmentType);
            boolean homework = "homework".equals(assignmentType) || "chapter_practice".equals(assignmentType);
            if (!exam && !homework) {
                continue;
            }
            List<HomeworkSubmissionDetail> weakDetails = detailMap.getOrDefault(submission.getId(), List.of()).stream()
                    .filter(this::isWeakSubmissionDetail)
                    .collect(Collectors.toList());
            boolean lowScore = isLowScoreSubmission(submission, assignment);
            int wrongCount = Math.max(defaultZero(submission.getWrongCount()), weakDetails.size());
            if (wrongCount <= 0 && !lowScore) {
                continue;
            }

            Long chapterId = assignment == null ? null : assignment.getChapterId();
            CourseChapter chapter = nullableMapGet(chapterMap, chapterId);
            AiResource quizResource = assignment == null
                    ? null
                    : nullableMapGet(quizResourceMap, assignment.getQuizResourceId());
            HomeworkSubmissionDetail firstWeakDetail = weakDetails.isEmpty() ? null : weakDetails.get(0);
            String chapterTitle = StringUtils.defaultIfBlank(
                    chapter == null ? null : chapter.getTitle(),
                    assignment == null ? "" : assignment.getChapterTitleSnapshot());
            String knowledge = resolveAssessmentKnowledge(assignment, firstWeakDetail, chapterTitle, quizResource);
            if (StringUtils.isBlank(knowledge)) {
                continue;
            }

            String key = studentId + ":" + submission.getCourseId() + ":" + chapterId + ":" + normalize(knowledge);
            AssessmentMasteryEvidence evidence = evidenceMap.computeIfAbsent(key, ignored -> new AssessmentMasteryEvidence());
            evidence.courseId = submission.getCourseId();
            evidence.chapterId = chapterId;
            evidence.knowledgeName = knowledge;
            evidence.examCount += exam ? 1 : 0;
            evidence.homeworkCount += exam ? 0 : 1;
            evidence.wrongCount += wrongCount;
            evidence.lowScoreCount += lowScore ? 1 : 0;
            evidence.scoreTotal += assessmentScorePercent(submission, assignment);
            evidence.scoreCount++;
            evidence.lastTime = later(evidence.lastTime, submission.getSubmitTime());
        }

        for (AssessmentMasteryEvidence evidence : evidenceMap.values()) {
            upsertAssessmentMastery(studentId, evidence);
        }
    }

    private void upsertAssessmentMastery(Long studentId, AssessmentMasteryEvidence evidence) {
        LambdaQueryWrapper<StudentKnowledgeMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentKnowledgeMastery::getStudentId, studentId)
                .eq(StudentKnowledgeMastery::getKnowledgeName, evidence.knowledgeName)
                .eq(StudentKnowledgeMastery::getIsDelete, 0);
        if (evidence.courseId == null) {
            wrapper.isNull(StudentKnowledgeMastery::getCourseId);
        } else {
            wrapper.eq(StudentKnowledgeMastery::getCourseId, evidence.courseId);
        }
        if (evidence.chapterId == null) {
            wrapper.isNull(StudentKnowledgeMastery::getChapterId);
        } else {
            wrapper.eq(StudentKnowledgeMastery::getChapterId, evidence.chapterId);
        }
        wrapper.last("limit 1");
        StudentKnowledgeMastery mastery = masteryMapper.selectOne(wrapper);

        Date now = new Date();
        int score = evidence.resolveScore();
        if (mastery == null) {
            mastery = new StudentKnowledgeMastery();
            mastery.setStudentId(studentId);
            mastery.setKnowledgeName(evidence.knowledgeName);
            mastery.setCreateTime(now);
            mastery.setIsDelete(0);
        } else if (mastery.getMasteryScore() != null) {
            score = Math.min(mastery.getMasteryScore(), score);
        }
        mastery.setCourseId(evidence.courseId);
        mastery.setChapterId(evidence.chapterId);
        mastery.setMasteryScore(score);
        mastery.setStatus(score >= 80 ? "mastered" : score >= 60 ? "partial" : "not_mastered");
        mastery.setEvidenceSummary(evidence.summary(score));
        mastery.setLastEvidenceTime(evidence.lastTime == null ? now : evidence.lastTime);
        mastery.setUpdateTime(now);
        if (mastery.getId() == null) {
            masteryMapper.insert(mastery);
        } else {
            masteryMapper.updateById(mastery);
        }
    }

    private Date later(Date first, Date second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return second.after(first) ? second : first;
    }

    private Set<String> buildKeywords(StudentDailyRecommendationSession session, Long studentId) {
        Set<String> keywords = new LinkedHashSet<>();
        keywords.addAll(buildDemandKeywords(session));
        Course course = session.getCourseId() == null ? null : courseMapper.selectById(session.getCourseId());
        if (course != null) {
            addKeyword(keywords, course.getName());
            addKeyword(keywords, course.getDescription());
        }

        LambdaQueryWrapper<StudentKnowledgeMastery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentKnowledgeMastery::getStudentId, studentId)
                .eq(StudentKnowledgeMastery::getIsDelete, 0);
        if (session.getCourseId() != null) {
            wrapper.eq(StudentKnowledgeMastery::getCourseId, session.getCourseId());
        }
        wrapper.orderByAsc(StudentKnowledgeMastery::getMasteryScore).last("limit 3");
        masteryMapper.selectList(wrapper).forEach(item -> addKeyword(keywords, item.getKnowledgeName()));
        if (keywords.isEmpty()) {
            keywords.add("今日学习");
        }
        return keywords;
    }

    private Set<String> buildDemandKeywords(StudentDailyRecommendationSession session) {
        Set<String> keywords = new LinkedHashSet<>();
        addKeyword(keywords, session.getGoal());
        addKeyword(keywords, session.getDifficultyText());
        return keywords;
    }

    private void addKeyword(Set<String> keywords, String text) {
        String value = StringUtils.trimToEmpty(text);
        if (StringUtils.isBlank(value)) {
            return;
        }
        keywords.add(value.length() > 60 ? value.substring(0, 60) : value);
    }

    private List<ResourceCandidate> videoCourseCandidates(StudentDailyRecommendationSession session, Set<String> keywords) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getIsDelete, 0)
                .eq(Course::getType, "video")
                .eq(Course::getPublishStatus, "published")
                .orderByDesc(Course::getCreateTime)
                .last("limit 80");
        return courseMapper.selectList(wrapper).stream()
                .map(course -> {
                    ResourceCandidate candidate = new ResourceCandidate();
                    candidate.resourceId = course.getId();
                    candidate.courseId = course.getId();
                    candidate.resourceType = "video";
                    candidate.title = StringUtils.defaultIfBlank(course.getName(), "视频课程");
                    candidate.description = course.getDescription();
                    candidate.keywordScore = score(candidate.title, candidate.description, keywords);
                    candidate.score = candidate.keywordScore
                            + preferredScore(session.getPreferredResourceType(), "video")
                            + (Objects.equals(session.getCourseId(), course.getId()) ? 80 : 0);
                    return candidate;
                })
                .collect(Collectors.toList());
    }

    private List<ResourceCandidate> selectedCourseChapterCandidates(
            StudentDailyRecommendationSession session, Set<String> keywords) {
        if (session == null || session.getCourseId() == null) {
            return List.of();
        }
        Course course = courseMapper.selectById(session.getCourseId());
        if (course == null) {
            return List.of();
        }
        LambdaQueryWrapper<CourseChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseChapter::getCourseId, session.getCourseId())
                .eq(CourseChapter::getIsDelete, 0)
                .orderByAsc(CourseChapter::getSortOrder)
                .orderByAsc(CourseChapter::getId)
                .last("limit 24");
        return courseChapterMapper.selectList(wrapper).stream()
                .map(chapter -> {
                    ResourceCandidate candidate = new ResourceCandidate();
                    candidate.resourceId = chapter.getId();
                    candidate.courseId = course.getId();
                    candidate.chapterId = chapter.getId();
                    candidate.resourceType = "course_chapter_video";
                    candidate.title = StringUtils.defaultIfBlank(course.getName(), "视频课程")
                            + " · " + StringUtils.defaultIfBlank(chapter.getTitle(), "课程章节");
                    candidate.description = StringUtils.defaultString(course.getDescription())
                            + " " + StringUtils.defaultString(chapter.getTitle());
                    candidate.keywordScore = score(candidate.title, candidate.description, keywords);
                    candidate.score = candidate.keywordScore
                            + preferredScore(session.getPreferredResourceType(), "video")
                            + 70;
                    return candidate;
                })
                .collect(Collectors.toList());
    }

    private List<ResourceCandidate> microVideoCandidates(StudentDailyRecommendationSession session, Set<String> keywords) {
        LambdaQueryWrapper<AiResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiResource::getIsDelete, 0)
                .and(w -> w.eq(AiResource::getIsPublished, 1).or().isNull(AiResource::getIsPublished))
                .and(w -> w.eq(AiResource::getType, "micro_video")
                        .or().eq(AiResource::getType, "video")
                        .or().eq(AiResource::getSourceType, "micro_video")
                        .or().eq(AiResource::getSourceType, "video"))
                .orderByDesc(AiResource::getCreateTime)
                .last("limit 60");
        return aiResourceMapper.selectList(wrapper).stream()
                .map(resource -> {
                    ResourceCandidate candidate = new ResourceCandidate();
                    candidate.resourceId = resource.getId();
                    candidate.courseId = readLong(resource.getParamsJson(), "publishedCourseId");
                    candidate.chapterId = readLong(resource.getParamsJson(), "publishedChapterId");
                    candidate.resourceType = StringUtils.defaultIfBlank(resource.getType(), "micro_video");
                    candidate.title = StringUtils.defaultIfBlank(resource.getTitle(), "微课视频");
                    candidate.description = StringUtils.defaultIfBlank(resource.getContent(), resource.getParamsJson());
                    candidate.keywordScore = score(candidate.title, candidate.description, keywords);
                    candidate.score = candidate.keywordScore
                            + preferredScore(session.getPreferredResourceType(), "video");
                    return candidate;
                })
                .collect(Collectors.toList());
    }

    private List<ResourceCandidate> textCourseCandidates(StudentDailyRecommendationSession session, Set<String> keywords) {
        LambdaQueryWrapper<TextCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TextCourse::getCreateTime).last("limit 80");
        return textCourseMapper.selectList(wrapper).stream()
                .map(textCourse -> {
                    ResourceCandidate candidate = new ResourceCandidate();
                    candidate.resourceId = textCourse.getId();
                    candidate.resourceType = "text";
                    candidate.title = StringUtils.defaultIfBlank(textCourse.getName(), "图文教程");
                    candidate.description = textCourse.getDescription();
                    candidate.keywordScore = score(candidate.title, candidate.description, keywords);
                    candidate.score = candidate.keywordScore
                            + preferredScore(session.getPreferredResourceType(), "text");
                    return candidate;
                })
                .collect(Collectors.toList());
    }

    private Integer preferredScore(String preferredType, String resourceFamily) {
        if ("balanced".equals(preferredType)) {
            return 8;
        }
        return preferredType.equals(resourceFamily) ? 35 : 0;
    }

    private int score(String title, String description, Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return 0;
        }
        String haystack = normalize(StringUtils.defaultString(title) + " " + StringUtils.defaultString(description));
        int score = 0;
        for (String keyword : keywords) {
            String normalized = normalize(keyword);
            if (StringUtils.isBlank(normalized)) {
                continue;
            }
            if (haystack.contains(normalized)) {
                score += 80;
                continue;
            }
            for (String token : tokens(normalized)) {
                if (haystack.contains(token)) {
                    score += normalizedTitleContains(title, token) ? 35 : 18;
                }
            }
        }
        return score;
    }

    private int developmentDirectionScore(String title, String description, String developmentGoal) {
        Set<String> terms;
        if (StudentLearningContextService.GOAL_POSTGRADUATE.equals(developmentGoal)) {
            terms = Set.of("基础", "原理", "系统", "算法", "真题", "考试", "理论");
        } else if (StudentLearningContextService.GOAL_EMPLOYMENT.equals(developmentGoal)) {
            terms = Set.of("项目", "实战", "工程", "应用", "面试", "开发", "案例");
        } else {
            return 0;
        }
        String haystack = normalize(StringUtils.defaultString(title) + " " + StringUtils.defaultString(description));
        long matches = terms.stream().map(this::normalize).filter(haystack::contains).limit(2).count();
        return (int) matches * 25;
    }

    private String bestMatchedKeyword(String title, String description, Set<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        String haystack = normalize(StringUtils.defaultString(title) + " " + StringUtils.defaultString(description));
        for (String keyword : keywords) {
            String normalized = normalize(keyword);
            if (StringUtils.isBlank(normalized)) {
                continue;
            }
            if (haystack.contains(normalized)) {
                return limit(keyword, 48);
            }
            for (String token : tokens(normalized)) {
                if (haystack.contains(token)) {
                    return token;
                }
            }
        }
        return "";
    }

    private boolean normalizedTitleContains(String title, String token) {
        return normalize(title).contains(token);
    }

    private Set<String> tokens(String value) {
        Set<String> result = new LinkedHashSet<>();
        String normalized = normalize(value);
        for (String term : domainTerms()) {
            if (normalized.contains(term)) {
                result.add(term);
            }
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[a-z0-9]{2,}|[\\u4e00-\\u9fa5]{2,}").matcher(value);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() <= 8 && !stopTokens().contains(token)) {
                result.add(token);
            }
        }
        return result;
    }

    private Set<String> domainTerms() {
        return Set.of(
                "数据结构", "线性表", "顺序表", "链表", "单链表", "双链表", "循环链表", "数组", "栈", "队列", "树",
                "二叉树", "图", "哈希", "散列", "递归", "排序", "查找", "指针", "算法",
                "数据库", "redis", "mysql", "python", "java"
        );
    }

    private Set<String> explicitTopicFocus(StudentDailyRecommendationSession session, Set<String> demandKeywords) {
        Set<String> result = new LinkedHashSet<>();
        if (demandKeywords != null) {
            demandKeywords.forEach(keyword -> collectDomainFocus(result, keyword));
        }
        // 问卷里的“目标”和“困惑”可能只有“查漏补缺”等泛化描述，
        // 学生明确选择的课程才是当前推荐最可靠的主题信号。
        if (session != null && session.getCourseId() != null) {
            Course selectedCourse = courseMapper.selectById(session.getCourseId());
            if (selectedCourse != null) {
                collectDomainFocus(result, selectedCourse.getName());
                collectDomainFocus(result, selectedCourse.getDescription());
                collectDomainFocus(result, selectedCourse.getVideoContext());
            }
        }
        if (result.contains(normalize("数据结构"))) {
            // “算法、Java、Python”只能作为数据结构的实现方式或相关词，
            // 不能单独放宽主题边界，否则会再次混入语言入门课程。
            result.retainAll(dataStructureTerms());
        }
        return result;
    }

    private Set<String> dataStructureTerms() {
        return Set.of(
                "数据结构", "线性表", "顺序表", "链表", "单链表", "双链表", "循环链表", "数组",
                "栈", "队列", "树", "二叉树", "图", "哈希", "散列", "递归", "排序", "查找", "指针"
        ).stream().map(this::normalize).collect(Collectors.toSet());
    }

    private void collectDomainFocus(Set<String> target, String text) {
        String normalized = normalize(text);
        if (StringUtils.isBlank(normalized)) {
            return;
        }
        for (String term : domainTerms()) {
            String normalizedTerm = normalize(term);
            if (normalized.contains(normalizedTerm)) {
                target.add(normalizedTerm);
            }
        }
    }

    private boolean matchesTopicFocus(String title, String description, Set<String> topicFocus) {
        if (topicFocus == null || topicFocus.isEmpty()) {
            return true;
        }
        String haystack = normalize(StringUtils.defaultString(title) + " " + StringUtils.defaultString(description));
        return topicFocus.stream().anyMatch(haystack::contains);
    }

    private Set<String> stopTokens() {
        return Set.of("今天", "学习", "复习", "掌握", "理解", "不会", "不懂", "困难", "重点", "知识点", "课程", "内容", "需要", "希望");
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。、“”‘’（）《》【】：；！？]+", "");
    }

    private StudentResourceRecommendation toRecommendation(StudentDailyRecommendationSession session, ResourceCandidate candidate) {
        Date now = new Date();
        StudentResourceRecommendation recommendation = new StudentResourceRecommendation();
        recommendation.setStudentId(session.getStudentId());
        recommendation.setCourseId(candidate.courseId);
        recommendation.setChapterId(candidate.chapterId);
        recommendation.setResourceId(candidate.resourceId);
        recommendation.setResourceType(candidate.resourceType);
        recommendation.setResourceTitle(candidate.title);
        recommendation.setKnowledgeName(resolveKnowledgeName(session));
        recommendation.setRecommendationReason(buildReason(session, candidate));
        recommendation.setPracticeSuggestion(buildSuggestion(session, candidate.resourceType));
        recommendation.setRecommendationSource(SOURCE_DAILY_SURVEY);
        recommendation.setStatus("pending");
        recommendation.setCreateTime(now);
        recommendation.setUpdateTime(now);
        recommendation.setIsDelete(0);
        return recommendation;
    }

    private StudentResourceRecommendation fallbackRecommendation(
            StudentDailyRecommendationSession session, String keyword, int sequence) {
        Date now = new Date();
        String topic = StringUtils.defaultIfBlank(keyword, "今日学习");
        String[] titlePrefixes = {
                "继续学习", "专题阅读", "巩固练习", "知识梳理",
                "视频精讲", "图文教程", "典型例题", "进阶拓展"
        };
        StudentResourceRecommendation recommendation = new StudentResourceRecommendation();
        recommendation.setStudentId(session.getStudentId());
        recommendation.setResourceType("search");
        recommendation.setResourceTitle(titlePrefixes[Math.floorMod(sequence, titlePrefixes.length)] + "：" + topic);
        recommendation.setKnowledgeName(topic);
        recommendation.setRecommendationReason("当前同主题课程资源数量不足，已为你补充“" + topic + "”的站内资源入口。");
        recommendation.setPracticeSuggestion("打开后优先选择与“" + topic + "”直接相关的视频、图文或练习资源。");
        recommendation.setRecommendationSource(SOURCE_DAILY_SURVEY);
        recommendation.setStatus("pending");
        recommendation.setCreateTime(now);
        recommendation.setUpdateTime(now);
        recommendation.setIsDelete(0);
        return recommendation;
    }

    private String resolveKnowledgeName(StudentDailyRecommendationSession session) {
        return StringUtils.defaultIfBlank(session.getDifficultyText(),
                StringUtils.defaultIfBlank(session.getGoal(), "今日学习重点"));
    }

    private String buildReason(StudentDailyRecommendationSession session, ResourceCandidate candidate) {
        String preference = switch (session.getPreferredResourceType()) {
            case "video" -> "视频优先";
            case "text" -> "图文优先";
            default -> "均衡推荐";
        };
        String focus = StringUtils.defaultIfBlank(session.getDifficultyText(), session.getGoal());
        String typeLabel = typeLabel(candidate.resourceType);
        String directionText = switch (StringUtils.defaultString(candidate.developmentGoal)) {
            case StudentLearningContextService.GOAL_POSTGRADUATE -> "，也符合你的考研准备方向";
            case StudentLearningContextService.GOAL_EMPLOYMENT -> "，也贴近你的就业实战目标";
            default -> "";
        };
        if (StringUtils.isBlank(focus)) {
            return "根据你今天选择的“" + preference + "”，为你匹配了这份" + typeLabel + "资源" + directionText + "。";
        }
        if (candidate.demandScore > 0) {
            String matched = StringUtils.defaultIfBlank(candidate.matchedDemandKeyword, limit(focus, 48));
            return "你今天提到“" + matched + "”，系统按“" + preference + "”优先为你匹配了这份" + typeLabel + "资源" + directionText + "。";
        }
        return "这份" + typeLabel + "资源与当前课程或学习记录相关" + directionText + "，作为“" + preference + "”下的补充推荐。";
    }

    private String buildSuggestion(StudentDailyRecommendationSession session, String resourceType) {
        int minutes = session.getAvailableMinutes() == null || session.getAvailableMinutes() <= 0
                ? 20
                : Math.min(session.getAvailableMinutes(), 90);
        if ("text".equals(resourceType)) {
            return "先用 " + minutes + " 分钟读完教程重点，再记录 1 条仍不清楚的问题。";
        }
        return "先用 " + minutes + " 分钟完成资源学习，遇到卡点时暂停做笔记。";
    }

    private String typeLabel(String resourceType) {
        String type = StringUtils.defaultString(resourceType).toLowerCase(Locale.ROOT);
        if (type.contains("text") || type.contains("plan")) {
            return "图文";
        }
        if (type.contains("micro")) {
            return "微课视频";
        }
        if (type.contains("video")) {
            return "视频";
        }
        return "学习";
    }

    private StudentResourceRecommendation toBehaviorRecommendation(
            StudentDailyRecommendationSession session, BehaviorSignal signal, ResourceCandidate candidate) {
        Date now = new Date();
        StudentResourceRecommendation recommendation = new StudentResourceRecommendation();
        recommendation.setStudentId(session.getStudentId());
        recommendation.setCourseId(candidate.courseId == null ? signal.courseId : candidate.courseId);
        recommendation.setChapterId(candidate.chapterId == null ? signal.chapterId : candidate.chapterId);
        recommendation.setResourceId(candidate.resourceId);
        recommendation.setResourceType(candidate.resourceType);
        recommendation.setResourceTitle(candidate.title);
        recommendation.setKnowledgeName(StringUtils.defaultIfBlank(signal.knowledgeName, candidate.title));
        recommendation.setRecommendationReason(signal.reason);
        recommendation.setPracticeSuggestion(signal.suggestion);
        recommendation.setRecommendationSource(signal.source);
        recommendation.setStatus("pending");
        recommendation.setCreateTime(now);
        recommendation.setUpdateTime(now);
        recommendation.setIsDelete(0);
        return recommendation;
    }

    private StudentResourceRecommendation toPendingHomeworkRecommendation(
            StudentDailyRecommendationSession session, HomeworkAssignment assignment, Course course, Date now) {
        StudentResourceRecommendation recommendation = new StudentResourceRecommendation();
        recommendation.setStudentId(session.getStudentId());
        recommendation.setCourseId(assignment.getCourseId());
        recommendation.setChapterId(assignment.getChapterId());
        recommendation.setResourceId(assignment.getId());
        recommendation.setResourceType("homework_assignment");
        recommendation.setResourceTitle(StringUtils.defaultIfBlank(assignment.getTitle(), "待完成作业"));
        recommendation.setKnowledgeName(StringUtils.defaultIfBlank(
                assignment.getChapterTitleSnapshot(),
                course == null ? "作业任务" : course.getName()));
        recommendation.setRecommendationReason(buildPendingHomeworkReason(assignment, course, now));
        recommendation.setPracticeSuggestion("先完成这项临期任务，再继续安排视频或图文补学。");
        recommendation.setRecommendationSource(SOURCE_HOMEWORK_BEHAVIOR);
        recommendation.setStatus("pending");
        recommendation.setCreateTime(now);
        recommendation.setUpdateTime(now);
        recommendation.setIsDelete(0);
        return recommendation;
    }

    private String buildPendingHomeworkReason(HomeworkAssignment assignment, Course course, Date now) {
        String courseName = course == null ? "" : course.getName();
        String scope = StringUtils.isBlank(courseName) ? "" : "《" + courseName + "》";
        Date deadline = assignment.getDeadline();
        if (deadline == null) {
            return scope + "有一项未完成作业，建议今天优先处理。";
        }
        long deltaMillis = deadline.getTime() - now.getTime();
        if (deltaMillis < 0) {
            return scope + "作业已超过截止时间，建议优先补交或查看教师要求。";
        }
        long hours = Math.max(1, deltaMillis / (60L * 60L * 1000L));
        if (hours < 24) {
            return scope + "作业将在 " + hours + " 小时内截止，今天应优先完成。";
        }
        return scope + "作业将在 " + ((hours + 23) / 24) + " 天内截止，建议提前完成。";
    }

    private Map<Long, Course> loadCourses(Set<Long> ids) {
        Set<Long> safeIds = ids == null ? Set.of() : ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(safeIds).stream()
                .collect(Collectors.toMap(Course::getId, item -> item, (first, second) -> first));
    }

    private <K, V> V nullableMapGet(Map<K, V> map, K key) {
        return map == null || key == null ? null : map.get(key);
    }

    private Map<Long, CourseChapter> loadChapters(Set<Long> ids) {
        Set<Long> safeIds = ids == null ? Set.of() : ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        return courseChapterMapper.selectBatchIds(safeIds).stream()
                .collect(Collectors.toMap(CourseChapter::getId, item -> item, (first, second) -> first));
    }

    private Map<Long, VideoKnowledgeSegment> loadSegments(Set<Long> ids) {
        Set<Long> safeIds = ids == null ? Set.of() : ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        return videoKnowledgeSegmentMapper.selectBatchIds(safeIds).stream()
                .collect(Collectors.toMap(VideoKnowledgeSegment::getId, item -> item, (first, second) -> first));
    }

    private Map<Long, HomeworkAssignment> loadAssignments(Set<Long> ids) {
        Set<Long> safeIds = ids == null ? Set.of() : ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        return homeworkAssignmentMapper.selectBatchIds(safeIds).stream()
                .collect(Collectors.toMap(HomeworkAssignment::getId, item -> item, (first, second) -> first));
    }

    private Map<Long, AiResource> loadAiResources(Set<Long> ids) {
        Set<Long> safeIds = ids == null ? Set.of() : ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        return aiResourceMapper.selectBatchIds(safeIds).stream()
                .collect(Collectors.toMap(AiResource::getId, item -> item, (first, second) -> first));
    }

    private Map<Long, List<HomeworkSubmissionDetail>> loadSubmissionDetails(Set<Long> submissionIds) {
        Set<Long> safeIds = submissionIds == null ? Set.of() : submissionIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<HomeworkSubmissionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(HomeworkSubmissionDetail::getSubmissionId, safeIds)
                .orderByAsc(HomeworkSubmissionDetail::getQuestionNo);
        return homeworkSubmissionDetailMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(HomeworkSubmissionDetail::getSubmissionId));
    }

    private boolean isWeakSubmissionDetail(HomeworkSubmissionDetail detail) {
        if (detail == null) {
            return false;
        }
        if (Integer.valueOf(0).equals(detail.getIsCorrect())) {
            return true;
        }
        Integer score = detail.getScore();
        Integer fullScore = detail.getFullScore();
        return score != null && fullScore != null && score < fullScore;
    }

    private boolean isLowScoreSubmission(HomeworkSubmission submission, HomeworkAssignment assignment) {
        if (submission == null || submission.getTotalScore() == null) {
            return false;
        }
        Integer fullScore = assignment == null ? null : assignment.getTotalScore();
        if (fullScore == null || fullScore <= 0) {
            return submission.getTotalScore() < 60;
        }
        return submission.getTotalScore() * 100 < fullScore * 70;
    }

    private int assessmentScorePercent(HomeworkSubmission submission, HomeworkAssignment assignment) {
        if (submission == null || submission.getTotalScore() == null) {
            return 70;
        }
        Integer fullScore = assignment == null ? null : assignment.getTotalScore();
        if (fullScore == null || fullScore <= 0) {
            return Math.max(0, Math.min(100, submission.getTotalScore()));
        }
        return Math.max(0, Math.min(100, (int) Math.round(submission.getTotalScore() * 100.0 / fullScore)));
    }

    private String resolveAssessmentKnowledge(
            HomeworkAssignment assignment, HomeworkSubmissionDetail detail, String chapterTitle, AiResource quizResource) {
        if (StringUtils.isNotBlank(chapterTitle)) {
            return chapterTitle;
        }
        if (quizResource != null && StringUtils.isNotBlank(quizResource.getTitle())) {
            return limit(quizResource.getTitle(), 60);
        }
        if (detail != null && StringUtils.isNotBlank(detail.getStemSnapshot())) {
            return limit(detail.getStemSnapshot(), 36);
        }
        return assignment == null ? "作业错题" : StringUtils.defaultIfBlank(assignment.getTitle(), "作业错题");
    }

    private String buildAssessmentKeywordText(
            HomeworkAssignment assignment, List<HomeworkSubmissionDetail> details,
            String courseName, String chapterTitle, AiResource quizResource) {
        List<String> pieces = new ArrayList<>();
        pieces.add(courseName);
        pieces.add(chapterTitle);
        if (assignment != null) {
            pieces.add(assignment.getTitle());
            pieces.add(assignment.getQuizTitleSnapshot());
            pieces.add(assignment.getChapterTitleSnapshot());
        }
        if (quizResource != null) {
            pieces.add(quizResource.getTitle());
            pieces.add(quizResource.getContent());
            pieces.add(quizResource.getParamsJson());
        }
        for (HomeworkSubmissionDetail detail : details) {
            pieces.add(detail.getStemSnapshot());
            pieces.add(detail.getAiComment());
        }
        return pieces.stream().filter(StringUtils::isNotBlank).map(item -> limit(item, 80)).collect(Collectors.joining(" "));
    }

    private String buildAssessmentReason(boolean exam, BehaviorSignal signal, int evidenceCount, boolean lowScore) {
        String label = exam ? "最近考试" : "最近作业";
        String countText = evidenceCount > 0 ? "有 " + evidenceCount + " 道题失分或出错" : "整体得分偏低";
        if (lowScore && evidenceCount > 0) {
            countText += "，且整体得分偏低";
        }
        return label + "中，" + assessmentScope(signal) + countText + "，系统为你匹配讲解型资源。";
    }

    private String assessmentScope(BehaviorSignal signal) {
        if (StringUtils.isNotBlank(signal.courseName) && StringUtils.isNotBlank(signal.chapterTitle)) {
            return "《" + signal.courseName + "》" + signal.chapterTitle;
        }
        if (StringUtils.isNotBlank(signal.chapterTitle)) {
            return signal.chapterTitle;
        }
        if (StringUtils.isNotBlank(signal.courseName)) {
            return "《" + signal.courseName + "》";
        }
        return StringUtils.defaultIfBlank(signal.knowledgeName, "相关内容");
    }

    private String displayScope(BehaviorSignal signal) {
        if (StringUtils.isNotBlank(signal.courseName) && StringUtils.isNotBlank(signal.chapterTitle)) {
            return "《" + signal.courseName + "》" + signal.chapterTitle;
        }
        if (StringUtils.isNotBlank(signal.chapterTitle)) {
            return signal.chapterTitle;
        }
        if (StringUtils.isNotBlank(signal.courseName)) {
            return "《" + signal.courseName + "》";
        }
        return StringUtils.defaultIfBlank(signal.knowledgeName, "相关内容");
    }

    private String buildVideoBehaviorText(VideoSignalStats stats) {
        List<String> pieces = new ArrayList<>();
        if (stats.rewatchCount > 0) {
            pieces.add("回看 " + stats.rewatchCount + " 次");
        }
        if (stats.pauseSeconds > 0) {
            pieces.add("暂停约 " + stats.pauseSeconds + " 秒");
        }
        if (stats.interventionCount > 0) {
            pieces.add("触发 " + stats.interventionCount + " 次学习提示");
        }
        return pieces.isEmpty() ? "出现学习停顿" : "出现" + String.join("、", pieces);
    }

    private String recommendationKey(String resourceType, Long resourceId, Long courseId, String knowledgeName) {
        return StringUtils.defaultString(resourceType).toLowerCase(Locale.ROOT)
                + ":" + (resourceId == null ? "" : resourceId)
                + ":" + (courseId == null ? "" : courseId)
                + ":" + normalize(StringUtils.defaultString(knowledgeName));
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long readLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private DailyRecommendationTodayVO toTodayVO(
            StudentDailyRecommendationSession session, String fallbackStatus, boolean shouldPrompt, String promptType) {
        DailyRecommendationTodayVO vo = new DailyRecommendationTodayVO();
        vo.setRecommendDate(LocalDate.now());
        vo.setStatus(fallbackStatus);
        vo.setShouldPrompt(shouldPrompt);
        vo.setPromptType(promptType);
        vo.setRecommendations(listTodayRecommendations(session));
        if (session == null) {
            return vo;
        }
        vo.setSessionId(session.getId());
        vo.setRecommendDate(session.getRecommendDate());
        vo.setStatus(session.getStatus());
        vo.setCourseId(session.getCourseId());
        vo.setGoal(session.getGoal());
        vo.setDifficultyText(session.getDifficultyText());
        vo.setAvailableMinutes(session.getAvailableMinutes());
        vo.setPreferredResourceType(session.getPreferredResourceType());
        return vo;
    }

    private List<StudentLearningProfileVO.RecommendationItem> listTodayRecommendations(StudentDailyRecommendationSession session) {
        if (session == null || session.getStudentId() == null) {
            return List.of();
        }
        Long studentId = session.getStudentId();
        Set<String> topicFocus = explicitTopicFocus(session, buildDemandKeywords(session));
        Date start = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        LambdaQueryWrapper<StudentResourceRecommendation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentResourceRecommendation::getStudentId, studentId)
                .in(StudentResourceRecommendation::getRecommendationSource, TODAY_RECOMMENDATION_SOURCES)
                .eq(StudentResourceRecommendation::getIsDelete, 0)
                .ge(StudentResourceRecommendation::getCreateTime, start)
                .orderByDesc(StudentResourceRecommendation::getCreateTime)
                .last("limit 80");
        List<StudentResourceRecommendation> uniqueItems = deduplicateRecommendations(
                recommendationMapper.selectList(wrapper).stream()
                .filter(item -> SOURCE_HOMEWORK_BEHAVIOR.equals(item.getRecommendationSource())
                        || matchesTopicFocus(item.getResourceTitle(), item.getKnowledgeName(), topicFocus))
                .sorted(Comparator
                        .comparingInt((StudentResourceRecommendation item) -> sourceRank(item.getRecommendationSource()))
                        .thenComparing(StudentResourceRecommendation::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList()));
        List<StudentLearningProfileVO.RecommendationItem> recommendations = uniqueItems.stream()
                .limit(10)
                .map(this::toRecommendationVO)
                .collect(Collectors.toList());
        ensureTodayRecommendationViewMinimum(recommendations, session);
        return recommendations;
    }

    private List<StudentResourceRecommendation> deduplicateRecommendations(
            List<StudentResourceRecommendation> recommendations) {
        Set<String> usedResourceKeys = new HashSet<>();
        Set<String> usedTitles = new HashSet<>();
        List<StudentResourceRecommendation> result = new ArrayList<>();
        for (StudentResourceRecommendation recommendation : recommendations) {
            String resourceKey = storedRecommendationIdentity(recommendation);
            String titleKey = normalize(recommendation.getResourceTitle());
            if (!usedResourceKeys.add(resourceKey)
                    || (StringUtils.isNotBlank(titleKey) && !usedTitles.add(titleKey))) {
                continue;
            }
            result.add(recommendation);
        }
        return result;
    }

    private String storedRecommendationIdentity(StudentResourceRecommendation recommendation) {
        String type = StringUtils.defaultString(recommendation.getResourceType()).toLowerCase(Locale.ROOT);
        if (recommendation.getCourseId() != null && type.contains("video")) {
            return "course:" + recommendation.getCourseId();
        }
        if (recommendation.getCourseId() != null && recommendation.getChapterId() != null) {
            return "course:" + recommendation.getCourseId() + ":chapter:" + recommendation.getChapterId();
        }
        if (recommendation.getResourceId() != null) {
            return type + ":" + recommendation.getResourceId();
        }
        return "title:" + normalize(recommendation.getResourceTitle());
    }

    private void ensureTodayRecommendationViewMinimum(
            List<StudentLearningProfileVO.RecommendationItem> recommendations,
            StudentDailyRecommendationSession session) {
        if (recommendations.size() >= MIN_DAILY_RECOMMENDATION_COUNT) {
            return;
        }

        Set<String> usedResourceKeys = recommendations.stream()
                .map(this::recommendationItemIdentity)
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> usedTitles = recommendations.stream()
                .map(StudentLearningProfileVO.RecommendationItem::getResourceTitle)
                .map(this::normalize)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(HashSet::new));

        User student = new User();
        student.setId(session.getStudentId());
        for (StudentResourceRecommendation candidate : generateDailyRecommendations(session, student)) {
            if ("search".equalsIgnoreCase(candidate.getResourceType())) {
                continue;
            }
            String resourceKey = storedRecommendationIdentity(candidate);
            String titleKey = normalize(candidate.getResourceTitle());
            if (!usedResourceKeys.add(resourceKey)
                    || (StringUtils.isNotBlank(titleKey) && !usedTitles.add(titleKey))) {
                continue;
            }
            candidate.setId(-1_000_000L - recommendations.size());
            recommendations.add(toRecommendationVO(candidate));
            if (recommendations.size() >= MIN_DAILY_RECOMMENDATION_COUNT) {
                break;
            }
        }
    }

    private String recommendationItemIdentity(StudentLearningProfileVO.RecommendationItem recommendation) {
        String type = StringUtils.defaultString(recommendation.getResourceType()).toLowerCase(Locale.ROOT);
        if (recommendation.getCourseId() != null && type.contains("video")) {
            return "course:" + recommendation.getCourseId();
        }
        if (recommendation.getResourceId() != null) {
            return type + ":" + recommendation.getResourceId();
        }
        return "title:" + normalize(recommendation.getResourceTitle());
    }

    private int sourceRank(String source) {
        if (SOURCE_HOMEWORK_BEHAVIOR.equals(source)) {
            return 0;
        }
        if (SOURCE_DAILY_SURVEY.equals(source)) {
            return 1;
        }
        if (SOURCE_AI_TUTOR.equals(source)) {
            return 2;
        }
        if (SOURCE_LEARNING_HISTORY.equals(source)) {
            return 3;
        }
        if (SOURCE_VIDEO_BEHAVIOR.equals(source)) {
            return 4;
        }
        if (SOURCE_EXAM_BEHAVIOR.equals(source)) {
            return 5;
        }
        return 9;
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
        fillAction(vo, item);
        vo.setShortReason(item.getRecommendationReason());
        return vo;
    }

    private void fillResourceDisplay(StudentLearningProfileVO.RecommendationItem vo, StudentResourceRecommendation item) {
        String type = StringUtils.defaultString(item.getResourceType()).toLowerCase(Locale.ROOT);
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
        if (!"course_chapter_video".equals(type)) {
            vo.setResourceTitle(StringUtils.defaultIfBlank(course.getName(), vo.getResourceTitle()));
        }
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
        String type = StringUtils.defaultString(item.getResourceType()).toLowerCase(Locale.ROOT);
        if ("text".equals(type) && item.getResourceId() != null) {
            vo.setActionType("tutorial_read");
            vo.setActionUrl("/student/tutorial/" + item.getResourceId() + "/read");
            vo.setActionLabel("阅读教程");
            return;
        }
        if ("homework_assignment".equals(type) && item.getResourceId() != null) {
            vo.setActionType("homework_do");
            vo.setActionUrl("/student/homework/" + item.getResourceId());
            vo.setActionLabel("完成作业");
            return;
        }
        if (item.getCourseId() != null) {
            StringBuilder url = new StringBuilder("/learn/").append(item.getCourseId()).append("?from=daily");
            if (item.getChapterId() != null) {
                url.append("&chapterId=").append(item.getChapterId());
            }
            vo.setActionType("course_learn");
            vo.setActionUrl(url.toString());
            vo.setActionLabel(type.contains("video") ? "开始看课" : "开始学习");
            return;
        }
        String keyword = StringUtils.defaultIfBlank(item.getKnowledgeName(), item.getResourceTitle());
        vo.setActionType("search");
        vo.setActionUrl("/student/search?keyword=" + URLEncoder.encode(StringUtils.defaultString(keyword), StandardCharsets.UTF_8));
        vo.setActionLabel("搜索资源");
    }

    private Long readLong(String json, String field) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            long value = OBJECT_MAPPER.readTree(json).path(field).asLong(0L);
            return value <= 0 ? null : value;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String keywordText(Set<String> keywords) {
        return keywords.stream().findFirst().orElse("今日学习");
    }

    private String recommendationTopic(
            StudentDailyRecommendationSession session, Set<String> topicFocus, Set<String> fallbackKeywords) {
        if (session != null && session.getCourseId() != null) {
            Course course = courseMapper.selectById(session.getCourseId());
            if (course != null && StringUtils.isNotBlank(course.getName())) {
                return limit(course.getName(), 48);
            }
        }
        return keywordText(topicFocus == null || topicFocus.isEmpty() ? fallbackKeywords : topicFocus);
    }

    private String limit(String value, int maxLength) {
        String text = StringUtils.trimToEmpty(value);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private static class ResourceCandidate {
        private Long resourceId;
        private Long courseId;
        private Long chapterId;
        private String resourceType;
        private String title;
        private String description;
        private int keywordScore;
        private int demandScore;
        private int directionScore;
        private String developmentGoal;
        private String matchedDemandKeyword;
        private boolean topicMatched;
        private int score;
    }

    private static class BehaviorSignal {
        private String source;
        private Long courseId;
        private Long chapterId;
        private String courseName;
        private String chapterTitle;
        private String knowledgeName;
        private String keywordText;
        private String reason;
        private String suggestion;
        private int weight;
    }

    private static class AiQuestionStats {
        private Long courseId;
        private Long chapterId;
        private String knowledgeName;
        private int count;
        private Date lastTime;
        private final List<String> questions = new ArrayList<>();
    }

    private static class VideoSignalStats {
        private Long courseId;
        private Long chapterId;
        private Long segmentId;
        private int rewatchCount;
        private int pauseSeconds;
        private int interventionCount;
    }

    private static class AssessmentMasteryEvidence {
        private Long courseId;
        private Long chapterId;
        private String knowledgeName;
        private int examCount;
        private int homeworkCount;
        private int wrongCount;
        private int lowScoreCount;
        private int scoreTotal;
        private int scoreCount;
        private Date lastTime;

        private int resolveScore() {
            int averageScore = scoreCount <= 0 ? 70 : Math.max(0, Math.min(100, Math.round(scoreTotal * 1.0f / scoreCount)));
            int penalty = wrongCount * 7 + lowScoreCount * 10 + examCount * 6;
            return Math.max(20, Math.min(95, averageScore - penalty));
        }

        private String summary(int score) {
            String source = examCount > 0 && homeworkCount > 0
                    ? "最近作业和考试"
                    : examCount > 0 ? "最近考试" : "最近作业";
            return source + "中累计 " + wrongCount + " 道错题或失分题，"
                    + (lowScoreCount > 0 ? "其中 " + lowScoreCount + " 次整体得分偏低，" : "")
                    + "系统评估当前掌握度约 " + score + "%。";
        }
    }
}
