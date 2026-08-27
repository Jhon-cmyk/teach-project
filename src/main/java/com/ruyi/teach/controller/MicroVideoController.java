package com.ruyi.teach.controller;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.AiAgentClient;
import com.ruyi.teach.client.RemoteResourceClient;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.common.TraceContext;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.MicroCourseTaskMapper;
import com.ruyi.teach.model.dto.MicroPublishRequest;
import com.ruyi.teach.model.dto.MicroRenderRequest;
import com.ruyi.teach.model.dto.MicroScriptRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.MicroCourseTask;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AgentIndexService;
import com.ruyi.teach.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/ai/micro-video")
@CrossOrigin(origins = {"http://localhost:5173", "http://39.105.66.116"}, allowCredentials = "true")
@Tag(name = "AI Micro Video")
@Slf4j
public class MicroVideoController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ALIYUN_NLS_TOKEN_DOMAIN = "nls-meta.cn-shanghai.aliyuncs.com";
    private static final String ALIYUN_NLS_TOKEN_VERSION = "2019-02-28";
    private static final String ALIYUN_NLS_REGION = "cn-shanghai";
    private static final long TOKEN_REFRESH_WINDOW_SECONDS = 300;

    private final ExecutorService renderExecutor = Executors.newFixedThreadPool(2);
    @Resource
    private MicroCourseTaskMapper microCourseTaskMapper;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private AgentIndexService agentIndexService;

    @Resource
    private OssService ossService;

    @Resource
    private AiAgentClient aiAgentClient;

    @Resource
    private RemoteResourceClient remoteResourceClient;

    @Value("${aliyun.asr.app-key:}")
    private String aliyunAsrAppKey;

    @Value("${aliyun.asr.access-key-id:}")
    private String aliyunAsrAccessKeyId;

    @Value("${aliyun.asr.access-key-secret:}")
    private String aliyunAsrAccessKeySecret;

    @Value("${aliyun.tts.app-key:}")
    private String aliyunTtsAppKey;

    @Value("${aliyun.tts.access-key-id:}")
    private String aliyunTtsAccessKeyId;

    @Value("${aliyun.tts.access-key-secret:}")
    private String aliyunTtsAccessKeySecret;

    @Value("${aliyun.tts.endpoint:https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/tts}")
    private String aliyunTtsEndpoint;

    private volatile String cachedAliyunTtsToken;
    private volatile long cachedAliyunTtsTokenExpireEpochSeconds;

    @Operation(summary = "Stream micro video script generation")
    @PostMapping("/script")
    public void generateScript(@Valid @RequestBody MicroScriptRequest req,
                               HttpServletRequest servletRequest,
                               HttpServletResponse servletResponse) {
        User loginUser = getLoginTeacher(servletRequest);

        servletResponse.setContentType("application/x-ndjson;charset=UTF-8");
        servletResponse.setCharacterEncoding("UTF-8");
        servletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        servletResponse.setHeader("X-Accel-Buffering", "no");

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("agentType", "micro_video");
        payload.put("teacherId", loginUser.getId());
        payload.set("form", OBJECT_MAPPER.valueToTree(req));
        ObjectNode retrievalOptions = payload.putObject("retrievalOptions");
        retrievalOptions.put("mode", "auto");
        retrievalOptions.put("topK", 3);

        try {
            aiAgentClient.streamPrepare(payload, line -> {
                try {
                    servletResponse.getWriter().write(line);
                    servletResponse.getWriter().write("\n");
                    servletResponse.getWriter().flush();
                } catch (Exception ignored) {
                }
            });
        } catch (Exception e) {
            log.error("Micro video script request failed", e);
            writeEvent(servletResponse, "error", "微课脚本生成服务暂时不可用，请稍后重试");
        }
    }

    @Operation(summary = "Submit async micro video render task")
    @PostMapping("/render")
    public BaseResponse<MicroCourseTask> render(@Valid @RequestBody MicroRenderRequest req, HttpServletRequest request) {
        User loginUser = getLoginTeacher(request);
        if (req == null || StringUtils.isBlank(req.getScriptJson())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "scriptJson is required");
        }

        MicroCourseTask task = new MicroCourseTask();
        task.setTeacherId(loginUser.getId());
        task.setStatus("queued");
        task.setProgress(0);
        task.setTitle(extractTitle(req.getScriptJson(), req.getTitle()));
        task.setScriptJson(req.getScriptJson());
        task.setParamsJson(writeJson(req));
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        microCourseTaskMapper.insert(task);

        String traceId = TraceContext.currentTraceId();
        renderExecutor.submit(() -> {
            TraceContext.bind(traceId);
            try {
                runRenderTask(task.getId(), loginUser.getId(), req);
            } finally {
                TraceContext.clear();
            }
        });
        return ResultUtils.success(task);
    }

    @Operation(summary = "Get micro video render task")
    @GetMapping("/task/{id}")
    public BaseResponse<MicroCourseTask> task(@PathVariable Long id, HttpServletRequest request) {
        Long teacherId = getLoginTeacher(request).getId();
        MicroCourseTask task = microCourseTaskMapper.selectById(id);
        if (task == null || !teacherId.equals(task.getTeacherId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "task not found");
        }
        return ResultUtils.success(task);
    }

    @Operation(summary = "Save micro video to resources")
    @PostMapping("/save")
    public BaseResponse<MicroCourseTask> save(@Valid @RequestBody MicroPublishRequest req, HttpServletRequest request) {
        User loginUser = getLoginTeacher(request);
        if (req == null || req.getTaskId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "taskId is required");
        }

        MicroCourseTask task = microCourseTaskMapper.selectById(req.getTaskId());
        if (task == null || !loginUser.getId().equals(task.getTeacherId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "task not found");
        }
        if (!"succeeded".equals(task.getStatus()) || StringUtils.isBlank(task.getVideoUrl())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "micro video render is not complete");
        }
        if (!isAudioValid(task)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "micro video audio is invalid, please render again");
        }

        if (task.getResourceId() != null) {
            AiResource existing = aiResourceMapper.selectById(task.getResourceId());
            if (existing != null && loginUser.getId().equals(existing.getTeacherId())) {
                return ResultUtils.success(task);
            }
        }

        AiResource resource = new AiResource();
        resource.setTeacherId(loginUser.getId());
        resource.setType("micro_video");
        resource.setTitle(StringUtils.defaultIfBlank(req.getTitle(), task.getTitle()));
        resource.setContent(task.getScriptJson());
        ObjectNode params = OBJECT_MAPPER.createObjectNode();
        params.put("videoUrl", task.getVideoUrl());
        params.put("coverUrl", StringUtils.defaultString(task.getCoverUrl()));
        params.put("subtitleUrl", StringUtils.defaultString(task.getSubtitleUrl()));
        params.put("audioUrl", StringUtils.defaultString(task.getAudioUrl()));
        params.put("durationSeconds", task.getDurationSeconds() == null ? 0 : task.getDurationSeconds());
        params.put("taskId", task.getId());
        params.put("duration", StringUtils.defaultString(req.getDuration()));
        params.put("publishMode", "resource_only");
        params.put("warningsJson", StringUtils.defaultString(task.getWarningsJson(), "[]"));
        params.put("renderStatsJson", StringUtils.defaultString(task.getRenderStatsJson(), "{}"));
        resource.setParamsJson(params.toString());
        resource.setIsPublished(0);
        resource.setIsDelete(0);
        resource.setSourceType("micro_video_task");
        resource.setSourceId(task.getId());
        aiResourceMapper.insert(resource);
        agentIndexService.upsertAiResource(resource);
        MicroCourseTask update = new MicroCourseTask();
        update.setId(task.getId());
        update.setResourceId(resource.getId());
        microCourseTaskMapper.updateById(update);

        return ResultUtils.success(microCourseTaskMapper.selectById(task.getId()));
    }

    @Operation(summary = "Legacy publish endpoint, save to resources only")
    @PostMapping("/publish")
    public BaseResponse<MicroCourseTask> publish(@Valid @RequestBody MicroPublishRequest req, HttpServletRequest request) {
        return save(req, request);
    }

    private void runRenderTask(Long taskId, Long teacherId, MicroRenderRequest req) {
        updateTask(taskId, "running", 10, null, null, null, null, null, null, null, null, null, null);
        try {
            ObjectNode payload = OBJECT_MAPPER.createObjectNode();
            payload.put("taskId", taskId);
            payload.put("teacherId", teacherId);
            payload.put("scriptJson", req.getScriptJson());
            ObjectNode options = OBJECT_MAPPER.valueToTree(req);
            enrichAliyunTtsOptions(options);
            payload.set("options", options);

            JsonNode root = aiAgentClient.renderMicroVideo(payload);
            JsonNode data = root.has("data") ? root.get("data") : root;
            String status = data.path("status").asText("succeeded");
            if (!"succeeded".equals(status)) {
                throw new IllegalStateException(data.path("errorMessage").asText("render failed"));
            }
            ObjectNode storedData = uploadRenderedAssetsToOss(taskId, data);
            updateTask(
                    taskId,
                    "succeeded",
                    100,
                    storedData.path("videoUrl").asText(""),
                    storedData.path("coverUrl").asText(""),
                    storedData.path("subtitleUrl").asText(""),
                    storedData.path("audioUrl").asText(""),
                    storedData.path("durationSeconds").asInt(0),
                    storedData.has("warnings") ? storedData.get("warnings").toString() : "[]",
                    storedData.has("renderStats") ? storedData.get("renderStats").toString() : "{}",
                    storedData.path("scriptJson").asText(null),
                    writeRenderParams(req, storedData),
                    null
            );
        } catch (Exception e) {
            log.error("Micro video render task failed, taskId={}", taskId, e);
            updateTask(taskId, "failed", 100, null, null, null, null, null, null, null, null, null,
                    "微课渲染服务暂时不可用，请稍后重试");
        }
    }

    private ObjectNode uploadRenderedAssetsToOss(Long taskId, JsonNode data) {
        ObjectNode storedData = data != null && data.isObject()
                ? ((ObjectNode) data).deepCopy()
                : OBJECT_MAPPER.createObjectNode();
        String dir = "micro-video/" + taskId;

        storedData.put("videoUrl", uploadRenderedAsset(
                storedData.path("videoUrl").asText(""),
                dir,
                "micro_course.mp4",
                "video/mp4",
                true
        ));
        putUploadedOptionalAsset(storedData, "coverUrl", dir, "cover.jpg", "image/jpeg");
        putUploadedOptionalAsset(storedData, "subtitleUrl", dir, "subtitle.srt", "application/x-subrip");
        putUploadedOptionalAsset(storedData, "audioUrl", dir, "narration.wav", "audio/wav");

        return storedData;
    }

    private void putUploadedOptionalAsset(ObjectNode data,
                                          String fieldName,
                                          String dir,
                                          String fallbackFilename,
                                          String fallbackContentType) {
        String sourceUrl = data.path(fieldName).asText("");
        if (StringUtils.isBlank(sourceUrl)) {
            return;
        }
        data.put(fieldName, uploadRenderedAsset(sourceUrl, dir, fallbackFilename, fallbackContentType, false));
    }

    private String uploadRenderedAsset(String sourceUrl,
                                       String dir,
                                       String fallbackFilename,
                                       String fallbackContentType,
                                       boolean required) {
        if (StringUtils.isBlank(sourceUrl)) {
            if (required) {
                throw new IllegalStateException("rendered asset url is empty: " + fallbackFilename);
            }
            return "";
        }
        if (!StringUtils.startsWithIgnoreCase(sourceUrl, "http://")
                && !StringUtils.startsWithIgnoreCase(sourceUrl, "https://")) {
            if (required) {
                throw new IllegalStateException("rendered asset url is invalid: " + sourceUrl);
            }
            return sourceUrl;
        }

        try {
            return remoteResourceClient.withStream(
                    "micro-video-asset",
                    sourceUrl,
                    Duration.ofMinutes(5),
                    remote -> {
                        String filename = filenameFromUrl(sourceUrl, fallbackFilename);
                        String contentType = StringUtils.substringBefore(remote.contentType(), ";").trim();
                        if (StringUtils.isBlank(contentType)) {
                            contentType = fallbackContentType;
                        }
                        return ossService.uploadStream(
                                remote.body(),
                                filename,
                                dir,
                                contentType,
                                remote.contentLength()
                        );
                    }
            );
        } catch (Exception e) {
            throw new IllegalStateException("upload rendered asset to OSS failed: " + fallbackFilename + ", " + e.getMessage(), e);
        }
    }

    private String filenameFromUrl(String url, String fallbackFilename) {
        try {
            String path = URI.create(url).getPath();
            String filename = StringUtils.substringAfterLast(path, "/");
            if (StringUtils.isBlank(filename)) {
                return fallbackFilename;
            }
            return URLDecoder.decode(filename, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return fallbackFilename;
        }
    }

    private void updateTask(Long id,
                            String status,
                            Integer progress,
                            String videoUrl,
                            String coverUrl,
                            String subtitleUrl,
                            String audioUrl,
                            Integer durationSeconds,
                            String warningsJson,
                            String renderStatsJson,
                            String scriptJson,
                            String paramsJson,
                            String errorMessage) {
        MicroCourseTask update = new MicroCourseTask();
        update.setId(id);
        update.setStatus(status);
        update.setProgress(progress);
        update.setVideoUrl(videoUrl);
        update.setCoverUrl(coverUrl);
        update.setSubtitleUrl(subtitleUrl);
        update.setAudioUrl(audioUrl);
        update.setDurationSeconds(durationSeconds);
        update.setWarningsJson(warningsJson);
        update.setRenderStatsJson(renderStatsJson);
        if (StringUtils.isNotBlank(scriptJson)) {
            update.setScriptJson(scriptJson);
        }
        update.setParamsJson(paramsJson);
        update.setErrorMessage(errorMessage);
        microCourseTaskMapper.updateById(update);
    }

    private String writeRenderParams(MicroRenderRequest req, JsonNode data) {
        ObjectNode params = OBJECT_MAPPER.createObjectNode();
        params.set("request", OBJECT_MAPPER.valueToTree(req));
        params.put("videoUrl", data.path("videoUrl").asText(""));
        params.put("coverUrl", data.path("coverUrl").asText(""));
        params.put("subtitleUrl", data.path("subtitleUrl").asText(""));
        params.put("audioUrl", data.path("audioUrl").asText(""));
        params.put("durationSeconds", data.path("durationSeconds").asInt(0));
        params.set("warnings", data.has("warnings") ? data.get("warnings") : OBJECT_MAPPER.createArrayNode());
        params.set("renderStats", data.has("renderStats") ? data.get("renderStats") : OBJECT_MAPPER.createObjectNode());
        return params.toString();
    }

    private String extractRenderError(String responseBody) {
        if (StringUtils.isBlank(responseBody)) {
            return "empty response";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            String message = root.path("data").path("errorMessage").asText("");
            if (StringUtils.isBlank(message)) {
                message = root.path("errorMessage").asText("");
            }
            if (StringUtils.isBlank(message)) {
                message = root.path("msg").asText("");
            }
            return StringUtils.defaultIfBlank(message, responseBody);
        } catch (Exception ignored) {
            return StringUtils.abbreviate(responseBody, 1000);
        }
    }

    private void enrichAliyunTtsOptions(ObjectNode options) {
        String appKey = resolveAliyunTtsAppKey();
        String accessKeyId = resolveAliyunTtsAccessKeyId();
        String accessKeySecret = resolveAliyunTtsAccessKeySecret();
        if (StringUtils.isAnyBlank(appKey, accessKeyId, accessKeySecret)) {
            throw new IllegalStateException("Aliyun NLS config is missing. Set aliyun.asr app-key/access-key-id/access-key-secret in application.yml.");
        }

        ObjectNode aliyunTts = options.putObject("aliyunTts");
        aliyunTts.put("appKey", appKey);
        aliyunTts.put("token", getCachedAliyunTtsToken(accessKeyId, accessKeySecret));
        aliyunTts.put("endpoint", aliyunTtsEndpoint);
        options.put("ttsProvider", "aliyun");
    }

    private String resolveAliyunTtsAppKey() {
        return StringUtils.defaultIfBlank(aliyunTtsAppKey, aliyunAsrAppKey);
    }

    private String resolveAliyunTtsAccessKeyId() {
        return StringUtils.defaultIfBlank(aliyunTtsAccessKeyId, aliyunAsrAccessKeyId);
    }

    private String resolveAliyunTtsAccessKeySecret() {
        return StringUtils.defaultIfBlank(aliyunTtsAccessKeySecret, aliyunAsrAccessKeySecret);
    }

    private String getCachedAliyunTtsToken(String accessKeyId, String accessKeySecret) {
        long now = Instant.now().getEpochSecond();
        if (StringUtils.isNotBlank(cachedAliyunTtsToken)
                && cachedAliyunTtsTokenExpireEpochSeconds - now > TOKEN_REFRESH_WINDOW_SECONDS) {
            return cachedAliyunTtsToken;
        }
        synchronized (this) {
            now = Instant.now().getEpochSecond();
            if (StringUtils.isNotBlank(cachedAliyunTtsToken)
                    && cachedAliyunTtsTokenExpireEpochSeconds - now > TOKEN_REFRESH_WINDOW_SECONDS) {
                return cachedAliyunTtsToken;
            }
            return fetchAliyunTtsToken(accessKeyId, accessKeySecret);
        }
    }

    private String fetchAliyunTtsToken(String accessKeyId, String accessKeySecret) {
        try {
            DefaultProfile profile = DefaultProfile.getProfile(ALIYUN_NLS_REGION, accessKeyId, accessKeySecret);
            IAcsClient client = new DefaultAcsClient(profile);
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain(ALIYUN_NLS_TOKEN_DOMAIN);
            request.setSysVersion(ALIYUN_NLS_TOKEN_VERSION);
            request.setSysAction("CreateToken");
            CommonResponse response = client.getCommonResponse(request);
            JsonNode root = OBJECT_MAPPER.readTree(response.getData());
            JsonNode tokenNode = root.path("Token");
            String token = tokenNode.path("Id").asText("");
            long expireTime = tokenNode.path("ExpireTime").asLong(0);
            if (StringUtils.isBlank(token) || expireTime <= 0) {
                throw new IllegalStateException("Aliyun NLS CreateToken response is invalid: " + response.getData());
            }
            cachedAliyunTtsToken = token;
            cachedAliyunTtsTokenExpireEpochSeconds = expireTime;
            return token;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Aliyun NLS token: " + e.getMessage(), e);
        }
    }

    private boolean isAudioValid(MicroCourseTask task) {
        if (task == null || StringUtils.isBlank(task.getAudioUrl())) {
            return false;
        }
        if (StringUtils.isBlank(task.getRenderStatsJson())) {
            return true;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(task.getRenderStatsJson());
            JsonNode audio = root.path("audio");
            return !audio.has("valid") || audio.path("valid").asBoolean(true);
        } catch (Exception ignored) {
            return true;
        }
    }

    private User getLoginTeacher(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "not logged in");
        }
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "teacher only");
        }
        return loginUser;
    }

    private String extractTitle(String scriptJson, String fallback) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(scriptJson);
            return StringUtils.defaultIfBlank(root.path("title").asText(), fallback);
        } catch (Exception ignored) {
            return StringUtils.defaultIfBlank(fallback, "AI micro video");
        }
    }

    private String extractSummary(String scriptJson) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(scriptJson);
            return StringUtils.defaultIfBlank(root.path("summary").asText(), "AI generated micro video");
        } catch (Exception ignored) {
            return "AI generated micro video";
        }
    }

    private String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private void writeEvent(HttpServletResponse response, String type, String message) {
        try {
            ObjectNode node = OBJECT_MAPPER.createObjectNode();
            node.put("type", type);
            node.put("message", message);
            response.getWriter().write(node.toString());
            response.getWriter().write("\n");
            response.getWriter().flush();
        } catch (Exception ignored) {
        }
    }

    @PreDestroy
    public void shutdown() {
        renderExecutor.shutdownNow();
    }

}
