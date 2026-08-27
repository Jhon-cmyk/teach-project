package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CourseClassRelationMapper;
import com.ruyi.teach.mapper.LearningEventMapper;
import com.ruyi.teach.mapper.VideoLearningEventMapper;
import com.ruyi.teach.mapper.VideoLearningSessionMapper;
import com.ruyi.teach.model.dto.video.VideoInterventionCheckRequest;
import com.ruyi.teach.model.dto.video.VideoLearningEventBatchRequest;
import com.ruyi.teach.model.dto.video.VideoLearningSessionStartRequest;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.CourseClassRelation;
import com.ruyi.teach.model.entity.LearningEvent;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.VideoKnowledgeSegment;
import com.ruyi.teach.model.entity.VideoLearningEvent;
import com.ruyi.teach.model.entity.VideoLearningSession;
import com.ruyi.teach.model.vo.VideoInterventionVO;
import com.ruyi.teach.model.vo.VideoLearningProfileVO;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.UserService;
import com.ruyi.teach.service.VideoKnowledgeSegmentService;
import com.ruyi.teach.service.VideoLearningService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VideoLearningServiceImpl
        extends ServiceImpl<VideoLearningSessionMapper, VideoLearningSession>
        implements VideoLearningService {

    private static final Set<String> EVENT_TYPES = Set.of(
            "play", "pause", "resume", "seek_forward", "seek_backward", "rate_change",
            "heartbeat", "ended", "intervention_shown", "intervention_clicked"
    );

    @Resource
    private VideoLearningEventMapper videoLearningEventMapper;

    @Resource
    private LearningEventMapper learningEventMapper;

    @Resource
    private VideoKnowledgeSegmentService videoKnowledgeSegmentService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private CourseService courseService;

    @Resource
    private CourseClassRelationMapper courseClassRelationMapper;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startSession(VideoLearningSessionStartRequest request, User student) {
        requireStudent(student);
        if (request == null || request.getChapterId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "chapterId不能为空");
        }

        CourseChapter chapter = courseChapterService.getById(request.getChapterId());
        if (chapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "章节不存在");
        }
        Long courseId = request.getCourseId() == null ? chapter.getCourseId() : request.getCourseId();
        if (!Objects.equals(courseId, chapter.getCourseId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "章节与课程不匹配");
        }
        requireStudentCourseAccess(student, courseId);

        Date now = new Date();
        VideoLearningSession session = new VideoLearningSession();
        session.setStudentId(student.getId());
        session.setCourseId(courseId);
        session.setChapterId(chapter.getId());
        session.setStartedAt(now);
        session.setLastEventAt(now);
        session.setStatus("active");
        session.setInterventionCount(0);
        session.setMutedUntilEnd(0);
        session.setIsDelete(0);
        save(session);
        return session.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEvents(VideoLearningEventBatchRequest request, User student) {
        requireStudent(student);
        if (request == null || request.getSessionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sessionId不能为空");
        }
        VideoLearningSession session = requireStudentSession(request.getSessionId(), student);
        List<VideoLearningEventBatchRequest.EventItem> items =
                request.getEvents() == null ? List.of() : request.getEvents();
        if (items.isEmpty()) {
            return true;
        }

        List<VideoKnowledgeSegment> segments = videoKnowledgeSegmentService.listByChapterId(session.getChapterId());
        Date now = new Date();
        for (VideoLearningEventBatchRequest.EventItem item : items) {
            if (item == null || !EVENT_TYPES.contains(trim(item.getEventType()))) {
                continue;
            }
            VideoLearningEvent event = new VideoLearningEvent();
            event.setSessionId(session.getId());
            event.setStudentId(student.getId());
            event.setCourseId(session.getCourseId());
            event.setChapterId(session.getChapterId());
            event.setSegmentId(resolveSegmentId(item, segments));
            event.setEventType(trim(item.getEventType()));
            event.setFromSecond(nonNegative(item.getFromSecond()));
            event.setToSecond(nonNegative(item.getToSecond()));
            event.setDurationSecond(nonNegative(item.getDurationSecond()));
            event.setPlaybackRate(item.getPlaybackRate());
            event.setExtraJson(item.getExtraJson());
            event.setEventTime(now);
            videoLearningEventMapper.insert(event);
            syncVideoEventToLearningEvent(event, student, segments, now);

            if ("ended".equals(event.getEventType())) {
                session.setStatus("ended");
            }
            if ("intervention_clicked".equals(event.getEventType())
                    && event.getExtraJson() != null
                    && event.getExtraJson().contains("mute")) {
                session.setMutedUntilEnd(1);
            }
        }
        session.setLastEventAt(now);
        updateById(session);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoInterventionVO checkIntervention(VideoInterventionCheckRequest request, User student) {
        requireStudent(student);
        if (request == null || request.getSessionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sessionId不能为空");
        }

        VideoLearningSession session = requireStudentSession(request.getSessionId(), student);
        VideoInterventionVO empty = emptyIntervention();
        if (defaultZero(session.getMutedUntilEnd()) == 1 || defaultZero(session.getInterventionCount()) >= 3) {
            return empty;
        }

        Long segmentId = request.getSegmentId() == null ? resolveLastSegmentId(session.getId()) : request.getSegmentId();
        if (segmentId == null) {
            return empty;
        }
        VideoKnowledgeSegment segment = videoKnowledgeSegmentService.getById(segmentId);
        if (segment == null || !Objects.equals(segment.getChapterId(), session.getChapterId())) {
            return empty;
        }
        if (countEvents(session.getId(), segmentId, "intervention_shown", null) > 0) {
            return empty;
        }

        Date threeMinutesAgo = new Date(System.currentTimeMillis() - 180_000L);
        int recentRewatch = countEvents(session.getId(), segmentId, "seek_backward", threeMinutesAgo);
        int pauseSeconds = sumDuration(session.getId(), segmentId, "pause", null);

        String reason = "";
        String riskLevel = "";
        if (recentRewatch >= 2) {
            reason = "3分钟内回看 " + recentRewatch + " 次";
            riskLevel = "high";
        } else if (pauseSeconds >= 60) {
            reason = "累计暂停 " + pauseSeconds + " 秒";
            riskLevel = "medium";
        } else {
            return empty;
        }

        VideoLearningEvent shown = new VideoLearningEvent();
        shown.setSessionId(session.getId());
        shown.setStudentId(student.getId());
        shown.setCourseId(session.getCourseId());
        shown.setChapterId(session.getChapterId());
        shown.setSegmentId(segmentId);
        shown.setEventType("intervention_shown");
        shown.setDurationSecond(0);
        shown.setEventTime(new Date());
        shown.setExtraJson("{\"reason\":\"" + reason + "\"}");
        videoLearningEventMapper.insert(shown);

        session.setInterventionCount(defaultZero(session.getInterventionCount()) + 1);
        session.setLastEventAt(new Date());
        updateById(session);

        VideoInterventionVO vo = new VideoInterventionVO();
        vo.setTriggered(true);
        vo.setRiskLevel(riskLevel);
        vo.setSegmentId(segmentId);
        vo.setKnowledgeName(segment.getKnowledgeName());
        vo.setBehaviorSummary(reason + "，疑似在该知识点需要更多解释。");
        vo.setSuggestedPrompt(buildPrompt(segment, reason));
        return vo;
    }

    @Override
    public VideoLearningProfileVO getStudentProfile(Long teacherId, String teacherRole, Long classId, Long studentId, Integer days) {
        if (classId == null || studentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "classId和studentId不能为空");
        }
        int safeDays = days == null || days <= 0 ? 7 : Math.min(days, 30);
        requireTeacherStudentAccess(teacherId, teacherRole, classId, studentId);

        Date after = new Date(System.currentTimeMillis() - safeDays * 24L * 60L * 60L * 1000L);
        LambdaQueryWrapper<VideoLearningEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoLearningEvent::getStudentId, studentId)
                .ge(VideoLearningEvent::getEventTime, after)
                .orderByDesc(VideoLearningEvent::getEventTime);
        List<VideoLearningEvent> events = videoLearningEventMapper.selectList(wrapper);

        VideoLearningProfileVO profile = new VideoLearningProfileVO();
        profile.setDays(safeDays);
        profile.setTotalEvents(events.size());
        profile.setTotalRewatchCount((int) events.stream().filter(e -> "seek_backward".equals(e.getEventType())).count());
        profile.setTotalSkipCount((int) events.stream().filter(e -> "seek_forward".equals(e.getEventType())).count());
        profile.setTotalPauseSeconds(events.stream()
                .filter(e -> "pause".equals(e.getEventType()))
                .mapToInt(e -> defaultZero(e.getDurationSecond()))
                .sum());
        profile.setTotalInterventionCount((int) events.stream().filter(e -> "intervention_shown".equals(e.getEventType())).count());
        profile.setHighSpeedEventCount((int) events.stream()
                .filter(e -> e.getPlaybackRate() != null && e.getPlaybackRate().compareTo(new BigDecimal("1.25")) >= 0)
                .count());
        profile.setLatestIntervention(resolveLatestIntervention(events));
        profile.setWeakPoints(resolveWeakPoints(events));
        profile.setWeakPointCount(profile.getWeakPoints().size());
        profile.setConclusion(resolveProfileConclusion(profile));
        return profile;
    }

    private void requireStudent(User student) {
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可上报视频学习行为");
        }
    }

    private VideoLearningSession requireStudentSession(Long sessionId, User student) {
        VideoLearningSession session = getById(sessionId);
        if (session == null || !Objects.equals(session.getStudentId(), student.getId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学习会话不存在");
        }
        return session;
    }

    private void requireStudentCourseAccess(User student, Long courseId) {
        if (student.getClassId() == null) {
            return;
        }
        LambdaQueryWrapper<CourseClassRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseClassRelation::getCourseId, courseId)
                .eq(CourseClassRelation::getClassId, student.getClassId())
                .last("limit 1");
        CourseClassRelation relation = courseClassRelationMapper.selectOne(wrapper);
        if (relation == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前班级未分配该课程");
        }
    }

    private void requireTeacherStudentAccess(Long teacherId, String teacherRole, Long classId, Long studentId) {
        User student = userService.getById(studentId);
        if (student == null || !"student".equals(student.getUserRole()) || !Objects.equals(student.getClassId(), classId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学生不存在或不属于该班级");
        }
        if ("admin".equals(teacherRole)) {
            return;
        }
        if (!"teacher".equals(teacherRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        LambdaQueryWrapper<CourseClassRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(CourseClassRelation::getClassId, classId);
        List<CourseClassRelation> relations = courseClassRelationMapper.selectList(relationWrapper);
        if (relations.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该班级学生画像");
        }
        Set<Long> courseIds = relations.stream().map(CourseClassRelation::getCourseId).collect(Collectors.toSet());
        List<Course> courses = courseService.listByIds(courseIds);
        boolean owned = courses.stream().anyMatch(course -> Objects.equals(course.getTeacherId(), teacherId));
        if (!owned) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该班级学生画像");
        }
    }

    private Long resolveSegmentId(VideoLearningEventBatchRequest.EventItem item, List<VideoKnowledgeSegment> segments) {
        if (item.getSegmentId() != null) {
            return item.getSegmentId();
        }
        Integer second = item.getToSecond() != null ? item.getToSecond() : item.getFromSecond();
        if (second == null) {
            return null;
        }
        return segments.stream()
                .filter(segment -> second >= segment.getStartSecond() && second < segment.getEndSecond())
                .map(VideoKnowledgeSegment::getId)
                .findFirst()
                .orElse(null);
    }

    private void syncVideoEventToLearningEvent(VideoLearningEvent event,
                                               User student,
                                               List<VideoKnowledgeSegment> segments,
                                               Date now) {
        String genericType = switch (event.getEventType()) {
            case "pause" -> "video_pause";
            case "seek_backward" -> "video_rewatch";
            case "heartbeat", "ended", "play" -> "video_watch";
            default -> null;
        };
        if (genericType == null) {
            return;
        }
        VideoKnowledgeSegment segment = segments.stream()
                .filter(item -> Objects.equals(item.getId(), event.getSegmentId()))
                .findFirst()
                .orElse(null);
        LearningEvent learningEvent = new LearningEvent();
        learningEvent.setStudentId(student.getId());
        learningEvent.setClassId(student.getClassId());
        learningEvent.setCourseId(event.getCourseId());
        learningEvent.setChapterId(event.getChapterId());
        learningEvent.setResourceId(event.getSegmentId());
        learningEvent.setResourceType("video");
        learningEvent.setKnowledgeName(segment == null ? "" : segment.getKnowledgeName());
        learningEvent.setEventType(genericType);
        learningEvent.setDurationSecond(event.getDurationSecond());
        learningEvent.setExtraJson(event.getExtraJson());
        learningEvent.setEventTime(event.getEventTime() == null ? now : event.getEventTime());
        learningEvent.setCreateTime(now);
        learningEvent.setIsDelete(0);
        learningEventMapper.insert(learningEvent);
    }

    private Long resolveLastSegmentId(Long sessionId) {
        LambdaQueryWrapper<VideoLearningEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoLearningEvent::getSessionId, sessionId)
                .isNotNull(VideoLearningEvent::getSegmentId)
                .orderByDesc(VideoLearningEvent::getId)
                .last("limit 1");
        VideoLearningEvent event = videoLearningEventMapper.selectOne(wrapper);
        return event == null ? null : event.getSegmentId();
    }

    private int countEvents(Long sessionId, Long segmentId, String eventType, Date after) {
        LambdaQueryWrapper<VideoLearningEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoLearningEvent::getSessionId, sessionId)
                .eq(VideoLearningEvent::getSegmentId, segmentId)
                .eq(VideoLearningEvent::getEventType, eventType);
        if (after != null) {
            wrapper.ge(VideoLearningEvent::getEventTime, after);
        }
        return Math.toIntExact(videoLearningEventMapper.selectCount(wrapper));
    }

    private int sumDuration(Long sessionId, Long segmentId, String eventType, Date after) {
        LambdaQueryWrapper<VideoLearningEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoLearningEvent::getSessionId, sessionId)
                .eq(VideoLearningEvent::getSegmentId, segmentId)
                .eq(VideoLearningEvent::getEventType, eventType);
        if (after != null) {
            wrapper.ge(VideoLearningEvent::getEventTime, after);
        }
        return videoLearningEventMapper.selectList(wrapper).stream()
                .mapToInt(event -> defaultZero(event.getDurationSecond()))
                .sum();
    }

    private VideoInterventionVO emptyIntervention() {
        VideoInterventionVO vo = new VideoInterventionVO();
        vo.setTriggered(false);
        vo.setRiskLevel("none");
        vo.setBehaviorSummary("");
        vo.setSuggestedPrompt("");
        return vo;
    }

    private String buildPrompt(VideoKnowledgeSegment segment, String reason) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("我刚才在视频知识点「").append(segment.getKnowledgeName()).append("」这里").append(reason)
                .append("。请用更容易理解的方式重新解释一遍。");
        if (segment.getDescription() != null && !segment.getDescription().trim().isEmpty()) {
            prompt.append("老师标注说明：").append(segment.getDescription().trim()).append("。");
        }
        prompt.append("请先讲直觉，再给一个小例子，最后只给一个自检问题。");
        return prompt.toString();
    }

    private VideoLearningProfileVO.LatestIntervention resolveLatestIntervention(List<VideoLearningEvent> events) {
        VideoLearningEvent latest = events.stream()
                .filter(e -> "intervention_shown".equals(e.getEventType()))
                .findFirst()
                .orElse(null);
        if (latest == null) {
            return null;
        }
        VideoLearningProfileVO.LatestIntervention vo = new VideoLearningProfileVO.LatestIntervention();
        fillNames(latest, vo);
        vo.setEventTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(latest.getEventTime()));
        return vo;
    }

    private List<VideoLearningProfileVO.WeakPoint> resolveWeakPoints(List<VideoLearningEvent> events) {
        Map<Long, SegmentStats> statsMap = new LinkedHashMap<>();
        for (VideoLearningEvent event : events) {
            if (event.getSegmentId() == null) {
                continue;
            }
            SegmentStats stats = statsMap.computeIfAbsent(event.getSegmentId(), id -> new SegmentStats());
            stats.segmentId = event.getSegmentId();
            stats.courseId = event.getCourseId();
            stats.chapterId = event.getChapterId();
            if ("seek_backward".equals(event.getEventType())) {
                stats.rewatchCount++;
            } else if ("pause".equals(event.getEventType())) {
                stats.pauseSeconds += defaultZero(event.getDurationSecond());
            } else if ("seek_forward".equals(event.getEventType())) {
                stats.skipCount++;
            } else if ("intervention_shown".equals(event.getEventType())) {
                stats.interventionCount++;
            }
            if (isDetailEvent(event.getEventType())) {
                stats.detailEvents.add(event);
            }
        }

        return statsMap.values().stream()
                .filter(stats -> stats.interventionCount > 0 || stats.rewatchCount >= 2 || stats.pauseSeconds >= 60)
                .sorted(Comparator.comparingInt(SegmentStats::score).reversed())
                .limit(8)
                .map(this::toWeakPoint)
                .collect(Collectors.toList());
    }

    private VideoLearningProfileVO.WeakPoint toWeakPoint(SegmentStats stats) {
        VideoLearningProfileVO.WeakPoint vo = new VideoLearningProfileVO.WeakPoint();
        vo.setSegmentId(stats.segmentId);
        vo.setRewatchCount(stats.rewatchCount);
        vo.setPauseSeconds(stats.pauseSeconds);
        vo.setSkipCount(stats.skipCount);
        vo.setInterventionCount(stats.interventionCount);
        vo.setConclusion(stats.interventionCount > 0 || stats.rewatchCount >= 2 ? "疑似未掌握" : "需要关注");
        vo.setBehaviorDetails(resolveBehaviorDetails(stats.detailEvents));

        VideoKnowledgeSegment segment = videoKnowledgeSegmentService.getById(stats.segmentId);
        if (segment != null) {
            vo.setKnowledgeName(segment.getKnowledgeName());
            vo.setDifficulty(segment.getDifficulty());
        }
        CourseChapter chapter = courseChapterService.getById(stats.chapterId);
        if (chapter != null) {
            vo.setChapterTitle(chapter.getTitle());
        }
        Course course = courseService.getById(stats.courseId);
        if (course != null) {
            vo.setCourseName(course.getName());
        }
        return vo;
    }

    private void fillNames(VideoLearningEvent latest, VideoLearningProfileVO.LatestIntervention vo) {
        VideoKnowledgeSegment segment = latest.getSegmentId() == null ? null : videoKnowledgeSegmentService.getById(latest.getSegmentId());
        CourseChapter chapter = courseChapterService.getById(latest.getChapterId());
        Course course = courseService.getById(latest.getCourseId());
        vo.setKnowledgeName(segment == null ? "未标注知识点" : segment.getKnowledgeName());
        vo.setChapterTitle(chapter == null ? "未知章节" : chapter.getTitle());
        vo.setCourseName(course == null ? "未知课程" : course.getName());
    }

    private String resolveProfileConclusion(VideoLearningProfileVO profile) {
        if (defaultZero(profile.getWeakPointCount()) >= 3 || defaultZero(profile.getTotalInterventionCount()) >= 3) {
            return "疑似未掌握";
        }
        if (defaultZero(profile.getWeakPointCount()) > 0 || defaultZero(profile.getTotalPauseSeconds()) >= 120) {
            return "需要关注";
        }
        return "学习稳定";
    }

    private boolean isDetailEvent(String eventType) {
        return "pause".equals(eventType)
                || "seek_backward".equals(eventType)
                || "seek_forward".equals(eventType)
                || "intervention_shown".equals(eventType);
    }

    private List<VideoLearningProfileVO.BehaviorDetail> resolveBehaviorDetails(List<VideoLearningEvent> events) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm");
        return events.stream()
                .sorted(Comparator.comparing(VideoLearningEvent::getEventTime).reversed())
                .limit(6)
                .map(event -> {
                    VideoLearningProfileVO.BehaviorDetail detail = new VideoLearningProfileVO.BehaviorDetail();
                    detail.setEventType(event.getEventType());
                    detail.setLabel(resolveBehaviorLabel(event.getEventType()));
                    detail.setFromSecond(event.getFromSecond());
                    detail.setToSecond(event.getToSecond());
                    detail.setDurationSecond(defaultZero(event.getDurationSecond()));
                    detail.setTimeRange(resolveBehaviorTimeRange(event));
                    detail.setEventTime(event.getEventTime() == null ? "" : dateFormat.format(event.getEventTime()));
                    return detail;
                })
                .collect(Collectors.toList());
    }

    private String resolveBehaviorLabel(String eventType) {
        return switch (eventType) {
            case "pause" -> "暂停";
            case "seek_backward" -> "回看";
            case "seek_forward" -> "跳过";
            case "intervention_shown" -> "辅导";
            default -> "行为";
        };
    }

    private String resolveBehaviorTimeRange(VideoLearningEvent event) {
        if ("pause".equals(event.getEventType())) {
            return formatVideoSecond(event.getToSecond() == null ? event.getFromSecond() : event.getToSecond())
                    + " 停留 " + defaultZero(event.getDurationSecond()) + "s";
        }
        if ("intervention_shown".equals(event.getEventType())) {
            return formatVideoSecond(event.getToSecond() == null ? event.getFromSecond() : event.getToSecond());
        }
        return formatVideoSecond(event.getFromSecond()) + " -> " + formatVideoSecond(event.getToSecond());
    }

    private String formatVideoSecond(Integer second) {
        int safeSecond = Math.max(0, defaultZero(second));
        int hours = safeSecond / 3600;
        int minutes = (safeSecond % 3600) / 60;
        int seconds = safeSecond % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private Integer nonNegative(Integer value) {
        return value == null ? null : Math.max(value, 0);
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static class SegmentStats {
        private Long segmentId;
        private Long courseId;
        private Long chapterId;
        private int rewatchCount;
        private int pauseSeconds;
        private int skipCount;
        private int interventionCount;
        private List<VideoLearningEvent> detailEvents = new ArrayList<>();

        private int score() {
            return interventionCount * 100 + rewatchCount * 20 + pauseSeconds + skipCount * 5;
        }
    }
}
