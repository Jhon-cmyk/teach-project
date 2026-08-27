package com.ruyi.teach.service;

import com.ruyi.teach.client.AiAgentClient;
import com.ruyi.teach.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AgentWorkflowStateSyncService {

    private final AiAgentClient aiAgentClient;

    public AgentWorkflowStateSyncService(AiAgentClient aiAgentClient) {
        this.aiAgentClient = aiAgentClient;
    }

    @Async("agentIndexExecutor")
    public void markSaved(String requestId, Long teacherId, Long resourceId) {
        try {
            aiAgentClient.markWorkflowSaved(requestId, teacherId, resourceId);
        } catch (ExternalServiceException e) {
            log.warn(
                    "AI workflow state sync failed after resource save, resourceId={}",
                    resourceId
            );
        }
    }
}
