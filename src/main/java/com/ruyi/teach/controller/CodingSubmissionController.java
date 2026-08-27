package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.common.TraceContext;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.CodingRunRequest;
import com.ruyi.teach.model.entity.*;
import com.ruyi.teach.model.vo.CodingRunResultVO;
import com.ruyi.teach.model.vo.CodingSubmissionVO;
import com.ruyi.teach.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/coding/submission")
@Tag(name = "编程题提交")
@Slf4j
public class CodingSubmissionController {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("java", "python", "javascript", "cpp");
    private static final Pattern SCORE_PATTERN = Pattern.compile("<SCORE>(\\d{1,3})</SCORE>");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{1,3})");

    private static final long SUBMIT_COOLDOWN_MS = 30_000;
    private static final ConcurrentHashMap<String, Long> lastSubmitTime = new ConcurrentHashMap<>();

    @Resource
    private CodeExecutor codeExecutor;

    @Resource
    private CodingProblemService problemService;

    @Resource
    private CodingTestCaseService testCaseService;

    @Resource
    private CodingSubmissionService submissionService;

    @Resource
    private CodingProblemPublishService publishService;

    @Resource
    private DeepSeekService deepSeekService;

    @Operation(summary = "运行代码（不判分，仅跑样例用例）")
    @PostMapping("/run")
    public BaseResponse<CodingRunResultVO> runCode(@RequestBody CodingRunRequest req,
                                                    HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        validateRunRequest(req);

        CodingProblem problem = problemService.getById(req.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "编程题不存在");
        }

        LambdaQueryWrapper<CodingTestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodingTestCase::getProblemId, req.getProblemId())
                .eq(CodingTestCase::getIsSample, 1)
                .eq(CodingTestCase::getIsDelete, 0)
                .orderByAsc(CodingTestCase::getSortOrder);
        List<CodingTestCase> sampleCases = testCaseService.list(wrapper);

        List<CodingRunResultVO.TestCaseResult> caseResults = new ArrayList<>();

        if (sampleCases.isEmpty()) {
            CodingRunResultVO result = codeExecutor.execute(req.getLanguage(), req.getCode(), null,
                    (long) problem.getTimeLimitMs(), (long) problem.getMemoryLimitKb());
            return ResultUtils.success(result);
        }

        for (CodingTestCase tc : sampleCases) {
            CodingRunResultVO result = codeExecutor.execute(req.getLanguage(), req.getCode(), tc.getInput(),
                    (long) problem.getTimeLimitMs(), (long) problem.getMemoryLimitKb());

            CodingRunResultVO.TestCaseResult tr = new CodingRunResultVO.TestCaseResult();
            tr.setTestCaseId(tc.getId().intValue());
            tr.setPassed("accepted".equals(result.getStatus()) && isOutputMatch(result.getStdout(), tc.getExpectedOutput()));
            tr.setStatus(result.getStatus());
            tr.setStatusDescription(result.getStatusDescription());
            tr.setActualOutput(result.getStdout());
            tr.setExpectedOutput(tc.getExpectedOutput());
            tr.setStderr(result.getStderr());
            tr.setCompileOutput(result.getCompileOutput());
            tr.setIsSample(true);
            caseResults.add(tr);
        }

        String status = resolveRunStatus(caseResults);
        CodingRunResultVO vo = CodingRunResultVO.builder()
                .language(req.getLanguage())
                .status(status)
                .accepted("accepted".equals(status))
                .statusDescription(resolveRunStatusDescription(caseResults))
                .testCaseResults(caseResults)
                .build();

        return ResultUtils.success(vo);
    }

    @Operation(summary = "提交代码（判分 + AI评审）")
    @PostMapping("/submit")
    public BaseResponse<CodingRunResultVO> submitCode(@RequestBody CodingRunRequest req,
                                                       HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        validateRunRequest(req);

        CodingProblem problem = problemService.getById(req.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "编程题不存在");
        }
        CodingProblemPublish publish = requireActivePublish(req.getProblemId(), loginUser);
        checkSubmitCooldown(loginUser.getId(), req.getProblemId());

        CodingSubmission submission = createSubmission(req, loginUser, publish);

        LambdaQueryWrapper<CodingTestCase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodingTestCase::getProblemId, req.getProblemId())
                .eq(CodingTestCase::getIsDelete, 0)
                .orderByAsc(CodingTestCase::getSortOrder);
        List<CodingTestCase> allCases = testCaseService.list(wrapper);

        JudgeResult judgeResult = runAllTestCases(req, problem, allCases);

        int testScore = calculateTestScore(judgeResult);

        int aiScore = 0;
        String aiReviewMd = null;
        try {
            String systemPrompt = "你是一位编程教师，正在评审学生的代码提交。请根据代码质量、可读性、算法效率给出评分(0-100)和简短评语。\n" +
                    "重要：输出的第一行必须是用 <SCORE>数字</SCORE> 标签包裹的分数，例如 <SCORE>85</SCORE>，之后是Markdown格式的评审报告。\n" +
                    "注意：如果代码无法通过测试用例，评分必须大幅降低。全部测试未通过的代码最高不超过20分。";
            String userContent = "题目: " + problem.getTitle() + "\n题目描述: " + problem.getDescription()
                    + "\n学生使用的语言: " + req.getLanguage()
                    + "\n测试通过率: " + judgeResult.passedCount + "/" + judgeResult.totalCount
                    + "\n学生代码:\n```\n" + req.getCode() + "\n```";
            String aiResponse = deepSeekService.chat(systemPrompt, userContent, 1500);
            if (aiResponse == null || aiResponse.isEmpty()) {
                aiScore = 0;
                aiReviewMd = "> **AI 评审服务暂时不可用**，请稍后重试。你的代码已保存，测试用例评分已生效。";
            } else {
                aiScore = parseAiScore(aiResponse, testScore);
                aiReviewMd = extractAiReview(aiResponse);
            }
        } catch (Exception e) {
            aiScore = 0;
            aiReviewMd = "> **AI 评审服务暂时不可用**，请稍后重试。你的代码已保存，测试用例评分已生效。";
        }

        aiScore = Math.min(aiScore, testScore);
        int finalScore = testScore == 0 ? 0 : (int) Math.round(testScore * 0.7 + aiScore * 0.3);

        updateSubmission(submission, judgeResult, testScore, aiScore, finalScore, aiReviewMd);

        CodingRunResultVO vo = buildSubmitResult(req.getLanguage(), judgeResult, testScore, aiScore, finalScore, aiReviewMd, submission.getId());

        recordSubmitTime(loginUser.getId(), req.getProblemId());
        return ResultUtils.success(vo);
    }

    @Operation(summary = "提交代码（流式输出）")
    @PostMapping("/submit/stream")
    public SseEmitter submitCodeStream(@RequestBody CodingRunRequest req,
                                       HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        validateRunRequest(req);

        CodingProblem problem = problemService.getById(req.getProblemId());
        if (problem == null) {
            SseEmitter emitter = new SseEmitter(180_000L);
            try {
                emitter.send(SseEmitter.event().name("error").data("编程题不存在"));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        CodingProblemPublish publish;
        try {
            publish = requireActivePublish(req.getProblemId(), loginUser);
            checkSubmitCooldown(loginUser.getId(), req.getProblemId());
        } catch (BusinessException e) {
            SseEmitter emitter = new SseEmitter(180_000L);
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        CodingSubmission submission = createSubmission(req, loginUser, publish);

        SseEmitter emitter = new SseEmitter(180_000L);
        String traceId = TraceContext.currentTraceId();

        new Thread(() -> {
            TraceContext.bind(traceId);
            try {
                LambdaQueryWrapper<CodingTestCase> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CodingTestCase::getProblemId, req.getProblemId())
                        .eq(CodingTestCase::getIsDelete, 0)
                        .orderByAsc(CodingTestCase::getSortOrder);
                List<CodingTestCase> allCases = testCaseService.list(wrapper);

                int passedCount = 0;
                int totalScore = 0;
                int maxScore = 0;
                List<CodingRunResultVO.TestCaseResult> caseResults = new ArrayList<>();
                List<Map<String, Object>> judgeDetailList = new ArrayList<>();

                for (CodingTestCase tc : allCases) {
                    CodingRunResultVO result = codeExecutor.execute(req.getLanguage(), req.getCode(), tc.getInput(),
                            (long) problem.getTimeLimitMs(), (long) problem.getMemoryLimitKb());

                    boolean passed = "accepted".equals(result.getStatus()) && isOutputMatch(result.getStdout(), tc.getExpectedOutput());
                    int caseScore = passed ? tc.getScore() : 0;
                    if (passed) passedCount++;
                    totalScore += caseScore;
                    maxScore += tc.getScore();

                    CodingRunResultVO.TestCaseResult tr = new CodingRunResultVO.TestCaseResult();
                    tr.setTestCaseId(tc.getId().intValue());
                    tr.setPassed(passed);
                    tr.setStatus(result.getStatus());
                    tr.setStatusDescription(result.getStatusDescription());
                    tr.setActualOutput(tc.getIsSample() == 1 ? result.getStdout() : null);
                    tr.setExpectedOutput(tc.getIsSample() == 1 ? tc.getExpectedOutput() : null);
                    tr.setStderr(tc.getIsSample() == 1 ? result.getStderr() : null);
                    tr.setCompileOutput(tc.getIsSample() == 1 ? result.getCompileOutput() : null);
                    tr.setIsSample(tc.getIsSample() == 1);
                    caseResults.add(tr);

                    Map<String, Object> detail = new HashMap<>();
                    detail.put("testCaseId", tc.getId());
                    detail.put("passed", passed);
                    detail.put("isSample", tc.getIsSample());
                    if (!"accepted".equals(result.getStatus())) {
                        detail.put("error", result.getStatusDescription() != null ? result.getStatusDescription() : result.getStderr());
                    }
                    judgeDetailList.add(detail);

                    Map<String, Object> caseEvent = new HashMap<>();
                    caseEvent.put("index", caseResults.size() - 1);
                    caseEvent.put("passed", passed);
                    caseEvent.put("isSample", tc.getIsSample() == 1);
                    if (tc.getIsSample() == 1) {
                        caseEvent.put("status", result.getStatus());
                        caseEvent.put("statusDescription", result.getStatusDescription());
                        caseEvent.put("actualOutput", result.getStdout());
                        caseEvent.put("expectedOutput", tc.getExpectedOutput());
                        caseEvent.put("stderr", result.getStderr());
                        caseEvent.put("compileOutput", result.getCompileOutput());
                    }
                    emitter.send(SseEmitter.event().name("test_case_result").data(objectMapper.writeValueAsString(caseEvent)));
                }

                int totalCount = allCases.size();
                int testScore = totalCount == 0 ? 0 : (maxScore > 0 ? (int) Math.round(totalScore * 100.0 / maxScore) : (passedCount == totalCount ? 100 : 0));

                // 流式 AI 评审
                final int[] aiScoreRef = {0};
                final String[] aiReviewRef = {null};
                StringBuilder aiFullText = new StringBuilder();

                try {
                    String systemPrompt = "你是一位编程教师，正在评审学生的代码提交。请根据代码质量、可读性、算法效率给出评分(0-100)和简短评语。\n" +
                            "重要格式要求（必须严格遵守）：\n" +
                            "1. 输出的第一行必须是用 <SCORE>数字</SCORE> 标签包裹的分数，例如 <SCORE>85</SCORE>\n" +
                            "2. 第一行之后，必须空一行再开始评审报告\n" +
                            "3. 所有标题必须使用 Markdown 格式，即 '### 标题'（注意 # 和文字之间必须有空格，标题前必须有空行）\n" +
                            "4. 所有代码块必须用 ``` 包裹，并标注语言，例如 ```java\n代码\n```\n" +
                            "5. 列表项使用 '- ' 开头，每条列表项独占一行\n" +
                            "6. 不同段落之间必须有空行分隔\n" +
                            "7. 粗体使用 '**文字**'，注意 * 和文字之间不要有空格\n" +
                            "注意：如果代码无法通过测试用例，评分必须大幅降低。全部测试未通过的代码最高不超过20分。";
                    String userContent = "题目: " + problem.getTitle() + "\n题目描述: " + problem.getDescription()
                            + "\n学生使用的语言: " + req.getLanguage()
                            + "\n测试通过率: " + passedCount + "/" + totalCount
                            + "\n学生代码:\n```\n" + req.getCode() + "\n```";

                    String fullResponse = deepSeekService.streamChat(systemPrompt, userContent, 1500, chunk -> {
                        try {
                            aiFullText.append(chunk);
                            emitter.send(SseEmitter.event().name("ai_review_chunk").data(chunk));
                        } catch (IOException ignored) {}
                    });

                    if (fullResponse == null || fullResponse.isEmpty()) {
                        aiScoreRef[0] = 0;
                        aiReviewRef[0] = "> **AI 评审服务暂时不可用**，请稍后重试。你的代码已保存，测试用例评分已生效。";
                    } else {
                        aiScoreRef[0] = parseAiScore(fullResponse, testScore);
                        aiReviewRef[0] = extractAiReview(fullResponse);
                    }
                } catch (Exception e) {
                    aiScoreRef[0] = 0;
                    aiReviewRef[0] = "> **AI 评审服务暂时不可用**，请稍后重试。你的代码已保存，测试用例评分已生效。";
                }

                int aiScore = aiScoreRef[0];
                String aiReviewMd = aiReviewRef[0];
                aiScore = Math.min(aiScore, testScore);
                int finalScore = testScore == 0 ? 0 : (int) Math.round(testScore * 0.7 + aiScore * 0.3);

                // 更新提交记录
                submission.setStatus("judged");
                submission.setPassedCount(passedCount);
                submission.setTotalCount(totalCount);
                submission.setTestScore(testScore);
                submission.setAiScore(aiScore);
                submission.setFinalScore(finalScore);
                submission.setAiReviewMd(aiReviewMd);
                try {
                    submission.setJudgeDetail(objectMapper.writeValueAsString(judgeDetailList));
                } catch (Exception e) {
                    submission.setJudgeDetail("[]");
                }
                submissionService.updateById(submission);

                // 发送完成事件
                Map<String, Object> completeEvent = new HashMap<>();
                completeEvent.put("submissionId", submission.getId());
                completeEvent.put("passedCount", passedCount);
                completeEvent.put("totalCount", totalCount);
                completeEvent.put("testScore", testScore);
                completeEvent.put("aiScore", aiScore);
                completeEvent.put("finalScore", finalScore);
                completeEvent.put("accepted", passedCount == totalCount);
                emitter.send(SseEmitter.event().name("submission_complete").data(objectMapper.writeValueAsString(completeEvent)));
                emitter.complete();

                recordSubmitTime(loginUser.getId(), req.getProblemId());

            } catch (Exception e) {
                log.error("Coding submission failed, trace_id={}, submissionId={}",
                        traceId, submission.getId(), e);
                try {
                    submission.setStatus("error");
                    submissionService.updateById(submission);
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("判题服务异常，请稍后重试（错误码：50200）"));
                    emitter.complete();
                } catch (IOException ignored) {}
            } finally {
                TraceContext.clear();
            }
        }).start();

        return emitter;
    }

    @Operation(summary = "学生查询提交历史")
    @GetMapping("/history")
    public BaseResponse<List<CodingSubmissionVO>> getSubmissionHistory(
            @RequestParam Long problemId,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);

        LambdaQueryWrapper<CodingSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodingSubmission::getProblemId, problemId)
                .eq(CodingSubmission::getStudentId, loginUser.getId())
                .eq(CodingSubmission::getIsDelete, 0)
                .orderByDesc(CodingSubmission::getCreateTime);
        List<CodingSubmission> submissions = submissionService.list(wrapper);

        List<CodingSubmissionVO> voList = submissions.stream().map(s -> {
            CodingSubmissionVO vo = new CodingSubmissionVO();
            vo.setId(s.getId());
            vo.setProblemId(s.getProblemId());
            vo.setLanguage(s.getLanguage());
            vo.setStatus(s.getStatus());
            vo.setPassedCount(s.getPassedCount());
            vo.setTotalCount(s.getTotalCount());
            vo.setTestScore(s.getTestScore());
            vo.setAiScore(s.getAiScore());
            vo.setFinalScore(s.getFinalScore());
            vo.setAiReviewMd(s.getAiReviewMd());
            vo.setRuntimeMs(s.getRuntimeMs());
            vo.setMemoryKb(s.getMemoryKb());
            vo.setCode(s.getCode());
            vo.setCreateTime(s.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        return ResultUtils.success(voList);
    }

    @Operation(summary = "查询提交详情（含代码）")
    @GetMapping("/detail")
    public BaseResponse<CodingSubmissionVO> getSubmissionDetail(@RequestParam Long submissionId,
                                                                  HttpServletRequest request) {
        User loginUser = getLoginUser(request);

        CodingSubmission s = submissionService.getById(submissionId);
        if (s == null || (s.getIsDelete() != null && s.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }

        CodingSubmissionVO vo = new CodingSubmissionVO();
        vo.setId(s.getId());
        vo.setProblemId(s.getProblemId());
        vo.setStudentId(s.getStudentId());
        vo.setLanguage(s.getLanguage());
        vo.setCode(s.getCode());
        vo.setStatus(s.getStatus());
        vo.setPassedCount(s.getPassedCount());
        vo.setTotalCount(s.getTotalCount());
        vo.setTestScore(s.getTestScore());
        vo.setAiScore(s.getAiScore());
        vo.setFinalScore(s.getFinalScore());
        vo.setAiReviewMd(s.getAiReviewMd());
        vo.setRuntimeMs(s.getRuntimeMs());
        vo.setMemoryKb(s.getMemoryKb());
        vo.setCreateTime(s.getCreateTime());
        return ResultUtils.success(vo);
    }

    @Operation(summary = "教师删除学生提交记录")
    @PostMapping("/teacher/delete/{submissionId}")
    public BaseResponse<Boolean> deleteSubmission(@PathVariable Long submissionId,
                                                   HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可删除");
        }

        CodingSubmission submission = submissionService.getById(submissionId);
        if (submission == null || (submission.getIsDelete() != null && submission.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }

        CodingProblem problem = problemService.getById(submission.getProblemId());
        if (problem == null || !problem.getCreatorId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能删除自己题目下的提交记录");
        }

        boolean removed = submissionService.removeById(submissionId);
        return ResultUtils.success(removed);
    }

    // ==================== private helpers ====================

    private void validateRunRequest(CodingRunRequest req) {
        if (req.getProblemId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不能为空");
        }
        if (req.getLanguage() == null || req.getLanguage().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "语言不能为空");
        }
        if (req.getCode() == null || req.getCode().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码不能为空");
        }
        if (!SUPPORTED_LANGUAGES.contains(req.getLanguage().toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的语言: " + req.getLanguage());
        }
    }

    private boolean isOutputMatch(String actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        String a = actual.replace("\r\n", "\n").trim();
        String e = expected.replace("\r\n", "\n").trim();
        // 去除末尾空行
        while (a.endsWith("\n")) a = a.substring(0, a.length() - 1);
        while (e.endsWith("\n")) e = e.substring(0, e.length() - 1);
        // 忽略每行末尾空白
        a = Arrays.stream(a.split("\n")).map(s -> s.replaceAll("\\s+$", "")).collect(Collectors.joining("\n"));
        e = Arrays.stream(e.split("\n")).map(s -> s.replaceAll("\\s+$", "")).collect(Collectors.joining("\n"));
        return a.equals(e);
    }

    private String resolveRunStatus(List<CodingRunResultVO.TestCaseResult> caseResults) {
        if (caseResults.stream().allMatch(CodingRunResultVO.TestCaseResult::getPassed)) {
            return "accepted";
        }
        return caseResults.stream()
                .map(CodingRunResultVO.TestCaseResult::getStatus)
                .filter(status -> status != null && !"accepted".equals(status))
                .findFirst()
                .orElse("wrong_answer");
    }

    private String resolveRunStatusDescription(List<CodingRunResultVO.TestCaseResult> caseResults) {
        return caseResults.stream()
                .filter(tr -> !Boolean.TRUE.equals(tr.getPassed()))
                .map(tr -> {
                    if (tr.getStatusDescription() != null && !tr.getStatusDescription().isBlank()) {
                        return tr.getStatusDescription();
                    }
                    return tr.getStatus();
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("All sample tests passed");
    }

    private int parseAiScore(String aiResponse, int fallbackTestScore) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return 0;
        }
        // 优先匹配 <SCORE>数字</SCORE> 标签
        Matcher tagMatcher = SCORE_PATTERN.matcher(aiResponse);
        if (tagMatcher.find()) {
            int score = Integer.parseInt(tagMatcher.group(1));
            return Math.max(0, Math.min(100, score));
        }
        // 回退：提取第一个 1-3 位数字
        Matcher numMatcher = NUMBER_PATTERN.matcher(aiResponse);
        if (numMatcher.find()) {
            int score = Integer.parseInt(numMatcher.group(1));
            return Math.max(0, Math.min(100, score));
        }
        // 无法解析时不再回退到测试分，避免分数虚高
        return 0;
    }

    private String extractAiReview(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) return null;
        // 去掉第一行的 <SCORE>...</SCORE>
        String[] lines = aiResponse.split("\n", 2);
        if (lines.length > 1 && lines[0].contains("<SCORE>")) {
            return lines[1].trim();
        }
        return aiResponse;
    }

    private void checkSubmitCooldown(Long studentId, Long problemId) {
        String key = studentId + ":" + problemId;
        Long lastTime = lastSubmitTime.get(key);
        if (lastTime != null && (System.currentTimeMillis() - lastTime) < SUBMIT_COOLDOWN_MS) {
            long remaining = (SUBMIT_COOLDOWN_MS - (System.currentTimeMillis() - lastTime)) / 1000;
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交过于频繁，请 " + remaining + " 秒后再试");
        }
    }

    private void recordSubmitTime(Long studentId, Long problemId) {
        lastSubmitTime.put(studentId + ":" + problemId, System.currentTimeMillis());
    }

    private CodingProblemPublish requireActivePublish(Long problemId, User loginUser) {
        if (!"student".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅学生可提交编程题");
        }
        if (loginUser.getClassId() == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前学生未绑定班级，无法提交该编程题");
        }

        LambdaQueryWrapper<CodingProblemPublish> pubWrapper = new LambdaQueryWrapper<>();
        pubWrapper.eq(CodingProblemPublish::getProblemId, problemId)
                .eq(CodingProblemPublish::getClassId, loginUser.getClassId())
                .eq(CodingProblemPublish::getIsDelete, 0)
                .last("limit 1");
        CodingProblemPublish publish = publishService.getOne(pubWrapper, false);
        if (publish == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该编程题未发布给你所在班级");
        }
        if (publish.getDeadline() != null && new Date().after(publish.getDeadline())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该编程题已截止，无法提交");
        }
        return publish;
    }

    private CodingSubmission createSubmission(CodingRunRequest req, User loginUser, CodingProblemPublish publish) {
        CodingSubmission submission = new CodingSubmission();
        submission.setProblemId(req.getProblemId());
        submission.setPublishId(publish.getId());
        submission.setStudentId(loginUser.getId());
        submission.setLanguage(req.getLanguage());
        submission.setCode(req.getCode());
        submission.setStatus("running");
        submission.setPassedCount(0);
        submission.setTotalCount(0);
        submission.setTestScore(0);
        submission.setAiScore(0);
        submission.setFinalScore(0);
        submissionService.save(submission);
        return submission;
    }

    private static class JudgeResult {
        int passedCount;
        int totalCount;
        int totalScore;
        int maxScore;
        List<CodingRunResultVO.TestCaseResult> caseResults = new ArrayList<>();
        List<Map<String, Object>> judgeDetailList = new ArrayList<>();
    }

    private JudgeResult runAllTestCases(CodingRunRequest req, CodingProblem problem, List<CodingTestCase> allCases) {
        JudgeResult result = new JudgeResult();
        result.totalCount = allCases.size();

        for (CodingTestCase tc : allCases) {
            CodingRunResultVO execResult = codeExecutor.execute(req.getLanguage(), req.getCode(), tc.getInput(),
                    (long) problem.getTimeLimitMs(), (long) problem.getMemoryLimitKb());

            boolean passed = "accepted".equals(execResult.getStatus()) && isOutputMatch(execResult.getStdout(), tc.getExpectedOutput());
            int caseScore = passed ? tc.getScore() : 0;
            if (passed) result.passedCount++;
            result.totalScore += caseScore;
            result.maxScore += tc.getScore();

            CodingRunResultVO.TestCaseResult tr = new CodingRunResultVO.TestCaseResult();
            tr.setTestCaseId(tc.getId().intValue());
            tr.setPassed(passed);
            tr.setStatus(execResult.getStatus());
            tr.setStatusDescription(execResult.getStatusDescription());
            tr.setActualOutput(tc.getIsSample() == 1 ? execResult.getStdout() : null);
            tr.setExpectedOutput(tc.getIsSample() == 1 ? tc.getExpectedOutput() : null);
            tr.setStderr(tc.getIsSample() == 1 ? execResult.getStderr() : null);
            tr.setCompileOutput(tc.getIsSample() == 1 ? execResult.getCompileOutput() : null);
            tr.setIsSample(tc.getIsSample() == 1);
            result.caseResults.add(tr);

            Map<String, Object> detail = new HashMap<>();
            detail.put("testCaseId", tc.getId());
            detail.put("passed", passed);
            detail.put("isSample", tc.getIsSample());
            if (!"accepted".equals(execResult.getStatus())) {
                detail.put("error", execResult.getStatusDescription() != null ? execResult.getStatusDescription() : execResult.getStderr());
            }
            result.judgeDetailList.add(detail);
        }
        return result;
    }

    private int calculateTestScore(JudgeResult jr) {
        if (jr.totalCount == 0) {
            return 0;
        }
        return jr.maxScore > 0 ? (int) Math.round(jr.totalScore * 100.0 / jr.maxScore) : (jr.passedCount == jr.totalCount ? 100 : 0);
    }

    private void updateSubmission(CodingSubmission submission, JudgeResult jr, int testScore, int aiScore, int finalScore, String aiReviewMd) {
        submission.setStatus("judged");
        submission.setPassedCount(jr.passedCount);
        submission.setTotalCount(jr.totalCount);
        submission.setTestScore(testScore);
        submission.setAiScore(aiScore);
        submission.setFinalScore(finalScore);
        submission.setAiReviewMd(aiReviewMd);
        try {
            submission.setJudgeDetail(objectMapper.writeValueAsString(jr.judgeDetailList));
        } catch (Exception e) {
            submission.setJudgeDetail("[]");
        }
        submissionService.updateById(submission);
    }

    private CodingRunResultVO buildSubmitResult(String language, JudgeResult jr, int testScore, int aiScore, int finalScore, String aiReviewMd, Long submissionId) {
        return CodingRunResultVO.builder()
                .language(language)
                .status(jr.passedCount == jr.totalCount ? "accepted" : "wrong_answer")
                .accepted(jr.passedCount == jr.totalCount)
                .submissionId(submissionId)
                .passedCount(jr.passedCount)
                .totalCount(jr.totalCount)
                .testScore(testScore)
                .aiScore(aiScore)
                .finalScore(finalScore)
                .aiReviewMd(aiReviewMd)
                .statusDescription(jr.passedCount == jr.totalCount ? "All tests passed" : jr.passedCount + "/" + jr.totalCount + " tests passed")
                .testCaseResults(jr.caseResults)
                .build();
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUser;
    }
}
