package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.model.dto.PersonalPracticeCreateRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.PersonalPracticeCreateVO;
import com.ruyi.teach.service.DeepSeekService;
import com.ruyi.teach.service.HomeworkAssignmentService;
import com.ruyi.teach.service.PersonalPracticeService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PersonalPracticeServiceImpl implements PersonalPracticeService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private HomeworkAssignmentService assignmentService;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private DeepSeekService deepSeekService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PersonalPracticeCreateVO create(PersonalPracticeCreateRequest request, User student) {
        if (student == null || !"student".equals(student.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可创建个人练习");
        }
        String knowledgeName = StringUtils.trimToEmpty(request == null ? null : request.getKnowledgeName());
        if (StringUtils.isBlank(knowledgeName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识点不能为空");
        }

        HomeworkAssignment existing = findExistingPractice(student.getId(), request, knowledgeName);
        if (existing != null) {
            return toVO(existing);
        }

        SourceQuiz source = findTeacherQuiz(request, student, knowledgeName);
        if (source == null) {
            source = findPlatformQuiz(knowledgeName);
        }
        if (source == null) {
            source = generateAiQuiz(request, knowledgeName);
        }

        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setTeacherId(0L);
        assignment.setClassId(student.getClassId());
        assignment.setTargetStudentId(student.getId());
        assignment.setCourseId(request.getCourseId());
        assignment.setChapterId(request.getChapterId());
        assignment.setQuizResourceId(source.resource.getId());
        assignment.setTitle("“" + knowledgeName + "”个性化巩固练习");
        assignment.setQuizTitleSnapshot(source.resource.getTitle());
        assignment.setContentSnapshot(source.resource.getContent());
        assignment.setParamsSnapshot(source.resource.getParamsJson());
        assignment.setTeacherNote("根据你的学习画像匹配，完成后会用于更新薄弱点判断。");
        assignment.setAnswerMode("online");
        assignment.setImageGranularity("per_question");
        assignment.setGradingMode("auto");
        assignment.setAssignmentType("personal_practice");
        assignment.setSourceType(source.sourceType);
        assignment.setStatus("published");
        assignment.setAllowRedo(1);
        assignment.setMaxAttemptCount(5);
        assignment.setQuestionCount(resolveQuestionCount(source.resource.getContent()));
        assignment.setTotalScore(100);
        assignment.setCreateTime(new Date());
        assignment.setUpdateTime(new Date());
        assignment.setIsDelete(0);
        assignmentService.save(assignment);
        return toVO(assignment);
    }

    private HomeworkAssignment findExistingPractice(Long studentId,
                                                     PersonalPracticeCreateRequest request,
                                                     String knowledgeName) {
        LambdaQueryWrapper<HomeworkAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkAssignment::getTargetStudentId, studentId)
                .eq(HomeworkAssignment::getAssignmentType, "personal_practice")
                .eq(HomeworkAssignment::getStatus, "published")
                .eq(HomeworkAssignment::getIsDelete, 0)
                .like(HomeworkAssignment::getTitle, knowledgeName);
        if (request.getCourseId() != null) {
            wrapper.eq(HomeworkAssignment::getCourseId, request.getCourseId());
        }
        if (request.getChapterId() != null) {
            wrapper.eq(HomeworkAssignment::getChapterId, request.getChapterId());
        }
        wrapper.orderByDesc(HomeworkAssignment::getCreateTime).last("limit 1");
        return assignmentService.getOne(wrapper, false);
    }

    private SourceQuiz findTeacherQuiz(PersonalPracticeCreateRequest request, User student, String knowledgeName) {
        if (request.getCourseId() == null) return null;
        LambdaQueryWrapper<HomeworkAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HomeworkAssignment::getCourseId, request.getCourseId())
                .eq(HomeworkAssignment::getStatus, "published")
                .eq(HomeworkAssignment::getIsDelete, 0)
                .isNull(HomeworkAssignment::getTargetStudentId)
                .in(HomeworkAssignment::getAssignmentType, "chapter_practice", "homework")
                .isNotNull(HomeworkAssignment::getQuizResourceId);
        if (request.getChapterId() != null) {
            wrapper.eq(HomeworkAssignment::getChapterId, request.getChapterId());
        }
        if (student.getClassId() != null) {
            wrapper.and(w -> w.isNull(HomeworkAssignment::getClassId)
                    .or().eq(HomeworkAssignment::getClassId, student.getClassId()));
        }
        wrapper.orderByDesc(HomeworkAssignment::getCreateTime).last("limit 8");
        List<HomeworkAssignment> candidates = assignmentService.list(wrapper);
        for (HomeworkAssignment candidate : candidates) {
            AiResource resource = aiResourceMapper.selectById(candidate.getQuizResourceId());
            String searchable = StringUtils.defaultString(resource == null ? null : resource.getTitle())
                    + "\n" + StringUtils.defaultString(resource == null ? null : resource.getContent());
            if (isUsableQuiz(resource) && searchable.contains(knowledgeName)) {
                return new SourceQuiz(resource, "teacher_bank");
            }
        }
        for (HomeworkAssignment candidate : candidates) {
            AiResource resource = aiResourceMapper.selectById(candidate.getQuizResourceId());
            if (isUsableQuiz(resource)) {
                return new SourceQuiz(resource, "teacher_bank");
            }
        }
        return null;
    }

    private SourceQuiz findPlatformQuiz(String knowledgeName) {
        LambdaQueryWrapper<AiResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiResource::getType, "quiz")
                .eq(AiResource::getIsPublished, 1)
                .eq(AiResource::getIsDelete, 0)
                .and(w -> w.like(AiResource::getTitle, knowledgeName)
                        .or().like(AiResource::getContent, knowledgeName))
                .and(w -> w.isNull(AiResource::getSourceType)
                        .or().ne(AiResource::getSourceType, "personal_ai"))
                .orderByDesc(AiResource::getUpdateTime)
                .last("limit 1");
        AiResource resource = aiResourceMapper.selectOne(wrapper);
        return isUsableQuiz(resource) ? new SourceQuiz(resource, "platform_bank") : null;
    }

    private SourceQuiz generateAiQuiz(PersonalPracticeCreateRequest request, String knowledgeName) {
        String systemPrompt = "你是严谨的学科命题教师。只输出可直接作答的Markdown试卷，不输出开场白。"
                + "固定使用“## 一、单项选择题”“## 二、判断题”“## 三、填空题”“## 参考答案与解析”四个标题。"
                + "必须包含3道单选题、1道判断题、1道填空题，最后逐题给出参考答案与简短解析。"
                + "题号必须使用1.到5.，单选项使用A.到D.，总分100分，难度由易到难。";
        String userPrompt = "围绕知识点“" + knowledgeName + "”生成一份个性化巩固练习。"
                + "课程ID：" + Objects.toString(request.getCourseId(), "未指定")
                + "，章节ID：" + Objects.toString(request.getChapterId(), "未指定") + "。"
                + "题目必须检验理解和应用，避免偏题、歧义题。";
        String content = cleanAiContent(deepSeekService.chat(systemPrompt, userPrompt, 2600));
        if (StringUtils.isBlank(content) || resolveQuestionCount(content) < 3) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "暂时无法生成合格练习，请稍后重试");
        }

        ObjectNode params = OBJECT_MAPPER.createObjectNode();
        params.put("source", "personal_ai");
        params.put("knowledgeName", knowledgeName);
        if (request.getCourseId() != null) params.put("courseId", request.getCourseId());
        if (request.getChapterId() != null) params.put("chapterId", request.getChapterId());
        params.put("questionCount", resolveQuestionCount(content));
        params.put("totalScore", 100);

        AiResource resource = new AiResource();
        resource.setTeacherId(0L);
        resource.setType("quiz");
        resource.setTitle(knowledgeName + "个性化巩固练习");
        resource.setContent(content);
        resource.setParamsJson(params.toString());
        resource.setIsPublished(1);
        resource.setIsDelete(0);
        resource.setSourceType("personal_ai");
        resource.setCreateTime(new Date());
        resource.setUpdateTime(new Date());
        aiResourceMapper.insert(resource);
        return new SourceQuiz(resource, "ai_generated");
    }

    private boolean isUsableQuiz(AiResource resource) {
        return resource != null
                && "quiz".equals(resource.getType())
                && (resource.getIsDelete() == null || resource.getIsDelete() == 0)
                && StringUtils.isNotBlank(resource.getContent());
    }

    private String cleanAiContent(String content) {
        return StringUtils.trimToEmpty(content)
                .replaceFirst("(?s)^```(?:markdown|md)?\\s*", "")
                .replaceFirst("(?s)\\s*```$", "")
                .trim();
    }

    private int resolveQuestionCount(String content) {
        Matcher matcher = Pattern.compile("(?m)^\\s*(?:第\\s*)?(\\d+)(?:\\s*题)?[.．、]\\s+").matcher(StringUtils.defaultString(content));
        int count = 0;
        while (matcher.find()) count++;
        return Math.max(1, Math.min(20, count));
    }

    private PersonalPracticeCreateVO toVO(HomeworkAssignment assignment) {
        PersonalPracticeCreateVO vo = new PersonalPracticeCreateVO();
        vo.setAssignmentId(assignment.getId());
        vo.setTitle(assignment.getTitle());
        vo.setSourceType(assignment.getSourceType());
        vo.setSourceLabel(switch (StringUtils.defaultString(assignment.getSourceType())) {
            case "teacher_bank" -> "教师题库";
            case "platform_bank" -> "平台题库";
            case "ai_generated" -> "AI 补充题";
            default -> "个性化题库";
        });
        vo.setQuestionCount(assignment.getQuestionCount());
        return vo;
    }

    private record SourceQuiz(AiResource resource, String sourceType) {}
}
