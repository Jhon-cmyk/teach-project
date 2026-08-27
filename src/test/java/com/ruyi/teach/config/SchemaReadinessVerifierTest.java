package com.ruyi.teach.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaReadinessVerifierTest {

    @Test
    void completeFlywaySchemaPassesReadOnlyVerification() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(new ArrayList<>(SchemaReadinessVerifier.REQUIRED_TABLES))
                .thenReturn(new ArrayList<>(SchemaReadinessVerifier.REQUIRED_COLUMNS))
                .thenReturn(new ArrayList<>(SchemaReadinessVerifier.REQUIRED_INDEXES));

        assertThatCode(() -> new SchemaReadinessVerifier(jdbcTemplate).verify())
                .doesNotThrowAnyException();
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void missingObjectsFailFastWithMigrationGuidance() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("course"))
                .thenReturn(List.of("course.categoryId"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> new SchemaReadinessVerifier(jdbcTemplate).verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Run the required Flyway migrations")
                .hasMessageContaining("table:ai_model_config")
                .hasMessageContaining("column:micro_course_task.audio_url")
                .hasMessageContaining("index:course.idx_course_category_id");
    }
}
