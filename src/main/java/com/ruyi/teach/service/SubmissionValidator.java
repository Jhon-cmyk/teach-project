package com.ruyi.teach.service;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SubmissionValidator {

    private final HomeworkAssignmentMapper assignmentMapper;
    private final AiResourceMapper aiResourceMapper;
    private final AnswerParser answerParser;

    public SubmissionValidator(HomeworkAssignmentMapper assignmentMapper,
                               AiResourceMapper aiResourceMapper,
                               AnswerParser answerParser) {
        this.assignmentMapper = assignmentMapper;
        this.aiResourceMapper = aiResourceMapper;
        this.answerParser = answerParser;
    }

    public ValidatedHomework validateHomework(HomeworkSubmitRequest request, User loginUser) {
        HomeworkAssignment assignment = request == null || request.getAssignmentId() == null
                ? null
                : assignmentMapper.selectById(request.getAssignmentId());
        return validateHomework(request, loginUser, assignment);
    }

    public ValidatedHomework validateHomework(HomeworkSubmitRequest request,
                                              User loginUser,
                                              HomeworkAssignment assignment) {
        if (request == null || request.getAssignmentId() == null
                || (StringUtils.isBlank(request.getStudentAnswerJson()) && !answerParser.hasImagePayload(request))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "作业ID和答案不能为空");
        }
        if (loginUser == null || !"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可提交作业");
        }
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }
        if (!"published".equals(assignment.getStatus())) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "该作业当前不可提交（状态：" + assignment.getStatus() + "）"
            );
        }
        requireActiveQuizResource(assignment);
        if (assignment.getClassId() != null && !assignment.getClassId().equals(loginUser.getClassId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该作业不属于你所在的班级");
        }
        if (assignment.getTargetStudentId() != null
                && !assignment.getTargetStudentId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该练习不属于当前学生");
        }
        if (assignment.getDeadline() != null && new Date().after(assignment.getDeadline())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该作业已过截止时间，无法提交");
        }

        String answerMode = normalizeAnswerMode(assignment.getAnswerMode());
        boolean hasImages = answerParser.hasImagePayload(request);
        String submissionType = normalizeSubmissionType(request.getSubmissionType(), answerMode, hasImages);
        validatePayload(request, answerMode, submissionType, hasImages);
        return new ValidatedHomework(assignment, submissionType, normalizeGradingMode(assignment.getGradingMode()));
    }

    private void requireActiveQuizResource(HomeworkAssignment assignment) {
        if (assignment.getQuizResourceId() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库资源不存在或已删除");
        }
        AiResource resource = aiResourceMapper.selectById(assignment.getQuizResourceId());
        if (resource == null
                || (resource.getIsDelete() != null && resource.getIsDelete() == 1)
                || !"quiz".equals(resource.getType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库资源不存在或已删除");
        }
    }

    private void validatePayload(HomeworkSubmitRequest request,
                                 String assignmentAnswerMode,
                                 String submissionType,
                                 boolean hasImages) {
        boolean hasText = StringUtils.isNotBlank(request.getStudentAnswerJson())
                && !"[]".equals(request.getStudentAnswerJson().trim());
        if ("image".equals(assignmentAnswerMode) && !hasImages) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "this homework requires image answers");
        }
        if ("online".equals(submissionType) && !hasText) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "answer cannot be empty");
        }
        if (isImageType(submissionType) && !hasImages) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "image answer cannot be empty");
        }
    }

    private String normalizeAnswerMode(String answerMode) {
        return "image".equals(answerMode) || "mixed".equals(answerMode) ? answerMode : "online";
    }

    private String normalizeGradingMode(String gradingMode) {
        return "ai_review".equals(gradingMode) ? "ai_review" : "auto";
    }

    private String normalizeSubmissionType(String requestedType, String assignmentAnswerMode, boolean hasImages) {
        if ("image".equals(requestedType) || "mixed".equals(requestedType) || "online".equals(requestedType)) {
            return requestedType;
        }
        if ("image".equals(assignmentAnswerMode)) {
            return "image";
        }
        if ("mixed".equals(assignmentAnswerMode) && hasImages) {
            return "mixed";
        }
        return hasImages ? "image" : "online";
    }

    private boolean isImageType(String submissionType) {
        return "image".equals(submissionType) || "mixed".equals(submissionType);
    }

    public record ValidatedHomework(HomeworkAssignment assignment,
                                    String submissionType,
                                    String gradingMode) {
    }
}
