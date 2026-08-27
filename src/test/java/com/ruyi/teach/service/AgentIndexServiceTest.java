package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.AiAgentClient;
import com.ruyi.teach.model.entity.AiResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AgentIndexServiceTest {

    private AgentIndexService service;
    private AiAgentClient aiAgentClient;

    @BeforeEach
    void setUp() {
        service = new AgentIndexService();
        aiAgentClient = mock(AiAgentClient.class);
        AgentIndexTaskDispatcher dispatcher = mock(AgentIndexTaskDispatcher.class);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(dispatcher).dispatchAfterCommit(any(Runnable.class));

        ReflectionTestUtils.setField(service, "aiAgentClient", aiAgentClient);
        ReflectionTestUtils.setField(service, "agentIndexTaskDispatcher", dispatcher);
    }

    @Test
    void resourceUpsertLeavesChunkingToPythonAndCarriesCourseMetadata() {
        AiResource resource = new AiResource();
        resource.setId(31L);
        resource.setTeacherId(1001L);
        resource.setType("plan");
        resource.setTitle("Tree traversal lesson");
        resource.setContent("Updated lesson content");
        resource.setParamsJson("""
                {"courseId":101,"courseName":"Data Structures"}
                """);

        service.upsertAiResource(resource);

        ArgumentCaptor<ObjectNode> payloadCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(aiAgentClient).syncIndex(
                org.mockito.ArgumentMatchers.eq("/agent/index/upsert"),
                payloadCaptor.capture()
        );
        ObjectNode document = (ObjectNode) payloadCaptor.getValue().path("documents").get(0);
        assertThat(document.path("teacherId").asLong()).isEqualTo(1001L);
        assertThat(document.path("courseId").asLong()).isEqualTo(101L);
        assertThat(document.path("courseName").asText()).isEqualTo("Data Structures");
        assertThat(document.path("resourceType").asText()).isEqualTo("plan");
        assertThat(document.path("content").asText()).contains("Updated lesson content");
        assertThat(document.has("chunks")).isFalse();
    }

    @Test
    void teachingCaseDeleteClearsBothPrivateAndPlatformScopes() {
        service.deleteTeachingCase(1001L, 77L);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ObjectNode> payloadCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(aiAgentClient, times(2)).syncIndex(pathCaptor.capture(), payloadCaptor.capture());

        assertThat(pathCaptor.getAllValues())
                .containsOnly("/agent/index/delete", "/agent/index/delete");
        List<ObjectNode> payloads = payloadCaptor.getAllValues();
        assertThat(payloads)
                .extracting(payload -> payload.path("scope").asText())
                .containsExactly("mine", "platform");
        assertThat(payloads.get(0).path("teacherId").asLong()).isEqualTo(1001L);
        assertThat(payloads.get(1).has("teacherId")).isFalse();
        assertThat(payloads)
                .allSatisfy(payload -> {
                    assertThat(payload.path("sourceType").asText()).isEqualTo("case");
                    assertThat(payload.path("sourceId").asText()).isEqualTo("77");
                });
    }
}
