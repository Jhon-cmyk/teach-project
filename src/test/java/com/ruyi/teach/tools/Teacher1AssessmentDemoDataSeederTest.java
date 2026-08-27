package com.ruyi.teach.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.HomeworkAssignmentService;
import com.ruyi.teach.service.HomeworkSubmissionService;
import com.ruyi.teach.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Explicitly invoked local demo-data seeder for teacher1's 2026-08-10 assessments.
 *
 * <p>This class is deliberately guarded by an environment variable. A normal
 * test run skips it without opening a write path. Existing active submissions
 * are never changed; only missing assignment/student pairs are submitted through
 * the same application services used by the student UI.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.flyway.enabled=false"
)
@EnabledIfEnvironmentVariable(named = "RUN_TEACHER1_ASSESSMENT_SEED", matches = "YES_I_KNOW_THIS_WRITES_DEV_DB")
class Teacher1AssessmentDemoDataSeederTest {

    private static final String WRITE_GUARD = "RUN_TEACHER1_ASSESSMENT_SEED";
    private static final String WRITE_GUARD_VALUE = "YES_I_KNOW_THIS_WRITES_DEV_DB";
    private static final String SEED_MARKER = "teacher1-demo-seed-20260810-v1";
    private static final Long TEACHER_ID = 4L;
    private static final Long CLASS_ID = 1L;
    private static final Set<Long> STUDENT_IDS = Set.of(5L, 7L, 8L, 9L, 10L, 11L, 12L, 16L, 17L);
    private static final Set<Long> HOMEWORK_IDS = Set.of(75L, 76L, 77L, 78L, 79L, 80L);
    private static final Set<Long> EXAM_IDS = Set.of(81L, 82L, 83L, 84L);

    private final HomeworkAssignmentService assignmentService;
    private final HomeworkSubmissionService submissionService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    Teacher1AssessmentDemoDataSeederTest(HomeworkAssignmentService assignmentService,
                                         HomeworkSubmissionService submissionService,
                                         UserService userService,
                                         ObjectMapper objectMapper,
                                         JdbcTemplate jdbcTemplate) {
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void seedMissingStudentSubmissions() throws Exception {
        Assumptions.assumeTrue(
                WRITE_GUARD_VALUE.equals(System.getenv(WRITE_GUARD)),
                "Explicit write guard is absent; demo-data seeder skipped"
        );
        assertEquals("teach_platform", jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));

        List<User> students = loadAndValidateStudents();
        List<HomeworkAssignment> assignments = loadAndValidateAssignments();
        Object submissionServiceTarget = AopTestUtils.getUltimateTargetObject(submissionService);

        int created = 0;
        int skipped = 0;
        for (HomeworkAssignment assignment : assignments) {
            List<QuestionSeed> questions = parseQuestions(submissionServiceTarget, assignment);
            assertFalse(questions.isEmpty(), "No questions parsed for assignment " + assignment.getId());

            for (User student : students) {
                long existing = submissionService.count(new LambdaQueryWrapper<HomeworkSubmission>()
                        .eq(HomeworkSubmission::getAssignmentId, assignment.getId())
                        .eq(HomeworkSubmission::getStudentId, student.getId())
                        .eq(HomeworkSubmission::getIsDelete, 0));
                if (existing > 0) {
                    skipped++;
                    continue;
                }

                HomeworkSubmitRequest request = new HomeworkSubmitRequest();
                request.setAssignmentId(assignment.getId());
                request.setSubmissionType("online");
                request.setStudentAnswerJson(objectMapper.writeValueAsString(
                        buildAnswers(assignment, student, questions)
                ));

                Long submissionId;
                if (HOMEWORK_IDS.contains(assignment.getId())) {
                    submissionId = submissionService.submitHomeworkAsync(request, student);
                } else {
                    submissionId = submissionService.submitExam(request, student);
                }
                assertNotNull(submissionId);
                created++;
            }
        }

        long activeTargetSubmissions = submissionService.count(new LambdaQueryWrapper<HomeworkSubmission>()
                .in(HomeworkSubmission::getAssignmentId, allAssignmentIds())
                .in(HomeworkSubmission::getStudentId, STUDENT_IDS)
                .eq(HomeworkSubmission::getIsDelete, 0));
        assertEquals(90L, activeTargetSubmissions, "Every target student should have one active submission per assessment");
        System.out.printf(
                Locale.ROOT,
                "teacher1 assessment demo seed complete: created=%d, preserved=%d, total=%d%n",
                created,
                skipped,
                activeTargetSubmissions
        );
    }

    private List<User> loadAndValidateStudents() {
        List<User> students = new ArrayList<>(userService.listByIds(STUDENT_IDS));
        students.sort(Comparator.comparing(User::getId));
        assertEquals(STUDENT_IDS.size(), students.size());
        for (User student : students) {
            assertEquals("student", student.getUserRole(), "Unexpected role for user " + student.getId());
            assertEquals(CLASS_ID, student.getClassId(), "Unexpected class for user " + student.getId());
            assertTrue(student.getIsDelete() == null || student.getIsDelete() == 0);
        }
        return students;
    }

