package com.ruyi.teach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.config.XfyunKnowledgeBaseProperties;
import com.ruyi.teach.exception.ExternalServiceException;
import com.ruyi.teach.model.vo.knowledge.KnowledgeFileVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeRepositoryVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeSearchHitVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class XfyunKnowledgeBaseClient {

    private static final String SERVICE_NAME = "Xfyun knowledge base";
    private static final long VECTOR_STATUS_POLL_INTERVAL_MILLIS = 1_200L;
    private static final long VECTOR_STATUS_MAX_WAIT_MILLIS = 60_000L;
    private static final String PUBLIC_MESSAGE = "星火知识库服务暂时不可用，请稍后重试";

    private final XfyunKnowledgeBaseProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public XfyunKnowledgeBaseClient(XfyunKnowledgeBaseProperties properties,
                                    ObjectMapper objectMapper,
                                    ExternalClientProperties externalClientProperties) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(externalClientProperties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    XfyunKnowledgeBaseClient(XfyunKnowledgeBaseProperties properties,
                             ObjectMapper objectMapper,
                             HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public KnowledgeRepositoryVO createRepository(String name, String description, String tags) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("repoName", StringUtils.trim(name));
        payload.put("repoDesc", StringUtils.trimToEmpty(description));
        payload.put("repoTags", StringUtils.trimToEmpty(tags));
        JsonNode root = postJson("/openapi/v1/repo/create", payload);
        return new KnowledgeRepositoryVO(
                root.path("data").asText(),
                StringUtils.trim(name),
                StringUtils.trimToEmpty(description),
                StringUtils.trimToEmpty(tags),
                null
        );
    }

    public List<KnowledgeRepositoryVO> listRepositories() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("currentPage", 1);
        payload.put("pageSize", 100);
        JsonNode data = postJson("/openapi/v1/repo/list", payload).path("data");
        JsonNode records = data.isArray() ? data : firstArray(data, "rows", "records", "list", "items");
        List<KnowledgeRepositoryVO> result = new ArrayList<>();
        if (records != null && records.isArray()) {
            for (JsonNode item : records) {
                result.add(new KnowledgeRepositoryVO(
                        text(item, "repoId"), text(item, "repoName"), text(item, "repoDesc"),
                        text(item, "repoTags"), text(item, "createTime")
                ));
            }
        }
        result.sort(Comparator.comparing(KnowledgeRepositoryVO::createTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public List<KnowledgeFileVO> listFiles(String repoId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("repoId", requireText(repoId, "知识库 ID 不能为空"));
        payload.put("currentPage", 1);
        payload.put("pageSize", 100);
        JsonNode data = postJson("/openapi/v1/repo/file/list", payload).path("data");
        JsonNode records = data.isArray() ? data : firstArray(data, "rows", "records", "list", "items");
        List<KnowledgeFileVO> result = new ArrayList<>();
        if (records != null && records.isArray()) {
            for (JsonNode item : records) {
                result.add(new KnowledgeFileVO(
                        text(item, "fileId"), text(item, "fileName"), text(item, "fileType"),
                        text(item, "fileStatus"), text(item, "extName"), nullableInt(item, "quantity"),
                        text(item, "expirationStatus"), text(item, "createTime"), text(item, "expireTime")
                ));
            }
        }
        return result;
    }

    public KnowledgeUploadVO uploadAndAddToRepository(String repoId,
                                                       String fileName,
                                                       byte[] bytes,
                                                       String contentType) {
        requireConfigured();
        String safeRepoId = requireText(repoId, "知识库 ID 不能为空");
        String safeFileName = requireText(fileName, "文件名称不能为空");
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String boundary = "----TeachKnowledge" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = multipartBody(boundary, Map.of(
                "parseType", "AUTO",
                "needSummary", "false"
        ), "file", safeFileName, StringUtils.defaultIfBlank(contentType, "application/octet-stream"), bytes);
        JsonNode root = send("/openapi/v1/file/upload", "multipart/form-data; boundary=" + boundary, body);
        JsonNode data = root.path("data");
        String fileId = data.path("fileId").asText();
        if (fileId.isBlank()) {
            throw new ExternalServiceException(SERVICE_NAME, PUBLIC_MESSAGE);
        }

        waitUntilVectored(fileId);

        ObjectNode addPayload = objectMapper.createObjectNode();
        addPayload.put("repoId", safeRepoId);
        addPayload.putArray("fileIds").add(fileId);
        JsonNode addResult = postJson("/openapi/v1/repo/file/add", addPayload).path("data");
        if (addResult.path("failedList").isArray()) {
            for (JsonNode failed : addResult.path("failedList")) {
                if (fileId.equals(failed.asText())) {
                    log.warn("Xfyun file uploaded but repository binding failed, repoId={}, fileId={}",
                            safeRepoId, fileId);
                    throw new ExternalServiceException(SERVICE_NAME, "资料已上传但加入知识库失败，请稍后重试");
                }
            }
        }
        return new KnowledgeUploadVO(fileId, safeFileName, data.path("parseType").asText("AUTO"), "vectored");
    }

    private void waitUntilVectored(String fileId) {
        long deadline = System.nanoTime() + VECTOR_STATUS_MAX_WAIT_MILLIS * 1_000_000L;
        String latestStatus = "uploaded";
        while (System.nanoTime() < deadline) {
            latestStatus = getFileStatuses(List.of(fileId)).getOrDefault(fileId, latestStatus);
            if ("vectored".equalsIgnoreCase(latestStatus)) {
                return;
            }
            if ("failed".equalsIgnoreCase(latestStatus)) {
                throw new ExternalServiceException(SERVICE_NAME, "资料解析失败，请检查文件格式后重新上传");
            }
            try {
                Thread.sleep(VECTOR_STATUS_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExternalServiceException(SERVICE_NAME, "资料解析等待已中断，请稍后重试");
            }
        }
        log.warn("Xfyun file vectorization timed out, fileId={}, latestStatus={}", fileId, latestStatus);
        throw new ExternalServiceException(SERVICE_NAME, "资料已上传，向量化仍在处理中，请稍后在知识库中刷新状态");
    }

    public Map<String, String> getFileStatuses(List<String> fileIds) {
        List<String> safeIds = fileIds == null ? List.of() : fileIds.stream()
                .filter(StringUtils::isNotBlank).distinct().limit(100).toList();
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        JsonNode data = postMultipartFields("/openapi/v1/file/status", Map.of(
                "fileIds", String.join(",", safeIds)
        )).path("data");
        Map<String, String> result = new HashMap<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                result.put(text(item, "fileId"), text(item, "fileStatus"));
            }
        }
        return result;
    }

    public void deleteFile(String fileId) {
        postMultipartFields("/openapi/v1/file/del", Map.of(
                "fileIds", requireText(fileId, "文件 ID 不能为空")
        ));
    }

    public List<KnowledgeSearchHitVO> search(String repoId, String question) {
        if (!isConfigured() || StringUtils.isBlank(repoId) || StringUtils.isBlank(question)) {
            return List.of();
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.putArray("repoIds").add(repoId);
        payload.put("topN", Math.max(1, Math.min(properties.getTopN(), 20)));
        payload.put("esTopN", Math.max(1, Math.min(properties.getEsTopN(), 20)));
        payload.put("content", question.trim());
        payload.put("es", true);
        payload.put("embedding", true);
        payload.put("reRank", true);
        payload.putObject("chatExtends").put("retrievalFilterPolicy", "STRICT");

        JsonNode data = postJson("/openapi/v1/vector/search", payload).path("data");
        List<KnowledgeSearchHitVO> result = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                double score = normalizeScore(item.path("score").asDouble(0D));
                if (StringUtils.isBlank(text(item, "content")) || score < properties.getMinimumScore()) {
                    continue;
                }
                result.add(new KnowledgeSearchHitVO(
                        text(item, "content"), score, text(item, "fileId"), text(item, "fileName"),
                        nullableInt(item, "index"), text(item, "type"), text(item, "fileType")
                ));
            }
        }
        result.sort(Comparator.comparingDouble(KnowledgeSearchHitVO::score).reversed());
        return result.stream().limit(Math.max(1, properties.getTopN())).toList();
    }

    private JsonNode postJson(String path, JsonNode payload) {
        return send(path, "application/json", payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode postMultipartFields(String path, Map<String, String> fields) {
        String boundary = "----TeachKnowledge" + UUID.randomUUID().toString().replace("-", "");
        return send(path, "multipart/form-data; boundary=" + boundary,
                multipartBody(boundary, fields, null, null, null, null));
    }

    private JsonNode send(String path, String contentType, byte[] body) {
        requireConfigured();
        try {
            long timestamp = Instant.now().getEpochSecond();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(StringUtils.removeEnd(properties.getBaseUrl(), "/") + path))
                    .timeout(properties.getTimeout())
                    .header("Content-Type", contentType)
                    .header("Accept", "application/json")
                    .header("appId", properties.getAppId())
                    .header("timeStamp", String.valueOf(timestamp))
                    .header("signature", signature(properties.getAppId(), properties.getSecret(), timestamp))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Xfyun knowledge request failed, path={}, status={}", path, response.statusCode());
                throw new ExternalServiceException(SERVICE_NAME, PUBLIC_MESSAGE);
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("code").asInt(-1) != 0) {
                log.warn("Xfyun knowledge business error, path={}, code={}, sid={}, desc={}",
                        path, root.path("code").asInt(), root.path("sid").asText(), root.path("desc").asText());
                throw new ExternalServiceException(SERVICE_NAME,
                        StringUtils.defaultIfBlank(root.path("desc").asText(), PUBLIC_MESSAGE));
            }
            return root;
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Xfyun knowledge request unavailable, path={}, cause={}", path, e.getClass().getSimpleName());
            throw new ExternalServiceException(SERVICE_NAME, PUBLIC_MESSAGE, e);
        }
    }

    static String signature(String appId, String secret, long timestamp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest((appId + timestamp).getBytes(StandardCharsets.UTF_8));
            StringBuilder auth = new StringBuilder(md5.length * 2);
            for (byte value : md5) {
                auth.append(String.format("%02x", value & 0xff));
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(auth.toString().getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException("生成星火知识库签名失败", e);
        }
    }

    private byte[] multipartBody(String boundary,
                                 Map<String, String> fields,
                                 String fileField,
                                 String fileName,
                                 String fileContentType,
                                 byte[] fileBytes) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                writeUtf8(output, "--" + boundary + "\r\n");
                writeUtf8(output, "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                writeUtf8(output, entry.getValue() + "\r\n");
            }
            if (fileField != null && fileBytes != null) {
                writeUtf8(output, "--" + boundary + "\r\n");
                writeUtf8(output, "Content-Disposition: form-data; name=\"" + fileField
                        + "\"; filename=\"" + escapeFileName(fileName) + "\"\r\n");
                writeUtf8(output, "Content-Type: " + fileContentType + "\r\n\r\n");
                output.write(fileBytes);
                writeUtf8(output, "\r\n");
            }
            writeUtf8(output, "--" + boundary + "--\r\n");
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("构造知识库上传请求失败", e);
        }
    }

    private void writeUtf8(ByteArrayOutputStream output, String text) throws Exception {
        output.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeFileName(String fileName) {
        return StringUtils.defaultIfBlank(fileName, "knowledge.md")
                .replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ExternalServiceException(SERVICE_NAME, "星火知识库尚未配置，请先设置后端环境变量");
        }
    }

    private String requireText(String value, String message) {
        if (StringUtils.isBlank(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private JsonNode firstArray(JsonNode parent, String... keys) {
        if (parent == null || parent.isMissingNode() || parent.isNull()) return null;
        for (String key : keys) {
            if (parent.path(key).isArray()) return parent.path(key);
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : StringUtils.trimToNull(value.asText());
    }

    private Integer nullableInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }

    private double normalizeScore(double score) {
        return score > 0D && score <= 1D ? score * 100D : score;
    }
}
