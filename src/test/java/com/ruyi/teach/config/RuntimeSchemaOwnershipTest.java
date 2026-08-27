package com.ruyi.teach.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeSchemaOwnershipTest {

    @Test
    void applicationStartupCodeDoesNotContainSchemaMutation() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        List<String> violations = new ArrayList<>();

        try (var files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                String normalized = source.toLowerCase(Locale.ROOT);
                boolean startupHook = normalized.contains("@postconstruct")
                        || normalized.contains("implements applicationrunner")
                        || normalized.contains("implements commandlinerunner");
                boolean schemaMutation = normalized.contains("create table")
                        || normalized.contains("alter table")
                        || normalized.contains("create index");
                if (startupHook && schemaMutation) {
                    violations.add(sourceRoot.relativize(file).toString());
                }
                if (file.getFileName().toString().endsWith("SchemaInitializer.java")) {
                    violations.add(sourceRoot.relativize(file).toString());
                }
            }
        }

        assertThat(violations)
                .as("Runtime startup code must not create or alter database schema")
                .isEmpty();
    }
}
