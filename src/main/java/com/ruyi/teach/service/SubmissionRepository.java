package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionDetailMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionImageMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionMapper;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.HomeworkSubmissionImage;
import com.ruyi.teach.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * The write boundary for submission aggregates.
 */
@Repository
public class SubmissionRepository {

    private final HomeworkAssignmentMapper assignmentMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final HomeworkSubmissionDetailMapper detailMapper;
    private final HomeworkSubmissionImageMapper imageMapper;
    private final SubmissionValidator submissionValidator;
    private final AnswerParser answerParser;

    public SubmissionRepository(HomeworkAssignmentMapper assignmentMapper,
                                HomeworkSubmissionMapper submissionMapper,
                                HomeworkSubmissionDetailMapper detailMapper,
                                HomeworkSubmissionImageMapper imageMapper,
                                SubmissionValidator submissionValidator,
                                AnswerParser answerParser) {
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.detailMapper = detailMapper;
        this.imageMapper = imageMapper;
        this.submissionValidator = submissionValidator;
        this.answerParser = answerParser;
    }

    /**
     * Saves the submission header and all image rows atomically. Locking the
     * assignment row prevents two concurrent requests from allocating the same
     * attempt number or both passing the max-attempt check.
     */
    @Transactional(rollbackFor = Exception.class)
    public HomeworkSubmission createHomeworkSubmission(HomeworkSubmitRequest request, User loginUser) {
        if (request == null || request.getAssignmentId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "作业ID和答案不能为空");
        }
        HomeworkAssignment lockedAssignment = assignmentMapper.selectByIdForUpdate(request.getAssignmentId());
        SubmissionValidator.ValidatedHomework validated =
                submissionValidator.validateHomework(request, loginUser, lockedAssignment);

        LambdaQueryWrapper<HomeworkSubmission> query = new LambdaQueryWrapper<>();
        query.eq(HomeworkSubmission::getAssignmentId, request.getAssignmentId())
                .eq(HomeworkSubmission::getStudentId, loginUser.getId())
                .eq(HomeworkSubmission::getIsDelete, 0)
                .orderByAsc(HomeworkSubmission::getAttemptNo);
        List<HomeworkSubmission> existing = submissionMapper.selectList(query);

