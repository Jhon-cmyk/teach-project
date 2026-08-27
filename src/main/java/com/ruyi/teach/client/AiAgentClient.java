package com.ruyi.teach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.exception.ExternalServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class AiAgentClient {

    private static final String SERVICE_NAME = "AI agent";
    private static final String PUBLIC_MESSAGE = "AI Agent 服务暂时不可用，请稍后重试";

    private final ExternalHttpClient httpClient;
    private final ExternalClientProperties properties;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String deepSeekApiKey;

    public AiAgentClient(ExternalHttpClient httpClient,
                         ExternalClientProperties properties,
                         ObjectMapper objectMapper,
                         @Value("${ai-agent.base-url:http://localhost:5000}") String baseUrl,
                         @Value("${deepseek.api-key:}") String deepSeekApiKey) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.baseUrl = StringUtils.removeEnd(baseUrl, "/");
        this.deepSeekApiKey = deepSeekApiKey;
    }

    public void streamPrepare(ObjectNode payload, Consumer<String> lineConsumer) {
        Map<String, String> headers = StringUtils.isBlank(deepSeekApiKey)
                ? Map.of()
                : Map.of("X-DeepSeek-Key", deepSeekApiKey);
        httpClient.postJsonLines(
                SERVICE_NAME,
                uri("/agent/prepare/stream"),
                payload.toString(),
                headers,
                properties.getAiAgentTimeout(),
                PUBLIC_MESSAGE,
                lineConsumer
        );
    }

    public JsonNode renderMicroVideo(ObjectNode payload) {
        String body = httpClient.postJson(
                SERVICE_NAME,
                uri("/micro-video/render"),
                payload.toString(),
                Map.of(),
                properties.getAiAgentTimeout(),
                ExternalHttpClient.RetryPolicy.NEVER,
                "微课渲染服务暂时不可用，请稍后重试"
        );
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ExternalServiceException(
                    SERVICE_NAME,
                    "微课渲染服务返回了无效数据",
                    e
            );
        }
    }

    public void syncIndex(String path, ObjectNode payload) {
        httpClient.postJson(
                SERVICE_NAME,
                uri(path),
                payload.toString(),
                Map.of(),
                properties.getDefaultReadTimeout(),
                ExternalHttpClient.RetryPolicy.TRANSIENT,
                "AI 索引同步服务暂时不可用"
        );
    }

    public void markWorkflowSaved(String requestId, Long teacherId, Long resourceId) {
        if (StringUtils.isBlank(requestId)
                || !requestId.matches("^[a-f0-9]{32}$")
                || teacherId == null
                || resourceId == null) {
            return;
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("teacherId", teacherId);
        payload.put("resourceId", resourceId);
        payload.put("confirmed", true);
        httpClient.postJson(
                SERVICE_NAME,
                uri("/agent/runs/" + requestId + "/saved"),
                payload.toString(),
                Map.of(),
                properties.getDefaultReadTimeout(),
                ExternalHttpClient.RetryPolicy.NEVER,
                "AI 工作流状态同步失败"
        );
    }

    public JsonNode getWorkflowRun(String requestId, Long teacherId) {
        if (StringUtils.isBlank(requestId)
                || !requestId.matches("^[a-f0-9]{32}$")
                || teacherId == null
                || teacherId <= 0) {
            throw new IllegalArgumentException("Invalid workflow query");
        }
        String body = httpClient.getString(
                SERVICE_NAME,
                uri("/agent/runs/" + requestId + "?teacherId=" + teacherId),
                Map.of(),
                properties.getDefaultReadTimeout(),
                ExternalHttpClient.RetryPolicy.NEVER,
                "AI 工作流记录暂时不可用"
        );
        try {
            JsonNode response = objectMapper.readTree(body);
            if (response.path("code").asInt(-1) != 0 || response.path("data").isMissingNode()) {
                throw new ExternalServiceException(
                        SERVICE_NAME,
                        "AI 工作流记录暂时不可用"
                );
            }
            return response.path("data");
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(
                    SERVICE_NAME,
                    "AI 工作流记录暂时不可用",
                    e
            );
        }
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }
}
