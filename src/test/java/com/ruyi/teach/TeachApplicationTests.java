package com.ruyi.teach;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.flywaydb.core.api.MigrationVersion;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class TeachApplicationTests {

    private static final String TEST_DATABASE_NAME = "teach_test";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName(TEST_DATABASE_NAME)
            .withUsername("teach_test")
            .withPassword("teach_test")
            .withReuse(false);

    @DynamicPropertySource
    static void configureIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoadsWithIsolatedMigratedDatabase() {
        String currentDatabase = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class
        );

        assertEquals(TEST_DATABASE_NAME, currentDatabase);
        Integer modelDefaults = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_model_config",
                Integer.class
        );
        Integer categoryDefaults = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_category",
                Integer.class
        );

        assertTrue(successfulMigrations != null && successfulMigrations >= 2);
        assertTrue(modelDefaults != null && modelDefaults >= 8);
        assertTrue(categoryDefaults != null && categoryDefaults >= 18);

        // Rebuild this isolated test database to the real V1 schema first, then
        // simulate an existing V1 installation that has no Flyway history yet.
        Flyway v1Flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .cleanDisabled(false)
                .load();
        v1Flyway.clean();
        v1Flyway.migrate();
        jdbcTemplate.execute("DROP TABLE flyway_schema_history");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .cleanDisabled(true)
                .load()
                .migrate();

        Integer baselineRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND type = 'BASELINE' AND success = 1",
                Integer.class
        );
        Integer versionTwoRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND type = 'SQL' AND success = 1",
                Integer.class
        );
        Integer versionThreeRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '3' AND type = 'SQL' AND success = 1",
                Integer.class
        );
        String chapterVideoNullable = jdbcTemplate.queryForObject(
                """
                SELECT IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'course_chapter'
                  AND COLUMN_NAME = 'video_url'
                """,
                String.class
        );
        Integer modelDefaultsAfterUpgrade = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_model_config",
                Integer.class
        );
        Integer categoryDefaultsAfterUpgrade = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_category",
                Integer.class
        );

        assertEquals(1, baselineRows);
        assertEquals(1, versionTwoRows);
        assertEquals(1, versionThreeRows);
        assertEquals("YES", chapterVideoNullable);
        assertEquals(modelDefaults, modelDefaultsAfterUpgrade);
        assertEquals(categoryDefaults, categoryDefaultsAfterUpgrade);
    }

}
