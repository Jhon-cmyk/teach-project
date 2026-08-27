package com.ruyi.teach.service;

import com.ruyi.teach.client.AiAgentClient;
import com.ruyi.teach.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentWorkflowStateSyncServiceTest {

    @Test
    void externalStateSyncFailureDoesNotRollBackSavedResource() {
        AiAgentClient client = mock(AiAgentClient.class);
        AgentWorkflowStateSyncService service =
                new AgentWorkflowStateSyncService(client);
        doThrow(new ExternalServiceException("AI agent", "unavailable"))
                .when(client)
                .markWorkflowSaved(
                        "0123456789abcdef0123456789abcdef",
                        1001L,
                        88L
                );

        assertThatCode(() -> service.markSaved(
                "0123456789abcdef0123456789abcdef",
                1001L,
                88L
        )).doesNotThrowAnyException();

        verify(client).markWorkflowSaved(
                "0123456789abcdef0123456789abcdef",
                1001L,
                88L
        );
    }
}
