package com.ruyi.teach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * Judge0 沙箱客户端，封装提交代码、认证和轮询结果。
 */
@Slf4j
@Component
public class Judge0Client {

    private static final String SERVICE_NAME = "Judge0";
    private static final Map<String, Integer> LANGUAGE_MAP = Map.of(
            "java", 62,
            "python", 71,
            "cpp", 54,
            "javascript", 63
    );

    private final ExternalHttpClient httpClient;
    private final ExternalClientProperties properties;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String authToken;
    private final int defaultTimeoutMs;
    private final int defaultMemoryKb;

    public Judge0Client(ExternalHttpClient httpClient,
                        ExternalClientProperties properties,
                        ObjectMapper objectMapper,
                        @Value("${judge0.base-url:http://localhost:2358}") String baseUrl,
                        @Value("${judge0.auth-token:}") String authToken,
                        @Value("${judge0.default-timeout-ms:5000}") int defaultTimeoutMs,
                        @Value("${judge0.default-memory-kb:262144}") int defaultMemoryKb) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.baseUrl = StringUtils.removeEnd(baseUrl, "/");
        this.authToken = authToken;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.defaultMemoryKb = defaultMemoryKb;
    }

    public JudgeResult submitAndWait(String language,
                                     String sourceCode,
                                     String stdin,
                                     int timeoutMs,
                                     int memoryKb) {
        Integer languageId = LANGUAGE_MAP.get(language);
        if (languageId == null) {
            return JudgeResult.error("不支持的语言: " + language);
        }

        int effectiveTimeoutMs = timeoutMs > 0 ? timeoutMs : defaultTimeoutMs;
        int cpuTimeLimit = Math.max(1, effectiveTimeoutMs / 1000);
        int memoryLimit = memoryKb > 0 ? memoryKb : defaultMemoryKb;

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("language_id", languageId);
            payload.put("source_code", encodeBase64(sourceCode));
            if (stdin != null && !stdin.isEmpty()) {
                payload.put("stdin", encodeBase64(stdin));
            }
            payload.put("cpu_time_limit", cpuTimeLimit);
            payload.put("memory_limit", memoryLimit);
            payload.put("enable_network", false);

            String submitBody = httpClient.postJson(
                    SERVICE_NAME,
                    URI.create(baseUrl + "/submissions?base64_encoded=true&wait=true"),
                    payload.toString(),
                    authHeaders(),
                    waitRequestTimeout(effectiveTimeoutMs),
                    ExternalHttpClient.RetryPolicy.NEVER,
                    "代码沙箱暂时不可用"
            );
            JsonNode submitResult = objectMapper.readTree(submitBody);
            int submitStatusId = submitResult.path("status").path("id").asInt(0);
            if (submitStatusId != 0 && submitStatusId != 1 && submitStatusId != 2) {
                return parseResult(submitResult);
            }

            String token = submitResult.path("token").asText(null);
            if (StringUtils.isBlank(token)) {
                return JudgeResult.error("沙箱未返回 token");
            }

            URI pollUri = URI.create(baseUrl + "/submissions/" + token
                    + "?base64_encoded=true");
            for (int i = 0; i < 30; i++) {
                Thread.sleep(1000);
                String pollBody = httpClient.getString(
                        SERVICE_NAME,
                        pollUri,
                        authHeaders(),
                        properties.getJudge0Timeout(),
                        ExternalHttpClient.RetryPolicy.TRANSIENT,
                        "代码沙箱暂时不可用"
                );
                JsonNode result = objectMapper.readTree(pollBody);
                int statusId = result.path("status").path("id").asInt(0);
                if (statusId != 1 && statusId != 2) {
                    return parseResult(result);
                }
            }
            return JudgeResult.error("沙箱执行超时（轮询30秒无结果）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JudgeResult.error("沙箱执行被中断");
        } catch (ExternalServiceException e) {
            log.warn("Judge0 unavailable, cause={}",
                    e.getCause() == null ? e.getClass().getSimpleName() : e.getCause().getClass().getSimpleName());
            return JudgeResult.error("沙箱调用异常");
        } catch (Exception e) {
            log.error("Judge0 response parsing failed", e);
            return JudgeResult.error("沙箱返回数据异常");
        }
    }

    private JudgeResult parseResult(JsonNode result) {
        JsonNode statusNode = result.path("status");
        int statusId = statusNode.path("id").asInt(0);

        JudgeResult judgeResult = new JudgeResult();
        judgeResult.statusId = statusId;
        judgeResult.statusDescription = statusNode.path("description").asText("unknown");
        judgeResult.accepted = statusId == 3;
        judgeResult.stdout = decodeBase64(result.path("stdout").asText(null));
        judgeResult.stderr = decodeBase64(result.path("stderr").asText(null));
        judgeResult.compileOutput = decodeBase64(result.path("compile_output").asText(null));
        judgeResult.time = result.path("time").asText(null);
        judgeResult.memory = result.path("memory").asInt(0);
        judgeResult.exitCode = result.path("exit_code").asInt(-1);
        return judgeResult;
    }

    private Duration waitRequestTimeout(int effectiveTimeoutMs) {
        Duration configuredTimeout = properties.getJudge0Timeout();
        Duration executionTimeout = Duration.ofMillis((long) effectiveTimeoutMs + 30_000L);
        return configuredTimeout.compareTo(executionTimeout) >= 0
                ? configuredTimeout
                : executionTimeout;
    }

    private String encodeBase64(String value) {
        String safeValue = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(safeValue.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeBase64(String encodedValue) {
        if (StringUtils.isBlank(encodedValue)) {
            return null;
        }
        try {
            String normalizedValue = encodedValue.replaceAll("\\s+", "");
            return new String(Base64.getDecoder().decode(normalizedValue), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Judge0 returned an invalid Base64 text field");
            return encodedValue;
        }
    }

    private Map<String, String> authHeaders() {
        return StringUtils.isBlank(authToken)
                ? Map.of()
                : Map.of("X-Auth-Token", authToken);
    }

    public static Set<String> getSupportedLanguages() {
        return LANGUAGE_MAP.keySet();
    }

    public static boolean isLanguageSupported(String language) {
        return LANGUAGE_MAP.containsKey(language);
    }

    public static class JudgeResult {
        public boolean accepted;
        public int statusId;
        public String statusDescription;
        public String stdout;
        public String stderr;
        public String compileOutput;
        public String time;
        public int memory;
        public int exitCode;

        public static JudgeResult error(String message) {
            JudgeResult result = new JudgeResult();
            result.accepted = false;
            result.statusId = -1;
            result.statusDescription = message;
            return result;
        }
    }
}