    private List<HomeworkAssignment> loadAndValidateAssignments() {
        List<HomeworkAssignment> assignments = new ArrayList<>(assignmentService.listByIds(allAssignmentIds()));
        assignments.sort(Comparator.comparing(HomeworkAssignment::getId));
        assertEquals(10, assignments.size());
        for (HomeworkAssignment assignment : assignments) {
            assertEquals(TEACHER_ID, assignment.getTeacherId());
            assertEquals(CLASS_ID, assignment.getClassId());
            assertEquals("published", assignment.getStatus());
            assertTrue(assignment.getIsDelete() == null || assignment.getIsDelete() == 0);
            String expectedType = HOMEWORK_IDS.contains(assignment.getId()) ? "homework" : "exam";
            assertEquals(expectedType, assignment.getAssignmentType());
            assertTrue(StringUtils.isNotBlank(assignment.getContentSnapshot()));
        }
        return assignments;
    }

    @SuppressWarnings("unchecked")
    private List<QuestionSeed> parseQuestions(Object serviceTarget, HomeworkAssignment assignment) {
        List<Object> metas = ReflectionTestUtils.invokeMethod(serviceTarget, "parseQuestionMetas", assignment);
        assertNotNull(metas);
        List<QuestionSeed> result = new ArrayList<>();
        for (Object meta : metas) {
            List<Object> options = (List<Object>) ReflectionTestUtils.getField(meta, "options");
            List<String> optionLabels = options == null ? List.of() : options.stream()
                    .map(option -> String.valueOf(ReflectionTestUtils.getField(option, "label")))
                    .filter(StringUtils::isNotBlank)
                    .toList();
            result.add(new QuestionSeed(
                    stringField(meta, "no"),
                    stringField(meta, "globalNo"),
                    stringField(meta, "type"),
                    stringField(meta, "stem"),
                    stringField(meta, "standardAnswer"),
                    (Integer) ReflectionTestUtils.getField(meta, "fullScore"),
                    optionLabels
            ));
        }
        return result;
    }

    private List<Map<String, Object>> buildAnswers(HomeworkAssignment assignment,
                                                    User student,
                                                    List<QuestionSeed> questions) {
        List<Map<String, Object>> answers = new ArrayList<>();
        int targetCorrectPercent = 58 + Math.floorMod(
                student.getId().intValue() * 17 + assignment.getId().intValue() * 13,
                36
        );
        for (int index = 0; index < questions.size(); index++) {
            QuestionSeed question = questions.get(index);
            int roll = Math.floorMod(
                    student.getId().intValue() * 31
                            + assignment.getId().intValue() * 19
                            + (index + 1) * 23,
                    100
            );
            boolean shouldBeCorrect = roll < targetCorrectPercent;

            Map<String, Object> answer = new LinkedHashMap<>();
            answer.put("num", StringUtils.defaultIfBlank(question.globalNo(), String.valueOf(index + 1)));
            answer.put("originalQuestionNo", StringUtils.defaultIfBlank(question.no(), String.valueOf(index + 1)));
            answer.put("type", StringUtils.defaultIfBlank(question.type(), "text"));
            answer.put("stem", question.stem());
            answer.put("answer", shouldBeCorrect ? correctAnswer(question) : incorrectAnswer(question));
            answer.put("demoDataSource", SEED_MARKER);
            answers.add(answer);
        }
        return answers;
    }

    private String correctAnswer(QuestionSeed question) {
        return StringUtils.defaultIfBlank(question.standardAnswer(), "已完成作答");
    }

    private String incorrectAnswer(QuestionSeed question) {
        String type = StringUtils.defaultString(question.type()).toLowerCase(Locale.ROOT);
        String standard = StringUtils.defaultString(question.standardAnswer()).toUpperCase(Locale.ROOT);
        if ("judge".equals(type)) {
            boolean trueAnswer = standard.contains("正确") || standard.contains("对")
                    || standard.contains("TRUE") || standard.contains("√") || "1".equals(standard.trim());
            return trueAnswer ? "错误" : "正确";
        }
        if ("radio".equals(type) || "checkbox".equals(type)) {
            for (String label : question.optionLabels()) {
                String normalized = StringUtils.trimToEmpty(label).toUpperCase(Locale.ROOT);
                if (StringUtils.isNotBlank(normalized) && !standard.contains(normalized)) {
                    return normalized;
                }
            }
            return standard.contains("A") ? "B" : "A";
        }
        return "暂不确定";
    }

    private String stringField(Object target, String fieldName) {
        Object value = ReflectionTestUtils.getField(target, fieldName);
        return value == null ? null : String.valueOf(value);
    }

    private Set<Long> allAssignmentIds() {
        Set<Long> ids = new java.util.HashSet<>(HOMEWORK_IDS);
        ids.addAll(EXAM_IDS);
        return ids;
    }

    private record QuestionSeed(String no,
                                String globalNo,
                                String type,
                                String stem,
                                String standardAnswer,
                                Integer fullScore,
                                List<String> optionLabels) {
        private QuestionSeed {
            optionLabels = Objects.requireNonNullElse(optionLabels, List.of());
        }
    }
}
