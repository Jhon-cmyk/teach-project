package com.ruyi.teach.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayBaselineMigrationTest {

    private static final String BASELINE_MIGRATION =
            "db/migration/V1__baseline_schema.sql";
    private static final String REFERENCE_DATA_MIGRATION =
            "db/migration/V2__seed_required_reference_data.sql";
    private static final String ANIMATION_CHAPTER_MIGRATION =
            "db/migration/V3__allow_animation_only_course_chapters.sql";

    @Test
    void baselineContainsAllCoreTablesWithoutBusinessData() throws IOException {
        String sql = readMigration(BASELINE_MIGRATION);

        assertEquals(62, count(sql, "(?im)^CREATE TABLE"));
        assertTrue(sql.contains("CREATE TABLE `user`"));
        assertTrue(sql.contains("CREATE TABLE `course`"));
        assertTrue(sql.contains("CREATE TABLE `homework_assignment`"));
        assertTrue(sql.contains("CREATE TABLE `coding_problem`"));
        assertTrue(sql.contains("CREATE TABLE `ai_resource`"));

        assertFalse(Pattern.compile("(?im)^\\s*(INSERT|REPLACE)\\s+INTO").matcher(sql).find());
    }

    @Test
    void baselineDoesNotContainDestructiveOrDatabaseScopedStatements() throws IOException {
        String sql = readMigration(BASELINE_MIGRATION);

        assertFalse(Pattern.compile("(?im)^\\s*DROP\\s+TABLE").matcher(sql).find());
        assertFalse(Pattern.compile("(?im)^\\s*(CREATE|DROP)\\s+DATABASE").matcher(sql).find());
        assertFalse(Pattern.compile("(?im)^\\s*USE\\s+").matcher(sql).find());
        assertFalse(sql.contains("flyway_schema_history"));
    }

    @Test
    void requiredReferenceDataIsVersionedWithoutSchemaChangesOrSecrets() throws IOException {
        String sql = readMigration(REFERENCE_DATA_MIGRATION);

        assertTrue(sql.contains("INSERT INTO ai_model_config"));
        assertTrue(sql.contains("INSERT INTO course_category"));
        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
        assertTrue(sql.contains("'chat_stream'"));
        assertTrue(sql.contains("'编程'"));
        assertFalse(Pattern.compile("(?im)^\\s*(CREATE|ALTER|DROP)\\s+").matcher(sql).find());
        assertFalse(Pattern.compile("(?i)(api[_-]?key|access[_-]?key|password)\\s*=\\s*['\"][^'\"]+")
                .matcher(sql)
                .find());
    }

    @Test
    void animationOnlyChapterMigrationMakesVideoOptional() throws IOException {
        String sql = readMigration(ANIMATION_CHAPTER_MIGRATION);

        assertTrue(sql.contains("ALTER TABLE `course_chapter`"));
        assertTrue(Pattern.compile("(?i)`video_url`\\s+varchar\\(1024\\)\\s+NULL")
                .matcher(sql)
                .find());
        assertFalse(Pattern.compile("(?im)^\\s*(DROP|TRUNCATE|DELETE)\\s+")
                .matcher(sql)
                .find());
    }

    private String readMigration(String resource) throws IOException {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(inputStream, "Flyway baseline migration is missing");
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int count(String value, String regex) {
        return (int) Pattern.compile(regex).matcher(value).results().count();
    }
}
