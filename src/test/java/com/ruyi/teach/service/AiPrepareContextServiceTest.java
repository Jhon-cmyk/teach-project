package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.model.dto.PrepareAgentRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiPrepareContextServiceTest {

    private final AiPrepareContextService service = new AiPrepareContextService();

    @Test
    void buildsPayloadWithoutDatabaseRetrievalForAnimationAgent() {
        PrepareAgentRequest request = new PrepareAgentRequest();
        request.setAgentType("anim");
        request.setForm(Map.of("topic", "二叉树遍历"));
        request.setSourceContent("source");
        request.setCourseId(101L);

        ObjectNode payload = service.buildAgentPayload(12L, request);

        assertThat(payload.path("agentType").asText()).isEqualTo("anim");
        assertThat(payload.path("teacherId").asLong()).isEqualTo(12L);
        assertThat(payload.path("courseId").asLong()).isEqualTo(101L);
        assertThat(payload.path("form").path("topic").asText()).isEqualTo("二叉树遍历");
        assertThat(payload.path("sourceContent").asText()).isEqualTo("source");
        assertThat(payload.path("context").path("graphNodes")).isEmpty();
        assertThat(payload.path("context").path("resources")).isEmpty();
        assertThat(payload.path("context").path("cases")).isEmpty();
    }

    @Test
    void normalizesLegacyLessonPlanFormFieldsIndependently() {
        PrepareAgentRequest request = new PrepareAgentRequest();
        request.setAgentType("plan");
        request.setRetrievalOptions(Map.of("mode", "off"));
        request.setForm(Map.of(
                "methods", List.of("案例教学法"),
                "activities", List.of("随堂练习"),
                "excludedSections", List.of("板书设计"),
                "requireBlackboard", true
        ));

        ObjectNode payload = service.buildAgentPayload(9L, request);

        assertThat(payload.path("form").path("selectedMethods").get(0).asText())
                .isEqualTo("案例教学法");
        assertThat(payload.path("form").path("selectedActivities").get(0).asText())
                .isEqualTo("随堂练习");
        assertThat(payload.path("form").path("excludedSections").toString()).contains("板书设计");
        assertThat(payload.path("context").path("cases")).isEmpty();
    }
}
