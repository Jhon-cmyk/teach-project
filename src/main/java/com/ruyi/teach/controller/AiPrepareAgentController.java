package com.ruyi.teach.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.AiAgentClient;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.exception.ExternalServiceException;
import com.ruyi.teach.model.dto.ExportPlanRequest;
import com.ruyi.teach.model.dto.PrepareAgentRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AiPrepareContextService;
import com.ruyi.teach.service.LessonPlanExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@RestController
@RequestMapping("/ai/agent")
@CrossOrigin(origins = {"http://localhost:5173", "http://39.105.66.116"}, allowCredentials = "true")
@Tag(name = "AI prepare agents")
@Slf4j
public class AiPrepareAgentController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private AiPrepareContextService aiPrepareContextService;

    @Resource
    private AiAgentClient aiAgentClient;

    @Resource
    private LessonPlanExportService lessonPlanExportService;

    @Operation(summary = "Stream AI prepare agent output")
    @PostMapping("/stream")
    public void streamAgent(
            @Valid @RequestBody PrepareAgentRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        User loginUser = getLoginTeacher(servletRequest);
        prepareNdjsonResponse(servletResponse);
        ObjectNode payload = aiPrepareContextService.buildAgentPayload(loginUser.getId(), request);
        payload.put("actorRole", loginUser.getUserRole());
        payload.put("sessionId", sessionCorrelationId(servletRequest));

        try {
            aiAgentClient.streamPrepare(payload, line -> writeLine(servletResponse, line));
        } catch (ClientStreamDisconnectedException e) {
            log.info("AI prepare stream disconnected by client");
        } catch (ExternalServiceException e) {
            log.warn("AI prepare agent unavailable, service={}", e.getServiceName());
            writeEvent(servletResponse, "error", "AI 备课服务暂时不可用，请稍后重试");
        }
    }

    @Operation(summary = "Get an AI prepare workflow execution record")
    @GetMapping("/runs/{requestId}")
    public BaseResponse<JsonNode> getWorkflowRun(
            @PathVariable String requestId,
            HttpServletRequest servletRequest
    ) {
        User loginUser = getLoginTeacher(servletRequest);
        if (requestId == null || !requestId.matches("^[a-f0-9]{32}$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "invalid requestId");
        }
        return ResultUtils.success(
                aiAgentClient.getWorkflowRun(requestId, loginUser.getId())
        );
    }

    @Operation(summary = "Export generated lesson plan")
    @PostMapping("/export/plan")
    public void exportPlan(
            @Valid @RequestBody ExportPlanRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        getLoginTeacher(servletRequest);
        LessonPlanExportService.ExportedPlan exported = lessonPlanExportService.export(
                request.getFormat(),
                request.getTitle(),
                request.getContentMarkdown()
        );
        try {
            servletResponse.setContentType(exported.contentType());
            servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
            servletResponse.setHeader(
                    "Content-Disposition",
                    "attachment; filename*=UTF-8''"
                            + URLEncoder.encode(exported.fileName(), StandardCharsets.UTF_8).replace("+", "%20")
            );
            servletResponse.getOutputStream().write(exported.bytes());
            servletResponse.getOutputStream().flush();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "export failed");
        }
    }

    private void prepareNdjsonResponse(HttpServletResponse response) {
        response.setContentType("application/x-ndjson;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private User getLoginTeacher(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"teacher".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "teacher or admin only");
        }
        return loginUser;
    }

    private String sessionCorrelationId(HttpServletRequest request) {
        String sessionId = request.getSession(false).getId();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void writeLine(HttpServletResponse response, String line) {
        try {
            response.getWriter().write(line);
            response.getWriter().write("\n");
            response.getWriter().flush();
        } catch (IOException e) {
            throw new ClientStreamDisconnectedException(e);
        }
    }

    private void writeEvent(HttpServletResponse response, String type, String message) {
        try {
            ObjectNode event = OBJECT_MAPPER.createObjectNode();
            event.put("type", type);
            event.put("message", message == null ? "" : message);
            writeLine(response, event.toString());
        } catch (ClientStreamDisconnectedException ignored) {
        }
    }

    private static class ClientStreamDisconnectedException extends RuntimeException {
        private ClientStreamDisconnectedException(IOException cause) {
            super(cause);
        }
    }
}
