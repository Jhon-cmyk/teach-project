package com.ruyi.teach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.common.TraceContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AiAgentClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private AiAgentClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        ExternalClientProperties properties = new ExternalClientProperties();
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setDefaultReadTimeout(Duration.ofSeconds(2));
        client = new AiAgentClient(
                new ExternalHttpClient(properties),
                properties,
                objectMapper,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                ""
        );
    }

    @AfterEach
    void tearDown() {
        TraceContext.clear();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void marksConfirmedWorkflowSavedAfterResourcePersistence() {
        String requestId = "0123456789abcdef0123456789abcdef";
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        server.createContext(
                "/agent/runs/" + requestId + "/saved",
                exchange -> {
                    requestBody.set(objectMapper.readTree(exchange.getRequestBody()));
                    respond(exchange, "{\"code\":0}");
                }
        );

        client.markWorkflowSaved(requestId, 1001L, 88L);

        assertThat(requestBody.get().path("teacherId").asLong()).isEqualTo(1001L);
        assertThat(requestBody.get().path("resourceId").asLong()).isEqualTo(88L);
        assertThat(requestBody.get().path("confirmed").asBoolean()).isTrue();
    }

    @Test
    void ignoresMalformedWorkflowRequestId() {
        assertThatCode(() -> client.markWorkflowSaved("../invalid", 1001L, 88L))
                .doesNotThrowAnyException();
    }

    @Test
    void getsWorkflowRunForCurrentTeacher() {
        String requestId = "fedcba9876543210fedcba9876543210";
        AtomicReference<String> query = new AtomicReference<>();
        AtomicReference<String> traceId = new AtomicReference<>();
        server.createContext(
                "/agent/runs/" + requestId,
                exchange -> {
                    query.set(exchange.getRequestURI().getQuery());
                    traceId.set(exchange.getRequestHeaders().getFirst(TraceContext.HEADER_NAME));
                    respond(
                            exchange,
                            """
                            {"code":0,"data":{"requestId":"%s","observability":{"trace_id":"trace-java-001"}}}
                            """.formatted(requestId)
                    );
                }
        );
        TraceContext.bind("trace-java-001");

        JsonNode result = client.getWorkflowRun(requestId, 1001L);

        assertThat(query.get()).isEqualTo("teacherId=1001");
        assertThat(traceId.get()).isEqualTo("trace-java-001");
        assertThat(result.path("requestId").asText()).isEqualTo(requestId);
        assertThat(result.path("observability").path("trace_id").asText())
                .isEqualTo("trace-java-001");
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
