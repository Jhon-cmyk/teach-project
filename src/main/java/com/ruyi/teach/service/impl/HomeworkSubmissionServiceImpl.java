package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionDetailMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionImageMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionMapper;
import com.ruyi.teach.model.dto.ExamGradeRequest;
import com.ruyi.teach.model.dto.HomeworkSubmissionReviewRequest;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.HomeworkSubmissionImage;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.HomeworkHistoryVO;
import com.ruyi.teach.model.vo.HomeworkReportVO;
import com.ruyi.teach.service.DeepSeekService;
import com.ruyi.teach.service.AnswerParser;
import com.ruyi.teach.service.AutoGradingService;
import com.ruyi.teach.service.HomeworkAssignmentService;
import com.ruyi.teach.service.HomeworkReportService;
import com.ruyi.teach.service.HomeworkSubmissionService;
import com.ruyi.teach.service.SubmissionRepository;
import com.ruyi.teach.service.VisionProvider;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.util.function.Consumer;

@Service
public class HomeworkSubmissionServiceImpl
        extends ServiceImpl<HomeworkSubmissionMapper, HomeworkSubmission>
        implements HomeworkSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(HomeworkSubmissionServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private HomeworkAssignmentService assignmentService;

    @Resource
    private HomeworkSubmissionDetailMapper detailMapper;

    @Resource
    private HomeworkAssignmentMapper assignmentMapper;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private HomeworkSubmissionImageMapper imageMapper;

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private VisionProvider visionProvider;

    @Resource
    private AnswerParser answerParser;

    @Resource
    private AutoGradingService autoGradingService;

    @Resource
    private SubmissionRepository submissionRepository;

    @Resource
    private HomeworkReportService homeworkReportService;

    private AnswerParser activeAnswerParser() {
        return answerParser != null ? answerParser : fallbackAnswerParser();
    }

    private AnswerParser fallbackAnswerParser() {
        return new AnswerParser(objectMapper);
    }

    // ====================== 提交作业 ======================

    @Override
    public Long submitHomework(HomeworkSubmitRequest req, User loginUser) {
        HomeworkSubmission submission = createJudgingSubmission(req, loginUser);
        finishSubmission(submission);
        return submission.getId();
    }

    // ====================== 学生历史 ======================

    @Override
    public List<HomeworkHistoryVO> getStudentHistory(User loginUser) {
        LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
        sw.eq(HomeworkSubmission::getStudentId, loginUser.getId())
                .eq(HomeworkSubmission::getIsDelete, 0)
                .orderByDesc(HomeworkSubmission::getCreateTime);
        List<HomeworkSubmission> submissions = this.list(sw);

        if (submissions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> aIds = submissions.stream()
                .map(HomeworkSubmission::getAssignmentId)
                .collect(Collectors.toSet());
        Map<Long, HomeworkAssignment> aMap = assignmentService.listByIds(aIds)
                .stream()
                .collect(Collectors.toMap(HomeworkAssignment::getId, a -> a));
        Map<Long, AiResource> quizResourceMap = listQuizResourceMap(aMap.values());

        // 过滤掉考试类型的提交记录
        submissions = submissions.stream()
                .filter(s -> {
                    HomeworkAssignment a = aMap.get(s.getAssignmentId());
                    return isVisibleStudentHomeworkHistoryAssignment(a)
                            && isActiveAssignmentQuizResource(a, quizResourceMap);
                })
                .collect(Collectors.toList());

        return submissions.stream().map(s -> {
            HomeworkHistoryVO vo = new HomeworkHistoryVO();
            vo.setSubmissionId(s.getId());
            vo.setAssignmentId(s.getAssignmentId());
            HomeworkAssignment a = aMap.get(s.getAssignmentId());
            vo.setTitle(a != null ? a.getTitle() : "未知作业");
            vo.setCourseName(null);
            vo.setSubmitTime(s.getSubmitTime());
            vo.setTotalScore(s.getTotalScore());
            vo.setCorrectCount(s.getCorrectCount());
            vo.setWrongCount(s.getWrongCount());
            vo.setSubmitStatus(s.getSubmitStatus());
            String report = s.getAiReportMarkdown();
            vo.setReportSummary(report != null && report.length() > 100
                    ? report.substring(0, 100) + "..." : report);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStudentHomeworkHistory(Long submissionId, User loginUser) {
        if (submissionId == null || submissionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交记录 ID 不合法");
        }
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业记录不存在或已删除");
        }
        if (!Objects.equals(submission.getStudentId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能删除自己的作业记录");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (!isVisibleStudentHomeworkHistoryAssignment(assignment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该提交不属于可删除的作业记录");
        }
        if (!this.removeById(submissionId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除作业记录失败");
        }
    }

    private boolean isVisibleStudentHomeworkHistoryAssignment(HomeworkAssignment assignment) {
        return assignment != null
                && (assignment.getIsDelete() == null || assignment.getIsDelete() == 0)
                && ("homework".equals(StringUtils.defaultIfBlank(assignment.getAssignmentType(), "homework"))
                    || "personal_practice".equals(assignment.getAssignmentType()));
    }

    private Map<Long, AiResource> listQuizResourceMap(Collection<HomeworkAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> resourceIds = assignments.stream()
                .filter(assignment -> assignment != null
                        && (assignment.getIsDelete() == null || assignment.getIsDelete() == 0))
                .map(HomeworkAssignment::getQuizResourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (resourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return aiResourceMapper.selectBatchIds(resourceIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AiResource::getId, r -> r, (a, b) -> a));
    }

    private boolean isActiveAssignmentQuizResource(HomeworkAssignment assignment,
                                                   Map<Long, AiResource> quizResourceMap) {
        if (assignment == null || assignment.getQuizResourceId() == null || quizResourceMap == null) {
            return false;
        }
        AiResource resource = quizResourceMap.get(assignment.getQuizResourceId());
        return resource != null
                && (resource.getIsDelete() == null || resource.getIsDelete() == 0)
                && "quiz".equals(resource.getType());
    }

    private boolean isActiveAssignmentQuizResource(HomeworkAssignment assignment) {
        if (assignment == null || assignment.getQuizResourceId() == null) {
            return false;
        }
        AiResource resource = aiResourceMapper.selectById(assignment.getQuizResourceId());
        return resource != null
                && (resource.getIsDelete() == null || resource.getIsDelete() == 0)
                && "quiz".equals(resource.getType());
    }

    private boolean isVisibleStudentExamHistoryAssignment(HomeworkAssignment assignment) {
        return assignment != null
                && (assignment.getIsDelete() == null || assignment.getIsDelete() == 0)
                && "exam".equals(assignment.getAssignmentType());
    }

    private boolean isVisibleStudentReportAssignment(HomeworkAssignment assignment) {
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            return false;
        }
        String type = StringUtils.defaultIfBlank(assignment.getAssignmentType(), "homework");
        return ("homework".equals(type) || "exam".equals(type) || "chapter_practice".equals(type)
                || "personal_practice".equals(type))
                && isActiveAssignmentQuizResource(assignment);
    }

    // ====================== 单次报告详情 ======================


    @Override
    public Long submitHomeworkAsync(HomeworkSubmitRequest req, User loginUser) {
        HomeworkSubmission submission = createJudgingSubmission(req, loginUser);
        finishSubmission(submission);
        return submission.getId();
    }

    private void finishSubmission(HomeworkSubmission submission) {
        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment != null && "personal_practice".equals(assignment.getAssignmentType())) {
            gradeSubmissionSync(submission.getId());
            return;
        }
        prepareSubmittedHomeworkForTeacherReview(submission.getId());
    }

    @Override
    public String streamGradeSubmission(Long submissionId, User loginUser, Consumer<String> onChunk) {
        HomeworkSubmission submission = requireOwnedSubmission(submissionId, loginUser);
        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment != null && "personal_practice".equals(assignment.getAssignmentType())) {
            String report = StringUtils.defaultIfBlank(submission.getAiReportMarkdown(), "专项练习已完成自动批改。");
            if (onChunk != null) onChunk.accept(report);
            return report;
        }
        if ("judging".equals(submission.getSubmitStatus()) || "submitted".equals(submission.getSubmitStatus())) {
            prepareSubmittedHomeworkForTeacherReview(submission.getId());
            submission = this.getById(submissionId);
        }
        String message = "作业已提交，等待教师批改。";
        if (onChunk != null) {
            onChunk.accept(message);
        }
        return message;
    }

    private HomeworkSubmission createJudgingSubmission(HomeworkSubmitRequest req, User loginUser) {
        return submissionRepository.createHomeworkSubmission(req, loginUser);
    }

    private boolean hasImagePayload(HomeworkSubmitRequest req) {
        return answerParser != null
                ? answerParser.hasImagePayload(req)
                : fallbackAnswerParser().hasImagePayload(req);
    }

    private boolean isImageSubmission(HomeworkSubmission submission) {
        return submission != null && isImageType(submission.getSubmissionType());
    }

    private boolean isImageType(String submissionType) {
        return "image".equals(submissionType) || "mixed".equals(submissionType);
    }

    private void saveSubmissionImages(Long submissionId, HomeworkSubmitRequest req) {
        int order = 0;
        if (req.getWholePaperImageUrls() != null) {
            for (String imageUrl : req.getWholePaperImageUrls()) {
                if (StringUtils.isBlank(imageUrl)) continue;
                insertSubmissionImage(submissionId, null, imageUrl, order++);
            }
        }
        Set<String> allowedQuestionNos = parseImageAnswerQuestionNos(req.getStudentAnswerJson());
        if (req.getQuestionImageItems() != null) {
            for (HomeworkSubmitRequest.QuestionImageItem item : req.getQuestionImageItems()) {
                if (item == null || item.getImageUrls() == null) continue;
                String questionNo = StringUtils.trimToEmpty(item.getQuestionNo());
                if (!allowedQuestionNos.contains(questionNo)) continue;
                for (String imageUrl : item.getImageUrls()) {
                    if (StringUtils.isBlank(imageUrl)) continue;
                    insertSubmissionImage(submissionId, questionNo, imageUrl, order++);
                }
            }
        }
    }

    private Set<String> parseImageAnswerQuestionNos(String studentAnswerJson) {
        return answerParser != null
                ? answerParser.parseImageAnswerQuestionNos(studentAnswerJson)
                : fallbackAnswerParser().parseImageAnswerQuestionNos(studentAnswerJson);
    }

    private void insertSubmissionImage(Long submissionId, String questionNo, String imageUrl, int order) {
        HomeworkSubmissionImage image = new HomeworkSubmissionImage();
        image.setSubmissionId(submissionId);
        image.setQuestionNo(StringUtils.defaultIfBlank(questionNo, null));
        image.setImageUrl(imageUrl);
        image.setImageOrder(order);
        image.setStatus("pending");
        imageMapper.insert(image);
    }

    private void prepareSubmittedHomeworkForTeacherReview(Long submissionId) {
        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }

        try {
            Map<String, List<String>> imageUrlsByQuestion = Collections.emptyMap();
            if (isImageSubmission(submission)) {
                List<HomeworkSubmissionImage> images = listSubmissionImages(submission.getId());
                if (images.isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "image answer cannot be empty");
                }
                imageUrlsByQuestion = buildImageUrlsByQuestion(images);
                markImagesStoredForTeacherReview(images);
                submission.setVisionStatus("completed");
                submission.setVisionResultJson(toJson(imageUrlsByQuestion));
            }

            List<Map<String, Object>> fullAnswers = prepareAnswerItems(
                    submission.getStudentAnswerJson(),
                    imageUrlsByQuestion
            );
            enrichAnswerItemsWithQuestionMeta(fullAnswers, assignment);
            String aiRaw = applyAiJudgmentToTextAnswers(fullAnswers, assignment);
            String fullAnswerJson = toJson(fullAnswers);

            submission.setStudentAnswerJson(fullAnswerJson);
            submission.setSubmitStatus("review_pending");
            submission.setReviewStatus("pending");
            submission.setTotalScore(null);
            submission.setCorrectCount(null);
            submission.setWrongCount(null);
            submission.setAiSuggestedTotalScore(null);
            submission.setAiReportMarkdown(null);
            submission.setAiReportJson(null);
            submission.setAiRawResponse(aiRaw);
            submission.setJudgeTime(new Date());
            persistSubmissionWithDetails(submission, fullAnswerJson, null);
        } catch (BusinessException e) {
            markVisionFailed(submission, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("准备教师批改数据失败, submissionId={}", submissionId, e);
            submission.setSubmitStatus("failed");
            submission.setReviewStatus("none");
            submission.setAiReportMarkdown("作业提交已保存，但生成批改数据失败：" + e.getMessage());
            this.updateById(submission);
            writeDetailsSafe(submission.getId(), submission.getStudentAnswerJson(), null);
        }
    }

    private void enrichAnswerItemsWithQuestionMeta(List<Map<String, Object>> answers, HomeworkAssignment assignment) {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        List<QuestionMeta> metas = parseQuestionMetas(assignment);
        Map<String, QuestionMeta> byStem = new HashMap<>();
        Map<String, QuestionMeta> byTypeNo = new HashMap<>();
        Map<String, QuestionMeta> byGlobalNo = new HashMap<>();
        for (QuestionMeta meta : metas) {
            if (StringUtils.isNotBlank(meta.stem)) {
                byStem.put(normalizeMetaText(meta.stem), meta);
            }
            if (StringUtils.isNotBlank(meta.type) && StringUtils.isNotBlank(meta.no)) {
                byTypeNo.put(meta.type + "#" + meta.no, meta);
            }
            if (StringUtils.isNotBlank(meta.globalNo)) {
                byGlobalNo.put(meta.globalNo, meta);
            }
        }

        int fallbackFullScore = weightedFallbackFullScore(answers.size());

        for (int i = 0; i < answers.size(); i++) {
            Map<String, Object> item = answers.get(i);
            QuestionMeta meta = null;
            String stemKey = normalizeMetaText(String.valueOf(item.getOrDefault("stem", "")));
            if (StringUtils.isNotBlank(stemKey)) {
                meta = byStem.get(stemKey);
            }
            String type = String.valueOf(item.getOrDefault("type", ""));
            String globalNo = String.valueOf(item.getOrDefault("num", ""));
            if (meta == null && StringUtils.isNotBlank(globalNo)) {
                meta = byGlobalNo.get(globalNo);
            }
            String originalNo = String.valueOf(item.getOrDefault("originalQuestionNo", item.getOrDefault("num", "")));
            if (meta == null && StringUtils.isNotBlank(type) && StringUtils.isNotBlank(originalNo)) {
                meta = byTypeNo.get(type + "#" + originalNo);
            }
            if (meta == null && i < metas.size()) {
                meta = metas.get(i);
            }

            if (meta != null) {
                if (StringUtils.isNotBlank(meta.standardAnswer)) {
                    item.put("standardAnswer", meta.standardAnswer);
                }
                if (!meta.options.isEmpty()) {
                    item.put("optionsJson", writeJsonQuietly(meta.options));
                }
                if (meta.fullScore != null) {
                    item.put("fullScore", meta.fullScore);
                }
                if (StringUtils.isBlank(type) && StringUtils.isNotBlank(meta.type)) {
                    item.put("type", meta.type);
                }
                if (StringUtils.isBlank(String.valueOf(item.getOrDefault("stem", ""))) && StringUtils.isNotBlank(meta.stem)) {
                    item.put("stem", meta.stem);
                }
            }

            if (fullScoreOf(item) <= 0 && fallbackFullScore > 0) {
                item.put("fullScore", fallbackFullScore);
            }
        }
    }

    private String applyAiJudgmentToTextAnswers(List<Map<String, Object>> answers, HomeworkAssignment assignment) {
        return autoGradingService.applyTextAnswerJudgments(answers, assignment);
    }

    private int fullScoreOf(Map<String, Object> item) {
        Object raw = item == null ? null : item.get("fullScore");
        if (raw == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(raw)));
        } catch (Exception ignore) {
            return 0;
        }
    }

    private void gradeSubmissionSync(Long submissionId) {
        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }

        gradePreparedSubmissionSync(submission, assignment, Collections.emptyMap(), false);
    }

    private void gradePreparedSubmissionSync(HomeworkSubmission submission,
                                             HomeworkAssignment assignment,
                                             Map<String, List<String>> imageUrlsByQuestion,
                                             boolean hasManualReviewQuestions) {
        try {
            List<Map<String, Object>> fullAnswers = prepareAnswerItems(
                    submission.getStudentAnswerJson(),
                    imageUrlsByQuestion
            );
            List<Map<String, Object>> aiAnswers = new ArrayList<>();
            boolean hasAutoGradableQuestions = false;

            for (Map<String, Object> item : fullAnswers) {
                boolean hasImages = hasImageUrls(item.get("imageUrls"));
                boolean hasTextAnswer = hasTextAnswer(item.get("answer"));
                if (hasImages) {
                    item.put("aiComment", "图片作答，待教师批改");
                } else if (hasTextAnswer) {
                    aiAnswers.add(item);
                    hasAutoGradableQuestions = true;
                } else {
                    item.put("score", 0);
                    item.put("isCorrect", 0);
                    item.put("aiComment", "未作答，系统记为 0 分");
                }
            }

            String fullAnswerJson = toJson(fullAnswers);
            String aiResponse = "";
            if (hasAutoGradableQuestions) {
                aiResponse = autoGradingService.gradeHomeworkPaper(
                        assignment.getContentSnapshot(),
                        aiAnswers
                );

                if (StringUtils.isBlank(aiResponse)) {
                    submission.setSubmitStatus("failed");
                    submission.setAiReportMarkdown("AI判题服务暂时不可用，请稍后重试。您的作答已保存。");
                    submission.setStudentAnswerJson(fullAnswerJson);
                    persistSubmissionWithDetails(submission, fullAnswerJson, null);
                    return;
                }
            } else if (!hasManualReviewQuestions) {
                aiResponse = "综合评分： 0分\n\n# 答题分析\n本次提交没有可自动批改的文字答案。\n\n# 辅导建议\n请完成作答后再提交。\n<!--STATS:{\"totalScore\":0,\"correctCount\":0,\"wrongCount\":" + fullAnswers.size() + "}-->";
            } else {
                aiResponse = "本次提交中的图片作答题已保存，等待教师批改。";
            }

            applyPreparedAiResponseToSubmission(
                    submission,
                    fullAnswerJson,
                    aiResponse,
                    hasManualReviewQuestions
            );
        } catch (Exception e) {
            log.error("AI判题异常, submissionId={}", submission.getId(), e);
            submission.setSubmitStatus("failed");
            submission.setAiReportMarkdown("AI判题过程中出现异常：" + e.getMessage() + "。您的作答已保存。");
            this.updateById(submission);
            writeDetailsSafe(submission.getId(), submission.getStudentAnswerJson(), null);
        }
    }

    private void applyPreparedAiResponseToSubmission(HomeworkSubmission submission,
                                                     String studentAnswerJson,
                                                     String aiResponse,
                                                     boolean hasManualReviewQuestions) {
        submission.setStudentAnswerJson(StringUtils.defaultIfBlank(studentAnswerJson, "[]"));
        submission.setAiRawResponse(aiResponse);

        Integer parsedTotalScore = null;
        Integer parsedCorrectCount = null;
        Integer parsedWrongCount = null;

        Pattern statsPattern = Pattern.compile("<!--STATS:(\\{.*?\\})-->");
        Matcher matcher = statsPattern.matcher(StringUtils.defaultString(aiResponse));

        if (matcher.find()) {
            try {
                JsonNode stats = objectMapper.readTree(matcher.group(1));
                parsedTotalScore = stats.path("totalScore").asInt(0);
                parsedCorrectCount = stats.path("correctCount").asInt(0);
                parsedWrongCount = stats.path("wrongCount").asInt(0);

                submission.setTotalScore(parsedTotalScore);
                submission.setCorrectCount(parsedCorrectCount);
                submission.setWrongCount(parsedWrongCount);
                submission.setAiReportJson(matcher.group(1));
            } catch (Exception ignore) {
                extractScoreFallback(aiResponse, submission);
                parsedTotalScore = submission.getTotalScore();
            }
        } else {
            extractScoreFallback(aiResponse, submission);
            parsedTotalScore = submission.getTotalScore();
        }

        String cleanReport = StringUtils.defaultString(aiResponse)
                .replaceAll("<!--STATS:\\{.*?\\}-->", "")
                .trim();
        if (hasManualReviewQuestions) {
            cleanReport = StringUtils.defaultIfBlank(cleanReport, "图片作答题已保存，等待教师批改。");
            cleanReport = cleanReport + "\n\n> 含图片作答题，本次分数为 AI 初评，最终成绩以教师批改为准。";
        }

        if (parsedTotalScore != null) {
            cleanReport = cleanReport.replaceAll(
                    "综合评分[：:]\\s*\\d{1,3}\\s*分",
                    "综合评分： " + parsedTotalScore + "分"
            );
        }

        submission.setAiReportMarkdown(cleanReport);
        submission.setSubmitStatus("submitted");
        submission.setJudgeTime(new Date());
        persistSubmissionWithDetails(submission, submission.getStudentAnswerJson(), aiResponse);
        if (hasManualReviewQuestions || "ai_review".equals(submission.getGradingModeSnapshot())) {
            markReviewPending(submission.getId());
        }
    }

    private List<Map<String, Object>> prepareAnswerItems(String studentAnswerJson,
                                                         Map<String, List<String>> imageUrlsByQuestion) {
        return activeAnswerParser().prepareAnswerItems(studentAnswerJson, imageUrlsByQuestion);
    }

    private boolean hasTextAnswer(Object answer) {
        return activeAnswerParser().hasTextAnswer(answer);
    }

    private boolean hasImageUrls(Object imageUrls) {
        return activeAnswerParser().hasImageUrls(imageUrls);
    }

    private String toJson(Object value) {
        try {
            return activeAnswerParser().toJson(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, List<String>> buildImageUrlsByQuestion(List<HomeworkSubmissionImage> images) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (images == null) {
            return result;
        }
        for (HomeworkSubmissionImage image : images) {
            if (image == null || StringUtils.isBlank(image.getImageUrl())) {
                continue;
            }
            String key = StringUtils.defaultIfBlank(image.getQuestionNo(), "__whole__");
            result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(image.getImageUrl());
        }
        return result;
    }

    private void markImagesStoredForTeacherReview(List<HomeworkSubmissionImage> images) {
        if (images == null) {
            return;
        }
        for (HomeworkSubmissionImage image : images) {
            image.setStatus("completed");
            image.setRecognizedText(null);
            image.setVisionJson(null);
            image.setErrorMessage(null);
            imageMapper.updateById(image);
        }
    }

    private void gradeImageSubmissionSync(Long submissionId) {
        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "submission not found");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "homework not found");
        }

        try {
            List<HomeworkSubmissionImage> images = listSubmissionImages(submission.getId());
            if (images.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "image answer cannot be empty");
            }

            Map<String, List<String>> imageUrlsByQuestion = buildImageUrlsByQuestion(images);
            markImagesStoredForTeacherReview(images);
            submission.setVisionStatus("completed");
            submission.setVisionResultJson(toJson(imageUrlsByQuestion));
            this.updateById(submission);

            gradePreparedSubmissionSync(submission, assignment, imageUrlsByQuestion, true);
        } catch (BusinessException e) {
            markVisionFailed(submission, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("image homework grading failed, submissionId={}", submissionId, e);
            markVisionFailed(submission, e.getMessage());
        }
    }

    private List<HomeworkSubmissionImage> listSubmissionImages(Long submissionId) {
        LambdaQueryWrapper<HomeworkSubmissionImage> iw = new LambdaQueryWrapper<>();
        iw.eq(HomeworkSubmissionImage::getSubmissionId, submissionId)
                .eq(HomeworkSubmissionImage::getIsDelete, 0)
                .orderByAsc(HomeworkSubmissionImage::getImageOrder)
                .orderByAsc(HomeworkSubmissionImage::getId);
        return imageMapper.selectList(iw);
    }

    private List<VisionProvider.RecognizedAnswer> recognizeImages(HomeworkAssignment assignment,
                                                                  HomeworkSubmission submission,
                                                                  List<HomeworkSubmissionImage> images) {
        VisionProvider.VisionHomeworkRequest request = new VisionProvider.VisionHomeworkRequest();
        request.setAssignmentId(assignment.getId());
        request.setSubmissionId(submission.getId());
        request.setPaperContent(assignment.getContentSnapshot());

        Map<String, VisionProvider.ImageGroup> groupMap = new LinkedHashMap<>();
        for (HomeworkSubmissionImage image : images) {
            String key = StringUtils.defaultIfBlank(image.getQuestionNo(), "__whole__");
            VisionProvider.ImageGroup group = groupMap.computeIfAbsent(key, k -> {
                VisionProvider.ImageGroup item = new VisionProvider.ImageGroup();
                item.setQuestionNo("__whole__".equals(k) ? null : k);
                return item;
            });
            group.getImageUrls().add(image.getImageUrl());
        }
        request.setImageGroups(new ArrayList<>(groupMap.values()));
        return visionProvider.recognizeHomeworkImages(request);
    }

    private void updateImageRecognition(List<HomeworkSubmissionImage> images,
                                        List<VisionProvider.RecognizedAnswer> recognized,
                                        String errorMessage) {
        Map<String, VisionProvider.RecognizedAnswer> byUrl = new HashMap<>();
        if (recognized != null) {
            for (VisionProvider.RecognizedAnswer answer : recognized) {
                if (answer.getImageUrls() == null) continue;
                for (String url : answer.getImageUrls()) {
                    byUrl.put(url, answer);
                }
            }
        }

        for (HomeworkSubmissionImage image : images) {
            VisionProvider.RecognizedAnswer answer = byUrl.get(image.getImageUrl());
            if (answer != null) {
                image.setStatus("completed");
                image.setRecognizedText(answer.getRecognizedText());
                image.setVisionJson(answer.getRawJson());
                image.setErrorMessage(null);
            } else if (StringUtils.isNotBlank(errorMessage)) {
                image.setStatus("failed");
                image.setErrorMessage(errorMessage);
            }
            imageMapper.updateById(image);
        }
    }

    private String mergeRecognizedAnswers(String existingJson, List<VisionProvider.RecognizedAnswer> recognized) {
        List<Map<String, Object>> answers = new ArrayList<>();
        if (StringUtils.isNotBlank(existingJson)) {
            try {
                List<Map<String, Object>> parsed = objectMapper.readValue(existingJson,
                        new TypeReference<List<Map<String, Object>>>() {});
                if (parsed != null) {
                    answers.addAll(parsed);
                }
            } catch (Exception ignore) {
            }
        }

        int index = 1;
        if (recognized != null) {
            for (VisionProvider.RecognizedAnswer answer : recognized) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("num", StringUtils.defaultIfBlank(answer.getQuestionNo(), String.valueOf(index)));
                item.put("type", "image");
                item.put("stem", StringUtils.isBlank(answer.getQuestionNo()) ? "Whole-paper image answer" : "Image answer");
                item.put("answer", StringUtils.defaultIfBlank(answer.getRecognizedText(), ""));
                item.put("recognizedText", StringUtils.defaultIfBlank(answer.getRecognizedText(), ""));
                item.put("visionConfidence", answer.getConfidence());
                item.put("imageUrls", answer.getImageUrls());
                answers.add(item);
                index++;
            }
        }

        try {
            return objectMapper.writeValueAsString(answers);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void markVisionFailed(HomeworkSubmission submission, String message) {
        if (submission == null) {
            return;
        }
        List<HomeworkSubmissionImage> images = listSubmissionImages(submission.getId());
        updateImageRecognition(images, Collections.emptyList(), message);
        submission.setVisionStatus("failed");
        submission.setSubmitStatus("failed");
        submission.setAiReportMarkdown("Image recognition or grading failed: " + StringUtils.defaultIfBlank(message, "unknown error"));
        this.updateById(submission);
    }

    private HomeworkSubmission requireOwnedSubmission(Long submissionId, User loginUser) {
        if (submissionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "submissionId不能为空");
        }
        if (loginUser == null || !"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可操作");
        }

        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null || (submission.getIsDelete() != null && submission.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }
        if (!Objects.equals(submission.getStudentId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该提交记录");
        }
        return submission;
    }

    private void applyAiResponseToSubmission(HomeworkSubmission submission, String aiResponse) {
        submission.setAiRawResponse(aiResponse);

        Integer parsedTotalScore = null;
        Integer parsedCorrectCount = null;
        Integer parsedWrongCount = null;

        Pattern statsPattern = Pattern.compile("<!--STATS:(\\{.*?\\})-->");
        Matcher matcher = statsPattern.matcher(aiResponse);

        if (matcher.find()) {
            try {
                JsonNode stats = objectMapper.readTree(matcher.group(1));
                parsedTotalScore = stats.path("totalScore").asInt(0);
                parsedCorrectCount = stats.path("correctCount").asInt(0);
                parsedWrongCount = stats.path("wrongCount").asInt(0);

                submission.setTotalScore(parsedTotalScore);
                submission.setCorrectCount(parsedCorrectCount);
                submission.setWrongCount(parsedWrongCount);
                submission.setAiReportJson(matcher.group(1));
            } catch (Exception ignore) {
                extractScoreFallback(aiResponse, submission);
                parsedTotalScore = submission.getTotalScore();
            }
        } else {
            extractScoreFallback(aiResponse, submission);
            parsedTotalScore = submission.getTotalScore();
        }

        String cleanReport = aiResponse.replaceAll("<!--STATS:\\{.*?\\}-->", "").trim();

        // 用结构化统计值覆盖正文中的“综合评分”，避免出现 40 / 60 不一致
        if (parsedTotalScore != null) {
            cleanReport = cleanReport.replaceAll(
                    "综合评分[：:]\\s*\\d{1,3}\\s*分",
                    "综合评分： " + parsedTotalScore + "分"
            );
        }

        submission.setAiReportMarkdown(cleanReport);
        submission.setSubmitStatus("completed");
        submission.setJudgeTime(new Date());
        persistSubmissionWithDetails(submission, submission.getStudentAnswerJson(), aiResponse);
        if ("ai_review".equals(submission.getGradingModeSnapshot())) {
            markReviewPending(submission.getId());
        }
    }

    private void markReviewPending(Long submissionId) {
        submissionRepository.markReviewPending(submissionId);
    }

    private void writeDetailsSafe(Long submissionId, String studentAnswerJson, String aiResponse) {
        try {
            submissionRepository.replaceDetails(
                    submissionId,
                    activeAnswerParser().parseDetails(submissionId, studentAnswerJson, aiResponse)
            );
        } catch (Exception e) {
            log.warn("写入detail失败, submissionId={}", submissionId, e);
        }
    }

    @Override
    public HomeworkReportVO getStudentReport(Long submissionId, User loginUser) {
        if (loginUser == null || !"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可查看");
        }

        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null
                || (submission.getIsDelete() != null && submission.getIsDelete() == 1)
                || !Objects.equals(submission.getStudentId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "记录不存在");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (!isVisibleStudentReportAssignment(assignment)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业或考试不存在");
        }

        return buildHomeworkReportVO(submission);
    }

    @Override
    public HomeworkReportVO getTeacherSubmissionReport(Long submissionId, User loginUser) {
        if (loginUser == null || !"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可查看");
        }

        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null || (submission.getIsDelete() != null && submission.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }

        if (!Objects.equals(assignment.getTeacherId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该学生作答详情");
        }

        return buildHomeworkReportVO(submission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void teacherReviewHomeworkSubmission(HomeworkSubmissionReviewRequest req, User loginUser) {
        if (req == null || req.getSubmissionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "submissionId cannot be empty");
        }
        HomeworkSubmission submission = requireTeacherOwnedSubmission(req.getSubmissionId(), loginUser);
        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());

        LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(HomeworkSubmissionDetail::getSubmissionId, submission.getId())
                .orderByAsc(HomeworkSubmissionDetail::getId);
        List<HomeworkSubmissionDetail> normalizedDetails = detailMapper.selectList(dw);
        enrichReportDetails(normalizedDetails, assignment);
        Map<Long, HomeworkSubmissionDetail> normalizedDetailMap = normalizedDetails.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(HomeworkSubmissionDetail::getId, d -> d, (a, b) -> a));

        int totalScore = 0;
        int correctCount = 0;
        int wrongCount = 0;
        if (req.getDetails() != null) {
            for (HomeworkSubmissionReviewRequest.QuestionScore qs : req.getDetails()) {
                if (qs == null || qs.getId() == null) continue;
                HomeworkSubmissionDetail detail = detailMapper.selectById(qs.getId());
                if (detail == null || !Objects.equals(detail.getSubmissionId(), submission.getId())) {
                    continue;
                }
                HomeworkSubmissionDetail normalized = normalizedDetailMap.get(detail.getId());
                Integer fullScore = normalized != null ? normalized.getFullScore() : detail.getFullScore();
                if (fullScore != null && fullScore > 0) {
                    detail.setFullScore(fullScore);
                }
                Integer score = qs.getScore() == null ? 0 : Math.max(qs.getScore(), 0);
                if (detail.getFullScore() != null && detail.getFullScore() > 0) {
                    score = Math.min(score, detail.getFullScore());
                }
                detail.setScore(score);
                detail.setIsCorrect(score > 0 ? 1 : 0);
                detailMapper.updateById(detail);
                totalScore += score;
                if (score > 0) correctCount++;
                else wrongCount++;
            }
        }

        submission.setTotalScore(Math.min(totalScore, 100));
        submission.setCorrectCount(correctCount);
        submission.setWrongCount(wrongCount);
        submission.setTeacherRemark(req.getTeacherRemark());
        submission.setSubmitStatus("submitted");
        submission.setReviewStatus("approved");
        submission.setJudgeTime(new Date());
        this.updateById(submission);
    }

    @Override
    public String generateHomeworkReviewComment(HomeworkSubmissionReviewRequest req, User loginUser) {
        if (req == null || req.getSubmissionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "submissionId cannot be empty");
        }
        HomeworkSubmission submission = requireTeacherOwnedSubmission(req.getSubmissionId(), loginUser);
        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());

        LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(HomeworkSubmissionDetail::getSubmissionId, submission.getId())
                .orderByAsc(HomeworkSubmissionDetail::getId);
        List<HomeworkSubmissionDetail> details = detailMapper.selectList(dw);
        enrichReportDetails(details, assignment);

        Map<Long, Integer> currentScores = new HashMap<>();
        if (req.getDetails() != null) {
            for (HomeworkSubmissionReviewRequest.QuestionScore qs : req.getDetails()) {
                if (qs != null && qs.getId() != null) {
                    currentScores.put(qs.getId(), qs.getScore() == null ? 0 : Math.max(qs.getScore(), 0));
                }
            }
        }

        String comment = deepSeekService.chat(
                "你是一位负责、具体、克制的教师助教。请根据学生作答和教师最终给分生成中文作业总评，只输出评语正文。",
                buildHomeworkReviewCommentPrompt(assignment, details, currentScores),
                1200
        );
        return StringUtils.defaultIfBlank(comment, "本次作业已完成批改，请结合错题订正并复习相关知识点。").trim();
    }

    private String buildHomeworkReviewCommentPrompt(HomeworkAssignment assignment,
                                                    List<HomeworkSubmissionDetail> details,
                                                    Map<Long, Integer> currentScores) {
        StringBuilder sb = new StringBuilder();
        sb.append("作业标题：").append(assignment == null ? "" : StringUtils.defaultString(assignment.getTitle())).append("\n");
        sb.append("请生成一段适合学生查看的总评，要求：\n");
        sb.append("1. 先肯定已完成的部分，再指出主要问题。\n");
        sb.append("2. 明确提醒需要订正的题号或知识点。\n");
        sb.append("3. 不要提到AI，不要编造未出现的学习行为。\n");
        sb.append("4. 对“学生已上传图片作答”的题，不能说未提交、未作答或需要补交文字答案；若得分为0，只说明该题作答未达到要求，需要订正解题过程。\n");
        sb.append("5. 80到160字。\n\n");
        sb.append("逐题情况：\n");
        int total = 0;
        int full = 0;
        for (HomeworkSubmissionDetail detail : details) {
            int fullScore = detail.getFullScore() == null ? 0 : detail.getFullScore();
            int score = currentScores.getOrDefault(detail.getId(), detail.getScore() == null ? 0 : detail.getScore());
            if (fullScore > 0) {
                score = Math.min(score, fullScore);
            }
            total += score;
            full += fullScore;
            sb.append("第").append(StringUtils.defaultString(detail.getQuestionNo(), "--")).append("题");
            sb.append("，得分 ").append(score).append("/").append(fullScore);
            sb.append("，题型 ").append(StringUtils.defaultString(detail.getQuestionType(), ""));
            sb.append("，题干：").append(StringUtils.left(StringUtils.defaultString(detail.getStemSnapshot()), 120));
            boolean hasImageAnswer = hasImageUrls(detail.getImageUrlsJson());
            if (hasImageAnswer) {
                sb.append("，学生答案：学生已上传图片作答");
                if (score <= 0 && fullScore > 0) {
                    sb.append("，本题得分为0表示图片作答内容未达到要求，不代表未提交");
                }
            } else {
                sb.append("，学生答案：").append(StringUtils.left(StringUtils.defaultString(detail.getStudentAnswer()), 120));
            }
            sb.append("，参考答案：").append(StringUtils.left(StringUtils.defaultString(detail.getStandardAnswer()), 120));
            if (StringUtils.isNotBlank(detail.getAiComment())) {
                sb.append("，判定：").append(StringUtils.left(detail.getAiComment(), 120));
            }
            sb.append("\n");
        }
        sb.append("\n总分：").append(total).append("/").append(full).append("\n");
        return sb.toString();
    }

    @Override
    public void teacherRegradeHomeworkSubmission(Long submissionId, User loginUser) {
        HomeworkSubmission submission = requireTeacherOwnedSubmission(submissionId, loginUser);
        submissionRepository.resetForRegrade(submission);
        prepareSubmittedHomeworkForTeacherReview(submission.getId());
    }

    private HomeworkSubmission requireTeacherOwnedSubmission(Long submissionId, User loginUser) {
        if (loginUser == null || !"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "teacher only");
        }
        if (submissionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "submissionId cannot be empty");
        }
        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null || (submission.getIsDelete() != null && submission.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "submission not found");
        }
        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "homework not found");
        }
        if (!Objects.equals(assignment.getTeacherId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "no permission");
        }
        return submission;
    }

    private HomeworkReportVO buildHomeworkReportVO(HomeworkSubmission submission) {
        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }

        LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(HomeworkSubmissionDetail::getSubmissionId, submission.getId())
                .orderByAsc(HomeworkSubmissionDetail::getId);
        List<HomeworkSubmissionDetail> details = detailMapper.selectList(dw);
        enrichReportDetails(details, assignment);
        List<HomeworkSubmissionImage> images = listSubmissionImages(submission.getId());

        return homeworkReportService.build(submission, assignment, details, images);
    }

    private void enrichReportDetails(List<HomeworkSubmissionDetail> details, HomeworkAssignment assignment) {
        if (details == null || details.isEmpty()) {
            return;
        }

        List<QuestionMeta> metas = parseQuestionMetas(assignment);
        Map<String, QuestionMeta> byStem = new HashMap<>();
        Map<String, QuestionMeta> byTypeNo = new HashMap<>();
        Map<String, QuestionMeta> byGlobalNo = new HashMap<>();
        for (QuestionMeta meta : metas) {
            if (StringUtils.isNotBlank(meta.stem)) {
                byStem.put(normalizeMetaText(meta.stem), meta);
            }
            if (StringUtils.isNotBlank(meta.type) && StringUtils.isNotBlank(meta.no)) {
                byTypeNo.put(meta.type + "#" + meta.no, meta);
            }
            if (StringUtils.isNotBlank(meta.globalNo)) {
                byGlobalNo.put(meta.globalNo, meta);
            }
        }

        int fallbackFullScore = weightedFallbackFullScore(details.size());

        for (int i = 0; i < details.size(); i++) {
            HomeworkSubmissionDetail detail = details.get(i);
            QuestionMeta meta = null;
            String stemKey = normalizeMetaText(detail.getStemSnapshot());
            if (StringUtils.isNotBlank(stemKey)) {
                meta = byStem.get(stemKey);
            }
            String questionNo = StringUtils.trimToEmpty(detail.getQuestionNo());
            if (meta == null && StringUtils.isNotBlank(questionNo)) {
                meta = byGlobalNo.get(questionNo);
            }
            if (meta == null && StringUtils.isNotBlank(detail.getQuestionType()) && StringUtils.isNotBlank(questionNo)) {
                meta = byTypeNo.get(detail.getQuestionType() + "#" + questionNo);
            }
            if (meta == null && i < metas.size()) {
                meta = metas.get(i);
            }

            if (meta != null) {
                if (StringUtils.isBlank(detail.getQuestionNo()) && StringUtils.isNotBlank(meta.displayNo())) {
                    detail.setQuestionNo(meta.displayNo());
                }
                if (StringUtils.isNotBlank(meta.type)) {
                    detail.setQuestionType(meta.type);
                }
                if (StringUtils.isNotBlank(meta.stem)) {
                    detail.setStemSnapshot(meta.stem);
                }
                if (StringUtils.isNotBlank(meta.standardAnswer)) {
                    detail.setStandardAnswer(meta.standardAnswer);
                } else if (StringUtils.isNotBlank(detail.getStandardAnswer())) {
                    detail.setStandardAnswer(normalizeStandardAnswerForMeta(meta, detail.getStandardAnswer()));
                }
                if (!meta.options.isEmpty()) {
                    detail.setOptionsJson(writeJsonQuietly(meta.options));
                }
                if (meta.fullScore != null) {
                    detail.setFullScore(meta.fullScore);
                }
            }

            if (StringUtils.isBlank(detail.getStandardAnswer())) {
                detail.setStandardAnswer(extractStandardAnswerFromComment(detail.getAiComment()));
            }
            if ((detail.getFullScore() == null || detail.getFullScore() <= 0) && fallbackFullScore > 0) {
                detail.setFullScore(fallbackFullScore);
            }
            detail.setAiComment(cleanDisplayAiComment(detail.getAiComment(), detail.getStudentAnswer()));
        }
    }

    private void persistSubmissionWithDetails(HomeworkSubmission submission,
                                              String studentAnswerJson,
                                              String aiResponse) {
        submissionRepository.updateSubmissionAndReplaceDetails(
                submission,
                activeAnswerParser().parseDetails(submission.getId(), studentAnswerJson, aiResponse)
        );
    }

    private List<QuestionMeta> parseQuestionMetas(HomeworkAssignment assignment) {
        if (assignment == null || StringUtils.isBlank(assignment.getContentSnapshot())) {
            return Collections.emptyList();
        }

        PaperParts parts = splitPaperAndAnswers(assignment.getContentSnapshot());
        List<QuestionMeta> metas = parseQuestionList(parts.paper);
        fillStandardAnswers(metas, parts.answers);

        normalizeMetaFullScores(metas);
        return metas;
    }

    private void normalizeMetaFullScores(List<QuestionMeta> metas) {
        if (metas == null || metas.isEmpty()) {
            return;
        }
        int explicitTotal = 0;
        boolean allHaveExplicitScore = true;
        for (QuestionMeta meta : metas) {
            if (meta == null || meta.fullScore == null || meta.fullScore <= 0) {
                allHaveExplicitScore = false;
            } else {
                explicitTotal += meta.fullScore;
            }
        }
        if (allHaveExplicitScore && explicitTotal == 100) {
            return;
        }

        double totalWeight = 0;
        double[] exactScores = new double[metas.size()];
        int[] scores = new int[metas.size()];
        for (QuestionMeta meta : metas) {
            totalWeight += questionScoreWeight(meta == null ? null : meta.type);
        }
        if (totalWeight <= 0) {
            totalWeight = metas.size();
        }

        int minScore = metas.size() <= 100 ? 1 : 0;
        int assigned = 0;
        for (int i = 0; i < metas.size(); i++) {
            QuestionMeta meta = metas.get(i);
            double exact = questionScoreWeight(meta == null ? null : meta.type) * 100.0 / totalWeight;
            exactScores[i] = exact;
            scores[i] = Math.max(minScore, (int) Math.floor(exact));
            assigned += scores[i];
        }

        while (assigned > 100) {
            int idx = indexOfLargestReducibleScore(scores, minScore);
            if (idx < 0) break;
            scores[idx]--;
            assigned--;
        }
        while (assigned < 100) {
            int idx = indexOfLargestFraction(exactScores, scores);
            if (idx < 0) break;
            scores[idx]++;
            assigned++;
        }

        for (int i = 0; i < metas.size(); i++) {
            QuestionMeta meta = metas.get(i);
            if (meta != null) {
                meta.fullScore = scores[i];
            }
        }
    }

    private int weightedFallbackFullScore(int count) {
        return count <= 0 ? 0 : Math.max(1, 100 / count);
    }

    private double questionScoreWeight(String type) {
        String normalized = StringUtils.defaultString(type);
        switch (normalized) {
            case "radio":
            case "judge":
                return 1.0;
            case "checkbox":
                return 1.2;
            case "fill":
                return 1.5;
            case "text":
            case "image":
                return 3.0;
            default:
                return 2.0;
        }
    }

    private int indexOfLargestFraction(double[] exactScores, int[] scores) {
        int best = -1;
        double bestFraction = -1;
        for (int i = 0; i < exactScores.length; i++) {
            double fraction = exactScores[i] - Math.floor(exactScores[i]);
            if (fraction > bestFraction || (fraction == bestFraction && (best < 0 || scores[i] < scores[best]))) {
                best = i;
                bestFraction = fraction;
            }
        }
        return best;
    }

    private int indexOfLargestReducibleScore(int[] scores, int minScore) {
        int best = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > minScore && (best < 0 || scores[i] > scores[best])) {
                best = i;
            }
        }
        return best;
    }

    private PaperParts splitPaperAndAnswers(String raw) {
        String[] lines = normalizeQuizMarkdownLayout(raw).split("\n");
        int cutIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            String text = normalizePaperLine(lines[i]);
            if (isAnswerHeading(text)) {
                cutIndex = i;
                break;
            }
            if ("---".equals(text) && hasAnswerContentAfterSeparator(lines, i + 1)) {
                cutIndex = i;
                break;
            }
        }
        if (cutIndex < 0) {
            return new PaperParts(String.join("\n", lines), "");
        }
        return new PaperParts(
                String.join("\n", Arrays.copyOfRange(lines, 0, cutIndex)),
                String.join("\n", Arrays.copyOfRange(lines, cutIndex, lines.length))
        );
    }

    private boolean hasAnswerContentAfterSeparator(String[] lines, int startIndex) {
        if (lines == null) {
            return false;
        }
        Pattern answerEntryPattern = Pattern.compile("^(?:第\\s*\\d+\\s*题|[（(]?\\d+[)）]?[.．、]|\\d+\\s*[:：])\\s*[:：]?\\s*(?:答案|参考答案|标准答案|【答案】|\\[答案\\]).+");
        for (int i = startIndex; i < lines.length; i++) {
            String text = normalizePaperLine(lines[i]);
            if (StringUtils.isBlank(text)) {
                continue;
            }
            return isAnswerHeading(text) || answerEntryPattern.matcher(text).find();
        }
        return false;
    }

    private boolean isAnswerHeading(String text) {
        return StringUtils.isNotBlank(text)
                && (text.matches("^(答案解析区|参考答案(?:与解析)?|标准答案(?:与解析)?|答案(?:与解析|解析)?|解析部分|试题答案|答案汇总|附[:：]?\\s*参考答案|参考解答)$")
                || text.matches("^(?:[一二三四五六七八九十]+|\\d+)[、.．]?\\s*(?:单项选择题|单选题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题)\\s*(?:[（(][^）)]*[）)])?\\s*(?:参考)?答案(?:与解析|解析)?\\s*(?:[（(][^）)]*[）)])?$")
                || text.matches("^(?:单项选择题|单选题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题)\\s*(?:[（(][^）)]*[）)])?\\s*(?:参考)?答案(?:与解析|解析)?\\s*(?:[（(][^）)]*[）)])?$"));
    }

    private List<QuestionMeta> parseQuestionList(String paper) {
        List<QuestionMeta> result = new ArrayList<>();
        String currentType = "text";
        QuestionMeta current = null;
        int globalIndex = 0;

        for (String rawLine : StringUtils.defaultString(paper).split("\n")) {
            String line = normalizePaperLine(rawLine);
            if (StringUtils.isBlank(line)) {
                continue;
            }
            if ("---".equals(line)) {
                continue;
            }
            String sectionType = detectQuestionType(line);
            if (sectionType != null) {
                if (current != null) {
                    result.add(current);
                    current = null;
                }
                currentType = sectionType;
                continue;
            }

            Matcher qm = Pattern.compile("^(?:第\\s*(\\d+)\\s*题|[（(]?(\\d+)[)）]?[.．、])\\s*[:：]?\\s*(.+)$").matcher(line);
            if (qm.find()) {
                if (current != null) {
                    result.add(current);
                }
                String no = StringUtils.defaultIfBlank(qm.group(1), qm.group(2));
                String stem = StringUtils.trimToEmpty(qm.group(3));
                QuestionMeta meta = new QuestionMeta();
                meta.no = no;
                meta.globalNo = String.valueOf(++globalIndex);
                meta.type = currentType;
                meta.stem = stripScoreText(stem);
                meta.fullScore = extractFullScore(stem);
                current = meta;
                continue;
            }

            if (current != null && !isOptionLine(line)) {
                current.stem = StringUtils.trimToEmpty(current.stem + "\n" + line);
            } else if (current != null) {
                QuestionOption option = parseQuestionOption(line);
                if (option != null) {
                    current.options.add(option);
                }
            }
        }
        if (current != null) {
            result.add(current);
        }
        inferQuestionTypesFromOptions(result);
        return result;
    }

    private void inferQuestionTypesFromOptions(List<QuestionMeta> metas) {
        if (metas == null || metas.isEmpty()) {
            return;
        }
        for (QuestionMeta meta : metas) {
            if (meta == null || !"text".equals(meta.type) || meta.options.size() < 2) {
                continue;
            }
            boolean judgeLike = meta.options.size() == 2
                    && meta.options.stream().allMatch(option -> option.text.matches(".*(正确|错误|对|错|√|×|TRUE|FALSE).*"));
            if (judgeLike) {
                meta.type = "judge";
            } else if (StringUtils.defaultString(meta.stem).matches(".*(多项选择|多选|不定项).*")) {
                meta.type = "checkbox";
            } else {
                meta.type = "radio";
            }
        }
    }

    private void fillStandardAnswers(List<QuestionMeta> metas, String answers) {
        if (metas.isEmpty() || StringUtils.isBlank(answers)) {
            return;
        }

        Map<String, String> byTypeNo = new HashMap<>();
        Map<String, String> byUntypedNo = new HashMap<>();
        Map<String, String> byGlobalNo = new HashMap<>();
        List<AnswerEntry> sequential = new ArrayList<>();
        String currentType = "";

        String[] answerLines = normalizeQuizMarkdownLayout(answers).split("\n");
        for (int lineIndex = 0; lineIndex < answerLines.length; lineIndex++) {
            String rawLine = answerLines[lineIndex];
            String line = normalizePaperLine(rawLine);
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String sectionType = detectQuestionType(line);
            if (sectionType != null) {
                currentType = sectionType;
                if (isAnswerHeading(line)) {
                    continue;
                }
            }
            if (isAnswerHeading(line)) {
                continue;
            }

            Matcher matcher = answerEntryMatcher(line);
            if (!matcher.find()) {
                continue;
            }
            String no = StringUtils.defaultIfBlank(StringUtils.defaultIfBlank(matcher.group(1), matcher.group(2)), matcher.group(3));
            String answer = cleanupAnswerText(matcher.group(4));
            if (StringUtils.isBlank(answer)) {
                answer = collectMultilineAnswer(answerLines, lineIndex + 1);
            }
            if (StringUtils.isBlank(answer)) {
                continue;
            }
            sequential.add(new AnswerEntry(currentType, no, answer));
            byGlobalNo.put(String.valueOf(sequential.size()), answer);
            if (StringUtils.isNotBlank(currentType)) {
                byTypeNo.put(currentType + "#" + no, answer);
            } else {
                byUntypedNo.putIfAbsent(no, answer);
            }
        }

        boolean allowUntypedFallback = byTypeNo.isEmpty();
        boolean preferSequentialUntyped = allowUntypedFallback
                && sequential.size() == metas.size()
                && hasDuplicateQuestionNumbers(metas);
        for (int i = 0; i < metas.size(); i++) {
            QuestionMeta meta = metas.get(i);
            String answer = null;
            if (sequential.size() == metas.size() && StringUtils.isNotBlank(meta.globalNo)) {
                answer = byGlobalNo.get(meta.globalNo);
            }
            if (StringUtils.isBlank(answer)) {
                answer = byTypeNo.get(meta.type + "#" + meta.no);
            }
            if (StringUtils.isBlank(answer) && preferSequentialUntyped) {
                answer = answerFromSequentialAt(sequential, i, meta, true);
            }
            if (StringUtils.isBlank(answer) && allowUntypedFallback) {
                answer = byUntypedNo.get(meta.no);
            }
            if (!isPlausibleAnswerForType(meta, answer)) {
                answer = null;
            }
            if (StringUtils.isBlank(answer)) {
                answer = findSequentialAnswerForMeta(meta, sequential, i, allowUntypedFallback);
            }
            meta.standardAnswer = normalizeStandardAnswerForMeta(meta, answer);
        }
    }

    private Matcher answerEntryMatcher(String line) {
        return Pattern.compile("^(?:第\\s*(\\d+)\\s*题|[（(]?(\\d+)[)）]?[.．、]|(\\d+)\\s*[:：])\\s*[:：]?\\s*(.+)$")
                .matcher(StringUtils.defaultString(line));
    }

    private boolean isAnswerEntryLine(String line) {
        return answerEntryMatcher(line).find();
    }

    private String collectMultilineAnswer(String[] lines, int startIndex) {
        if (lines == null || startIndex >= lines.length) {
            return null;
        }
        List<String> answerLines = new ArrayList<>();
        for (int i = startIndex; i < lines.length; i++) {
            String line = normalizePaperLine(lines[i]);
            if (StringUtils.isBlank(line)) {
                if (answerLines.isEmpty()) {
                    continue;
                }
                break;
            }
            if (isAnswerHeading(line) || detectQuestionType(line) != null || isAnswerEntryLine(line)) {
                break;
            }
            if (line.matches("^(解析|分析)\\s*[:：].*$")) {
                break;
            }
            String cleaned = cleanupAnswerText(line);
            if (StringUtils.isNotBlank(cleaned)) {
                answerLines.add(cleaned);
            }
        }
        return answerLines.isEmpty() ? null : String.join("\n", answerLines).trim();
    }

    private boolean hasDuplicateQuestionNumbers(List<QuestionMeta> metas) {
        Set<String> seen = new HashSet<>();
        for (QuestionMeta meta : metas) {
            if (meta == null || StringUtils.isBlank(meta.no)) {
                continue;
            }
            if (!seen.add(meta.no)) {
                return true;
            }
        }
        return false;
    }

    private String answerFromSequentialAt(List<AnswerEntry> sequential, int index, QuestionMeta meta, boolean allowUntypedEntries) {
        if (sequential == null || index < 0 || index >= sequential.size()) {
            return null;
        }
        AnswerEntry entry = sequential.get(index);
        if (answerEntryTypeMatchesMeta(entry, meta, allowUntypedEntries) && isPlausibleAnswerForType(meta, entry.answer)) {
            return entry.answer;
        }
        return null;
    }

    private String detectQuestionType(String line) {
        String text = normalizePaperLine(line);
        if (!looksLikeQuestionTypeHeading(text)) {
            return null;
        }
        if (text.matches(".*(多项选择题|多选题|不定项).*")) return "checkbox";
        if (text.matches(".*(单选题|单项选择题|选择题).*")) return "radio";
        if (text.matches(".*判断题.*")) return "judge";
        if (text.matches(".*填空题.*")) return "fill";
        if (text.matches(".*(简答题|问答题|论述题|计算题|编程题|代码题|综合题).*")) return "text";
        return null;
    }

    private boolean looksLikeQuestionTypeHeading(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        String typeHeadingPattern = "(?:单项选择题|单选题|选择题|多项选择题|多选题|不定项选择题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题)\\s*(?:[（(][^）)]*[）)])?\\s*(?:参考)?(?:答案)?(?:与解析|解析)?\\s*(?:[（(][^）)]*[）)])?";
        if (text.matches("^" + typeHeadingPattern + "$")) {
            return true;
        }
        Matcher matcher = Pattern.compile("^(?:[一二三四五六七八九十]+|\\d+)[、.．]\\s*(.+)$").matcher(text);
        return matcher.find() && matcher.group(1).trim().matches("^" + typeHeadingPattern + "$");
    }

    private String normalizeQuizMarkdownLayout(String raw) {
        String text = StringUtils.defaultString(raw)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        String titles = "单项选择题|单选题|选择题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题";
        return text
                .replaceAll("([^\\n])(```[a-zA-Z0-9_-]*)", "$1\n$2")
                .replaceAll("(```)([ \\t]*(?:[A-H][.．、:：)）]))", "$1\n$2")
                .replaceAll("([^\\n])\\s*(#{1,6}\\s*(?:" + titles + "|答案解析区|参考答案与解析|参考答案|标准答案|答案解析))", "$1\n\n$2")
                .replaceAll("([^\\n])\\s*((?:" + titles + ")\\s*(?:参考)?答案(?:与解析|解析)?)", "$1\n\n$2")
                .replaceAll("([^\\n])\\s*(---)(?=\\s*(?:\\n|#{1,6}\\s*|答案解析区|参考答案|$))", "$1\n\n---")
                .replaceAll("([\\u3002\\uff01\\uff1f\\uff1b;])\\s*(\\d+[.．、]\\s+)", "$1\n\n$2")
                .replaceAll("([^\\n])\\s*([A-H][.．、:：)）]\\s+)", "$1\n$2")
                .replaceAll("\\n{4,}", "\n\n\n")
                .trim();
    }

    private String normalizePaperLine(String line) {
        return StringUtils.defaultString(line)
                .replaceAll("^#{1,6}\\s*", "")
                .replaceAll("^\\s*>\\s*", "")
                .replaceAll("^\\s*[-*+]\\s+", "")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .trim();
    }

    private boolean isOptionLine(String line) {
        return normalizePaperLine(line).matches("^[A-H][.．、:：)）\\s].+");
    }

    private QuestionOption parseQuestionOption(String line) {
        Matcher matcher = Pattern.compile("^([A-H])[.．、:：)）\\s]+(.+)$").matcher(normalizePaperLine(line));
        if (!matcher.find()) {
            return null;
        }
        return new QuestionOption(matcher.group(1).trim(), matcher.group(2).trim());
    }

    private Integer extractFullScore(String text) {
        Matcher matcher = Pattern.compile("[（(]\\s*(\\d+)\\s*分\\s*[）)]").matcher(StringUtils.defaultString(text));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String stripScoreText(String text) {
        return StringUtils.defaultString(text).replaceAll("[（(]\\s*\\d+\\s*分\\s*[）)]", "").trim();
    }

    private String cleanupAnswerText(String text) {
        return StringUtils.defaultString(text)
                .replaceAll("^\\s*(?:[【\\[]\\s*)?(答案|参考答案|标准答案)(?:\\s*[】\\]])?\\s*[:：]?\\s*", "")
                .replaceAll("\\s*(解析|分析)\\s*[:：].*$", "")
                .trim();
    }

    private String findSequentialAnswerForMeta(QuestionMeta meta, List<AnswerEntry> sequential, int preferredIndex, boolean allowUntypedEntries) {
        if (meta == null || sequential == null || sequential.isEmpty()) {
            return null;
        }
        if (preferredIndex >= 0 && preferredIndex < sequential.size()) {
            AnswerEntry preferred = sequential.get(preferredIndex);
            if (answerEntryTypeMatchesMeta(preferred, meta, allowUntypedEntries) && isPlausibleAnswerForType(meta, preferred.answer)) {
                return preferred.answer;
            }
        }
        for (AnswerEntry entry : sequential) {
            if (answerEntryMatchesMeta(entry, meta, allowUntypedEntries)) {
                return entry.answer;
            }
        }
        return null;
    }

    private boolean answerEntryTypeMatchesMeta(AnswerEntry entry, QuestionMeta meta, boolean allowUntypedEntries) {
        return entry != null
                && meta != null
                && ((allowUntypedEntries && StringUtils.isBlank(entry.type)) || StringUtils.equals(entry.type, meta.type));
    }

    private boolean answerEntryMatchesMeta(AnswerEntry entry, QuestionMeta meta, boolean allowUntypedEntries) {
        if (entry == null || meta == null) {
            return false;
        }
        if (!answerEntryTypeMatchesMeta(entry, meta, allowUntypedEntries)) {
            return false;
        }
        if (StringUtils.isNotBlank(entry.type)
                && StringUtils.isNotBlank(entry.no)
                && StringUtils.isNotBlank(meta.no)
                && !StringUtils.equals(entry.no, meta.no)) {
            return false;
        }
        return isPlausibleAnswerForType(meta, entry.answer);
    }

    private boolean isPlausibleAnswerForType(QuestionMeta meta, String answer) {
        if (meta == null || StringUtils.isBlank(answer)) {
            return false;
        }
        String type = StringUtils.defaultString(meta.type);
        String normalized = normalizeAnswerText(answer);
        if ("radio".equals(type)) {
            return normalized.matches("^[A-H]$") || optionLabelForAnswerText(meta, answer) != null;
        }
        if ("checkbox".equals(type)) {
            String compact = normalized.replaceAll("[,，、\\s]+", "");
            return compact.matches("^[A-H]+$") || optionLabelForAnswerText(meta, answer) != null;
        }
        if ("judge".equals(type)) {
            return normalized.matches("^(正确|错误|对|错|TRUE|FALSE|T|F|√|×)$");
        }
        return true;
    }

    private String normalizeStandardAnswerForMeta(QuestionMeta meta, String answer) {
        if (meta == null || StringUtils.isBlank(answer)) {
            return answer;
        }
        String type = StringUtils.defaultString(meta.type);
        if ("radio".equals(type) || "checkbox".equals(type)) {
            String label = optionLabelForAnswerText(meta, answer);
            if (StringUtils.isNotBlank(label)) {
                return label;
            }
            return normalizeAnswerText(answer).replaceAll("[,，、\\s]+", "");
        }
        if ("judge".equals(type)) {
            String normalized = normalizeAnswerText(answer);
            if ("对".equals(normalized) || "TRUE".equals(normalized) || "T".equals(normalized) || "√".equals(normalized)) {
                return "正确";
            }
            if ("错".equals(normalized) || "FALSE".equals(normalized) || "F".equals(normalized) || "×".equals(normalized)) {
                return "错误";
            }
        }
        return answer;
    }

    private String optionLabelForAnswerText(QuestionMeta meta, String answer) {
        if (meta == null || meta.options.isEmpty() || StringUtils.isBlank(answer)) {
            return null;
        }
        String normalized = normalizeMetaText(cleanupAnswerText(answer));
        for (QuestionOption option : meta.options) {
            if (StringUtils.equalsIgnoreCase(option.label, normalized)
                    || StringUtils.equalsIgnoreCase(normalizeMetaText(option.text), normalized)) {
                return option.label;
            }
        }
        return null;
    }

    private String normalizeAnswerText(String answer) {
        return StringUtils.defaultString(cleanupAnswerText(answer))
                .replaceAll("^选项\\s*", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String writeJsonQuietly(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractStandardAnswerFromComment(String comment) {
        if (StringUtils.isBlank(comment)) {
            return null;
        }
        Matcher matcher = Pattern.compile("标准答案\\s*[:：]\\s*([^\\n\\-，,。；;]+)").matcher(comment);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String cleanDisplayAiComment(String comment, String studentAnswer) {
        if (StringUtils.isBlank(comment)) {
            return null;
        }
        String cleaned = comment.replaceAll("<!--STATS:\\{.*?\\}-->", "").replace("**", "").trim();
        if (hasTextAnswer(studentAnswer) && cleaned.contains("未作答")) {
            return null;
        }
        return StringUtils.left(cleaned, 500);
    }

    private String normalizeMetaText(String text) {
        return StringUtils.defaultString(text).replaceAll("\\s+", "").trim();
    }

    private static class PaperParts {
        private final String paper;
        private final String answers;

        private PaperParts(String paper, String answers) {
            this.paper = paper;
            this.answers = answers;
        }
    }

    private static class QuestionMeta {
        private String no;
        private String globalNo;
        private String type;
        private String stem;
        private String standardAnswer;
        private Integer fullScore;
        private final List<QuestionOption> options = new ArrayList<>();

        private String displayNo() {
            return StringUtils.defaultIfBlank(globalNo, no);
        }
    }

    private static class QuestionOption {
        public final String label;
        public final String text;

        private QuestionOption(String label, String text) {
            this.label = label;
            this.text = text;
        }
    }

    private static class AnswerEntry {
        private final String type;
        private final String no;
        private final String answer;

        private AnswerEntry(String type, String no, String answer) {
            this.type = type;
            this.no = no;
            this.answer = answer;
        }
    }

    // ====================== 考试相关 ======================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitExam(HomeworkSubmitRequest req, User loginUser) {
        if (req.getAssignmentId() == null || StringUtils.isBlank(req.getStudentAnswerJson())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "考试ID和答案不能为空");
        }

        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可提交考试");
        }

        HomeworkAssignment assignment = assignmentService.getById(req.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "考试不存在");
        }
        if (!"exam".equals(assignment.getAssignmentType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该发布类型不是考试");
        }
        if (!"published".equals(assignment.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该考试当前不可提交");
        }

        if (assignment.getClassId() != null && !assignment.getClassId().equals(loginUser.getClassId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该考试不属于你所在的班级");
        }

        if (!isActiveAssignmentQuizResource(assignment)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题库资源不存在或已删除");
        }

        if (assignment.getDeadline() != null && new Date().after(assignment.getDeadline())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该考试已过截止时间，无法提交");
        }

        // 检查是否已有提交
        LambdaQueryWrapper<HomeworkSubmission> cw = new LambdaQueryWrapper<>();
        cw.eq(HomeworkSubmission::getAssignmentId, req.getAssignmentId())
                .eq(HomeworkSubmission::getStudentId, loginUser.getId())
                .eq(HomeworkSubmission::getIsDelete, 0);
        List<HomeworkSubmission> existingSubs = this.list(cw);

        if (!existingSubs.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "你已提交过该考试，不可重复提交");
        }

        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setAssignmentId(req.getAssignmentId());
        submission.setTeacherId(assignment.getTeacherId());
        submission.setStudentId(loginUser.getId());
        submission.setClassId(loginUser.getClassId());
        submission.setCourseId(assignment.getCourseId());
        submission.setAttemptNo(1);
        submission.setSubmitStatus("submitted");
        submission.setSubmissionType(StringUtils.defaultIfBlank(req.getSubmissionType(), "online"));
        String fullAnswerJson = prepareExamAnswerJson(req.getStudentAnswerJson(), assignment);
        submission.setStudentAnswerJson(fullAnswerJson);
        submission.setSubmitTime(new Date());
        this.save(submission);

        // 写入每题空白 detail（不含AI批改）
        writeDetailsSafe(submission.getId(), fullAnswerJson, null);

        if (hasImagePayload(req)) {
            saveSubmissionImages(submission.getId(), req);
        }

        return submission.getId();
    }

    @Override
    public void teacherAutoGradeExam(Long submissionId, User loginUser) {
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可操作");
        }

        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null || (submission.getIsDelete() != null && submission.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }

        if ("completed".equals(submission.getSubmitStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该答卷已批阅完成");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "考试不存在");
        }
        if (!Objects.equals(assignment.getTeacherId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权批阅该考试");
        }

        autoGradeExam(submissionId);
    }

    @Override
    public List<HomeworkHistoryVO> getExamHistory(User loginUser) {
        LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
        sw.eq(HomeworkSubmission::getStudentId, loginUser.getId())
                .eq(HomeworkSubmission::getIsDelete, 0)
                .orderByDesc(HomeworkSubmission::getCreateTime);
        List<HomeworkSubmission> submissions = this.list(sw);

        if (submissions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> aIds = submissions.stream()
                .map(HomeworkSubmission::getAssignmentId)
                .collect(Collectors.toSet());
        Map<Long, HomeworkAssignment> aMap = assignmentService.listByIds(aIds)
                .stream()
                .collect(Collectors.toMap(HomeworkAssignment::getId, a -> a));
        Map<Long, AiResource> quizResourceMap = listQuizResourceMap(aMap.values());

        return submissions.stream()
                .filter(s -> {
                    HomeworkAssignment a = aMap.get(s.getAssignmentId());
                    return isVisibleStudentExamHistoryAssignment(a)
                            && isActiveAssignmentQuizResource(a, quizResourceMap);
                })
                .map(s -> {
                    HomeworkHistoryVO vo = new HomeworkHistoryVO();
                    vo.setSubmissionId(s.getId());
                    vo.setAssignmentId(s.getAssignmentId());
                    HomeworkAssignment a = aMap.get(s.getAssignmentId());
                    vo.setTitle(a != null ? a.getTitle() : "未知考试");
                    vo.setCourseName(null);
                    vo.setSubmitTime(s.getSubmitTime());
                    vo.setTotalScore(s.getTotalScore());
                    vo.setSubmitStatus(s.getSubmitStatus());
                    vo.setReportSummary(s.getTeacherRemark());
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void teacherGradeExam(ExamGradeRequest req, User loginUser) {
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可批阅考试");
        }

        HomeworkSubmission submission = this.getById(req.getSubmissionId());
        if (submission == null || (submission.getIsDelete() != null && submission.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "考试不存在");
        }
        if (!Objects.equals(assignment.getTeacherId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权批阅该考试");
        }
        if ("completed".equals(submission.getSubmitStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该答卷已批阅完成，不能重复批阅");
        }

        LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(HomeworkSubmissionDetail::getSubmissionId, submission.getId())
                .orderByAsc(HomeworkSubmissionDetail::getId);
        List<HomeworkSubmissionDetail> normalizedDetails = detailMapper.selectList(dw);
        enrichReportDetails(normalizedDetails, assignment);
        Map<Long, HomeworkSubmissionDetail> normalizedDetailMap = normalizedDetails.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(HomeworkSubmissionDetail::getId, d -> d, (a, b) -> a));

        // 更新逐题得分
        int totalScore = 0;
        if (req.getDetails() != null) {
            for (ExamGradeRequest.QuestionScore qs : req.getDetails()) {
                if (qs.getId() == null) continue;
                HomeworkSubmissionDetail detail = detailMapper.selectById(qs.getId());
                if (detail != null) {
                    HomeworkSubmissionDetail normalized = normalizedDetailMap.get(detail.getId());
                    Integer fullScore = normalized != null ? normalized.getFullScore() : detail.getFullScore();
                    if (fullScore != null && fullScore > 0) {
                        detail.setFullScore(fullScore);
                    }
                    int score = qs.getScore() == null ? 0 : Math.max(qs.getScore(), 0);
                    if (detail.getFullScore() != null && detail.getFullScore() > 0) {
                        score = Math.min(score, detail.getFullScore());
                    }
                    detail.setScore(score);
                    detail.setIsCorrect(score > 0 ? 1 : 0);
                    detailMapper.updateById(detail);
                    totalScore += score;
                }
            }
        }

        submission.setTotalScore(Math.min(totalScore, 100));
        submission.setTeacherRemark(req.getTeacherRemark());
        submission.setSubmitStatus("completed");
        submission.setJudgeTime(new Date());
        this.updateById(submission);
    }

    @Override
    public String generateExamReviewComment(ExamGradeRequest req, User loginUser) {
        if (req == null || req.getSubmissionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "submissionId cannot be empty");
        }
        HomeworkSubmission submission = requireTeacherOwnedSubmission(req.getSubmissionId(), loginUser);
        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());

        LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(HomeworkSubmissionDetail::getSubmissionId, submission.getId())
                .orderByAsc(HomeworkSubmissionDetail::getId);
        List<HomeworkSubmissionDetail> details = detailMapper.selectList(dw);
        enrichReportDetails(details, assignment);

        Map<Long, Integer> currentScores = new HashMap<>();
        if (req.getDetails() != null) {
            for (ExamGradeRequest.QuestionScore qs : req.getDetails()) {
                if (qs != null && qs.getId() != null) {
                    currentScores.put(qs.getId(), qs.getScore() == null ? 0 : Math.max(qs.getScore(), 0));
                }
            }
        }

        String comment = deepSeekService.chat(
                "你是一位负责、具体、克制的教师助教。请根据考试批阅结果生成中文总评，只输出评语正文。",
                buildExamReviewCommentPrompt(assignment, details, currentScores),
                1200
        );
        comment = StringUtils.defaultIfBlank(comment, buildDefaultExamReviewComment(details, currentScores)).trim();
        return sanitizeExamReviewComment(comment, details, currentScores);
    }

    private String buildExamReviewCommentPrompt(HomeworkAssignment assignment,
                                                List<HomeworkSubmissionDetail> details,
                                                Map<Long, Integer> currentScores) {
        StringBuilder sb = new StringBuilder();
        sb.append("考试标题：").append(assignment == null ? "" : StringUtils.defaultString(assignment.getTitle())).append("\n");
        sb.append("请生成一段适合学生查看的考试总评，要求：\n");
        sb.append("1. 当前逐题得分就是批阅完成后的最终给分，必须视为已批阅数据。\n");
        sb.append("2. 基于逐题得分、题型、学生答案、参考答案和批阅意见来写。\n");
        sb.append("3. 先概括掌握情况，再指出主要薄弱点和建议订正方向。\n");
        sb.append("4. 可以点名题号或知识点，但不要罗列流水账。\n");
        sb.append("5. 不要提到AI，不要编造未出现的学习行为。\n");
        sb.append("6. 严禁出现“待批阅、等待批阅、请等待批阅、补交文字答案、未提交图片题”等表述。\n");
        sb.append("7. 对图片作答题，必须按当前得分描述为“已按图片作答内容评分”；若得分为0，只说明该题作答未达到要求，需要订正解题过程。\n");
        sb.append("8. 80到160字。\n\n");
        sb.append("逐题批阅数据：\n");
        int total = 0;
        int full = 0;
        for (HomeworkSubmissionDetail detail : details) {
            int score = currentScores.getOrDefault(detail.getId(), detail.getScore() == null ? 0 : detail.getScore());
            int fullScore = detail.getFullScore() == null ? 0 : detail.getFullScore();
            total += score;
            full += fullScore;
            sb.append("第").append(StringUtils.defaultString(detail.getQuestionNo(), "--")).append("题");
            sb.append("，题型 ").append(getQuestionTypeLabel(StringUtils.defaultString(detail.getQuestionType(), "")));
            sb.append("，得分 ").append(score).append("/").append(fullScore);
            sb.append("，题干：").append(StringUtils.left(StringUtils.defaultString(detail.getStemSnapshot()), 120));
            boolean hasImageAnswer = hasImageUrls(detail.getImageUrlsJson());
            if (hasImageAnswer) {
                sb.append("，学生答案：学生已上传图片作答，已按图片作答内容评分");
                if (score <= 0 && fullScore > 0) {
                    sb.append("，本题得分为0表示图片作答内容未达到要求，不代表未提交");
                }
            } else {
                sb.append("，学生答案：").append(StringUtils.left(StringUtils.defaultString(detail.getStudentAnswer()), 120));
            }
            sb.append("，参考答案：").append(StringUtils.left(StringUtils.defaultString(detail.getStandardAnswer()), 120));
            if (StringUtils.isNotBlank(detail.getAiComment())) {
                sb.append("，批阅意见：").append(StringUtils.left(detail.getAiComment(), 120));
            }
            sb.append("\n");
        }
        sb.append("\n总分：").append(total).append("/").append(full).append("\n");
        return sb.toString();
    }

    private String sanitizeExamReviewComment(String comment,
                                             List<HomeworkSubmissionDetail> details,
                                             Map<Long, Integer> currentScores) {
        String text = StringUtils.defaultString(comment).trim();
        if (StringUtils.isBlank(text)) {
            return buildDefaultExamReviewComment(details, currentScores);
        }
        String forbiddenPattern = ".*(待批阅|等待批阅|请等待|补交文字|未提交图片|未上传图片|还没有判|尚未批阅).*";
        if (text.matches(forbiddenPattern)) {
            return buildDefaultExamReviewComment(details, currentScores);
        }
        return text;
    }

    private String buildDefaultExamReviewComment(List<HomeworkSubmissionDetail> details,
                                                 Map<Long, Integer> currentScores) {
        int total = 0;
        int full = 0;
        List<String> weakQuestions = new ArrayList<>();
        List<String> doneQuestions = new ArrayList<>();
        for (HomeworkSubmissionDetail detail : details) {
            int fullScore = detail.getFullScore() == null ? 0 : detail.getFullScore();
            int score = currentScores.getOrDefault(detail.getId(), detail.getScore() == null ? 0 : detail.getScore());
            if (fullScore > 0) {
                score = Math.min(score, fullScore);
            }
            total += score;
            full += fullScore;
            String no = StringUtils.defaultString(detail.getQuestionNo(), "--");
            if (fullScore > 0 && score >= fullScore) {
                doneQuestions.add(no);
            } else {
                weakQuestions.add(no);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("本次考试已完成批阅，");
        if (!doneQuestions.isEmpty()) {
            sb.append("第").append(String.join("、", doneQuestions)).append("题掌握较好。");
        }
        if (!weakQuestions.isEmpty()) {
            sb.append("主要问题集中在第").append(String.join("、", weakQuestions)).append("题，");
            sb.append("请对照参考答案订正相关概念、判断条件和解题过程。");
        } else {
            sb.append("整体掌握扎实，建议继续保持并复盘关键解题思路。");
        }
        if (full > 0) {
            sb.append("当前得分").append(total).append("/").append(full).append("。");
        }
        return sb.toString();
    }

    // ====================== 考试 AI 自动批阅 ======================

    private String prepareExamAnswerJson(String studentAnswerJson, HomeworkAssignment assignment) {
        List<Map<String, Object>> answers = prepareAnswerItems(studentAnswerJson, Collections.emptyMap());
        enrichAnswerItemsWithQuestionMeta(answers, assignment);
        return toJson(answers);
    }

    /**
     * AI 自动批阅考试，调用 DeepSeek 按参考答案逐题评分
     */
    private void autoGradeExam(Long submissionId) {
        HomeworkSubmission submission = this.getById(submissionId);
        if (submission == null || !"submitted".equals(submission.getSubmitStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交记录不存在或状态不正确，无法批阅");
        }

        HomeworkAssignment assignment = assignmentService.getById(submission.getAssignmentId());
        if (assignment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "考试不存在，无法批阅");
        }

        LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(HomeworkSubmissionDetail::getSubmissionId, submissionId)
                .orderByAsc(HomeworkSubmissionDetail::getId);
        List<HomeworkSubmissionDetail> allDetails = detailMapper.selectList(dw);
        enrichReportDetails(allDetails, assignment);
        persistNormalizedFullScores(allDetails);
        List<Map<String, Object>> gradableAnswers = buildExamGradableAnswerItems(allDetails);
        if (gradableAnswers.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可由AI批阅的文字答案，图片作答题请教师手动批阅");
        }

        String prompt = buildExamGradingPrompt(
                assignment.getContentSnapshot(),
                toJson(gradableAnswers),
                100
        );

        String aiResponse = autoGradingService.gradeExamPaper(
                buildExamGradingSystemPrompt(),
                prompt
        );

        if (StringUtils.isBlank(aiResponse)) {
            log.warn("考试AI批阅返回为空, submissionId={}", submissionId);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI批阅无返回结果，请重试");
        }

        applyExamAiResponse(submission, aiResponse, allDetails);
    }

    private void persistNormalizedFullScores(List<HomeworkSubmissionDetail> details) {
        if (details == null) {
            return;
        }
        for (HomeworkSubmissionDetail detail : details) {
            if (detail != null && detail.getId() != null && detail.getFullScore() != null && detail.getFullScore() > 0) {
                HomeworkSubmissionDetail update = new HomeworkSubmissionDetail();
                update.setId(detail.getId());
                update.setFullScore(detail.getFullScore());
                detailMapper.updateById(update);
            }
        }
    }

    private List<Map<String, Object>> buildExamGradableAnswerItems(List<HomeworkSubmissionDetail> details) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (details == null) {
            return result;
        }
        for (HomeworkSubmissionDetail detail : details) {
            if (detail == null || hasImageUrls(detail.getImageUrlsJson())) {
                continue;
            }
            String answer = StringUtils.trimToEmpty(detail.getStudentAnswer());
            if (StringUtils.isBlank(answer)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("num", detail.getQuestionNo());
            item.put("type", detail.getQuestionType());
            item.put("stem", detail.getStemSnapshot());
            item.put("answer", answer);
            item.put("standardAnswer", StringUtils.defaultString(detail.getStandardAnswer()));
            item.put("fullScore", detail.getFullScore());
            result.add(item);
        }
        return result;
    }

    private boolean isObjectiveQuestionType(String type) {
        String normalized = StringUtils.defaultString(type);
        return "radio".equals(normalized)
                || "checkbox".equals(normalized)
                || "judge".equals(normalized)
                || "fill".equals(normalized);
    }

    private Boolean answerMatchesStandard(String type, String studentAnswer, String standardAnswer) {
        if (StringUtils.isBlank(studentAnswer) || StringUtils.isBlank(standardAnswer)) {
            return null;
        }
        String normalizedType = StringUtils.defaultString(type);
        if ("judge".equals(normalizedType) || normalizedType.contains("判断")
                || isJudgeAnswerText(studentAnswer) || isJudgeAnswerText(standardAnswer)) {
            String student = normalizeJudgeAnswer(studentAnswer);
            String standard = normalizeJudgeAnswer(standardAnswer);
            if (StringUtils.isBlank(student) || StringUtils.isBlank(standard)) {
                return null;
            }
            return StringUtils.equals(student, standard);
        }
        if ("radio".equals(normalizedType) || "checkbox".equals(normalizedType)) {
            String student = normalizeChoiceAnswer(studentAnswer);
            String standard = normalizeChoiceAnswer(standardAnswer);
            if (StringUtils.isBlank(student) || StringUtils.isBlank(standard)) {
                return null;
            }
            return StringUtils.equals(student, standard);
        }
        if ("fill".equals(normalizedType)) {
            return StringUtils.equals(normalizeFillAnswer(studentAnswer), normalizeFillAnswer(standardAnswer));
        }
        String student = normalizeComparableAnswer(studentAnswer);
        String standard = normalizeComparableAnswer(standardAnswer);
        if (StringUtils.isBlank(student) || StringUtils.isBlank(standard)) {
            return null;
        }
        return StringUtils.equals(student, standard);
    }

    private String normalizeJudgeAnswer(String answer) {
        String text = normalizeAnswerText(answer);
        if ("正确".equals(text) || "对".equals(text) || "TRUE".equals(text) || "T".equals(text) || "√".equals(text)) {
            return "正确";
        }
        if ("错误".equals(text) || "错".equals(text) || "FALSE".equals(text) || "F".equals(text) || "×".equals(text) || "X".equals(text)) {
            return "错误";
        }
        return text;
    }

    private boolean isJudgeAnswerText(String answer) {
        String text = normalizeJudgeAnswer(answer);
        return "正确".equals(text) || "错误".equals(text);
    }

    private String normalizeChoiceAnswer(String answer) {
        return normalizeComparableAnswer(answer);
    }

    private String normalizeFillAnswer(String answer) {
        return normalizeComparableAnswer(answer);
    }

    private String normalizeComparableAnswer(String answer) {
        return normalizeAnswerText(answer)
                .replaceAll("<[^>]*>", "")
                .replaceAll("[,，、;；。\\.：:\\s]+", "");
    }

    private String extractAnswerFromAiComment(String comment, String label) {
        if (StringUtils.isBlank(comment) || StringUtils.isBlank(label)) {
            return null;
        }
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*为\\s*([^，,。；;\\s]+)")
                .matcher(comment);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String buildExamGradingSystemPrompt() {
        return "你是一位极其严格的考试阅卷AI。请根据试卷与标准答案批改学生作答，严格按照参考答案评分。\n"
                + "只输出一个JSON对象，不要包含任何其他文字、代码块标记或解释。";
    }

    private String getQuestionTypeLabel(String type) {
        switch (type) {
            case "radio": return "单选题";
            case "checkbox": return "多选题";
            case "judge": return "判断题";
            case "fill": return "填空题";
            case "text": return "简答题";
            default: return type;
        }
    }

    private String buildExamGradingPrompt(String paperContent, String studentAnswerJson, int totalScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位极其严格的考试阅卷AI。请根据试卷与标准答案批改学生作答。\n\n");

        sb.append("【评分规则】\n");
        sb.append("1. 单选题：只能有一个正确答案。学生答案与参考答案完全一致得满分，否则0分。\n");
        sb.append("2. 多选题：全对得满分；漏选酌情给一半分；有错选0分。\n");
        sb.append("3. 判断题：\"正确\"与\"√\"等价，\"错误\"与\"×\"等价。答案一致得满分，否则0分。\n");
        sb.append("4. 填空题：完全匹配得满分，否则0分。\n");
        sb.append("5. 简答题：按要点给分。\n");
        sb.append("6. 题号必须与学生的题号（num）完全一致。\n\n");

        List<Map<String, Object>> answers;
        try {
            answers = objectMapper.readValue(studentAnswerJson,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            answers = Collections.emptyList();
        }

        sb.append("【学生作答详情】\n");
        if (answers.isEmpty()) {
            sb.append("（学生作答JSON解析失败，将使用原始JSON）\n").append(studentAnswerJson).append("\n\n");
        } else {
            for (Map<String, Object> ans : answers) {
                String num = String.valueOf(ans.getOrDefault("num", ""));
                String type = String.valueOf(ans.getOrDefault("type", ""));
                String stem = String.valueOf(ans.getOrDefault("stem", ""));
                String answer = String.valueOf(ans.getOrDefault("answer", ""));
                String standardAnswer = String.valueOf(ans.getOrDefault("standardAnswer", ""));
                String fullScore = String.valueOf(ans.getOrDefault("fullScore", ""));
                String typeLabel = getQuestionTypeLabel(type);
                sb.append("参考答案：\"").append(standardAnswer).append("\"\n");
                sb.append("本题满分：").append(fullScore).append("\n");
                sb.append("第").append(num).append("题（").append(typeLabel).append("）\n");
                sb.append("题干：").append(stem).append("\n");
                sb.append("学生答案：\"").append(answer).append("\"\n\n");
            }
        }

        sb.append("【试卷与标准答案】\n");
        sb.append("\"\"\"\n").append(paperContent).append("\n\"\"\"\n\n");

        int avgScore = (totalScore > 0 && !answers.isEmpty()) ? totalScore / answers.size() : 10;
        sb.append("【说明】试卷共约 ").append(answers.size()).append(" 题，总分 ").append(totalScore).append(" 分。");
        sb.append("若试卷未明确标注每题分值，可按每题约 ").append(avgScore).append(" 分计算，或根据题目难度合理分配。\n\n");

        sb.append("请逐题评分，返回以下JSON格式（不要加任何markdown代码块标记）：\n");
        sb.append("{\n");
        sb.append("  \"totalScore\": 总分,\n");
        sb.append("  \"correctCount\": 正确题数,\n");
        sb.append("  \"wrongCount\": 错误题数,\n");
        sb.append("  \"details\": [\n");
        sb.append("    {\n");
        sb.append("      \"questionNo\": \"题号（必须与学生的num一致）\",\n");
        sb.append("      \"score\": 该题得分,\n");
        sb.append("      \"fullScore\": 该题满分,\n");
        sb.append("      \"isCorrect\": 1或0,\n");
        sb.append("      \"comment\": \"评分说明\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private void applyExamAiResponse(HomeworkSubmission submission, String aiResponse, List<HomeworkSubmissionDetail> allDetails) {
        String jsonStr = aiResponse.trim()
                .replaceAll("(?s)^```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```$", "")
                .trim();

        JsonNode root;
        try {
            root = objectMapper.readTree(jsonStr);
        } catch (Exception e) {
            log.error("考试AI批阅JSON解析失败, submissionId={}, response={}", submission.getId(), aiResponse, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI批阅返回数据格式异常，请重试");
        }

        int totalScore = root.path("totalScore").asInt(0);
        int correctCount = root.path("correctCount").asInt(0);
        int wrongCount = root.path("wrongCount").asInt(0);

        submission.setTotalScore(totalScore);
        submission.setCorrectCount(correctCount);
        submission.setWrongCount(wrongCount);
        submission.setSubmitStatus("submitted");
        submission.setJudgeTime(new Date());
        this.updateById(submission);

        // 按插入顺序加载所有detail（id升序），用于顺序fallback匹配
        if (allDetails == null) {
            LambdaQueryWrapper<HomeworkSubmissionDetail> orderDw = new LambdaQueryWrapper<>();
            orderDw.eq(HomeworkSubmissionDetail::getSubmissionId, submission.getId())
                    .orderByAsc(HomeworkSubmissionDetail::getId);
            allDetails = detailMapper.selectList(orderDw);
        }

        JsonNode details = root.path("details");
        if (details.isArray()) {
            int detailIdx = 0;
            for (JsonNode d : details) {
                String questionNo = d.path("questionNo").asText();
                int score = d.path("score").asInt(0);
                int fullScore = d.path("fullScore").asInt(0);
                int isCorrect = d.path("isCorrect").asInt(0);
                String comment = d.path("comment").asText("");

                HomeworkSubmissionDetail matched = null;

                // 1. 先尝试按questionNo精确匹配（且未被更新过）
                if (!questionNo.isEmpty()) {
                    for (HomeworkSubmissionDetail detail : allDetails) {
                        if (questionNo.equals(detail.getQuestionNo()) && !hasImageUrls(detail.getImageUrlsJson())) {
                            matched = detail;
                            break;
                        }
                    }
                }

                // 2. 匹配不到则按顺序fallback
                while (matched == null && detailIdx < allDetails.size()) {
                    HomeworkSubmissionDetail candidate = allDetails.get(detailIdx);
                    detailIdx++;
                    if (!hasImageUrls(candidate.getImageUrlsJson())
                            && StringUtils.isNotBlank(candidate.getStudentAnswer())) {
                        matched = candidate;
                    }
                }

                if (matched != null) {
                    int resolvedFullScore = fullScore > 0
                            ? fullScore
                            : (matched.getFullScore() != null ? matched.getFullScore() : 0);
                    int resolvedScore = score;
                    String studentForMatch = StringUtils.defaultIfBlank(
                            matched.getStudentAnswer(),
                            extractAnswerFromAiComment(comment, "学生答案")
                    );
                    String standardForMatch = StringUtils.defaultIfBlank(
                            matched.getStandardAnswer(),
                            extractAnswerFromAiComment(comment, "参考答案")
                    );
                    Boolean standardMatch = answerMatchesStandard(
                            matched.getQuestionType(),
                            studentForMatch,
                            standardForMatch
                    );
                    if (Boolean.TRUE.equals(standardMatch) && resolvedFullScore > 0) {
                        resolvedScore = resolvedFullScore;
                        isCorrect = 1;
                    } else if (Boolean.FALSE.equals(standardMatch) && isObjectiveQuestionType(matched.getQuestionType())) {
                        resolvedScore = 0;
                        isCorrect = 0;
                    } else if (isCorrect == 1 && resolvedScore <= 0 && resolvedFullScore > 0) {
                        resolvedScore = resolvedFullScore;
                    }
                    int resolvedIsCorrect = isCorrect == 1 || resolvedScore > 0 ? 1 : 0;

                    matched.setScore(resolvedScore);
                    if (resolvedFullScore > 0) {
                        matched.setFullScore(resolvedFullScore);
                    }
                    matched.setIsCorrect(resolvedIsCorrect);
                    matched.setAiComment(comment);
                    detailMapper.updateById(matched);
                }
            }
        }

        // AI 未覆盖到的题目设为 0 分
        LambdaQueryWrapper<HomeworkSubmissionDetail> fallbackDw = new LambdaQueryWrapper<>();
        fallbackDw.eq(HomeworkSubmissionDetail::getSubmissionId, submission.getId())
                .isNull(HomeworkSubmissionDetail::getScore);
        List<HomeworkSubmissionDetail> ungraded = detailMapper.selectList(fallbackDw);
        for (HomeworkSubmissionDetail d : ungraded) {
            if (hasImageUrls(d.getImageUrlsJson())) {
                d.setAiComment("图片作答，待教师手动批阅");
                detailMapper.updateById(d);
                continue;
            }
            if (StringUtils.isBlank(d.getStudentAnswer())) {
                d.setScore(0);
                d.setIsCorrect(0);
                d.setAiComment("未作答，系统记为 0 分");
            }
            detailMapper.updateById(d);
        }

        // 重新计算总分（补充AI遗漏）
        LambdaQueryWrapper<HomeworkSubmissionDetail> recalcDw = new LambdaQueryWrapper<>();
        recalcDw.eq(HomeworkSubmissionDetail::getSubmissionId, submission.getId());
        List<HomeworkSubmissionDetail> all = detailMapper.selectList(recalcDw);
        int recalcTotal = 0;
        int recalcCorrect = 0;
        int recalcWrong = 0;
        boolean hasPendingManual = false;
        for (HomeworkSubmissionDetail d : all) {
            if (d.getScore() != null) recalcTotal += d.getScore();
            if (d.getScore() == null) {
                hasPendingManual = true;
                continue;
            }
            if (d.getIsCorrect() != null && d.getIsCorrect() == 1) recalcCorrect++;
            else recalcWrong++;
        }
        submission.setTotalScore(recalcTotal);
        submission.setCorrectCount(recalcCorrect);
        submission.setWrongCount(recalcWrong);
        submission.setSubmitStatus(hasPendingManual ? "submitted" : "completed");
        this.updateById(submission);
    }

    private void extractScoreFallback(String aiResponse, HomeworkSubmission submission) {
        Pattern scorePattern = Pattern.compile("综合评分[：:]\\s*(\\d{1,3})\\s*分");
        Matcher sm = scorePattern.matcher(aiResponse);
        if (sm.find()) {
            submission.setTotalScore(Integer.parseInt(sm.group(1)));
        }
    }

}
