package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseClassRelationMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseClassRelation;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.ResourcePreviewVO;
import com.ruyi.teach.model.vo.ResourceSearchItemVO;
import com.ruyi.teach.model.vo.ResourceSearchPageVO;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.PlatformTeachingCaseService;
import com.ruyi.teach.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private CourseClassRelationMapper relationMapper;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCourseWithClasses(Course course) {
        boolean saved = this.save(course);
        if (saved) {
            saveRelations(course.getId(), course.getClassIds());
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourseWithClasses(Course course) {
        // 1. 更新主表信息（无论主表内容是否发生变化导致返回 false，都不去阻断后续逻辑）
        this.updateById(course);

        // 2. 无条件清理旧的班级关联关系
        relationMapper.delete(new QueryWrapper<CourseClassRelation>().eq("course_id", course.getId()));

        // 3. 插入新的班级关联关系 (即使传过来是空数组，也相当于清空了班级)
        saveRelations(course.getId(), course.getClassIds());

        return true;
    }

    @Override
    public ResourceSearchPageVO searchPublicResourcePage(String keyword, String type, long current, long pageSize, String sortMode, User loginUser) {
        long safeCurrent = current <= 0 ? 1 : current;
        long safePageSize = pageSize <= 0 ? 6 : Math.min(pageSize, 20);
        String safeType = StringUtils.defaultIfBlank(type, "all").trim().toLowerCase();
        String safeSortMode = StringUtils.defaultIfBlank(sortMode, "relevance").trim().toLowerCase();
        String kw = StringUtils.trimToEmpty(keyword);
        boolean studentView = loginUser != null && "student".equals(loginUser.getUserRole());

        List<ResourceSearchItemVO> allItems = new ArrayList<>();

        // 1) 公开视频课程
        List<Course> matchedCourses = this.list(buildVideoWrapper(kw, studentView));
        allItems.addAll(
                matchedCourses.stream()
                        .map(this::toVideoItem)
                        .collect(Collectors.toList())
        );

        // 2) AI 资源属于教师备课室，学生端资源检索不暴露。
        if (!studentView) {
            QueryWrapper<AiResource> aiWrapper = new QueryWrapper<>();
            aiWrapper.eq("is_delete", 0);

            if (StringUtils.isNotBlank(kw)) {
                aiWrapper.and(w -> w.like("title", kw)
                        .or()
                        .like("content", kw)
                        .or()
                        .like("params_json", kw));
            }

            aiWrapper.orderByDesc("create_time");

            List<AiResource> aiResources = aiResourceMapper.selectList(aiWrapper);
            Map<Long, String> teacherNameMap = buildTeacherNameMap(aiResources);

            allItems.addAll(
                    aiResources.stream()
                            .map(resource -> toAiResourceItem(resource, teacherNameMap))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        }

        // 3) 平台教学案例
        List<TeachingCase> platformCases = teachingCaseMapper.selectList(buildPlatformCaseWrapper(kw));
        allItems.addAll(platformCases.stream()
                .map(this::toCaseItem)
                .collect(Collectors.toList()));

        ResourceSearchPageVO result = new ResourceSearchPageVO();
        result.setCurrent(safeCurrent);
        result.setPageSize(safePageSize);
        result.setVideoCount(countByType(allItems, "video"));
        result.setPlanCount(countByType(allItems, "plan"));
        result.setQuizCount(countByType(allItems, "quiz"));
        result.setAnimCount(countByType(allItems, "anim"));
        result.setMicroVideoCount(countByType(allItems, "micro_video"));
        result.setCaseCount(countByType(allItems, "case"));
        result.setSupportNotice(studentView
                ? "当前学生端仅接入：平台公开视频课程与平台教学案例；不会返回教师备课室资源。"
                : "当前已接入：公开视频课程、教师生成教案、练习题、互动课件、微课视频与平台教学案例。");

        List<ResourceSearchItemVO> filtered = allItems;
        if (!"all".equals(safeType)) {
            filtered = allItems.stream()
                    .filter(item -> safeType.equalsIgnoreCase(item.getType()))
                    .collect(Collectors.toList());
        }

        sortItems(filtered, kw, safeSortMode);

        int start = (int) ((safeCurrent - 1) * safePageSize);
        int end = Math.min(start + (int) safePageSize, filtered.size());

        List<ResourceSearchItemVO> pageRecords;
        if (start >= filtered.size()) {
            pageRecords = Collections.emptyList();
        } else {
            pageRecords = new ArrayList<>(filtered.subList(start, end));
        }

        result.setTotal(filtered.size());
        result.setRecords(pageRecords);
        return result;
    }

    @Override
    public ResourcePreviewVO getResourcePreview(Long id, String type, User loginUser) {
        if (id == null || StringUtils.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资源参数错误");
        }

        String safeType = type.trim().toLowerCase();
        boolean studentView = loginUser != null && "student".equals(loginUser.getUserRole());

        if ("video".equals(safeType)) {
            Course course = this.getById(id);
            if (course == null
                    || !"published".equalsIgnoreCase(course.getPublishStatus())
                    || (studentView && !"platform".equalsIgnoreCase(StringUtils.defaultString(course.getSourceType())))) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资源不存在");
            }

            ResourcePreviewVO vo = new ResourcePreviewVO();
            vo.setId(course.getId());
            vo.setType("video");
            vo.setTitle(StringUtils.defaultIfBlank(course.getName(), "未命名课程"));
            vo.setAuthor(StringUtils.defaultIfBlank(course.getTeacherName(), "金牌讲师"));
            vo.setCover(StringUtils.defaultString(course.getCoverImg(), ""));
            vo.setVideoUrl(StringUtils.defaultString(course.getVideoUrl(), ""));
            vo.setContent("");
            vo.setSummary(buildVideoPreview(course));
            vo.setCreateTime(formatDate(course.getCreateTime()));
            return vo;
        }

        if ("plan".equals(safeType) || "quiz".equals(safeType) || "anim".equals(safeType) || "micro_video".equals(safeType)) {
            if (studentView) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资源不存在");
            }

            AiResource resource = aiResourceMapper.selectById(id);
            if (resource == null || Objects.equals(resource.getIsDelete(), 1)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资源不存在");
            }

            String mappedType = normalizeAiType(resource.getType());
            if (!safeType.equals(mappedType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "资源类型不匹配");
            }

            String teacherName = "未命名教师";
            if (resource.getTeacherId() != null) {
                User teacher = userService.getById(resource.getTeacherId());
                if (teacher != null) {
                    teacherName = StringUtils.defaultIfBlank(teacher.getUserName(), teacher.getUserAccount());
                }
            }

            ResourcePreviewVO vo = new ResourcePreviewVO();
            vo.setId(resource.getId());
            vo.setType(mappedType);
            vo.setTitle(resolveAiTitle(resource));
            vo.setAuthor(teacherName);
            vo.setCover(readParam(resource, "coverUrl"));
            vo.setVideoUrl(readParam(resource, "videoUrl"));
            vo.setContent(StringUtils.defaultString(resource.getContent(), ""));
            vo.setSummary(buildAiPreview(resource));
            vo.setCreateTime(formatDate(resource.getCreateTime()));
            return vo;
        }

        if ("case".equals(safeType)) {
            TeachingCase teachingCase = teachingCaseMapper.selectById(id);
            if (teachingCase == null
                    || Objects.equals(teachingCase.getIsDelete(), 1)
                    || !PlatformTeachingCaseService.SCOPE_PLATFORM.equals(StringUtils.defaultString(teachingCase.getScope()))
                    || !PlatformTeachingCaseService.STATUS_APPROVED.equals(StringUtils.defaultString(teachingCase.getStatus()))) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资源不存在");
            }

            ResourcePreviewVO vo = new ResourcePreviewVO();
            vo.setId(teachingCase.getId());
            vo.setType("case");
            vo.setTitle(StringUtils.defaultIfBlank(teachingCase.getTitle(), "未命名案例"));
            vo.setAuthor(StringUtils.defaultIfBlank(teachingCase.getSourceName(), "平台案例库"));
            vo.setCover("");
            vo.setVideoUrl("");
            vo.setContent(StringUtils.defaultString(teachingCase.getPreviewText(), ""));
            vo.setSummary(buildCasePreview(teachingCase));
            vo.setCreateTime(formatDate(teachingCase.getCreateTime()));
            return vo;
        }

        throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该资源类型");
    }

    private LambdaQueryWrapper<Course> buildVideoWrapper(String keyword, boolean studentView) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getType, "video")
                .eq(Course::getPublishStatus, "published")
                .orderByDesc(Course::getCreateTime);

        if (studentView) {
            wrapper.eq(Course::getSourceType, "platform");
        }

        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Course::getName, keyword)
                    .or()
                    .like(Course::getDescription, keyword)
                    .or()
                    .like(Course::getTeacherName, keyword)
                    .or()
                    .like(Course::getVideoContext, keyword));
        }
        return wrapper;
    }

    private LambdaQueryWrapper<TeachingCase> buildPlatformCaseWrapper(String keyword) {
        LambdaQueryWrapper<TeachingCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachingCase::getScope, PlatformTeachingCaseService.SCOPE_PLATFORM)
                .eq(TeachingCase::getStatus, PlatformTeachingCaseService.STATUS_APPROVED)
                .eq(TeachingCase::getIsDelete, 0)
                .orderByDesc(TeachingCase::getUpdateTime);

        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(TeachingCase::getTitle, keyword)
                    .or()
                    .like(TeachingCase::getCourseName, keyword)
                    .or()
                    .like(TeachingCase::getSummary, keyword)
                    .or()
                    .like(TeachingCase::getKeywords, keyword)
                    .or()
                    .like(TeachingCase::getPreviewText, keyword));
        }
        return wrapper;
    }

    private ResourceSearchItemVO toVideoItem(Course course) {
        ResourceSearchItemVO item = new ResourceSearchItemVO();
        item.setId(course.getId());
        item.setType("video");
        item.setTitle(StringUtils.defaultIfBlank(course.getName(), "未命名课程"));
        item.setDesc(StringUtils.defaultIfBlank(course.getDescription(), "暂无课程简介"));
        item.setCover(StringUtils.defaultString(course.getCoverImg(), ""));
        item.setAuthor(StringUtils.defaultIfBlank(course.getTeacherName(), "金牌讲师"));
        item.setViews(0L);
        item.setDate(formatDate(course.getCreateTime()));
        item.setCourse(StringUtils.defaultIfBlank(course.getName(), "未归类"));
        item.setDuration("视频课程");
        item.setTags(buildVideoTags(course));
        item.setPreviewText(buildVideoPreview(course));
        item.setLink(StringUtils.defaultString(course.getVideoUrl(), ""));
        item.setSourceType(StringUtils.defaultIfBlank(course.getSourceType(), "teacher"));
        item.setSortTimestamp(course.getCreateTime() == null ? 0L : course.getCreateTime().getTime());
        return item;
    }

    private List<String> buildVideoTags(Course course) {
        List<String> tags = new ArrayList<>();
        tags.add("教学视频");
        tags.add("platform".equalsIgnoreCase(course.getSourceType()) ? "平台课程" : "教师课程");
        if (StringUtils.isNotBlank(course.getTeacherName())) {
            tags.add(course.getTeacherName());
        }
        return tags;
    }

    private String buildVideoPreview(Course course) {
        String raw = StringUtils.firstNonBlank(course.getVideoContext(), course.getDescription(), "暂无资源摘要");
        return truncate(cleanText(raw), 140);
    }

    private ResourceSearchItemVO toAiResourceItem(AiResource resource, Map<Long, String> teacherNameMap) {
        String mappedType = normalizeAiType(resource.getType());
        if (mappedType == null) {
            return null;
        }

        ResourceSearchItemVO item = new ResourceSearchItemVO();
        item.setId(resource.getId());
        item.setType(mappedType);
        item.setTitle(resolveAiTitle(resource));
        item.setDesc(buildAiDesc(resource, mappedType));
        item.setCover(readParam(resource, "coverUrl"));
        item.setAuthor(teacherNameMap.getOrDefault(resource.getTeacherId(), "未命名教师"));
        item.setViews(0L);
        item.setDate(formatDate(resource.getCreateTime()));
        item.setCourse("AI备课室资源");
        item.setDuration(buildAiDurationLabel(mappedType));
        item.setTags(buildAiTags(resource, mappedType, teacherNameMap));
        item.setPreviewText(buildAiPreview(resource));
        item.setLink(readParam(resource, "videoUrl"));
        item.setSourceType("ai_resource");
        item.setSortTimestamp(resource.getCreateTime() == null ? 0L : resource.getCreateTime().getTime());
        return item;
    }

    private ResourceSearchItemVO toCaseItem(TeachingCase teachingCase) {
        ResourceSearchItemVO item = new ResourceSearchItemVO();
        item.setId(teachingCase.getId());
        item.setType("case");
        item.setTitle(StringUtils.defaultIfBlank(teachingCase.getTitle(), "未命名案例"));
        item.setDesc(buildCasePreview(teachingCase));
        item.setCover("");
        item.setAuthor(StringUtils.defaultIfBlank(teachingCase.getSourceName(), "平台案例库"));
        item.setViews(0L);
        item.setDate(formatDate(teachingCase.getCreateTime()));
        item.setCourse(StringUtils.defaultIfBlank(teachingCase.getCourseName(), "教学案例"));
        item.setDuration("教学案例");
        item.setTags(buildCaseTags(teachingCase));
        item.setPreviewText(StringUtils.defaultString(teachingCase.getPreviewText(), buildCasePreview(teachingCase)));
        item.setLink(StringUtils.defaultString(teachingCase.getPdfUrl(), ""));
        item.setSourceType("platform_case");
        item.setSortTimestamp(teachingCase.getUpdateTime() == null ? 0L : teachingCase.getUpdateTime().getTime());
        return item;
    }

    private String normalizeAiType(String rawType) {
        if (StringUtils.isBlank(rawType)) {
            return null;
        }

        String value = rawType.trim().toLowerCase();

        if ("plan".equals(value)) {
            return "plan";
        }


        if ("quiz".equals(value) || "question".equals(value) || "exercise".equals(value)) {
            return "quiz";
        }

        if ("anim".equals(value)
                || "animation".equals(value)
                || "courseware".equals(value)
                || "interactive".equals(value)
                || "interactive_courseware".equals(value)) {
            return "anim";
        }

        if ("micro_video".equals(value)
                || "micro-video".equals(value)
                || "microcourse".equals(value)
                || "micro_course".equals(value)) {
            return "micro_video";
        }

        return null;
    }

    private String buildAiDesc(AiResource resource, String mappedType) {
        String contentPreview = truncate(cleanText(resolveAiSummary(resource)), 80);
        if ("plan".equals(mappedType)) {
            return StringUtils.isNotBlank(contentPreview) ? contentPreview : "教师生成的教案文档资源";
        }
        if ("micro_video".equals(mappedType)) {
            return StringUtils.isNotBlank(contentPreview) ? contentPreview : "教师生成的微课视频资源";
        }
        if ("quiz".equals(mappedType)) {
            return StringUtils.isNotBlank(contentPreview) ? contentPreview : "教师生成的练习题资源";
        }
        return StringUtils.isNotBlank(contentPreview) ? contentPreview : "教师生成的互动课件资源";
    }

    private String buildAiDurationLabel(String mappedType) {
        if ("micro_video".equals(mappedType)) {
            return "微课视频";
        }
        if ("plan".equals(mappedType)) {
            return "教案资源";
        }
        if ("quiz".equals(mappedType)) {
            return "练习题资源";
        }
        return "互动课件";
    }

    private List<String> buildAiTags(AiResource resource, String mappedType, Map<Long, String> teacherNameMap) {
        List<String> tags = new ArrayList<>();

        if ("plan".equals(mappedType)) {
            tags.add("教案文档");
        } else if ("quiz".equals(mappedType)) {
            tags.add("练习题");
        } else if ("anim".equals(mappedType)) {
            tags.add("互动课件");
        } else if ("micro_video".equals(mappedType)) {
            tags.add("微课视频");
            tags.addAll(readContentStringArray(resource, "knowledgePoints", 3));
            if (StringUtils.isNotBlank(readParam(resource, "audioUrl"))) {
                tags.add("配音音轨");
            }
            if (StringUtils.isNotBlank(readParam(resource, "subtitleUrl"))) {
                tags.add("字幕文件");
            }
            if (microVideoKeyframeCount(resource) > 0) {
                tags.add("关键帧增强");
            }
        }

        tags.add("教师生成资源");
        tags.add(resource.getIsPublished() != null && resource.getIsPublished() == 1 ? "已发布" : "未发布");

        String teacherName = teacherNameMap.get(resource.getTeacherId());
        if (StringUtils.isNotBlank(teacherName)) {
            tags.add(teacherName);
        }

        return tags;
    }

    private String buildAiPreview(AiResource resource) {
        String raw = resolveAiSummary(resource);
        return truncate(cleanText(raw), 180);
    }

    private List<String> buildCaseTags(TeachingCase teachingCase) {
        List<String> tags = new ArrayList<>();
        tags.add("教学案例");
        tags.add("平台案例");
        if (StringUtils.isNotBlank(teachingCase.getCourseName())) {
            tags.add(teachingCase.getCourseName());
        }
        if (StringUtils.isNotBlank(teachingCase.getKeywords())) {
            for (String keyword : teachingCase.getKeywords().split("[,，、;；\\s]+")) {
                if (StringUtils.isNotBlank(keyword) && tags.size() < 8) {
                    tags.add(keyword.trim());
                }
            }
        }
        return tags;
    }

    private String buildCasePreview(TeachingCase teachingCase) {
        String raw = StringUtils.firstNonBlank(
                teachingCase.getSummary(),
                teachingCase.getPreviewText(),
                teachingCase.getKeywords(),
                teachingCase.getTitle(),
                "暂无案例摘要"
        );
        return truncate(cleanText(raw), 180);
    }

    private String resolveAiTitle(AiResource resource) {
        return StringUtils.firstNonBlank(
                readContentText(resource, "title"),
                resource == null ? "" : resource.getTitle(),
                "未命名资源"
        );
    }

    private String resolveAiSummary(AiResource resource) {
        return StringUtils.firstNonBlank(
                readParam(resource, "summary"),
                readContentText(resource, "summary"),
                readContentText(resource, "description"),
                buildContentFallbackSummary(resource),
                resource == null ? "" : resource.getContent(),
                resource == null ? "" : resource.getTitle(),
                "暂无资源摘要"
        );
    }

    private String buildContentFallbackSummary(AiResource resource) {
        String title = readContentText(resource, "title");
        String knowledgePoints = readContentText(resource, "knowledgePoints");
        if (StringUtils.isBlank(title) && StringUtils.isBlank(knowledgePoints)) {
            return "";
        }
        if (StringUtils.isBlank(knowledgePoints)) {
            return title;
        }
        if (StringUtils.isBlank(title)) {
            return knowledgePoints;
        }
        return title + "：" + knowledgePoints;
    }

    private String readContentText(AiResource resource, String field) {
        if (resource == null || StringUtils.isBlank(resource.getContent())) {
            return "";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(resource.getContent());
            JsonNode value = root.path(field);
            if (value.isMissingNode() || value.isNull()) {
                return "";
            }
            if (value.isArray()) {
                List<String> values = new ArrayList<>();
                value.forEach(node -> {
                    if (node.isTextual() && StringUtils.isNotBlank(node.asText())) {
                        values.add(node.asText());
                    }
                });
                return String.join("、", values);
            }
            return value.asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private List<String> readContentStringArray(AiResource resource, String field, int limit) {
        if (resource == null || StringUtils.isBlank(resource.getContent())) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(resource.getContent());
            JsonNode value = root.path(field);
            if (!value.isArray()) {
                return Collections.emptyList();
            }

            List<String> result = new ArrayList<>();
            for (JsonNode node : value) {
                if (result.size() >= limit) {
                    break;
                }
                String text = node.asText("");
                if (StringUtils.isNotBlank(text)) {
                    result.add(text);
                }
            }
            return result;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String readParam(AiResource resource, String field) {
        if (resource == null || StringUtils.isBlank(resource.getParamsJson())) {
            return "";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(resource.getParamsJson());
            return root.path(field).asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private int microVideoKeyframeCount(AiResource resource) {
        String statsJson = readParam(resource, "renderStatsJson");
        if (StringUtils.isBlank(statsJson)) {
            return 0;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(statsJson);
            return root.path("keyframeCount").asInt(0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Map<Long, String> buildTeacherNameMap(List<AiResource> resources) {
        Set<Long> teacherIds = resources.stream()
                .map(AiResource::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (teacherIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<User> users = userService.listByIds(teacherIds);
        return users.stream().collect(Collectors.toMap(
                User::getId,
                user -> StringUtils.defaultIfBlank(user.getUserName(), user.getUserAccount()),
                (a, b) -> a
        ));
    }

    private long countByType(List<ResourceSearchItemVO> list, String type) {
        return list.stream().filter(item -> type.equalsIgnoreCase(item.getType())).count();
    }

    private void sortItems(List<ResourceSearchItemVO> list, String keyword, String sortMode) {
        if ("newest".equalsIgnoreCase(sortMode)) {
            list.sort(Comparator.comparingLong(this::getSortTimestamp).reversed());
            return;
        }

        if ("popular".equalsIgnoreCase(sortMode)) {
            list.sort(Comparator
                    .comparingLong(ResourceSearchItemVO::getViews)
                    .reversed()
                    .thenComparing(Comparator.comparingLong(this::getSortTimestamp).reversed()));
            return;
        }

        list.sort(Comparator
                .comparingInt((ResourceSearchItemVO item) -> relevanceScore(item, keyword))
                .reversed()
                .thenComparing(Comparator.comparingLong(this::getSortTimestamp).reversed()));
    }

    private int relevanceScore(ResourceSearchItemVO item, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return 0;
        }

        String lowerKeyword = keyword.toLowerCase();
        int score = 0;

        if (containsIgnoreCase(item.getTitle(), lowerKeyword)) {
            score += 8;
        }
        if (containsIgnoreCase(item.getDesc(), lowerKeyword)) {
            score += 4;
        }
        if (containsIgnoreCase(item.getPreviewText(), lowerKeyword)) {
            score += 3;
        }
        if (containsIgnoreCase(item.getAuthor(), lowerKeyword)) {
            score += 2;
        }
        if (containsIgnoreCase(item.getCourse(), lowerKeyword)) {
            score += 2;
        }
        if (item.getTags() != null) {
            for (String tag : item.getTags()) {
                if (containsIgnoreCase(tag, lowerKeyword)) {
                    score += 1;
                }
            }
        }

        return score;
    }

    private boolean containsIgnoreCase(String source, String lowerKeyword) {
        return StringUtils.isNotBlank(source) && source.toLowerCase().contains(lowerKeyword);
    }

    private long getSortTimestamp(ResourceSearchItemVO item) {
        return item.getSortTimestamp() == null ? 0L : item.getSortTimestamp();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "--";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String cleanText(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        return text
                .replaceAll("<[^>]+>", " ")
                .replaceAll("#+", " ")
                .replaceAll("\\*+", " ")
                .replaceAll("`+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncate(String text, int maxLength) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private void saveRelations(Long courseId, List<Long> classIds) {
        if (classIds != null && !classIds.isEmpty()) {
            for (Long classId : classIds) {
                // 👇 新增防御性判断：跳过 null 值
                if (classId == null) {
                    continue;
                }

                CourseClassRelation relation = new CourseClassRelation();
                relation.setCourseId(courseId);
                relation.setClassId(classId);
                relationMapper.insert(relation);
            }
        }
    }
}
