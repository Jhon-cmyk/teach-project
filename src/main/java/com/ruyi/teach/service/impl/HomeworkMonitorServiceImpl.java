package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.HomeworkSubmissionDetailMapper;
import com.ruyi.teach.mapper.UserMapper;
import com.ruyi.teach.model.dto.HomeworkTeacherMonitorReportRequest;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkMonitorReport;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.HomeworkTeacherMonitorItemVO;
import com.ruyi.teach.model.vo.HomeworkTeacherMonitorReportVO;
import com.ruyi.teach.model.vo.HomeworkTeacherMonitorStudentVO;
import com.ruyi.teach.service.DeepSeekService;
import com.ruyi.teach.service.HomeworkAssignmentService;
import com.ruyi.teach.service.HomeworkMonitorReportService;
import com.ruyi.teach.service.HomeworkMonitorService;
import com.ruyi.teach.service.HomeworkSubmissionService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeworkMonitorServiceImpl implements HomeworkMonitorService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ASSIGNMENT_TYPE_HOMEWORK = "homework";
    private static final String ASSIGNMENT_TYPE_EXAM = "exam";

    @Resource
    private HomeworkAssignmentService assignmentService;

    @Resource
    private HomeworkSubmissionService submissionService;

    @Resource
    private HomeworkSubmissionDetailMapper detailMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private HomeworkMonitorReportService homeworkMonitorReportService;

    @Override
    public List<HomeworkTeacherMonitorItemVO> listTeacherMonitor(User loginUser) {
        validateTeacher(loginUser);

        List<HomeworkAssignment> assignments = listTeacherAssignments(loginUser.getId());
        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<HomeworkSubmission>> submissionMap = groupSubmissionsByAssignment(assignments);
        Map<Long, Integer> classStudentTotalMap = buildClassStudentTotalMap(assignments);

        List<HomeworkTeacherMonitorItemVO> result = new ArrayList<>();
        for (HomeworkAssignment assignment : assignments) {
            List<HomeworkSubmission> submissions = submissionMap.getOrDefault(assignment.getId(), Collections.emptyList());
            HomeworkTeacherMonitorItemVO vo = buildMonitorItemVO(
                    assignment,
                    submissions,
                    classStudentTotalMap.getOrDefault(assignment.getClassId(), 0)
            );
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<HomeworkTeacherMonitorStudentVO> getTeacherMonitorDetail(Long assignmentId, User loginUser) {
        validateTeacher(loginUser);

        HomeworkAssignment assignment = requireOwnedHomeworkAssignment(assignmentId, loginUser.getId());

        if (assignment.getClassId() == null) {
            return Collections.emptyList();
        }

        List<User> students = Optional.ofNullable(userMapper.selectStudentsByClassId(assignment.getClassId()))
                .orElse(Collections.emptyList());

        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
        sw.eq(HomeworkSubmission::getAssignmentId, assignmentId)
                .eq(HomeworkSubmission::getIsDelete, 0)
                .orderByDesc(HomeworkSubmission::getCreateTime);

        List<HomeworkSubmission> submissions = submissionService.list(sw);
        Map<Long, Integer> pendingReviewCountMap = buildPendingReviewCountMap(submissions);

        Map<Long, HomeworkSubmission> bestSubmissionByStudent = new HashMap<>();
        for (HomeworkSubmission submission : submissions) {
            Long studentId = submission.getStudentId();
            HomeworkSubmission current = bestSubmissionByStudent.get(studentId);
            if (ASSIGNMENT_TYPE_HOMEWORK.equals(assignment.getAssignmentType())) {
                bestSubmissionByStudent.put(studentId, pickLater(current, submission));
            } else {
                bestSubmissionByStudent.put(studentId, pickDisplaySubmission(current, submission));
            }
        }

        List<HomeworkTeacherMonitorStudentVO> result = new ArrayList<>();
        for (User student : students) {
            HomeworkSubmission submission = bestSubmissionByStudent.get(student.getId());

            HomeworkTeacherMonitorStudentVO vo = new HomeworkTeacherMonitorStudentVO();
            vo.setStudentId(student.getId());
            vo.setStudentName(resolveStudentName(student));

            if (submission == null) {
                vo.setSubmitStatus("pending");
            } else {
                vo.setSubmitStatus(StringUtils.defaultIfBlank(submission.getSubmitStatus(), "pending"));
                vo.setSubmitTime(submission.getSubmitTime());
                vo.setTotalScore(submission.getTotalScore());
                vo.setAiSuggestedTotalScore(submission.getAiSuggestedTotalScore());
                vo.setPendingReviewQuestionCount(pendingReviewCountMap.getOrDefault(submission.getId(), 0));
                vo.setReviewStatus(submission.getReviewStatus());
                vo.setCorrectCount(submission.getCorrectCount());
                vo.setWrongCount(submission.getWrongCount());
                vo.setSubmissionId(submission.getId());
            }

            result.add(vo);
        }

        result.sort(
                Comparator.comparingInt((HomeworkTeacherMonitorStudentVO v) -> statusOrder(v.getSubmitStatus()))
                        .thenComparing(v -> v.getTotalScore() == null ? -1 : v.getTotalScore())
                        .thenComparing(v -> v.getStudentId() == null ? 0L : v.getStudentId())
        );

        return result;
    }

    private Map<Long, Integer> buildPendingReviewCountMap(List<HomeworkSubmission> submissions) {
        List<Long> submissionIds = submissions.stream()
                .filter(s -> "review_pending".equals(s.getSubmitStatus()))
                .map(HomeworkSubmission::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (submissionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
        dw.in(HomeworkSubmissionDetail::getSubmissionId, submissionIds);
        List<HomeworkSubmissionDetail> details = detailMapper.selectList(dw);
        Map<Long, Integer> result = new HashMap<>();
        for (HomeworkSubmissionDetail detail : details) {
            if (detail.getSubmissionId() == null) {
                continue;
            }
            boolean needsReview = detail.getScore() == null
                    && StringUtils.isNotBlank(detail.getImageUrlsJson());
            if (needsReview) {
                result.merge(detail.getSubmissionId(), 1, Integer::sum);
            }
        }
        return result;
    }

    @Override
    public HomeworkTeacherMonitorReportVO generateTeacherMonitorReport(HomeworkTeacherMonitorReportRequest request,
                                                                       User loginUser) {
        validateTeacher(loginUser);

        HomeworkTeacherMonitorReportRequest safeRequest =
                request == null ? new HomeworkTeacherMonitorReportRequest() : request;

        List<HomeworkAssignment> assignments = listTeacherAssignments(loginUser.getId());
        assignments = applyReportFilter(assignments, safeRequest);

        List<HomeworkTeacherMonitorItemVO> itemList = new ArrayList<>();
        List<HomeworkSubmission> latestCompletedSubmissions = new ArrayList<>();
        Map<String, TypeStat> typeStatMap = new LinkedHashMap<>();

        if (!assignments.isEmpty()) {
            Map<Long, List<HomeworkSubmission>> submissionMap = groupSubmissionsByAssignment(assignments);
            Map<Long, Integer> classStudentTotalMap = buildClassStudentTotalMap(assignments);

            for (HomeworkAssignment assignment : assignments) {
                List<HomeworkSubmission> submissionList = submissionMap.getOrDefault(assignment.getId(), Collections.emptyList());
                itemList.add(buildMonitorItemVO(
                        assignment,
                        submissionList,
                        classStudentTotalMap.getOrDefault(assignment.getClassId(), 0)
                ));

                Map<Long, HomeworkSubmission> latestCompletedMap =
                        pickLatestCompletedByStudent(submissionList, assignment.getAssignmentType());
                latestCompletedSubmissions.addAll(latestCompletedMap.values());
            }

            List<Long> submissionIds = latestCompletedSubmissions.stream()
                    .map(HomeworkSubmission::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (!submissionIds.isEmpty()) {
                LambdaQueryWrapper<HomeworkSubmissionDetail> dw = new LambdaQueryWrapper<>();
                dw.in(HomeworkSubmissionDetail::getSubmissionId, submissionIds)
                        .orderByAsc(HomeworkSubmissionDetail::getCreateTime);
                List<HomeworkSubmissionDetail> detailList = detailMapper.selectList(dw);
                typeStatMap = aggregateTypeStats(detailList);
            }
        }

        ReportSnapshot snapshot = buildReportSnapshot(itemList, latestCompletedSubmissions);

        // 解析本次报告的练习题标题快照（筛选了具体练习题才有值）
        String quizTitleSnapshot = resolveQuizTitleSnapshot(itemList, safeRequest.getQuizResourceId());

        String reportMarkdown;
        if (assignments.isEmpty()) {
            reportMarkdown = buildNoDataReport(safeRequest, quizTitleSnapshot);
        } else if (latestCompletedSubmissions.isEmpty()) {
            reportMarkdown = buildNoCompletedReport(itemList, safeRequest, quizTitleSnapshot);
        } else {
            String prompt = buildAiPrompt(itemList, latestCompletedSubmissions, typeStatMap, safeRequest, quizTitleSnapshot);
            String aiReport = deepSeekService.chat(buildAiSystemPrompt(safeRequest), prompt, 2600);
            reportMarkdown = StringUtils.isNotBlank(aiReport)
                    ? aiReport.trim()
                    : buildLocalReport(itemList, latestCompletedSubmissions, typeStatMap, safeRequest, quizTitleSnapshot);
        }

        HomeworkMonitorReport report = new HomeworkMonitorReport();
        report.setTeacherId(loginUser.getId());
        report.setClassId(safeRequest.getClassId());
        report.setPublishDate(StringUtils.isBlank(safeRequest.getPublishDate()) ? null : safeRequest.getPublishDate());
        report.setQuizResourceId(safeRequest.getQuizResourceId());
        report.setQuizTitleSnapshot(quizTitleSnapshot);
        report.setReportTitle(buildReportTitle(safeRequest, quizTitleSnapshot));
        report.setReportMarkdown(reportMarkdown);
        report.setAssignmentIdsJson(toJson(itemList.stream()
                .map(HomeworkTeacherMonitorItemVO::getAssignmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())));
        report.setSummaryJson(toJson(snapshot.toMap()));
        homeworkMonitorReportService.save(report);

        HomeworkMonitorReport saved = homeworkMonitorReportService.getById(report.getId());
        return toReportVO(saved, true);
    }

    @Override
    public List<HomeworkTeacherMonitorReportVO> listTeacherMonitorReportHistory(User loginUser) {
        validateTeacher(loginUser);

        LambdaQueryWrapper<HomeworkMonitorReport> qw = new LambdaQueryWrapper<>();
        qw.eq(HomeworkMonitorReport::getTeacherId, loginUser.getId())
                .eq(HomeworkMonitorReport::getIsDelete, 0)
                .orderByDesc(HomeworkMonitorReport::getCreateTime)
                .last("limit 20");

        List<HomeworkMonitorReport> reports = homeworkMonitorReportService.list(qw);
        return reports.stream()
                .map(report -> toReportVO(report, false))
                .collect(Collectors.toList());
    }

    @Override
    public HomeworkTeacherMonitorReportVO getTeacherMonitorReportDetail(Long reportId, User loginUser) {
        validateTeacher(loginUser);

        if (reportId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "reportId 不能为空");
        }

        HomeworkMonitorReport report = homeworkMonitorReportService.getById(reportId);
        if (report == null || (report.getIsDelete() != null && report.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "报告不存在");
        }
        if (!Objects.equals(report.getTeacherId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该报告");
        }

        return toReportVO(report, true);
    }

    private void validateTeacher(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可查看教师端学情分析");
        }
    }

    private HomeworkAssignment requireOwnedHomeworkAssignment(Long assignmentId, Long teacherId) {
        if (assignmentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "assignmentId 不能为空");
        }

        HomeworkAssignment assignment = assignmentService.getById(assignmentId);
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }
        if (!Objects.equals(teacherId, assignment.getTeacherId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该作业");
        }
        String type = StringUtils.defaultIfBlank(assignment.getAssignmentType(), ASSIGNMENT_TYPE_HOMEWORK);
        if (!ASSIGNMENT_TYPE_HOMEWORK.equals(type) && !ASSIGNMENT_TYPE_EXAM.equals(type)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "章节练习不进入教师端学情分析");
        }
        return assignment;
    }

    private List<HomeworkAssignment> listTeacherAssignments(Long teacherId) {
        LambdaQueryWrapper<HomeworkAssignment> aw = new LambdaQueryWrapper<>();
        aw.eq(HomeworkAssignment::getTeacherId, teacherId)
                .in(HomeworkAssignment::getAssignmentType, ASSIGNMENT_TYPE_HOMEWORK, "exam")
                .eq(HomeworkAssignment::getIsDelete, 0)
                .ne(HomeworkAssignment::getStatus, "draft")
                .orderByDesc(HomeworkAssignment::getCreateTime);

        return assignmentService.list(aw);
    }

    private Map<Long, List<HomeworkSubmission>> groupSubmissionsByAssignment(List<HomeworkAssignment> assignments) {
        List<Long> assignmentIds = assignments.stream()
                .map(HomeworkAssignment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (assignmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<HomeworkSubmission> sw = new LambdaQueryWrapper<>();
        sw.in(HomeworkSubmission::getAssignmentId, assignmentIds)
                .eq(HomeworkSubmission::getIsDelete, 0)
                .orderByDesc(HomeworkSubmission::getCreateTime);

        List<HomeworkSubmission> submissions = submissionService.list(sw);
        return submissions.stream().collect(Collectors.groupingBy(HomeworkSubmission::getAssignmentId));
    }

    private Map<Long, Integer> buildClassStudentTotalMap(List<HomeworkAssignment> assignments) {
        Map<Long, Integer> result = new HashMap<>();
        Set<Long> classIds = assignments.stream()
                .map(HomeworkAssignment::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long classId : classIds) {
            List<User> students = Optional.ofNullable(userMapper.selectStudentsByClassId(classId))
                    .orElse(Collections.emptyList());
            result.put(classId, students.size());
        }
        return result;
    }

    private HomeworkTeacherMonitorItemVO buildMonitorItemVO(HomeworkAssignment assignment,
                                                            List<HomeworkSubmission> submissions,
                                                            Integer studentTotal) {
        Map<Long, HomeworkSubmission> latestSubmissionMap = pickLatestByStudent(submissions);
        List<HomeworkSubmission> latestCompletedList = latestSubmissionMap.values().stream()
                .filter(s -> countsAsCompleted(s, assignment.getAssignmentType()))
                .collect(Collectors.toList());

        int total = studentTotal == null ? 0 : studentTotal;
        int completedCount = latestCompletedList.size();
        int reviewPendingCount = (int) latestSubmissionMap.values().stream()
                .filter(s -> "review_pending".equals(s.getSubmitStatus()))
                .count();
        int pendingCount = Math.max(total - completedCount - reviewPendingCount, 0);
        double completionRate = total == 0 ? 0D : round(completedCount * 100D / total);

        double avgScore = 0D;
        if (!latestCompletedList.isEmpty()) {
            avgScore = round(
                    latestCompletedList.stream()
                            .filter(s -> s.getTotalScore() != null)
                            .mapToInt(HomeworkSubmission::getTotalScore)
                            .average()
                            .orElse(0D)
            );
        }

        int lowScoreCount = (int) latestCompletedList.stream()
                .filter(s -> s.getTotalScore() != null && s.getTotalScore() < 60)
                .count();

        HomeworkTeacherMonitorItemVO vo = new HomeworkTeacherMonitorItemVO();
        vo.setAssignmentId(assignment.getId());
        vo.setTitle(assignment.getTitle());
        vo.setQuizResourceId(assignment.getQuizResourceId());
        vo.setQuizTitle(StringUtils.defaultIfBlank(
                assignment.getQuizTitleSnapshot(),
                assignment.getTitle()));
        vo.setClassId(assignment.getClassId());
        vo.setCourseId(assignment.getCourseId());
        vo.setPublishTime(assignment.getCreateTime());
        vo.setDeadline(assignment.getDeadline());
        vo.setQuestionCount(assignment.getQuestionCount());
        vo.setTotalScore(assignment.getTotalScore());
        vo.setStudentTotal(total);
        vo.setCompletedCount(completedCount);
        vo.setReviewPendingCount(reviewPendingCount);
        vo.setPendingCount(pendingCount);
        vo.setCompletionRate(completionRate);
        vo.setAvgScore(avgScore);
        vo.setLowScoreCount(lowScoreCount);
        vo.setStatus(assignment.getStatus());
        vo.setAssignmentType(assignment.getAssignmentType());
        vo.setAnswerMode(assignment.getAnswerMode());
        vo.setImageGranularity(assignment.getImageGranularity());
        vo.setGradingMode(assignment.getGradingMode());
        vo.setDurationMinutes(assignment.getDurationMinutes());
        return vo;
    }

    private Map<Long, HomeworkSubmission> pickLatestByStudent(List<HomeworkSubmission> submissions) {
        Map<Long, HomeworkSubmission> result = new HashMap<>();
        for (HomeworkSubmission submission : submissions) {
            if (submission.getStudentId() == null) {
                continue;
            }
            HomeworkSubmission current = result.get(submission.getStudentId());
            result.put(submission.getStudentId(), pickLater(current, submission));
        }
        return result;
    }

    private boolean countsAsCompleted(HomeworkSubmission submission, String assignmentType) {
        if (submission == null) {
            return false;
        }
        String status = submission.getSubmitStatus();
        if (ASSIGNMENT_TYPE_EXAM.equals(assignmentType)) {
            return "submitted".equals(status) || "completed".equals(status);
        }
        // Legacy auto-graded homework used "completed". The current teacher-review
        // flow saves a final grade as "submitted + approved", so both states must
        // be treated as completed by monitor cards and generated reports.
        return "completed".equals(status)
                || ("submitted".equals(status)
                && "approved".equals(submission.getReviewStatus())
                && submission.getTotalScore() != null);
    }

    private Map<Long, HomeworkSubmission> pickLatestCompletedByStudent(List<HomeworkSubmission> submissions,
                                                                        String assignmentType) {
        Map<Long, HomeworkSubmission> result = new HashMap<>();
        for (HomeworkSubmission submission : submissions) {
            if (submission.getStudentId() == null) {
                continue;
            }
            if (!countsAsCompleted(submission, assignmentType)) {
                continue;
            }
            HomeworkSubmission current = result.get(submission.getStudentId());
            result.put(submission.getStudentId(), pickLater(current, submission));
        }
        return result;
    }

    private HomeworkSubmission pickDisplaySubmission(HomeworkSubmission current, HomeworkSubmission candidate) {
        if (current == null) {
            return candidate;
        }
        boolean currentCompleted = "completed".equals(current.getSubmitStatus());
        boolean candidateCompleted = "completed".equals(candidate.getSubmitStatus());

        if (currentCompleted && !candidateCompleted) {
            return current;
        }
        if (!currentCompleted && candidateCompleted) {
            return candidate;
        }
        return pickLater(current, candidate);
    }

    private HomeworkSubmission pickLater(HomeworkSubmission left, HomeworkSubmission right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }

        long leftTime = compareTime(left);
        long rightTime = compareTime(right);

        if (rightTime > leftTime) {
            return right;
        }
        if (rightTime < leftTime) {
            return left;
        }

        long leftId = left.getId() == null ? 0L : left.getId();
        long rightId = right.getId() == null ? 0L : right.getId();
        return rightId >= leftId ? right : left;
    }

    private long compareTime(HomeworkSubmission submission) {
        if (submission.getJudgeTime() != null) {
            return submission.getJudgeTime().getTime();
        }
        if (submission.getSubmitTime() != null) {
            return submission.getSubmitTime().getTime();
        }
        if (submission.getCreateTime() != null) {
            return submission.getCreateTime().getTime();
        }
        return 0L;
    }

    private List<HomeworkAssignment> applyReportFilter(List<HomeworkAssignment> assignments,
                                                       HomeworkTeacherMonitorReportRequest request) {
        return assignments.stream()
                .filter(a -> request.getClassId() == null || Objects.equals(a.getClassId(), request.getClassId()))
                .filter(a -> StringUtils.isBlank(request.getPublishDate()) || sameDay(a.getCreateTime(), request.getPublishDate()))
                .filter(a -> request.getQuizResourceId() == null || Objects.equals(a.getQuizResourceId(), request.getQuizResourceId()))
                .filter(a -> StringUtils.isBlank(request.getAssignmentType())
                        || request.getAssignmentType().equals(a.getAssignmentType()))
                .collect(Collectors.toList());
    }

    private boolean sameDay(Date date, String yyyyMMdd) {
        if (date == null || StringUtils.isBlank(yyyyMMdd)) {
            return false;
        }
        return yyyyMMdd.equals(new SimpleDateFormat("yyyy-MM-dd").format(date));
    }

    private String resolveStudentName(User user) {
        if (user == null) {
            return "未知学生";
        }

        List<String> getterNames = Arrays.asList(
                "getUserName",
                "getUsername",
                "getName",
                "getNickName",
                "getNickname",
                "getRealName",
                "getUserAccount"
        );

        for (String getterName : getterNames) {
            try {
                Method method = user.getClass().getMethod(getterName);
                Object value = method.invoke(user);
                if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                    return (String) value;
                }
            } catch (Exception ignore) {
            }
        }

        return "学生 " + user.getId();
    }

    private int statusOrder(String status) {
        if ("pending".equals(status)) {
            return 0;
        }
        if ("failed".equals(status)) {
            return 1;
        }
        if ("submitted".equals(status) || "judging".equals(status)) {
            return 2;
        }
        if ("review_pending".equals(status)) {
            return 3;
        }
        if ("completed".equals(status)) {
            return 4;
        }
        return 9;
    }

    private Map<String, TypeStat> aggregateTypeStats(List<HomeworkSubmissionDetail> detailList) {
        Map<String, TypeStat> result = new LinkedHashMap<>();
        for (HomeworkSubmissionDetail detail : detailList) {
            String type = normalizeQuestionType(detail.getQuestionType());
            TypeStat stat = result.computeIfAbsent(type, k -> new TypeStat());
            stat.totalCount++;

            Double accuracyPoint = null;

            if (detail.getIsCorrect() != null) {
                accuracyPoint = detail.getIsCorrect() == 1 ? 1D : 0D;
            } else if (detail.getFullScore() != null && detail.getFullScore() > 0 && detail.getScore() != null) {
                accuracyPoint = Math.max(0D, Math.min(1D, detail.getScore() * 1D / detail.getFullScore()));
            } else if (detail.getScore() != null) {
                accuracyPoint = detail.getScore() > 0 ? 1D : 0D;
            }

            if (accuracyPoint != null) {
                stat.evaluableCount++;
                stat.accuracySum += accuracyPoint;
                if (accuracyPoint >= 0.999D) {
                    stat.correctCount++;
                }
            }
        }
        return result;
    }

    private String normalizeQuestionType(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "未标注题型";
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);

        if (text.contains("single") || text.contains("choice") || text.contains("select") || text.contains("选择")) {
            return "选择题";
        }
        if (text.contains("multiple") || text.contains("多选")) {
            return "多选题";
        }
        if (text.contains("fill") || text.contains("blank") || text.contains("填空")) {
            return "填空题";
        }
        if (text.contains("judge") || text.contains("true") || text.contains("false") || text.contains("判断")) {
            return "判断题";
        }
        if (text.contains("short") || text.contains("essay") || text.contains("subjective")
                || text.contains("简答") || text.contains("问答")) {
            return "简答题";
        }
        if (text.contains("code") || text.contains("program") || text.contains("编程")) {
            return "编程题";
        }
        return raw;
    }

    private String buildAiSystemPrompt(HomeworkTeacherMonitorReportRequest request) {
        String typeCn = ASSIGNMENT_TYPE_EXAM.equals(request.getAssignmentType()) ? "考试"
                : (ASSIGNMENT_TYPE_HOMEWORK.equals(request.getAssignmentType()) ? "作业" : "练习任务");
        String base = "你是一位教师学情分析助手。请严格依据给定统计数据输出中文 Markdown 报告，不要杜撰不存在的班级名称。"
                + "班级统一写成\"班级 {classId}\"。"
                + "本次分析类型为" + typeCn + "。"
                + "语气专业、克制、可执行，不要输出代码块。";

        boolean allQuiz = request.getQuizResourceId() == null;
        boolean allClass = request.getClassId() == null;

        if (allQuiz) {
            // 全部试卷 → 让 AI 做跨练习题的横向对比
            return base + "报告必须包含以下 6 个二级标题："
                    + "## 整体完成情况、"
                    + "## 各试卷表现对比、"
                    + "## 平均水平判断、"
                    + "## 低分风险提醒、"
                    + "## 题型薄弱分析、"
                    + "## 后续教学建议。";
        }
        if (allClass) {
            // 指定练习题 + 全部班级 → 让 AI 做跨班级的横向对比（同一套题自然可比）
            return base + "报告必须包含以下 6 个二级标题："
                    + "## 该试卷整体完成情况、"
                    + "## 班级横向对比、"
                    + "## 平均水平判断、"
                    + "## 低分风险提醒、"
                    + "## 题型薄弱分析、"
                    + "## 后续教学建议。";
        }
        // 指定练习题 + 指定班级 → 聚焦单点诊断
        return base + "报告必须包含以下 5 个二级标题："
                + "## 班级整体完成情况、"
                + "## 平均水平判断、"
                + "## 低分风险提醒、"
                + "## 题型薄弱分析、"
                + "## 后续教学建议。";
    }

    private String buildAiPrompt(List<HomeworkTeacherMonitorItemVO> items,
                                 List<HomeworkSubmission> latestCompletedSubmissions,
                                 Map<String, TypeStat> typeStatMap,
                                 HomeworkTeacherMonitorReportRequest request,
                                 String quizTitleSnapshot) {
        StringBuilder sb = new StringBuilder();

        String rangeClass = request.getClassId() == null ? "全部班级" : ("班级 " + request.getClassId());
        String rangeDate = StringUtils.isBlank(request.getPublishDate()) ? "全部日期" : request.getPublishDate();
        String rangeQuiz = request.getQuizResourceId() == null
                ? "全部试卷"
                : "《" + StringUtils.defaultIfBlank(quizTitleSnapshot, "未命名试卷") + "》";

        int expectedTotal = items.stream().mapToInt(i -> safeInt(i.getStudentTotal())).sum();
        int completedTotal = latestCompletedSubmissions.size();
        int pendingTotal = Math.max(expectedTotal - completedTotal, 0);
        double overallAvg = round(latestCompletedSubmissions.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToInt(HomeworkSubmission::getTotalScore)
                .average()
                .orElse(0D));
        long lowRiskTotal = latestCompletedSubmissions.stream()
                .filter(s -> s.getTotalScore() != null && s.getTotalScore() < 60)
                .count();

        sb.append("【统计范围】\n");
        sb.append("- 班级范围：").append(rangeClass).append("\n");
        sb.append("- 试卷范围：").append(rangeQuiz).append("\n");
        sb.append("- 发布日期：").append(rangeDate).append("\n");
        sb.append("- 练习任务数量：").append(items.size()).append("\n");
        sb.append("- 应交人次：").append(expectedTotal).append("\n");
        sb.append("- 已完成人次：").append(completedTotal).append("\n");
        sb.append("- 未完成人次：").append(pendingTotal).append("\n");
        sb.append("- 完成样本平均分：").append(overallAvg).append("\n");
        sb.append("- 低分人数(<60)：").append(lowRiskTotal).append("\n\n");

        sb.append("【练习任务明细】\n");
        for (HomeworkTeacherMonitorItemVO item : items) {
            sb.append("- 《").append(StringUtils.defaultIfBlank(item.getTitle(), "未命名作业")).append("》")
                    .append(" | 试卷 ").append(StringUtils.defaultIfBlank(item.getQuizTitle(), "-"))
                    .append(" | 班级 ").append(item.getClassId() == null ? "-" : item.getClassId())
                    .append(" | 完成 ").append(safeInt(item.getCompletedCount())).append("/").append(safeInt(item.getStudentTotal()))
                    .append(" | 完成率 ").append(formatOne(item.getCompletionRate())).append("%")
                    .append(" | 平均分 ").append(formatOne(item.getAvgScore()))
                    .append(" | 低分人数 ").append(safeInt(item.getLowScoreCount()))
                    .append(" | 发布状态 ").append(StringUtils.defaultIfBlank(item.getStatus(), "-"))
                    .append("\n");
        }
        sb.append("\n");

        // 仅在"全部试卷"维度下聚合各练习题对比数据，供 AI 输出「## 各练习题表现对比」章节
        if (request.getQuizResourceId() == null) {
            Map<Long, List<HomeworkTeacherMonitorItemVO>> byQuiz = items.stream()
                    .filter(i -> i.getQuizResourceId() != null)
                    .collect(Collectors.groupingBy(HomeworkTeacherMonitorItemVO::getQuizResourceId, LinkedHashMap::new, Collectors.toList()));

            if (!byQuiz.isEmpty()) {
                sb.append("【试卷维度统计】\n");
                byQuiz.forEach((qid, quizItems) -> {
                    int qTotal = quizItems.stream().mapToInt(i -> safeInt(i.getStudentTotal())).sum();
                    int qCompleted = quizItems.stream().mapToInt(i -> safeInt(i.getCompletedCount())).sum();
                    int qLowScore = quizItems.stream().mapToInt(i -> safeInt(i.getLowScoreCount())).sum();
                    double qRate = qTotal == 0 ? 0D : round(qCompleted * 100D / qTotal);
                    double qAvg = round(quizItems.stream()
                            .filter(i -> i.getAvgScore() != null && safeInt(i.getCompletedCount()) > 0)
                            .mapToDouble(HomeworkTeacherMonitorItemVO::getAvgScore)
                            .average()
                            .orElse(0D));
                    String qTitle = StringUtils.defaultIfBlank(quizItems.get(0).getQuizTitle(), "未命名试卷");
                    sb.append("- 《").append(qTitle).append("》")
                            .append(" | 发布次数 ").append(quizItems.size())
                            .append(" | 完成 ").append(qCompleted).append("/").append(qTotal)
                            .append(" | 完成率 ").append(formatOne(qRate)).append("%")
                            .append(" | 平均分 ").append(formatOne(qAvg))
                            .append(" | 低分人数 ").append(qLowScore)
                            .append("\n");
                });
                sb.append("\n");
            }
        }

        sb.append("【题型统计】\n");
        if (typeStatMap.isEmpty()) {
            sb.append("- 暂无可用题型明细数据\n");
        } else {
            List<Map.Entry<String, TypeStat>> sorted = typeStatMap.entrySet().stream()
                    .sorted(Comparator
                            .comparingDouble((Map.Entry<String, TypeStat> e) -> e.getValue().accuracyRate())
                            .thenComparingInt(e -> -e.getValue().totalCount))
                    .collect(Collectors.toList());

            for (Map.Entry<String, TypeStat> entry : sorted) {
                TypeStat stat = entry.getValue();
                sb.append("- ").append(entry.getKey())
                        .append(" | 题目样本 ").append(stat.totalCount)
                        .append(" | 可评价样本 ").append(stat.evaluableCount)
                        .append(" | 正确率 ").append(formatOne(stat.accuracyRate())).append("%")
                        .append("\n");
            }
        }

        sb.append("\n请根据以上真实数据输出教师可直接阅读的诊断报告。");
        return sb.toString();
    }

    private String typeLabel(HomeworkTeacherMonitorReportRequest request) {
        return ASSIGNMENT_TYPE_EXAM.equals(request.getAssignmentType()) ? "考试"
                : (ASSIGNMENT_TYPE_HOMEWORK.equals(request.getAssignmentType()) ? "作业" : "练习任务");
    }

    private String buildNoDataReport(HomeworkTeacherMonitorReportRequest request, String quizTitleSnapshot) {
        String rangeClass = request.getClassId() == null ? "全部班级" : ("班级 " + request.getClassId());
        String rangeQuiz = request.getQuizResourceId() == null
                ? "全部试卷"
                : "《" + StringUtils.defaultIfBlank(quizTitleSnapshot, "未命名试卷") + "》";
        String rangeDate = StringUtils.isBlank(request.getPublishDate()) ? "全部日期" : request.getPublishDate();
        String tLabel = typeLabel(request);

        return "# " + tLabel + "学情诊断报告\n\n"
                + "## 班级整体完成情况\n"
                + "- 当前筛选范围：**" + rangeClass + " / " + rangeQuiz + " / " + rangeDate + "**\n"
                + "- 暂无" + tLabel + "数据，暂时无法生成学情分析。\n\n"
                + "## 平均水平判断\n"
                + "- 暂无可用于判断的成绩样本。\n\n"
                + "## 低分风险提醒\n"
                + "- 暂无风险样本。\n\n"
                + "## 题型薄弱分析\n"
                + "- 暂无题型明细数据。\n\n"
                + "## 后续教学建议\n"
                + "- 可先在教师端发布" + tLabel + "，并等待学生完成至少一批真实作答后再分析。";
    }

    private String buildNoCompletedReport(List<HomeworkTeacherMonitorItemVO> items,
                                          HomeworkTeacherMonitorReportRequest request,
                                          String quizTitleSnapshot) {
        int expectedTotal = items.stream().mapToInt(i -> safeInt(i.getStudentTotal())).sum();
        int completedTotal = items.stream().mapToInt(i -> safeInt(i.getCompletedCount())).sum();
        int pendingTotal = Math.max(expectedTotal - completedTotal, 0);

        String rangeClass = request.getClassId() == null ? "全部班级" : ("班级 " + request.getClassId());
        String rangeQuiz = request.getQuizResourceId() == null
                ? "全部试卷"
                : "《" + StringUtils.defaultIfBlank(quizTitleSnapshot, "未命名试卷") + "》";
        String rangeDate = StringUtils.isBlank(request.getPublishDate()) ? "全部日期" : request.getPublishDate();
        String tLabel = typeLabel(request);

        return "# " + tLabel + "学情诊断报告\n\n"
                + "## 班级整体完成情况\n"
                + "- 当前筛选范围：**" + rangeClass + " / " + rangeQuiz + " / " + rangeDate + "**\n"
                + "- 练习任务数量：" + items.size() + "\n"
                + "- 应交人次：" + expectedTotal + "\n"
                + "- 已完成人次：" + completedTotal + "\n"
                + "- 未完成人次：" + pendingTotal + "\n\n"
                + "## 平均水平判断\n"
                + "- 当前还没有已批改的真实成绩样本，无法对平均水平做出有效判断。\n\n"
                + "## 低分风险提醒\n"
                + "- 暂无低分统计样本。\n\n"
                + "## 题型薄弱分析\n"
                + "- 当前还没有足够的已批改明细，暂时无法判断薄弱题型。\n\n"
                + "## 后续教学建议\n"
                + "- 可优先催促学生完成提交。\n"
                + "- 等首批学生完成后，再重新生成诊断报告。";
    }

    private String buildLocalReport(List<HomeworkTeacherMonitorItemVO> items,
                                    List<HomeworkSubmission> latestCompletedSubmissions,
                                    Map<String, TypeStat> typeStatMap,
                                    HomeworkTeacherMonitorReportRequest request,
                                    String quizTitleSnapshot) {
        int expectedTotal = items.stream().mapToInt(i -> safeInt(i.getStudentTotal())).sum();
        int completedTotal = latestCompletedSubmissions.size();
        int pendingTotal = Math.max(expectedTotal - completedTotal, 0);
        double overallAvg = round(latestCompletedSubmissions.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToInt(HomeworkSubmission::getTotalScore)
                .average()
                .orElse(0D));
        long lowRiskTotal = latestCompletedSubmissions.stream()
                .filter(s -> s.getTotalScore() != null && s.getTotalScore() < 60)
                .count();

        StringBuilder sb = new StringBuilder();
        String tLabel = typeLabel(request);
        sb.append("# " + tLabel + "学情诊断报告\n\n");

        sb.append("## 班级整体完成情况\n");
        sb.append("- 统计班级范围：")
                .append(request.getClassId() == null ? "全部班级" : ("班级 " + request.getClassId()))
                .append("\n");
        sb.append("- 试卷范围：")
                .append(request.getQuizResourceId() == null
                        ? "全部试卷"
                        : "《" + StringUtils.defaultIfBlank(quizTitleSnapshot, "未命名试卷") + "》")
                .append("\n");
        sb.append("- 发布日期范围：")
                .append(StringUtils.isBlank(request.getPublishDate()) ? "全部日期" : request.getPublishDate())
                .append("\n");
        sb.append("- 涉及练习任务数：").append(items.size()).append("\n");
        sb.append("- 应交人次：").append(expectedTotal).append("\n");
        sb.append("- 已完成人次：").append(completedTotal).append("\n");
        sb.append("- 未完成人次：").append(pendingTotal).append("\n");
        sb.append("- 整体完成情况：")
                .append(expectedTotal == 0 ? "暂无应交样本" : formatOne(completedTotal * 100D / expectedTotal) + "%")
                .append("\n\n");

        sb.append("## 平均水平判断\n");
        sb.append("- 已批改样本平均分：**").append(formatOne(overallAvg)).append("**\n");
        if (overallAvg >= 85) {
            sb.append("- 整体掌握较好，班级多数学生能够较稳定完成当前作业任务。\n\n");
        } else if (overallAvg >= 70) {
            sb.append("- 班级整体处于中等水平，核心知识点基本掌握，但稳定性仍需提升。\n\n");
        } else if (overallAvg >= 60) {
            sb.append("- 班级基础掌握不够稳，建议尽快做一次针对性回顾与巩固。\n\n");
        } else {
            sb.append("- 班级整体风险较高，建议优先回到基础知识点进行补讲和重练。\n\n");
        }

        sb.append("## 低分风险提醒\n");
        sb.append("- 低分人数（<60）：**").append(lowRiskTotal).append("**\n");
        List<HomeworkTeacherMonitorItemVO> riskyAssignments = items.stream()
                .filter(i -> safeInt(i.getLowScoreCount()) > 0)
                .sorted(Comparator.comparingInt((HomeworkTeacherMonitorItemVO i) -> safeInt(i.getLowScoreCount())).reversed())
                .collect(Collectors.toList());

        if (riskyAssignments.isEmpty()) {
            sb.append("- 当前没有明显的低分风险集中作业。\n\n");
        } else {
            for (HomeworkTeacherMonitorItemVO item : riskyAssignments) {
                sb.append("- 《").append(StringUtils.defaultIfBlank(item.getTitle(), "未命名作业")).append("》")
                        .append("（班级 ").append(item.getClassId()).append("）")
                        .append(" 低分人数 ").append(safeInt(item.getLowScoreCount()))
                        .append("，完成率 ").append(formatOne(item.getCompletionRate())).append("%")
                        .append("，平均分 ").append(formatOne(item.getAvgScore()))
                        .append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 题型薄弱分析\n");
        if (typeStatMap.isEmpty()) {
            sb.append("- 目前没有足够的题型明细可供分析。\n\n");
        } else {
            List<Map.Entry<String, TypeStat>> sortedTypes = typeStatMap.entrySet().stream()
                    .sorted(Comparator
                            .comparingDouble((Map.Entry<String, TypeStat> e) -> e.getValue().accuracyRate())
                            .thenComparingInt(e -> -e.getValue().totalCount))
                    .collect(Collectors.toList());

            for (int i = 0; i < Math.min(sortedTypes.size(), 3); i++) {
                Map.Entry<String, TypeStat> entry = sortedTypes.get(i);
                TypeStat stat = entry.getValue();
                sb.append("- ").append(entry.getKey())
                        .append("：正确率约 **").append(formatOne(stat.accuracyRate())).append("%**")
                        .append("，题目样本 ").append(stat.totalCount)
                        .append("，可评价样本 ").append(stat.evaluableCount)
                        .append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 后续教学建议\n");
        if (pendingTotal > 0) {
            sb.append("- 先优先跟进未完成学生，避免样本不足影响整体判断。\n");
        }
        if (lowRiskTotal > 0) {
            sb.append("- 对低分学生建议做一次分层辅导，优先回补基础知识点与易错题。\n");
        } else {
            sb.append("- 可在下一轮练习中加入少量进阶题，检测学生迁移应用能力。\n");
        }
        if (!typeStatMap.isEmpty()) {
            Map.Entry<String, TypeStat> weakest = typeStatMap.entrySet().stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<String, TypeStat> e) -> e.getValue().accuracyRate()))
                    .findFirst()
                    .orElse(null);
            if (weakest != null) {
                sb.append("- 下次课堂建议针对 **").append(weakest.getKey()).append("** 做一次专题讲解与例题训练。\n");
            }
        }
        return sb.toString();
    }

    private HomeworkTeacherMonitorReportVO toReportVO(HomeworkMonitorReport report, boolean includeMarkdown) {
        HomeworkTeacherMonitorReportVO vo = new HomeworkTeacherMonitorReportVO();
        vo.setReportId(report.getId());
        vo.setReportTitle(report.getReportTitle());
        vo.setClassId(report.getClassId());
        vo.setPublishDate(report.getPublishDate());
        vo.setQuizResourceId(report.getQuizResourceId());
        vo.setQuizTitle(report.getQuizTitleSnapshot());
        vo.setCreateTime(report.getCreateTime());
        vo.setReportMarkdown(includeMarkdown ? report.getReportMarkdown() : null);

        try {
            if (StringUtils.isNotBlank(report.getAssignmentIdsJson())) {
                vo.setAssignmentIds(OBJECT_MAPPER.readValue(
                        report.getAssignmentIdsJson(),
                        new TypeReference<List<Long>>() {}
                ));
            } else {
                vo.setAssignmentIds(Collections.emptyList());
            }
        } catch (Exception e) {
            vo.setAssignmentIds(Collections.emptyList());
        }

        try {
            if (StringUtils.isNotBlank(report.getSummaryJson())) {
                Map<String, Object> summary = OBJECT_MAPPER.readValue(
                        report.getSummaryJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
                vo.setAssignmentCount(toInteger(summary.get("assignmentCount")));
                vo.setStudentTotal(toInteger(summary.get("studentTotal")));
                vo.setCompletedCount(toInteger(summary.get("completedCount")));
                vo.setPendingCount(toInteger(summary.get("pendingCount")));
                vo.setOverallCompletionRate(toDouble(summary.get("overallCompletionRate")));
                vo.setOverallAvgScore(toDouble(summary.get("overallAvgScore")));
                vo.setLowScoreCount(toInteger(summary.get("lowScoreCount")));
            }
        } catch (Exception ignore) {
        }

        return vo;
    }

    /**
     * 从筛选后的作业列表里挑一条 quizTitle 作为快照。
     * 只在 request 指定了 quizResourceId 时有意义；"全部试卷"时返回 null。
     */
    private String resolveQuizTitleSnapshot(List<HomeworkTeacherMonitorItemVO> items, Long quizResourceId) {
        if (quizResourceId == null) {
            return null;
        }
        return items.stream()
                .filter(i -> Objects.equals(quizResourceId, i.getQuizResourceId()))
                .map(HomeworkTeacherMonitorItemVO::getQuizTitle)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private String buildReportTitle(HomeworkTeacherMonitorReportRequest request, String quizTitleSnapshot) {
        String classPart = request.getClassId() == null ? "全部班级" : ("班级 " + request.getClassId());
        String quizPart = (request.getQuizResourceId() != null && StringUtils.isNotBlank(quizTitleSnapshot))
                ? "《" + quizTitleSnapshot + "》"
                : "全部试卷";
        String datePart = StringUtils.isBlank(request.getPublishDate()) ? "全部日期" : request.getPublishDate();
        String typeLabel = ASSIGNMENT_TYPE_EXAM.equals(request.getAssignmentType()) ? "考试"
                : (ASSIGNMENT_TYPE_HOMEWORK.equals(request.getAssignmentType()) ? "作业" : "练习任务");
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        return classPart + " / " + quizPart + " " + typeLabel + "学情诊断报告（" + datePart + "，生成于 " + now + "）";
    }

    private String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Integer toInteger(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Long) {
            return ((Long) obj).intValue();
        }
        if (obj instanceof Double) {
            return ((Double) obj).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }

    private Double toDouble(Object obj) {
        if (obj == null) {
            return 0D;
        }
        if (obj instanceof Double) {
            return (Double) obj;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        }
        if (obj instanceof Long) {
            return ((Long) obj).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0D;
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatOne(Double value) {
        if (value == null) {
            return "0.0";
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private static class TypeStat {
        private int totalCount;
        private int evaluableCount;
        private int correctCount;
        private double accuracySum;

        private double accuracyRate() {
            if (evaluableCount <= 0) {
                return 0D;
            }
            return BigDecimal.valueOf(accuracySum * 100D / evaluableCount)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }

    private static class ReportSnapshot {
        private int assignmentCount;
        private int studentTotal;
        private int completedCount;
        private int pendingCount;
        private double overallCompletionRate;
        private double overallAvgScore;
        private int lowScoreCount;

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("assignmentCount", assignmentCount);
            map.put("studentTotal", studentTotal);
            map.put("completedCount", completedCount);
            map.put("pendingCount", pendingCount);
            map.put("overallCompletionRate", overallCompletionRate);
            map.put("overallAvgScore", overallAvgScore);
            map.put("lowScoreCount", lowScoreCount);
            return map;
        }
    }

    private ReportSnapshot buildReportSnapshot(List<HomeworkTeacherMonitorItemVO> itemList,
                                               List<HomeworkSubmission> latestCompletedSubmissions) {
        ReportSnapshot snapshot = new ReportSnapshot();
        snapshot.assignmentCount = itemList.size();
        snapshot.studentTotal = itemList.stream().mapToInt(i -> safeInt(i.getStudentTotal())).sum();
        snapshot.completedCount = latestCompletedSubmissions.size();
        snapshot.pendingCount = Math.max(snapshot.studentTotal - snapshot.completedCount, 0);
        snapshot.overallCompletionRate = snapshot.studentTotal == 0
                ? 0D
                : round(snapshot.completedCount * 100D / snapshot.studentTotal);
        snapshot.overallAvgScore = round(latestCompletedSubmissions.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToInt(HomeworkSubmission::getTotalScore)
                .average()
                .orElse(0D));
        snapshot.lowScoreCount = (int) latestCompletedSubmissions.stream()
                .filter(s -> s.getTotalScore() != null && s.getTotalScore() < 60)
                .count();
        return snapshot;
    }

    @Override
    public Boolean deleteTeacherMonitorReport(Long reportId, User loginUser) {
        validateTeacher(loginUser);

        if (reportId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "reportId 不能为空");
        }

        HomeworkMonitorReport report = homeworkMonitorReportService.getById(reportId);
        if (report == null || (report.getIsDelete() != null && report.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "报告不存在");
        }
        if (!Objects.equals(report.getTeacherId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该报告");
        }

        // MyBatis-Plus 的 removeById 会自动触发 @TableLogic 逻辑删除
        return homeworkMonitorReportService.removeById(reportId);
    }

    @Override
    public Boolean deleteTeacherAssignment(Long assignmentId, User loginUser) {
        validateTeacher(loginUser);

        if (assignmentId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "assignmentId 不能为空");
        }

        HomeworkAssignment assignment = assignmentService.getById(assignmentId);
        if (assignment == null || (assignment.getIsDelete() != null && assignment.getIsDelete() == 1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }
        if (!Objects.equals(assignment.getTeacherId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该作业");
        }

        // 章节练习不在教师端学情分析页面展示，应走章节管理页面的删除入口
        String type = StringUtils.defaultIfBlank(assignment.getAssignmentType(), ASSIGNMENT_TYPE_HOMEWORK);
        if (!ASSIGNMENT_TYPE_HOMEWORK.equals(type) && !ASSIGNMENT_TYPE_EXAM.equals(type)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "章节练习请到章节管理页面删除");
        }

        // 仅软删作业本身，学生提交记录（homework_submission）和答题明细保留。
        // 未来若教师恢复这份作业（DB 层改 isDelete=0），学生的作答数据仍然完整可用。
        // 历史诊断报告（homework_monitor_report）里引用到本作业的，属于快照，不联动。
        return assignmentService.removeById(assignmentId);
    }
}
