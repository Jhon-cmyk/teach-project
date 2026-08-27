package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.AiModelClient;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

@Service
@Slf4j
public class TutorVisionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final AiModelClient aiModelClient;

    public TutorVisionService(AiModelClient aiModelClient) {
        this.aiModelClient = aiModelClient;
    }

    private static final String SYSTEM_PROMPT =
            "你是学生端的拍题讲解助手。学生会上传不会的题目截图，也可能补充一句问题。"
                    + "请先识别图片中的题干和关键信息，再用简体中文讲解。"
                    + "输出必须清晰简洁，固定包含：## 题目识别、## 解题思路、## 步骤讲解、## 易错点。"
                    + "如果图片不清晰或题干缺失，请先说明需要学生补充的信息，不要编造题目。"
                    + "讲解要帮助学生理解，不要只给最终答案。";

    @Value("${ai.vision.base-url:}")
    private String visionBaseUrl;

    @Value("${ai.vision.api-key:}")
    private String visionApiKey;

    @Value("${ai.vision.model:}")
    private String visionModel;

    @Value("${ai.tutor.image-max-size-mb:8}")
    private long imageMaxSizeMb;

    public void streamQuestionExplanation(MultipartFile file, String userPrompt, HttpServletResponse response) {
        prepareStreamResponse(response);

        try {
            validateImage(file);

            if (isBlank(visionBaseUrl) || isBlank(visionApiKey) || isBlank(visionModel)) {
                write(response, "图片解析模型尚未配置，请在后端配置 ai.vision.base-url、ai.vision.api-key 和 ai.vision.model。");
                return;
            }

            ObjectNode payload = buildPayload(file, userPrompt);
            aiModelClient.streamOpenAiCompatible(
                    "vision-model",
                    visionBaseUrl,
                    visionApiKey,
                    payload,
                    chunk -> write(response, chunk)
            );
        } catch (BusinessException e) {
            write(response, e.getMessage());
        } catch (Exception e) {
            log.error("Vision service request failed", e);
            write(response, "\n[AI_SERVICE_ERROR] 图片解析服务暂时不可用，请稍后重试");
        }
    }

    private ObjectNode buildPayload(MultipartFile file, String userPrompt) throws IOException {
        String contentType = file.getContentType();
        String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        String imageDataUrl = "data:" + contentType + ";base64," + base64Image;

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        payload.put("model", visionModel);
        payload.put("stream", true);
        payload.put("temperature", 0.35);
        payload.put("max_tokens", 3000);

        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        ArrayNode content = userMessage.putArray("content");
        content.addObject()
                .put("type", "text")
                .put("text", isBlank(userPrompt) ? "请解析这张题目截图，并给出讲解。" : userPrompt);
        ObjectNode image = content.addObject();
        image.put("type", "image_url");
        image.putObject("image_url").put("url", imageDataUrl);

        return payload;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先上传题目截图");
        }
        long maxBytes = imageMaxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能超过 " + imageMaxSizeMb + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法识别图片格式");
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (!normalized.equals("image/jpeg") && !normalized.equals("image/png") && !normalized.equals("image/webp")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 JPG、PNG、WebP 图片");
        }
    }

    private void prepareStreamResponse(HttpServletResponse response) {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private void write(HttpServletResponse response, String text) {
        try {
            response.getWriter().write(text);
            response.getWriter().flush();
        } catch (IOException ignored) {
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
