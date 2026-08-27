package com.ruyi.teach.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.client.AiAgentClient;
import com.ruyi.teach.exception.ExternalServiceException;
import com.ruyi.teach.model.dto.PrepareAgentRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AiPrepareContextService;
import com.ruyi.teach.service.LessonPlanExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPrepareAgentControllerTest {

    private final AiPrepareContextService contextService = mock(AiPrepareContextService.class);
    private final AiAgentClient agentClient = mock(AiAgentClient.class);
    private final LessonPlanExportService exportService = mock(LessonPlanExportService.class);
    private final AiPrepareAgentController controller = new AiPrepareAgentController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "aiPrepareContextService", contextService);
        ReflectionTestUtils.setField(controller, "aiAgentClient", agentClient);
        ReflectionTestUtils.setField(controller, "lessonPlanExportService", exportService);
    }

    @Test
    void streamsAgentLinesUsingPrebuiltContext() throws Exception {
        PrepareAgentRequest request = request();
        ObjectNode payload = new ObjectMapper().createObjectNode().put("teacherId", 12);
        when(contextService.buildAgentPayload(12L, request)).thenReturn(payload);
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("{\"type\":\"status\"}");
            consumer.accept("{\"type\":\"done\"}");
            return null;
        }).when(agentClient).streamPrepare(any(ObjectNode.class), any());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest servletRequest = loggedInTeacherRequest();
        String rawSessionId = servletRequest.getSession(false).getId();

        controller.streamAgent(request, servletRequest, response);

        assertThat(response.getContentType()).startsWith("application/x-ndjson");
        assertThat(response.getContentAsString()).isEqualTo(
                "{\"type\":\"status\"}\n{\"type\":\"done\"}\n"
        );
        ArgumentCaptor<ObjectNode> payloadCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(agentClient).streamPrepare(payloadCaptor.capture(), any());
        assertThat(payloadCaptor.getValue().path("actorRole").asText()).isEqualTo("teacher");
        assertThat(payloadCaptor.getValue().path("sessionId").asText())
                .matches("^[a-f0-9]{24}$")
                .isNotEqualTo(rawSessionId);
    }

    @Test
    void writesStableNdjsonErrorWhenAgentIsUnavailable() throws Exception {
        PrepareAgentRequest request = request();
        when(contextService.buildAgentPayload(12L, request))
                .thenReturn(new ObjectMapper().createObjectNode());
        doThrow(new ExternalServiceException("AI agent", "internal detail"))
                .when(agentClient).streamPrepare(any(ObjectNode.class), any());
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.streamAgent(request, loggedInTeacherRequest(), response);

        assertThat(response.getContentAsString()).contains("\"type\":\"error\"");
        assertThat(response.getContentAsString()).contains("AI 备课服务暂时不可用");
        assertThat(response.getContentAsString()).doesNotContain("internal detail");
    }

    @Test
    void treatsClientDisconnectAsNormalStreamTermination() throws Exception {
        PrepareAgentRequest request = request();
        when(contextService.buildAgentPayload(12L, request))
                .thenReturn(new ObjectMapper().createObjectNode());
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("{\"type\":\"status\"}");
            return null;
        }).when(agentClient).streamPrepare(any(ObjectNode.class), any());
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenThrow(new IOException("client disconnected"));

        controller.streamAgent(request, loggedInTeacherRequest(), response);

        verify(agentClient).streamPrepare(any(ObjectNode.class), any());
    }

    @Test
    void getsWorkflowRecordOnlyThroughCurrentTeacherIdentity() {
        String requestId = "0123456789abcdef0123456789abcdef";
        JsonNode workflow = new ObjectMapper()
                .createObjectNode()
                .put("requestId", requestId);
        when(agentClient.getWorkflowRun(requestId, 12L)).thenReturn(workflow);

        BaseResponse<JsonNode> response = controller.getWorkflowRun(
                requestId,
                loggedInTeacherRequest()
        );

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().path("requestId").asText())
                .isEqualTo(requestId);
        verify(agentClient).getWorkflowRun(requestId, 12L);
    }

    private PrepareAgentRequest request() {
        PrepareAgentRequest request = new PrepareAgentRequest();
        request.setAgentType("plan");
        request.setForm(Map.of("topic", "排序算法"));
        return request;
    }

    private MockHttpServletRequest loggedInTeacherRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        User teacher = new User();
        teacher.setId(12L);
        teacher.setUserRole("teacher");
        SessionUserContext.login(request, teacher);
        return request;
    }
}
