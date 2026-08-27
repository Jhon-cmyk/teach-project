package com.ruyi.teach.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Read-only replacement for the old startup DDL initializers.
 * Missing schema is reported clearly and must be fixed through Flyway.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaReadinessVerifier implements ApplicationRunner {

    static final Set<String> REQUIRED_TABLES = lowercaseSet(
            "admin_audit_log",
            "admin_import_batch",
            "ai_model_config",
            "course_category",
            "learning_event",
            "student_daily_recommendation_session",
            "student_knowledge_mastery",
            "student_learning_preference",
            "student_resource_recommendation",
            "homework_submission_image",
            "major_curriculum_course",
            "mental_state_record",
            "teacher_course_assignment",
            "teacher_registration_code",
            "teaching_case_asset",
            "video_knowledge_segment",
            "video_timeline_analysis_task"
    );

    static final Set<String> REQUIRED_COLUMNS = lowercaseSet(
            "course.categoryId",
            "course.face_detection_required",
            "course.knowledgeRepoId",
            "course.knowledgeRepoName",
            "course.knowledgeKeywords",
            "course.knowledgeSyncStatus",
            "course.knowledgeUpdatedAt",
            "fatigue_record.course_id",
            "fatigue_record.chapter_id",
            "homework_assignment.answerMode",
            "homework_assignment.imageGranularity",
            "homework_assignment.gradingMode",
            "homework_assignment.targetStudentId",
            "homework_assignment.sourceType",
            "homework_submission.submissionType",
            "homework_submission.gradingModeSnapshot",
            "homework_submission.reviewStatus",
            "homework_submission.aiSuggestedTotalScore",
            "homework_submission.visionStatus",
            "homework_submission.visionResultJson",
            "homework_submission_detail.imageUrlsJson",
            "homework_submission_detail.recognizedText",
            "homework_submission_detail.visionConfidence",
            "homework_submission_detail.aiSuggestedScore",
            "mental_state_record.learning_profile_days",
            "mental_state_record.learning_context_summary",
            "mental_state_record.learning_profile_snapshot",
            "micro_course_task.audio_url",
            "micro_course_task.duration_seconds",
            "micro_course_task.warnings_json",
            "micro_course_task.render_stats_json",
            "student_learning_preference.profileCompleted",
            "student_learning_preference.personalityType",
            "student_learning_preference.assessmentJson",
            "student_learning_preference.aiQuestionCount",
            "student_learning_preference.aiProfileSummary",
            "student_learning_preference.aiProfileJson",
            "student_learning_preference.lastAiQuestionTime",
            "student_resource_recommendation.recommendationSource",
            "user.teacher_title",
            "user.teacher_register_code"
    );

    static final Set<String> REQUIRED_INDEXES = lowercaseSet(
            "course.idx_course_category_id",
            "course.idx_course_knowledge_repo",
            "course_category.uk_course_category_name",
            "homework_assignment.idx_homework_target_student",
            "learning_event.idx_learning_event_ai_profile"
    );

    private final JdbcTemplate jdbcTemplate;

    public SchemaReadinessVerifier(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        verify();
    }

    public void verify() {
        Set<String> tables = normalize(jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                String.class
        ));
        Set<String> columns = normalize(jdbcTemplate.queryForList(
                "SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE()",
                String.class
        ));
        Set<String> indexes = normalize(jdbcTemplate.queryForList(
                "SELECT CONCAT(TABLE_NAME, '.', INDEX_NAME) FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE()",
                String.class
        ));

        List<String> missing = new ArrayList<>();
        addMissing(missing, "table:", REQUIRED_TABLES, tables);
        addMissing(missing, "column:", REQUIRED_COLUMNS, columns);
        addMissing(missing, "index:", REQUIRED_INDEXES, indexes);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Database schema is incomplete. Run the required Flyway migrations before starting "
                            + "the application. Missing objects: " + String.join(", ", missing)
            );
        }
    }

    private static void addMissing(List<String> target,
                                   String prefix,
                                   Set<String> required,
                                   Set<String> actual) {
        required.stream()
                .filter(item -> !actual.contains(item))
                .sorted()
                .map(item -> prefix + item)
                .forEach(target::add);
    }

    private static Set<String> normalize(List<String> values) {
        Set<String> result = new HashSet<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .forEach(result::add);
        }
        return result;
    }

    private static Set<String> lowercaseSet(String... values) {
        return normalize(List.of(values));
    }
}
