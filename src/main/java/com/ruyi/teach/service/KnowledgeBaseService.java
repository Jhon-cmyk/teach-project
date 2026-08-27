package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.teach.client.XfyunKnowledgeBaseClient;
import com.ruyi.teach.config.XfyunKnowledgeBaseProperties;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CourseClassRelationMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.model.dto.TutorChatRequest;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseClassRelation;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.knowledge.KnowledgeFileVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeRepositoryVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeSearchHitVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class KnowledgeBaseService {

    private static final int MAX_UPLOAD_BYTES = 20 * 1024 * 1024;
    private static final List<StarterDocument> STARTER_DOCUMENTS = List.of(
            new StarterDocument("数据结构考研理论与核心考点.md",
                    "knowledge-base/data-structures-theory.md"),
            new StarterDocument("数据结构就业实战与面试指南.md",
                    "knowledge-base/data-structures-practice.md"),
            new StarterDocument("智慧教学平台AI助教使用说明.md",
                    "knowledge-base/platform-ai-assistant-guide.md")
    );

    private final XfyunKnowledgeBaseClient client;
    private final XfyunKnowledgeBaseProperties properties;
    private final CourseMapper courseMapper;
    private final CourseClassRelationMapper relationMapper;

    public KnowledgeBaseService(XfyunKnowledgeBaseClient client,
                                XfyunKnowledgeBaseProperties properties,
                                CourseMapper courseMapper,
                                CourseClassRelationMapper relationMapper) {
        this.client = client;
        this.properties = properties;
        this.courseMapper = courseMapper;
        this.relationMapper = relationMapper;
    }

    public ConfigurationStatus configurationStatus() {
        return new ConfigurationStatus(
                properties.isEnabled(),
                properties.isConfigured(),
                properties.getBaseUrl(),
                StringUtils.isNotBlank(properties.getAppId()),
                StringUtils.isNotBlank(properties.getSecret()),
                properties.getMinimumScore(),
                properties.getTopN()
        );
    }

    public List<KnowledgeRepositoryVO> listRepositories() {
        return client.listRepositories();
    }

    public KnowledgeRepositoryVO createRepository(String name, String description, String tags) {
        if (StringUtils.isBlank(name) || name.trim().length() > 120) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库名称不能为空且不能超过120个字符");
        }
        return client.createRepository(name, limit(description, 500), limit(tags, 200));
    }

    public List<CourseBinding> listCourseBindings() {
        return courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getIsDelete, 0)
                        .orderByAsc(Course::getName)
                        .orderByAsc(Course::getId))
                .stream().map(this::toBinding).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseBinding bindCourse(Long courseId, String repoId, String repoName, String keywords) {
        Course course = requireCourse(courseId);
        String safeRepoId = requireText(repoId, "请选择要绑定的知识库");
        String safeRepoName = StringUtils.defaultIfBlank(StringUtils.trim(repoName), safeRepoId);
        Course update = new Course();
        update.setId(course.getId());
        update.setKnowledgeRepoId(limit(safeRepoId, 64));
        update.setKnowledgeRepoName(limit(safeRepoName, 120));
        update.setKnowledgeKeywords(normalizeKeywords(keywords));
        update.setKnowledgeSyncStatus("empty");
        update.setKnowledgeUpdatedAt(new Date());
        courseMapper.updateById(update);
        return toBinding(courseMapper.selectById(courseId));
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseBinding unbindCourse(Long courseId) {
        Course course = requireCourse(courseId);
        Course update = new Course();
        update.setId(course.getId());
        update.setKnowledgeRepoId("");
        update.setKnowledgeRepoName("");
        update.setKnowledgeKeywords("");
        update.setKnowledgeSyncStatus("empty");
        update.setKnowledgeUpdatedAt(new Date());
        courseMapper.updateById(update);
        return toBinding(courseMapper.selectById(courseId));
    }

    public KnowledgeUploadVO upload(String repoId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要上传的课程资料");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE_ERROR, "星火知识库单个文件不能超过20MB");
        }
        try {
            KnowledgeUploadVO result = client.uploadAndAddToRepository(
                    repoId, safeFileName(file.getOriginalFilename()), file.getBytes(), file.getContentType());
            updateCoursesByRepo(repoId, "ready");
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取上传文件失败", e);
        }
    }

    public List<KnowledgeUploadVO> uploadStarterPack(String repoId) {
        String safeRepoId = requireText(repoId, "知识库 ID 不能为空");
        try {
            StringBuilder bundle = new StringBuilder("# 智慧教学平台课程知识库初始资料\n\n");
            for (StarterDocument document : STARTER_DOCUMENTS) {
                ClassPathResource resource = new ClassPathResource(document.classpathLocation());
                bundle.append(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                        .append("\n\n---\n\n");
            }
            KnowledgeUploadVO uploaded = client.uploadAndAddToRepository(
                    safeRepoId,
                    "智慧教学平台-数据结构课程知识库.md",
                    bundle.toString().getBytes(StandardCharsets.UTF_8),
                    "text/markdown; charset=UTF-8");
            updateCoursesByRepo(safeRepoId, "ready");
            return List.of(uploaded);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "初始化资料上传失败", e);
        }
    }

    public List<KnowledgeFileVO> listFilesAndRefresh(String repoId) {
        List<KnowledgeFileVO> files = client.listFiles(requireText(repoId, "知识库 ID 不能为空"));
        refreshCourseSyncStatus(repoId, files);
        return files;
    }

    public Map<String, String> refreshFileStatuses(String repoId, List<String> fileIds) {
        Map<String, String> statuses = client.getFileStatuses(fileIds);
        if (StringUtils.isNotBlank(repoId)) {
            List<KnowledgeFileVO> files = client.listFiles(repoId);
            refreshCourseSyncStatus(repoId, files);
        }
        return statuses;
    }

    public void deleteFile(String repoId, String fileId) {
        client.deleteFile(requireText(fileId, "文件 ID 不能为空"));
        if (StringUtils.isNotBlank(repoId)) {
            List<KnowledgeFileVO> files = client.listFiles(repoId);
            refreshCourseSyncStatus(repoId, files);
        }
    }

    public RetrievalContext retrieveForStudent(User student, TutorChatRequest request) {
        if (!properties.isConfigured() || student == null || request == null
                || StringUtils.isBlank(request.getMessage())) {
            return RetrievalContext.empty();
        }
        try {
            Course course = resolveCourse(student, request);
            if (course == null || StringUtils.isBlank(course.getKnowledgeRepoId())) {
                return RetrievalContext.empty();
            }
            List<KnowledgeSearchHitVO> rawHits = client.search(course.getKnowledgeRepoId(), request.getMessage());
            if (rawHits.isEmpty()) {
                return new RetrievalContext(course, List.of(), false,
                        "课程知识库未检索到达到可信阈值的资料，本次回答不得声称有课程资料依据。", Map.of());
            }

            Map<String, String> fileNames = new LinkedHashMap<>();
            try {
                for (KnowledgeFileVO file : client.listFiles(course.getKnowledgeRepoId())) {
                    if (StringUtils.isNotBlank(file.fileId()) && StringUtils.isNotBlank(file.fileName())) {
                        fileNames.put(file.fileId(), file.fileName());
                    }
                }
            } catch (Exception exception) {
                log.info("Knowledge file names unavailable, repoId={}", course.getKnowledgeRepoId());
            }

            List<KnowledgeSearchHitVO> hits = rawHits.stream().map(hit -> new KnowledgeSearchHitVO(
                    hit.content(), hit.score(), hit.fileId(),
                    StringUtils.defaultIfBlank(hit.fileName(), fileNames.get(hit.fileId())),
                    hit.index(), hit.type(), hit.fileType()
            )).toList();
            return new RetrievalContext(course, hits, true, buildEvidencePrompt(course, hits), fileNames);
        } catch (Exception exception) {
            log.warn("Knowledge retrieval degraded, studentId={}, cause={}",
                    student.getId(), exception.getClass().getSimpleName());
            return RetrievalContext.empty();
        }
    }

    private Course resolveCourse(User student, TutorChatRequest request) {
        Long requestedCourseId = request.getCourseId();
        if (requestedCourseId == null && request.getContext() != null) {
            requestedCourseId = readLong(request.getContext().get("courseId"));
        }
        if (requestedCourseId != null) {
            Course requested = courseMapper.selectById(requestedCourseId);
            return isAccessible(student, requested) ? requested : null;
        }

        List<Course> candidates = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getIsDelete, 0)
                        .isNotNull(Course::getKnowledgeRepoId)
                        .ne(Course::getKnowledgeRepoId, ""))
                .stream().filter(course -> isAccessible(student, course)).toList();
        if (candidates.isEmpty()) return null;

        String question = StringUtils.defaultString(request.getMessage()).toLowerCase(Locale.ROOT);
        return candidates.stream()
                .map(course -> Map.entry(course, routeScore(course, question)))
                .filter(entry -> entry.getValue() > 0 || candidates.size() == 1)
                .max(Comparator.<Map.Entry<Course, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(entry -> entry.getKey().getId()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private boolean isAccessible(User student, Course course) {
        if (course == null || course.getIsDelete() != null && course.getIsDelete() == 1) return false;
        if ("platform".equals(course.getSourceType()) && "published".equals(course.getPublishStatus())) return true;
        if (student.getClassId() == null) return false;
        return relationMapper.selectCount(new LambdaQueryWrapper<CourseClassRelation>()
                .eq(CourseClassRelation::getCourseId, course.getId())
                .eq(CourseClassRelation::getClassId, student.getClassId())) > 0;
    }

    private int routeScore(Course course, String question) {
        int score = 0;
        String courseName = StringUtils.trimToEmpty(course.getName()).toLowerCase(Locale.ROOT);
        if (!courseName.isEmpty() && question.contains(courseName)) score += 10;
        for (String keyword : splitKeywords(course.getKnowledgeKeywords())) {
            if (question.contains(keyword.toLowerCase(Locale.ROOT))) score += 3;
        }
        return score;
    }

    private String buildEvidencePrompt(Course course, List<KnowledgeSearchHitVO> hits) {
        StringBuilder builder = new StringBuilder();
        builder.append("【课程私有知识库证据】\n")
                .append("课程：").append(course.getName()).append("\n")
                .append("回答规则：只能把以下片段作为课程事实依据；引用时使用[资料1]格式；")
                .append("证据不足时明确说明，不得补造课程结论。\n\n");
        int index = 1;
        for (KnowledgeSearchHitVO hit : hits) {
            builder.append("[资料").append(index++).append("] ")
                    .append(StringUtils.defaultIfBlank(hit.fileName(), "课程知识库资料"))
                    .append("（相关度").append(String.format(Locale.ROOT, "%.1f", hit.score())).append("）\n")
                    .append(limit(StringUtils.normalizeSpace(hit.content()), 2600)).append("\n\n");
        }
        builder.append("回答末尾增加“参考资料”小节，只列实际使用的资料编号与文件名。");
        return limit(builder.toString(), 14000);
    }

    private void refreshCourseSyncStatus(String repoId, List<KnowledgeFileVO> files) {
        String status;
        if (files == null || files.isEmpty()) {
            status = "empty";
        } else if (files.stream().anyMatch(file -> "failed".equalsIgnoreCase(file.fileStatus()))) {
            status = "failed";
        } else if (files.stream().allMatch(file -> "vectored".equalsIgnoreCase(file.fileStatus()))) {
            status = "ready";
        } else {
            status = "processing";
        }
        updateCoursesByRepo(repoId, status);
    }

    private void updateCoursesByRepo(String repoId, String status) {
        if (StringUtils.isBlank(repoId)) return;
        Course update = new Course();
        update.setKnowledgeSyncStatus(status);
        update.setKnowledgeUpdatedAt(new Date());
        courseMapper.update(update, new LambdaQueryWrapper<Course>()
                .eq(Course::getKnowledgeRepoId, repoId)
                .eq(Course::getIsDelete, 0));
    }

    private Course requireCourse(Long courseId) {
        if (courseId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择课程");
        Course course = courseMapper.selectById(courseId);
        if (course == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        return course;
    }

    private CourseBinding toBinding(Course course) {
        return new CourseBinding(
                course.getId(), course.getName(), course.getSourceType(), course.getTeacherName(),
                course.getPublishStatus(), course.getKnowledgeRepoId(), course.getKnowledgeRepoName(),
                course.getKnowledgeKeywords(), StringUtils.defaultIfBlank(course.getKnowledgeSyncStatus(), "empty"),
                course.getKnowledgeUpdatedAt()
        );
    }

    private String normalizeKeywords(String value) {
        return limit(String.join(",", splitKeywords(value).stream().distinct().limit(30).toList()), 500);
    }

    private List<String> splitKeywords(String value) {
        if (StringUtils.isBlank(value)) return List.of();
        return List.of(value.split("[,，;；\\s]+"))
                .stream().map(StringUtils::trim).filter(StringUtils::isNotBlank).toList();
    }

    private String safeFileName(String value) {
        return limit(StringUtils.defaultIfBlank(value, "knowledge-document.md")
                .replace("\\", "_").replace("/", "_").replace("\r", "_").replace("\n", "_"), 200);
    }

    private String requireText(String value, String message) {
        if (StringUtils.isBlank(value)) throw new BusinessException(ErrorCode.PARAMS_ERROR, message);
        return value.trim();
    }

    private String limit(String value, int maxLength) {
        String safe = StringUtils.defaultString(value);
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private Long readLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try {
            String text = StringUtils.trimToEmpty(String.valueOf(value));
            return text.isEmpty() ? null : Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record ConfigurationStatus(boolean enabled,
                                      boolean configured,
                                      String baseUrl,
                                      boolean appIdConfigured,
                                      boolean secretConfigured,
                                      double minimumScore,
                                      int topN) {
    }

    public record CourseBinding(Long courseId,
                                String courseName,
                                String sourceType,
                                String teacherName,
                                String publishStatus,
                                String repoId,
                                String repoName,
                                String keywords,
                                String syncStatus,
                                Date updatedAt) {
    }

    public record RetrievalContext(Course course,
                                   List<KnowledgeSearchHitVO> hits,
                                   boolean hasEvidence,
                                   String promptContext,
                                   Map<String, String> fileNames) {
        public static RetrievalContext empty() {
            return new RetrievalContext(null, List.of(), false, "", Map.of());
        }

        public Map<String, Object> eventMetadata() {
            if (course == null) return Map.of("knowledgeBaseUsed", false);
            return Map.of(
                    "knowledgeBaseUsed", hasEvidence,
                    "knowledgeCourseId", course.getId(),
                    "knowledgeRepoId", StringUtils.defaultString(course.getKnowledgeRepoId()),
                    "knowledgeHitCount", hits == null ? 0 : hits.size(),
                    "knowledgeSources", hits == null ? List.of() : hits.stream()
                            .map(hit -> StringUtils.defaultIfBlank(hit.fileName(), hit.fileId()))
                            .filter(Objects::nonNull).distinct().limit(5).toList()
            );
        }
    }

    private record StarterDocument(String fileName, String classpathLocation) {
    }
}
