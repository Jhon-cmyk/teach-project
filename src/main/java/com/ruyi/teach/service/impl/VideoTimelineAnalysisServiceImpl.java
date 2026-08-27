package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.VideoTimelineAnalysisTaskMapper;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.VideoKnowledgeSegment;
import com.ruyi.teach.model.entity.VideoTimelineAnalysisTask;
import com.ruyi.teach.model.vo.VideoTimelineAnalysisTaskVO;
import com.ruyi.teach.service.AliyunAsrService;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.DeepSeekService;
import com.ruyi.teach.service.VideoKnowledgeSegmentService;
import com.ruyi.teach.service.VideoTimelineAnalysisService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

@Service
public class VideoTimelineAnalysisServiceImpl
        extends ServiceImpl<VideoTimelineAnalysisTaskMapper, VideoTimelineAnalysisTask>
        implements VideoTimelineAnalysisService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_SUCCEEDED = "succeeded";
    private static final String STATUS_FAILED = "failed";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private CourseService courseService;

    @Resource
    private AliyunAsrService aliyunAsrService;

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private VideoKnowledgeSegmentService videoKnowledgeSegmentService;

    @Resource
    @Qualifier("videoTimelineAnalysisExecutor")
    private Executor videoTimelineAnalysisExecutor;

    @Override
    public Long startAnalysis(Long chapterId, User loginUser) {
        return startAnalysisInternal(chapterId, loginUser, false);
    }

    @Override
    public Long startAutoAnalysis(Long chapterId, User loginUser) {
        return startAnalysisInternal(chapterId, loginUser, true);
    }

    private Long startAnalysisInternal(Long chapterId, User loginUser, boolean autoApply) {
        CourseChapter chapter = requireChapter(chapterId);
        Course course = requireTeacherOwner(chapter, loginUser);
        if (StringUtils.isBlank(chapter.getVideoUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该选集还没有上传视频，无法生成时间轴");
        }

        VideoTimelineAnalysisTask activeTask = findActiveTask(chapterId);
        if (activeTask != null) {
            return activeTask.getId();
        }

        Date now = new Date();
        VideoTimelineAnalysisTask task = new VideoTimelineAnalysisTask();
        task.setCourseId(course.getId());
        task.setChapterId(chapter.getId());
        task.setTeacherId(loginUser.getId());
        task.setStatus(STATUS_PENDING);
        task.setSourceVideoUrl(chapter.getVideoUrl());
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setIsDelete(0);
        save(task);

        try {
            videoTimelineAnalysisExecutor.execute(() -> processTask(task.getId(), autoApply));
        } catch (Exception e) {
            markFailed(task.getId(), "AI 分析任务队列已满，请稍后重试");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 分析任务队列已满，请稍后重试");
        }
        return task.getId();
    }

    @Override
    public VideoTimelineAnalysisTaskVO getTask(Long chapterId, Long taskId, User loginUser) {
        if (taskId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "taskId 不能为空");
        }
        CourseChapter chapter = requireChapter(chapterId);
        requireTeacherOwner(chapter, loginUser);

        VideoTimelineAnalysisTask task = getById(taskId);
        if (task == null || task.getIsDelete() != null && task.getIsDelete() == 1
                || !chapterId.equals(task.getChapterId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "AI 分析任务不存在");
        }
        return toVO(task);
    }

    @Override
    public VideoTimelineAnalysisTaskVO getLatestTask(Long chapterId, User loginUser) {
        CourseChapter chapter = requireChapter(chapterId);
        requireTeacherOwner(chapter, loginUser);

        LambdaQueryWrapper<VideoTimelineAnalysisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoTimelineAnalysisTask::getChapterId, chapterId)
                .eq(VideoTimelineAnalysisTask::getIsDelete, 0)
                .orderByDesc(VideoTimelineAnalysisTask::getCreateTime)
                .last("LIMIT 1");
        VideoTimelineAnalysisTask task = getOne(wrapper, false);
        return task == null ? null : toVO(task);
    }

    private void processTask(Long taskId, boolean autoApply) {
        markRunning(taskId);
        VideoTimelineAnalysisTask task = getById(taskId);
        if (task == null) {
            return;
        }

        try {
            String transcriptJson = aliyunAsrService.transcribeAudio(task.getSourceVideoUrl());
            if (StringUtils.isBlank(transcriptJson) || "[]".equals(transcriptJson.trim())) {
                throw new IllegalStateException("未识别到有效语音内容，请确认视频声音清晰且可被云端访问");
            }
            updateTranscript(taskId, transcriptJson);

            List<VideoKnowledgeSegment> segments = generateSegmentsWithRetry(task, transcriptJson);
            if (segments.isEmpty()) {
                throw new IllegalStateException("AI 未生成有效知识点时间轴");
            }

            String resultJson = OBJECT_MAPPER.writeValueAsString(segments);
            markSucceeded(taskId, resultJson);
            if (autoApply) {
                applySegmentsIfStillEmpty(task, segments);
            }
        } catch (Exception e) {
            markFailed(taskId, abbreviate(e.getMessage(), 1000));
        }
    }

    private void applySegmentsIfStillEmpty(VideoTimelineAnalysisTask task, List<VideoKnowledgeSegment> segments) {
        CourseChapter currentChapter = courseChapterService.getById(task.getChapterId());
        if (currentChapter == null || !StringUtils.equals(currentChapter.getVideoUrl(), task.getSourceVideoUrl())) {
            return;
        }
        if (!videoKnowledgeSegmentService.listByChapterId(task.getChapterId()).isEmpty()) {
            return;
        }
        Date now = new Date();
        for (VideoKnowledgeSegment segment : segments) {
            segment.setId(null);
            segment.setChapterId(task.getChapterId());
            segment.setCreateTime(now);
            segment.setUpdateTime(now);
            segment.setIsDelete(0);
        }
        videoKnowledgeSegmentService.saveBatch(segments);
    }

    private List<VideoKnowledgeSegment> generateSegmentsWithRetry(VideoTimelineAnalysisTask task, String transcriptJson) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String aiText = deepSeekService.chat(
                        buildSystemPrompt(),
                        buildUserPrompt(task, transcriptJson, attempt),
                        3000
                );
                if (StringUtils.isBlank(aiText)) {
                    throw new IllegalStateException("AI 未返回内容");
                }
                return normalizeAndValidate(task.getChapterId(), aiText);
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw new IllegalStateException("AI 结果解析失败：" + (lastError == null ? "未知错误" : lastError.getMessage()));
    }

    private String buildSystemPrompt() {
        return """
                你是教学视频知识点时间轴分析助手。
                你只能输出严格 JSON，不要输出 Markdown，不要解释。
                输出格式必须是：
                {"segments":[{"startSecond":0,"endSecond":60,"knowledgeName":"...","description":"...","difficulty":"低|中|高"}]}
                """;
    }

    private String buildUserPrompt(VideoTimelineAnalysisTask task, String transcriptJson, int attempt) {
        String transcript = abbreviate(transcriptJson, 50000);
        String retryHint = attempt == 1 ? "" : "\n上一次输出无法解析或时间段不合法，请只返回合法 JSON，并确保时间段递增且不重叠。";
        return """
                请根据下面的教学视频语音转写，生成 4-10 个知识点时间段。
                要求：
                1. startSecond/endSecond 使用整数秒。
                2. 时间段按开始时间递增，不能重叠，endSecond 必须大于 startSecond。
                3. knowledgeName 简洁明确，适合教师时间轴展示。
                4. description 写给教师审核，说明该段重点或难点。
                5. difficulty 只能是 低、中、高。
                6. 只输出 JSON，不要代码块。
                                
                courseId: %s
                chapterId: %s
                transcriptJson:
                %s
                %s
                """.formatted(task.getCourseId(), task.getChapterId(), transcript, retryHint);
    }

    private List<VideoKnowledgeSegment> normalizeAndValidate(Long chapterId, String aiText) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(extractJson(aiText));
        JsonNode segmentsNode = root.isArray() ? root : root.path("segments");
        if (!segmentsNode.isArray()) {
            throw new IllegalStateException("AI JSON 中缺少 segments 数组");
        }

        List<VideoKnowledgeSegment> segments = new ArrayList<>();
        Date now = new Date();
        int index = 1;
        for (JsonNode node : segmentsNode) {
            Integer start = readInt(node, "startSecond");
            Integer end = readInt(node, "endSecond");
            String name = text(node, "knowledgeName");
            String description = text(node, "description");
            String difficulty = normalizeDifficulty(text(node, "difficulty"));

            if (start == null || end == null || start < 0 || end <= start) {
                throw new IllegalStateException("存在不合法的时间段");
            }
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("存在空知识点名称");
            }

            VideoKnowledgeSegment segment = new VideoKnowledgeSegment();
            segment.setChapterId(chapterId);
            segment.setStartSecond(start);
            segment.setEndSecond(end);
            segment.setKnowledgeName(StringUtils.abbreviate(name.trim(), 120));
            segment.setDescription(StringUtils.abbreviate(StringUtils.defaultString(description).trim(), 500));
            segment.setDifficulty(difficulty);
            segment.setSortOrder(index++);
            segment.setCreateTime(now);
            segment.setUpdateTime(now);
            segment.setIsDelete(0);
            segments.add(segment);
        }

        if (segments.isEmpty()) {
            throw new IllegalStateException("segments 数组为空");
        }
        if (segments.size() > 10) {
            throw new IllegalStateException("知识点数量超过 10 个");
        }

        segments.sort(Comparator.comparing(VideoKnowledgeSegment::getStartSecond)
                .thenComparing(VideoKnowledgeSegment::getEndSecond));
        for (int i = 1; i < segments.size(); i++) {
            if (segments.get(i).getStartSecond() < segments.get(i - 1).getEndSecond()) {
                throw new IllegalStateException("知识点时间段存在重叠");
            }
        }
        for (int i = 0; i < segments.size(); i++) {
            segments.get(i).setSortOrder(i + 1);
        }
        return segments;
    }

    private String extractJson(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int objectIndex = value.indexOf('{');
        int arrayIndex = value.indexOf('[');
        int start;
        if (objectIndex < 0) {
            start = arrayIndex;
        } else if (arrayIndex < 0) {
            start = objectIndex;
        } else {
            start = Math.min(objectIndex, arrayIndex);
        }
        if (start < 0) {
            throw new IllegalStateException("AI 返回内容不是 JSON");
        }
        char startChar = value.charAt(start);
        int end = startChar == '[' ? value.lastIndexOf(']') : value.lastIndexOf('}');
        if (end <= start) {
            throw new IllegalStateException("AI 返回 JSON 不完整");
        }
        return value.substring(start, end + 1);
    }

    private Integer readInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        if (value.isTextual() && value.asText().trim().matches("\\d+")) {
            return Integer.parseInt(value.asText().trim());
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String normalizeDifficulty(String difficulty) {
        String value = StringUtils.defaultString(difficulty).trim();
        if ("低".equals(value) || "简单".equals(value) || "low".equalsIgnoreCase(value)) {
            return "低";
        }
        if ("高".equals(value) || "困难".equals(value) || "hard".equalsIgnoreCase(value)) {
            return "高";
        }
        return "中";
    }

    private VideoTimelineAnalysisTask findActiveTask(Long chapterId) {
        LambdaQueryWrapper<VideoTimelineAnalysisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoTimelineAnalysisTask::getChapterId, chapterId)
                .eq(VideoTimelineAnalysisTask::getIsDelete, 0)
                .in(VideoTimelineAnalysisTask::getStatus, List.of(STATUS_PENDING, STATUS_RUNNING))
                .orderByDesc(VideoTimelineAnalysisTask::getCreateTime)
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }

    private CourseChapter requireChapter(Long chapterId) {
        if (chapterId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "chapterId 不能为空");
        }
        CourseChapter chapter = courseChapterService.getById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "章节不存在");
        }
        return chapter;
    }

    private Course requireTeacherOwner(CourseChapter chapter, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Course course = courseService.getById(chapter.getCourseId());
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        if ("admin".equals(loginUser.getUserRole())) {
            return course;
        }
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可生成知识点时间轴");
        }
        if (course.getTeacherId() == null || !course.getTeacherId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能分析自己课程的选集");
        }
        return course;
    }

    private void markRunning(Long taskId) {
        VideoTimelineAnalysisTask task = new VideoTimelineAnalysisTask();
        task.setId(taskId);
        task.setStatus(STATUS_RUNNING);
        task.setStartedAt(new Date());
        task.setUpdateTime(new Date());
        updateById(task);
    }

    private void updateTranscript(Long taskId, String transcriptJson) {
        VideoTimelineAnalysisTask task = new VideoTimelineAnalysisTask();
        task.setId(taskId);
        task.setTranscriptJson(transcriptJson);
        task.setUpdateTime(new Date());
        updateById(task);
    }

    private void markSucceeded(Long taskId, String resultJson) {
        VideoTimelineAnalysisTask task = new VideoTimelineAnalysisTask();
        task.setId(taskId);
        task.setStatus(STATUS_SUCCEEDED);
        task.setResultJson(resultJson);
        task.setErrorMessage("");
        task.setFinishedAt(new Date());
        task.setUpdateTime(new Date());
        updateById(task);
    }

    private void markFailed(Long taskId, String errorMessage) {
        VideoTimelineAnalysisTask task = new VideoTimelineAnalysisTask();
        task.setId(taskId);
        task.setStatus(STATUS_FAILED);
        task.setErrorMessage(StringUtils.defaultIfBlank(errorMessage, "AI 分析失败"));
        task.setFinishedAt(new Date());
        task.setUpdateTime(new Date());
        updateById(task);
    }

    private VideoTimelineAnalysisTaskVO toVO(VideoTimelineAnalysisTask task) {
        VideoTimelineAnalysisTaskVO vo = new VideoTimelineAnalysisTaskVO();
        vo.setTaskId(task.getId());
        vo.setChapterId(task.getChapterId());
        vo.setStatus(task.getStatus());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreateTime(task.getCreateTime());
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        if (STATUS_SUCCEEDED.equals(task.getStatus()) && StringUtils.isNotBlank(task.getResultJson())) {
            try {
                ArrayNode nodes = (ArrayNode) OBJECT_MAPPER.readTree(task.getResultJson());
                List<VideoKnowledgeSegment> segments = new ArrayList<>();
                for (JsonNode node : nodes) {
                    segments.add(OBJECT_MAPPER.treeToValue(node, VideoKnowledgeSegment.class));
                }
                vo.setSegments(segments);
            } catch (Exception e) {
                vo.setErrorMessage("AI 草稿解析失败，请重新生成");
            }
        }
        return vo;
    }

    private String abbreviate(String value, int maxLength) {
        return value == null ? "" : StringUtils.abbreviate(value, maxLength);
    }
}
