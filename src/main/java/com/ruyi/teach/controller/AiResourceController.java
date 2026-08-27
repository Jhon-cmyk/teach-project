package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.AiResourceCreateRequest;
import com.ruyi.teach.model.dto.AiResourceUpdateRequest;
import com.ruyi.teach.model.dto.MicroVideoCoursePublishRequest;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseGraphNodeMapper;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.QuizResourceVO;
import com.ruyi.teach.service.AgentIndexService;
import com.ruyi.teach.service.AgentWorkflowStateSyncService;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.HomeworkAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai/resource")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Tag(name = "AI备课资源管理")
@Slf4j
public class AiResourceController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private CourseGraphNodeMapper courseGraphNodeMapper;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private AgentIndexService agentIndexService;

    @Resource
    private AgentWorkflowStateSyncService agentWorkflowStateSyncService;

    @Resource
    private CourseService courseService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private HomeworkAssignmentService homeworkAssignmentService;

    /**
     * 获取当前登录教师ID（复用 Session 鉴权逻辑）
     */
    private Long getLoginTeacherId(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        return loginUser.getId();
    }

    @Data
    public static class SaveResult {
        private Long id;
        private Boolean alreadySaved;
    }

    @Operation(summary = "保存AI生成的资源(教案/试题/课件)，自动去重")
    @PostMapping("/save")
    public BaseResponse<SaveResult> save(@Valid @RequestBody AiResourceCreateRequest requestBody,
                                         HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        AiResource resource = toAiResource(requestBody);

        // 优先按：教师 + sourceType + sourceId 查重
        AiResource existing = null;
        if (resource.getSourceId() != null && StringUtils.isNotBlank(resource.getSourceType())) {
            LambdaQueryWrapper<AiResource> qw = new LambdaQueryWrapper<>();
            qw.eq(AiResource::getTeacherId, teacherId)
                    .eq(AiResource::getSourceType, resource.getSourceType())
                    .eq(AiResource::getSourceId, resource.getSourceId())
                    .eq(AiResource::getIsDelete, 0)
                    .last("limit 1");
            existing = aiResourceMapper.selectOne(qw);
        }

        // 如果没有来源字段，再按：教师 + 类型 + 标题 + 内容 兜底查重
        if (existing == null) {
            LambdaQueryWrapper<AiResource> qw = new LambdaQueryWrapper<>();
            qw.eq(AiResource::getTeacherId, teacherId)
                    .eq(AiResource::getType, resource.getType())
                    .eq(AiResource::getTitle, resource.getTitle())
                    .eq(AiResource::getContent, resource.getContent())
                    .eq(AiResource::getIsDelete, 0)
                    .last("limit 1");
            existing = aiResourceMapper.selectOne(qw);
        }

        SaveResult result = new SaveResult();

        if (existing != null) {
            boolean needBackfill = false;

            if (existing.getSourceId() == null && resource.getSourceId() != null) {
                existing.setSourceId(resource.getSourceId());
                needBackfill = true;
            }

            if (StringUtils.isBlank(existing.getSourceType()) && StringUtils.isNotBlank(resource.getSourceType())) {
                existing.setSourceType(resource.getSourceType());
                needBackfill = true;
            }

            if (needBackfill) {
                aiResourceMapper.updateById(existing);
                agentIndexService.upsertAiResource(existing);
            }

            result.setId(existing.getId());
            result.setAlreadySaved(true);
            markWorkflowSaved(
                    requestBody.getAgentRequestId(),
                    teacherId,
                    existing.getId()
            );
            return ResultUtils.success(result);
        }

        resource.setTeacherId(teacherId);
        if (resource.getIsPublished() == null) {
            resource.setIsPublished(0);
        }
        if (resource.getIsDelete() == null) {
            resource.setIsDelete(0);
        }

        aiResourceMapper.insert(resource);
        agentIndexService.upsertAiResource(resource);

        result.setId(resource.getId());
        result.setAlreadySaved(false);
        markWorkflowSaved(
                requestBody.getAgentRequestId(),
                teacherId,
                resource.getId()
        );
        return ResultUtils.success(result);
    }

    @Operation(summary = "查询当前教师已保存的资源key列表")
    @GetMapping("/saved-keys")
    public BaseResponse<List<String>> savedKeys(HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);

        LambdaQueryWrapper<AiResource> qw = new LambdaQueryWrapper<>();
        qw.eq(AiResource::getTeacherId, teacherId)
                .eq(AiResource::getIsDelete, 0)
                .isNotNull(AiResource::getSourceId)
                .isNotNull(AiResource::getSourceType)
                .orderByDesc(AiResource::getCreateTime);

        List<AiResource> list = aiResourceMapper.selectList(qw);

        List<String> keys = list.stream()
                .filter(item -> item.getSourceId() != null && StringUtils.isNotBlank(item.getSourceType()))
                .map(item -> item.getSourceType() + "-" + item.getSourceId())
                .distinct()
                .collect(Collectors.toList());

        return ResultUtils.success(keys);
    }

    @Operation(summary = "查询我的资源列表(可按类型筛选)")
    @GetMapping("/list")
    public BaseResponse<List<AiResource>> list(
            @RequestParam(required = false) String type,
            HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        LambdaQueryWrapper<AiResource> qw = new LambdaQueryWrapper<>();
        qw.eq(AiResource::getTeacherId, teacherId)
                .eq(AiResource::getIsDelete, 0)
                .eq(type != null, AiResource::getType, type)
                .orderByDesc(AiResource::getCreateTime);
        return ResultUtils.success(aiResourceMapper.selectList(qw));
    }

    @Operation(summary = "获取我的试卷列表(含场景和题数)")
    @GetMapping("/quiz/list")
    public BaseResponse<List<QuizResourceVO>> quizList(HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        LambdaQueryWrapper<AiResource> qw = new LambdaQueryWrapper<>();
        qw.eq(AiResource::getTeacherId, teacherId)
                .eq(AiResource::getType, "quiz")
                .eq(AiResource::getIsDelete, 0)
                .orderByDesc(AiResource::getCreateTime);

        List<AiResource> list = aiResourceMapper.selectList(qw);
        List<QuizResourceVO> result = new ArrayList<>();

        for (AiResource r : list) {
            QuizResourceVO vo = new QuizResourceVO();
            vo.setId(r.getId());
            vo.setTitle(r.getTitle());
            vo.setContent(r.getContent());
            vo.setParamsJson(r.getParamsJson());
            vo.setIsPublished(r.getIsPublished());
            vo.setCreateTime(r.getCreateTime());

            // parse scenario and questionCount from paramsJson
            if (StringUtils.isNotBlank(r.getParamsJson())) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om =
                            new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = om.readTree(r.getParamsJson());

                    if (root.has("scenario")) {
                        vo.setScenario(root.get("scenario").asText());
                    }

                    // calculate questionCount from typeCounts
                    if (root.has("typeCounts")) {
                        com.fasterxml.jackson.databind.JsonNode counts = root.get("typeCounts");
                        int total = 0;
                        java.util.Iterator<com.fasterxml.jackson.databind.JsonNode> it = counts.elements();
                        while (it.hasNext()) {
                            total += it.next().asInt(0);
                        }
                        vo.setQuestionCount(total);
                    }
                } catch (Exception ignored) {
                }
            }

            if (vo.getScenario() == null) {
                vo.setScenario("课堂检测");
            }
            if (vo.getQuestionCount() == null) {
                vo.setQuestionCount(0);
            }

            result.add(vo);
        }

        return ResultUtils.success(result);
    }

    @Operation(summary = "删除资源(逻辑删除)")
    @PostMapping("/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        AiResource existing = aiResourceMapper.selectById(id);
        if (existing == null || !existing.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权操作该资源");
        }
        aiResourceMapper.deleteById(id);
        softDeleteAssignmentsByQuizResource(teacherId, id);
        agentIndexService.deleteAiResource(teacherId, id);
        return ResultUtils.success(true);
    }

    private void softDeleteAssignmentsByQuizResource(Long teacherId, Long quizResourceId) {
        if (teacherId == null || quizResourceId == null) {
            return;
        }
        HomeworkAssignment update = new HomeworkAssignment();
        update.setIsDelete(1);
        update.setUpdateTime(new Date());

        LambdaQueryWrapper<HomeworkAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkAssignment::getTeacherId, teacherId)
                .eq(HomeworkAssignment::getQuizResourceId, quizResourceId)
                .eq(HomeworkAssignment::getIsDelete, 0)
                .in(HomeworkAssignment::getAssignmentType, "homework", "exam", "chapter_practice");
        homeworkAssignmentService.update(update, wrapper);
    }

    @Operation(summary = "更新资源为已发布状态")
    @PostMapping("/publish/{id}")
    public BaseResponse<Boolean> publishResource(@PathVariable Long id, HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);

        AiResource existing = aiResourceMapper.selectById(id);
        if (existing == null || !existing.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权操作该资源或资源不存在");
        }

        AiResource resource = new AiResource();
        resource.setId(id);
        resource.setIsPublished(1);

        boolean result = aiResourceMapper.updateById(resource) > 0;
        return ResultUtils.success(result);
    }

    @Operation(summary = "Publish a micro video resource to a teacher course chapter")
    @PostMapping("/micro-video/publish-to-course")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<MicroVideoCoursePublishResult> publishMicroVideoToCourse(
            @Valid @RequestBody MicroVideoCoursePublishRequest req,
            HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        if (req == null || req.getResourceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "resourceId is required");
        }

        AiResource existing = aiResourceMapper.selectById(req.getResourceId());
        if (existing == null || !teacherId.equals(existing.getTeacherId()) || Objects.equals(existing.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "resource not found or no permission");
        }
        if (!"micro_video".equalsIgnoreCase(StringUtils.defaultString(existing.getType()))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "only micro_video resources can be published to course");
        }

        ObjectNode params = parseParams(existing.getParamsJson());
        String videoUrl = StringUtils.trimToEmpty(params.path("videoUrl").asText(""));
        if (StringUtils.isBlank(videoUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "micro video url is missing");
        }

        String resourceTitle = StringUtils.defaultIfBlank(existing.getTitle(), "AI micro video");
        String chapterTitle = StringUtils.defaultIfBlank(req.getChapterTitle(), resourceTitle).trim();
        Course course;
        int existingChapterCount;

        if (req.getCourseId() == null) {
            course = createMicroVideoCourse(teacherId, request, existing, params, videoUrl, req);
            existingChapterCount = 0;
        } else {
            course = courseService.getById(req.getCourseId());
            if (course == null || Objects.equals(course.getIsDelete(), 1) || !teacherId.equals(course.getTeacherId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "course not found or no permission");
            }
            existingChapterCount = countActiveChapters(course.getId());
        }

        int sortOrder = req.getSortOrder() != null && req.getSortOrder() > 0
                ? req.getSortOrder()
                : nextChapterSortOrder(course.getId());

        CourseChapter chapter = new CourseChapter();
        chapter.setCourseId(course.getId());
        chapter.setTitle(chapterTitle);
        chapter.setVideoUrl(videoUrl);
        chapter.setSortOrder(sortOrder);
        chapter.setIsDelete(0);
        boolean saved = courseChapterService.save(chapter);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "failed to create course chapter");
        }

        if (StringUtils.isBlank(course.getVideoUrl()) || existingChapterCount == 0) {
            Course updateCourse = new Course();
            updateCourse.setId(course.getId());
            updateCourse.setVideoUrl(videoUrl);
            courseService.updateById(updateCourse);
        }

        params.put("publishedCourseId", course.getId());
        params.put("publishedChapterId", chapter.getId());
        params.put("publishedAt", Instant.now().toString());
        params.put("publishMode", "course_chapter");

        AiResource update = new AiResource();
        update.setId(existing.getId());
        update.setIsPublished(1);
        update.setParamsJson(params.toString());
        aiResourceMapper.updateById(update);
        agentIndexService.upsertAiResource(aiResourceMapper.selectById(existing.getId()));

        MicroVideoCoursePublishResult result = new MicroVideoCoursePublishResult();
        result.setCourseId(course.getId());
        result.setChapterId(chapter.getId());
        result.setCourseName(course.getName());
        result.setChapterTitle(chapter.getTitle());
        result.setVideoUrl(videoUrl);
        return ResultUtils.success(result);
    }

    @Operation(summary = "更新我的资源")
    @PostMapping("/update")
    public BaseResponse<Boolean> update(@Valid @RequestBody AiResourceUpdateRequest requestBody,
                                        HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);

        AiResource existing = aiResourceMapper.selectById(requestBody.getId());
        if (existing == null || !existing.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权操作该资源");
        }

        AiResource update = new AiResource();
        update.setId(requestBody.getId());
        update.setTitle(requestBody.getTitle());
        update.setContent(requestBody.getContent());
        update.setParamsJson(requestBody.getParamsJson());
        update.setType(requestBody.getType());
        update.setIsPublished(requestBody.getIsPublished());

        aiResourceMapper.updateById(update);
        agentIndexService.upsertAiResource(aiResourceMapper.selectById(requestBody.getId()));
        markWorkflowSaved(
                requestBody.getAgentRequestId(),
                teacherId,
                requestBody.getId()
        );
        return ResultUtils.success(true);
    }

    private void markWorkflowSaved(String agentRequestId,
                                   Long teacherId,
                                   Long resourceId) {
        if (StringUtils.isBlank(agentRequestId)) {
            return;
        }
        try {
            agentWorkflowStateSyncService.markSaved(
                    agentRequestId,
                    teacherId,
                    resourceId
            );
        } catch (RuntimeException e) {
            log.warn(
                    "AI workflow state sync scheduling failed after resource save, resourceId={}",
                    resourceId
            );
        }
    }

    private AiResource toAiResource(AiResourceCreateRequest requestBody) {
        AiResource resource = new AiResource();
        resource.setType(requestBody.getType());
        resource.setTitle(requestBody.getTitle());
        resource.setContent(requestBody.getContent());
        resource.setParamsJson(requestBody.getParamsJson());
        resource.setIsPublished(requestBody.getIsPublished());
        resource.setSourceId(requestBody.getSourceId());
        resource.setSourceType(requestBody.getSourceType());
        return resource;
    }

    @Operation(summary = "重建当前教师 AI 智能体检索索引")
    @PostMapping("/index/rebuild")
    public BaseResponse<Boolean> rebuildAgentIndex(HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);

        LambdaQueryWrapper<AiResource> resourceQw = new LambdaQueryWrapper<>();
        resourceQw.eq(AiResource::getTeacherId, teacherId)
                .eq(AiResource::getIsDelete, 0);

        LambdaQueryWrapper<TeachingCase> caseQw = new LambdaQueryWrapper<>();
        caseQw.eq(TeachingCase::getTeacherId, teacherId)
                .eq(TeachingCase::getIsDelete, 0);

        agentIndexService.rebuild(
                teacherId,
                aiResourceMapper.selectList(resourceQw),
                courseGraphNodeMapper.selectActiveNodes(),
                teachingCaseMapper.selectList(caseQw)
        );
        return ResultUtils.success(true);
    }

    private Course createMicroVideoCourse(Long teacherId,
                                          HttpServletRequest request,
                                          AiResource resource,
                                          ObjectNode params,
                                          String videoUrl,
                                          MicroVideoCoursePublishRequest req) {
        User loginUser = SessionUserContext.getOptional(request);
        Course course = new Course();
        course.setName(StringUtils.defaultIfBlank(req.getCourseName(), resource.getTitle()).trim());
        course.setDescription(buildMicroVideoCourseDescription(resource));
        course.setCoverImg(params.path("coverUrl").asText(""));
        course.setVideoUrl(videoUrl);
        course.setType("video");
        course.setTeacherId(teacherId);
        course.setTeacherName(loginUser == null ? "Teacher" : StringUtils.defaultIfBlank(loginUser.getUserName(), loginUser.getUserAccount()));
        course.setSourceType("teacher");
        course.setCreatorId(teacherId);
        course.setCreatorRole("teacher");
        course.setPublishStatus("published");
        course.setIsDelete(0);
        boolean saved = courseService.save(course);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "failed to create course");
        }
        return course;
    }

    private String buildMicroVideoCourseDescription(AiResource resource) {
        ObjectNode params = parseParams(resource.getParamsJson());
        String summary = "";
        try {
            JsonNode script = OBJECT_MAPPER.readTree(StringUtils.defaultString(resource.getContent(), "{}"));
            summary = script.path("summary").asText("");
        } catch (Exception ignored) {
        }
        return StringUtils.defaultIfBlank(summary, params.path("summary").asText("AI micro video course"));
    }

    private int countActiveChapters(Long courseId) {
        QueryWrapper<CourseChapter> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId).eq("is_delete", 0);
        return (int) courseChapterService.count(wrapper);
    }

    private int nextChapterSortOrder(Long courseId) {
        QueryWrapper<CourseChapter> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId)
                .eq("is_delete", 0)
                .orderByDesc("sort_order")
                .last("limit 1");
        CourseChapter latest = courseChapterService.getOne(wrapper, false);
        return latest == null || latest.getSortOrder() == null ? 1 : latest.getSortOrder() + 1;
    }

    private ObjectNode parseParams(String paramsJson) {
        if (StringUtils.isBlank(paramsJson)) {
            return OBJECT_MAPPER.createObjectNode();
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(paramsJson);
            if (node != null && node.isObject()) {
                return (ObjectNode) node;
            }
        } catch (Exception ignored) {
        }
        return OBJECT_MAPPER.createObjectNode();
    }

    @Data
    public static class MicroVideoCoursePublishResult {
        private Long courseId;
        private Long chapterId;
        private String courseName;
        private String chapterTitle;
        private String videoUrl;
    }
}
