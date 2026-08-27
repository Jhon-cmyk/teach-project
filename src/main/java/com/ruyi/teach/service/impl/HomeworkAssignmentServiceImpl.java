package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.dto.HomeworkChapterPracticePublishRequest;
import com.ruyi.teach.model.dto.HomeworkPublishRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.HomeworkAssignmentService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HomeworkAssignmentServiceImpl
        extends ServiceImpl<HomeworkAssignmentMapper, HomeworkAssignment>
        implements HomeworkAssignmentService {

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private SysClassMapper sysClassMapper;

    @Override
    public Long publishAssignment(HomeworkPublishRequest req, User loginUser) {
        // 这里保留你原来的“正式作业发布”逻辑
        // 只建议补一行 assignmentType，避免旧正式作业没类型
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可发布作业");
        }

        if (req.getQuizResourceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试卷资源ID不能为空");
        }

        AiResource quizResource = aiResourceMapper.selectById(req.getQuizResourceId());
        if (quizResource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "试卷资源不存在");
        }

        if (!"quiz".equals(quizResource.getType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能发布试卷类型的资源作为作业");
        }

        if (!loginUser.getId().equals(quizResource.getTeacherId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "不能发布其他教师的试卷资源");
        }

        if (req.getClassId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标班级不能为空");
        }

        boolean teachesTargetClass = sysClassMapper.selectMyClasses(loginUser.getId()).stream()
                .anyMatch(item -> Objects.equals(item.getId(), req.getClassId()));
        if (!teachesTargetClass) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能向本人任教的班级发布作业或考试");
        }

        String title = StringUtils.isNotBlank(req.getTitle()) ? req.getTitle() : quizResource.getTitle();
        if (StringUtils.isBlank(title)) {
            title = "未命名作业";
        }

        int[] parsed = parseQuestionCountAndScore(quizResource.getParamsJson(), quizResource.getContent());

        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setTeacherId(loginUser.getId());
        assignment.setClassId(req.getClassId());
        assignment.setCourseId(req.getCourseId());
        assignment.setQuizResourceId(req.getQuizResourceId());
        assignment.setTitle(title);
        assignment.setQuizTitleSnapshot(quizResource.getTitle());
        assignment.setContentSnapshot(quizResource.getContent());
        assignment.setParamsSnapshot(quizResource.getParamsJson());
        assignment.setTeacherNote(req.getTeacherNote());
        assignment.setAnswerMode(normalizeAnswerMode(req.getAnswerMode()));
        assignment.setImageGranularity(normalizeImageGranularity(req.getImageGranularity()));
        assignment.setGradingMode(normalizeGradingMode(req.getGradingMode()));

        String assignType = StringUtils.isNotBlank(req.getAssignmentType()) ? req.getAssignmentType() : "homework";
        assignment.setAssignmentType(assignType);
        assignment.setStatus("published");
        assignment.setDeadline(req.getDeadline());

        if ("exam".equals(assignType)) {
            assignment.setDurationMinutes(req.getDurationMinutes());
            assignment.setAllowRedo(0);
            assignment.setMaxAttemptCount(1);
        } else {
            assignment.setAllowRedo(req.getAllowRedo() != null ? req.getAllowRedo() : 0);
            assignment.setMaxAttemptCount(req.getMaxAttemptCount() != null && req.getMaxAttemptCount() > 0
                    ? req.getMaxAttemptCount() : 1);
        }

        assignment.setQuestionCount(parsed[0] > 0 ? parsed[0] : null);
        assignment.setTotalScore(parsed[1] > 0 ? parsed[1] : null);

        this.save(assignment);
        return assignment.getId();
    }

    @Override
    public Long publishChapterPractice(HomeworkChapterPracticePublishRequest req, User loginUser) {
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可发布章节练习");
        }

        if (req.getCourseId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }
        if (req.getChapterId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "章节ID不能为空");
        }
        if (req.getClassId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标班级不能为空");
        }
        if (req.getQuizResourceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "练习资源ID不能为空");
        }

        AiResource quizResource = aiResourceMapper.selectById(req.getQuizResourceId());
        if (quizResource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "练习资源不存在");
        }
        if (!"quiz".equals(quizResource.getType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能使用 quiz 资源发布章节练习");
        }
        if (!loginUser.getId().equals(quizResource.getTeacherId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "不能使用其他教师的练习资源");
        }

        CourseChapter chapter = courseChapterService.getById(req.getChapterId());
        if (chapter == null || (chapter.getIsDelete() != null && chapter.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "章节不存在");
        }
        if (!req.getCourseId().equals(chapter.getCourseId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "章节不属于当前课程");
        }

        List<Long> boundClassIds = courseMapper.selectClassIdsByCourseId(req.getCourseId());
        if (boundClassIds == null || !boundClassIds.contains(req.getClassId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标班级未绑定到当前课程");
        }

        String title = StringUtils.isNotBlank(req.getTitle()) ? req.getTitle() : quizResource.getTitle();
        if (StringUtils.isBlank(title)) {
            title = "未命名章节练习";
        }

        int[] parsed = parseQuestionCountAndScore(quizResource.getParamsJson(), quizResource.getContent());

        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setTeacherId(loginUser.getId());
        assignment.setClassId(req.getClassId());
        assignment.setCourseId(req.getCourseId());
        assignment.setChapterId(req.getChapterId());
        assignment.setChapterTitleSnapshot(chapter.getTitle());
        assignment.setQuizResourceId(req.getQuizResourceId());
        assignment.setTitle(title);
        assignment.setQuizTitleSnapshot(quizResource.getTitle());
        assignment.setContentSnapshot(quizResource.getContent());
        assignment.setParamsSnapshot(quizResource.getParamsJson());
        assignment.setTeacherNote(req.getTeacherNote());
        assignment.setAnswerMode("online");
        assignment.setImageGranularity("per_question");
        assignment.setGradingMode("auto");
        assignment.setAssignmentType("chapter_practice");
        assignment.setStatus("published");

        // 随堂练习固定长期有效
        assignment.setDeadline(null);
        assignment.setAllowRedo(1);
        assignment.setMaxAttemptCount(9999);

        assignment.setQuestionCount(parsed[0] > 0 ? parsed[0] : null);
        assignment.setTotalScore(parsed[1] > 0 ? parsed[1] : null);

        this.save(assignment);
        return assignment.getId();
    }

    private int[] parseQuestionCountAndScore(String paramsJson, String content) {
        int questionCount = 0;
        int totalScore = 0;

        if (StringUtils.isNotBlank(paramsJson)) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = om.readTree(paramsJson);

                if (root.has("totalCount")) {
                    questionCount = root.get("totalCount").asInt(0);
                } else if (root.has("questionCount")) {
                    questionCount = root.get("questionCount").asInt(0);
                } else if (root.has("total_count")) {
                    questionCount = root.get("total_count").asInt(0);
                }

                if (root.has("totalScore")) {
                    totalScore = root.get("totalScore").asInt(0);
                } else if (root.has("total_score")) {
                    totalScore = root.get("total_score").asInt(0);
                }

                if (questionCount == 0 && root.has("sections")) {
                    for (com.fasterxml.jackson.databind.JsonNode sec : root.get("sections")) {
                        questionCount += sec.path("count").asInt(0);
                    }
                }
            } catch (Exception ignore) {
            }
        }

        if (questionCount == 0 && StringUtils.isNotBlank(content)) {
            Pattern p = Pattern.compile("(?m)^\\s*(?:[#*>\\s]*)?(\\d+)[.．、]\\s+");
            Matcher m = p.matcher(content);
            int count = 0;
            while (m.find()) {
                count++;
            }
            questionCount = count;
        }

        if (questionCount > 0 && totalScore == 0) {
            totalScore = 100;
        }

        return new int[]{questionCount, totalScore};
    }

    private String normalizeAnswerMode(String value) {
        if ("image".equals(value) || "mixed".equals(value)) {
            return value;
        }
        return "online";
    }

    private String normalizeImageGranularity(String value) {
        if ("per_question".equals(value)) {
            return value;
        }
        return "per_question";
    }

    private String normalizeGradingMode(String value) {
        if ("ai_review".equals(value)) {
            return value;
        }
        return "auto";
    }
}
