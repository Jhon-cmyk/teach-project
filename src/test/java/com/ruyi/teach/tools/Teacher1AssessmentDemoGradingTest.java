package com.ruyi.teach.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruyi.teach.mapper.HomeworkSubmissionDetailMapper;
import com.ruyi.teach.model.dto.ExamGradeRequest;
import com.ruyi.teach.model.dto.HomeworkSubmissionReviewRequest;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.HomeworkSubmissionService;
import com.ruyi.teach.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Explicitly approves/grades only the submissions produced by the matching
 * teacher1 demo-data seeder. Existing real submissions are outside its scope.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.flyway.enabled=false"
)
@EnabledIfEnvironmentVariable(
        named = "RUN_TEACHER1_ASSESSMENT_GRADING",
        matches = "YES_I_KNOW_THIS_GRADES_DEV_DATA"
)
class Teacher1AssessmentDemoGradingTest {

    private static final String WRITE_GUARD = "RUN_TEACHER1_ASSESSMENT_GRADING";
    private static final String WRITE_GUARD_VALUE = "YES_I_KNOW_THIS_GRADES_DEV_DATA";
    private static final String SEED_MARKER = "teacher1-demo-seed-20260810-v1";
    private static final Long TEACHER_ID = 4L;
    private static final Set<Long> HOMEWORK_IDS = Set.of(75L, 76L, 77L, 78L, 79L, 80L);
    private static final Set<Long> EXAM_IDS = Set.of(81L, 82L, 83L, 84L);

    private final HomeworkSubmissionService submissionService;
    private final HomeworkSubmissionDetailMapper detailMapper;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    Teacher1AssessmentDemoGradingTest(HomeworkSubmissionService submissionService,
                                      HomeworkSubmissionDetailMapper detailMapper,
                                      UserService userService,
                                      JdbcTemplate jdbcTemplate) {
        this.submissionService = submissionService;
        this.detailMapper = detailMapper;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void saveAllSeededHomeworkReviewsAndExamGrades() {
        Assumptions.assumeTrue(
                WRITE_GUARD_VALUE.equals(System.getenv(WRITE_GUARD)),
                "Explicit grading guard is absent; demo grading skipped"
        );
        assertEquals("teach_platform", jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));

        User teacher = userService.getById(TEACHER_ID);
        assertNotNull(teacher);
        assertEquals("teacher", teacher.getUserRole());

        List<HomeworkSubmission> seeded = submissionService.list(
                new LambdaQueryWrapper<HomeworkSubmission>()
                        .in(HomeworkSubmission::getAssignmentId, allAssignmentIds())
                        .like(HomeworkSubmission::getStudentAnswerJson, SEED_MARKER)
                        .eq(HomeworkSubmission::getIsDelete, 0)
                        .orderByAsc(HomeworkSubmission::getAssignmentId)
                        .orderByAsc(HomeworkSubmission::getStudentId)
        );
        assertEquals(89, seeded.size(), "Only the 89 generated submissions may be graded here");

        int homeworkReviewsSaved = 0;
        int examGradesSaved = 0;
        int alreadyFinalized = 0;
        for (HomeworkSubmission submission : seeded) {
            if (HOMEWORK_IDS.contains(submission.getAssignmentId())) {
                if ("approved".equals(submission.getReviewStatus())) {
                    alreadyFinalized++;
                    continue;
                }
                saveHomeworkReview(submission, teacher);
                homeworkReviewsSaved++;
            } else if (EXAM_IDS.contains(submission.getAssignmentId())) {
                if ("completed".equals(submission.getSubmitStatus())) {
                    alreadyFinalized++;
                    continue;
                }
                saveExamGrade(submission, teacher);
                examGradesSaved++;
            }
        }

        long approvedSeededHomework = submissionService.count(
                new LambdaQueryWrapper<HomeworkSubmission>()
                        .in(HomeworkSubmission::getAssignmentId, HOMEWORK_IDS)
                        .like(HomeworkSubmission::getStudentAnswerJson, SEED_MARKER)
                        .eq(HomeworkSubmission::getReviewStatus, "approved")
                        .eq(HomeworkSubmission::getIsDelete, 0)
        );
        long completedSeededExams = submissionService.count(
                new LambdaQueryWrapper<HomeworkSubmission>()
                        .in(HomeworkSubmission::getAssignmentId, EXAM_IDS)
                        .like(HomeworkSubmission::getStudentAnswerJson, SEED_MARKER)
                        .eq(HomeworkSubmission::getSubmitStatus, "completed")
                        .eq(HomeworkSubmission::getIsDelete, 0)
        );
        assertEquals(53L, approvedSeededHomework);
        assertEquals(36L, completedSeededExams);
        System.out.printf(
                Locale.ROOT,
                "teacher1 demo grading complete: homeworkReviewsSaved=%d, examGradesSaved=%d, alreadyFinalized=%d%n",
                homeworkReviewsSaved,
                examGradesSaved,
                alreadyFinalized
        );
    }

