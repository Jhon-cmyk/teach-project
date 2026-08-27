package com.ruyi.teach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class AiModelClient {

    private static final String SERVICE_NAME = "AI model";
    private static final String PUBLIC_MESSAGE = "AI 服务暂时不可用，请稍后重试";

    private final ExternalHttpClient httpClient;
    private final ExternalClientProperties properties;
    private final ObjectMapper objectMapper;
    private final String deepSeekBaseUrl;
    private final String deepSeekApiKey;
    private final String deepSeekModel;

    public AiModelClient(ExternalHttpClient httpClient,
                         ExternalClientProperties properties,
                         ObjectMapper objectMapper,
                         @Value("${deepseek.base-url:https://api.deepseek.com/chat/completions}") String deepSeekBaseUrl,
                         @Value("${deepseek.api-key:}") String deepSeekApiKey,
                         @Value("${deepseek.model:deepseek-chat}") String deepSeekModel) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.deepSeekBaseUrl = deepSeekBaseUrl;
        this.deepSeekApiKey = deepSeekApiKey;
        this.deepSeekModel = deepSeekModel;
    }

    public String chat(String systemPrompt,
                       String userContent,
                       double temperature,
                       int maxTokens,
                       boolean jsonMode) {
        ObjectNode payload = buildPayload(
                deepSeekModel,
                systemPrompt,
                userContent,
                temperature,
                maxTokens,
                false,
                jsonMode
        );
        String body = httpClient.postJson(
                SERVICE_NAME,
                URI.create(deepSeekBaseUrl),
                payload.toString(),
                bearerHeader(deepSeekApiKey),
                properties.getAiModelTimeout(),
                ExternalHttpClient.RetryPolicy.NEVER,
                PUBLIC_MESSAGE
        );
        try {
            JsonNode content = objectMapper.readTree(body)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");
            if (content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
                throw new ExternalServiceException(SERVICE_NAME, PUBLIC_MESSAGE);
            }
            return content.asText();
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(SERVICE_NAME, PUBLIC_MESSAGE, e);
        }
    }

    public String streamChat(String systemPrompt,
                             String userContent,
                             double temperature,
                             int maxTokens,
                             boolean jsonMode,
                             Consumer<String> onChunk) {
        ObjectNode payload = buildPayload(
                deepSeekModel,
                systemPrompt,
                userContent,
                temperature,
                maxTokens,
                true,
                jsonMode
        );
        StringBuilder fullText = new StringBuilder();
        streamOpenAiCompatible(
                SERVICE_NAME,
                deepSeekBaseUrl,
                deepSeekApiKey,
                payload,
                chunk -> {
                    fullText.append(chunk);
                    if (onChunk != null) {
                        onChunk.accept(chunk);
                    }
                }
        );
        if (fullText.toString().isBlank()) {
            throw new ExternalServiceException(SERVICE_NAME, PUBLIC_MESSAGE);
        }
        return fullText.toString();
    }

    public void streamOpenAiCompatible(String serviceName,
                                       String endpoint,
                                       String apiKey,
                                       ObjectNode payload,
                                       Consumer<String> onChunk) {
        httpClient.postJsonLines(
                serviceName,
                URI.create(endpoint),
                payload.toString(),
                bearerHeader(apiKey),
                properties.getAiModelTimeout(),
                PUBLIC_MESSAGE,
                line -> consumeOpenAiStreamLine(serviceName, line, onChunk)
        );
    }

    private ObjectNode buildPayload(String model,
                                    String systemPrompt,
                                    String userContent,
                                    double temperature,
                                    int maxTokens,
                                    boolean stream,
                                    boolean jsonMode) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("stream", stream);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);
        if (jsonMode) {
            payload.putObject("response_format").put("type", "json_object");
        }
        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userContent);
        return payload;
    }

    private void consumeOpenAiStreamLine(String serviceName,
                                         String line,
                                         Consumer<String> onChunk) {
        if (line == null || !line.startsWith("data: ") || "data: [DONE]".equals(line)) {
            return;
        }
        try {
            JsonNode content = objectMapper.readTree(line.substring(6))
                    .path("choices")
                    .path(0)
                    .path("delta")
                    .path("content");
            if (!content.isMissingNode() && !content.isNull() && onChunk != null) {
                onChunk.accept(content.asText());
            }
        } catch (Exception e) {
            log.warn("AI stream chunk ignored, service={}, cause={}",
                    serviceName, e.getClass().getSimpleName());
        }
    }

    private Map<String, String> bearerHeader(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of();
        }
        return Map.of("Authorization", "Bearer " + apiKey);
    }
}