        boolean hasCompleted = existing.stream()
                .anyMatch(item -> "completed".equals(item.getSubmitStatus()));
        if (hasCompleted
                && (validated.assignment().getAllowRedo() == null
                || validated.assignment().getAllowRedo() == 0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该作业已完成且不允许重做");
        }
        Integer maxAttemptCount = validated.assignment().getMaxAttemptCount();
        if (maxAttemptCount != null && maxAttemptCount > 0 && existing.size() >= maxAttemptCount) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "已达最大提交次数（" + maxAttemptCount + "次）"
            );
        }

        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setAssignmentId(request.getAssignmentId());
        submission.setTeacherId(validated.assignment().getTeacherId());
        submission.setStudentId(loginUser.getId());
        submission.setClassId(loginUser.getClassId());
        submission.setCourseId(validated.assignment().getCourseId());
        submission.setAttemptNo(existing.size() + 1);
        submission.setSubmitStatus("judging");
        submission.setSubmissionType(validated.submissionType());
        submission.setGradingModeSnapshot(validated.gradingMode());
        submission.setReviewStatus("none");
        if (isImageType(validated.submissionType())) {
            submission.setVisionStatus("pending");
        }
        submission.setStudentAnswerJson(
                StringUtils.defaultIfBlank(request.getStudentAnswerJson(), "[]")
        );
        submission.setSubmitTime(new Date());
        submissionMapper.insert(submission);

        if (isImageType(validated.submissionType())) {
            saveSubmissionImages(submission.getId(), request);
        }
        return submission;
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceDetails(Long submissionId, List<HomeworkSubmissionDetail> details) {
        replaceDetailsInternal(submissionId, details);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSubmissionAndReplaceDetails(HomeworkSubmission submission,
                                                  List<HomeworkSubmissionDetail> details) {
        if (submission == null || submission.getId() == null) {
            throw new IllegalArgumentException("提交记录不能为空");
        }
        if (submissionMapper.updateById(submission) != 1) {
            throw new IllegalStateException("提交状态更新失败，submissionId=" + submission.getId());
        }
        replaceDetailsInternal(submission.getId(), details);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetForRegrade(HomeworkSubmission submission) {
        if (submission == null || submission.getId() == null) {
            throw new IllegalArgumentException("提交记录不能为空");
        }
        submission.setSubmitStatus("judging");
        submission.setReviewStatus("none");
        submission.setTotalScore(null);
        submission.setCorrectCount(null);
        submission.setWrongCount(null);
        if (submissionMapper.updateById(submission) != 1) {
            throw new IllegalStateException("提交状态重置失败，submissionId=" + submission.getId());
        }
        replaceDetailsInternal(submission.getId(), List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void markReviewPending(Long submissionId) {
        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return;
        }
        LambdaQueryWrapper<HomeworkSubmissionDetail> query = new LambdaQueryWrapper<>();
        query.eq(HomeworkSubmissionDetail::getSubmissionId, submissionId);
        List<HomeworkSubmissionDetail> details = detailMapper.selectList(query);

        int suggestedTotal = submission.getTotalScore() == null ? 0 : submission.getTotalScore();
        if (!details.isEmpty()) {
            suggestedTotal = 0;
            boolean hasDetailScore = false;
            for (HomeworkSubmissionDetail detail : details) {
                if (detail.getScore() != null) {
                    detail.setAiSuggestedScore(detail.getScore());
                    suggestedTotal += detail.getScore();
                    hasDetailScore = true;
                }
                detail.setScore(null);
                detail.setIsCorrect(null);
                detailMapper.updateById(detail);
            }
            if (!hasDetailScore && submission.getTotalScore() != null) {
                suggestedTotal = submission.getTotalScore();
            }
        }

        submission.setAiSuggestedTotalScore(suggestedTotal);
        submission.setTotalScore(null);
        submission.setCorrectCount(null);
        submission.setWrongCount(null);
        submission.setSubmitStatus("review_pending");
        submission.setReviewStatus("pending");
        submission.setJudgeTime(new Date());
        if (submissionMapper.updateById(submission) != 1) {
            throw new IllegalStateException("提交待复核状态更新失败，submissionId=" + submissionId);
        }
    }

    private void replaceDetailsInternal(Long submissionId, List<HomeworkSubmissionDetail> details) {
        LambdaQueryWrapper<HomeworkSubmissionDetail> delete = new LambdaQueryWrapper<>();
        delete.eq(HomeworkSubmissionDetail::getSubmissionId, submissionId);
        detailMapper.delete(delete);
        if (details == null) {
            return;
        }
        for (HomeworkSubmissionDetail detail : details) {
            detail.setId(null);
            detail.setSubmissionId(submissionId);
            detailMapper.insert(detail);
        }
    }

    private void saveSubmissionImages(Long submissionId, HomeworkSubmitRequest request) {
        int order = 0;
        if (request.getWholePaperImageUrls() != null) {
            for (String imageUrl : request.getWholePaperImageUrls()) {
                if (StringUtils.isBlank(imageUrl)) {
                    continue;
                }
                insertSubmissionImage(submissionId, null, imageUrl, order++);
            }
        }
        Set<String> allowedQuestionNos =
                answerParser.parseImageAnswerQuestionNos(request.getStudentAnswerJson());
        if (request.getQuestionImageItems() != null) {
            for (HomeworkSubmitRequest.QuestionImageItem item : request.getQuestionImageItems()) {
                if (item == null || item.getImageUrls() == null) {
                    continue;
                }
                String questionNo = StringUtils.trimToEmpty(item.getQuestionNo());
                if (!allowedQuestionNos.contains(questionNo)) {
                    continue;
                }
                for (String imageUrl : item.getImageUrls()) {
                    if (StringUtils.isBlank(imageUrl)) {
                        continue;
                    }
                    insertSubmissionImage(submissionId, questionNo, imageUrl, order++);
                }
            }
        }
    }

    private void insertSubmissionImage(Long submissionId,
                                       String questionNo,
                                       String imageUrl,
                                       int order) {
        HomeworkSubmissionImage image = new HomeworkSubmissionImage();
        image.setSubmissionId(submissionId);
        image.setQuestionNo(StringUtils.defaultIfBlank(questionNo, null));
        image.setImageUrl(imageUrl);
        image.setImageOrder(order);
        image.setStatus("pending");
        imageMapper.insert(image);
    }

    private boolean isImageType(String submissionType) {
        return "image".equals(submissionType) || "mixed".equals(submissionType);
    }
}