    private void saveHomeworkReview(HomeworkSubmission submission, User teacher) {
        List<HomeworkSubmissionDetail> details = listDetails(submission.getId());
        HomeworkSubmissionReviewRequest request = new HomeworkSubmissionReviewRequest();
        request.setSubmissionId(submission.getId());
        List<HomeworkSubmissionReviewRequest.QuestionScore> scores = new ArrayList<>();
        int total = 0;
        for (HomeworkSubmissionDetail detail : details) {
            int score = scoreFor(detail, true);
            total += score;
            HomeworkSubmissionReviewRequest.QuestionScore item =
                    new HomeworkSubmissionReviewRequest.QuestionScore();
            item.setId(detail.getId());
            item.setScore(score);
            scores.add(item);
        }
        request.setDetails(scores);
        request.setTeacherRemark(reviewRemark(Math.min(total, 100), false));
        submissionService.teacherReviewHomeworkSubmission(request, teacher);
    }

    private void saveExamGrade(HomeworkSubmission submission, User teacher) {
        List<HomeworkSubmissionDetail> details = listDetails(submission.getId());
        ExamGradeRequest request = new ExamGradeRequest();
        request.setSubmissionId(submission.getId());
        List<ExamGradeRequest.QuestionScore> scores = new ArrayList<>();
        int total = 0;
        for (HomeworkSubmissionDetail detail : details) {
            int score = scoreFor(detail, false);
            total += score;
            ExamGradeRequest.QuestionScore item = new ExamGradeRequest.QuestionScore();
            item.setId(detail.getId());
            item.setScore(score);
            scores.add(item);
        }
        request.setDetails(scores);
        request.setTeacherRemark(reviewRemark(Math.min(total, 100), true));
        submissionService.teacherGradeExam(request, teacher);
    }

    private List<HomeworkSubmissionDetail> listDetails(Long submissionId) {
        List<HomeworkSubmissionDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<HomeworkSubmissionDetail>()
                        .eq(HomeworkSubmissionDetail::getSubmissionId, submissionId)
                        .orderByAsc(HomeworkSubmissionDetail::getId)
        );
        details.sort(Comparator.comparing(HomeworkSubmissionDetail::getId));
        return details;
    }

    private int scoreFor(HomeworkSubmissionDetail detail, boolean preferSuggestedScore) {
        int fullScore = detail.getFullScore() == null ? 0 : Math.max(detail.getFullScore(), 0);
        if (preferSuggestedScore && detail.getAiSuggestedScore() != null) {
            return Math.min(Math.max(detail.getAiSuggestedScore(), 0), fullScore);
        }
        String studentAnswer = normalize(detail.getStudentAnswer());
        String standardAnswer = normalize(detail.getStandardAnswer());
        return StringUtils.isNotBlank(studentAnswer)
                && StringUtils.equals(studentAnswer, standardAnswer)
                ? fullScore
                : 0;
    }

    private String normalize(String value) {
        return StringUtils.trimToEmpty(value)
                .replaceAll("[,，、;；。\\.!！?？\\s]+", "")
                .toUpperCase(Locale.ROOT);
    }

    private String reviewRemark(int totalScore, boolean exam) {
        String work = exam ? "试卷" : "作业";
        if (totalScore >= 85) {
            return "本次" + work + "完成较好，基础概念和解题方法掌握较扎实。请继续复盘错题，巩固容易混淆的判断条件。";
        }
        if (totalScore >= 70) {
            return "本次" + work + "整体完成良好，但部分知识点还不够稳定。请对照参考答案订正错题，并梳理相关概念。";
        }
        return "本次" + work + "已完成，基础知识仍需加强。建议逐题订正错误答案，重新理解核心概念后再做一次同类练习。";
    }

    private Set<Long> allAssignmentIds() {
        Set<Long> ids = new java.util.HashSet<>(HOMEWORK_IDS);
        ids.addAll(EXAM_IDS);
        return ids;
    }
}
