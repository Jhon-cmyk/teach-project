package com.ruyi.teach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.ExternalServiceException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiModelClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private String endpoint;
    private AiModelClient client;
    private ExternalClientProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/chat";

        properties = new ExternalClientProperties();
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setAiModelTimeout(Duration.ofMillis(250));
        client = new AiModelClient(
                new ExternalHttpClient(properties),
                properties,
                objectMapper,
                endpoint,
                "test-key",
                "test-model"
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsConfiguredModelAndParsesChatContent() {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/chat", exchange -> {
            requestBody.set(objectMapper.readTree(exchange.getRequestBody()));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, "{\"choices\":[{\"message\":{\"content\":\"课程建议\"}}]}");
        });

        String content = client.chat("system", "question", 0.2, 1200, true);

        assertThat(content).isEqualTo("课程建议");
        assertThat(requestBody.get().path("model").asText()).isEqualTo("test-model");
        assertThat(requestBody.get().path("response_format").path("type").asText())
                .isEqualTo("json_object");
        assertThat(authorization).hasValue("Bearer test-key");
    }

    @Test
    void parsesOpenAiCompatibleStreamWithoutLeakingProtocolToCallers() {
        server.createContext("/chat", exchange -> respond(
                exchange,
                "data: {\"choices\":[{\"delta\":{\"content\":\"你\"}}]}\n"
                        + "data: {\"choices\":[{\"delta\":{\"content\":\"好\"}}]}\n"
                        + "data: [DONE]\n"
        ));
        StringBuilder chunks = new StringBuilder();

        String fullText = client.streamChat(
                "system",
                "question",
                0.3,
                100,
                false,
                chunks::append
        );

        assertThat(fullText).isEqualTo("你好");
        assertThat(chunks).hasToString("你好");
    }

    @Test
    void timeoutStopsQuicklyAndReturnsStableExternalServiceError() {
        server.createContext("/chat", exchange -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, "{\"choices\":[{\"message\":{\"content\":\"late\"}}]}");
        });
        long startedAt = System.nanoTime();

        assertThatThrownBy(() -> client.chat("system", "question", 0.2, 100, false))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("AI 服务暂时不可用，请稍后重试")
                .satisfies(error -> assertThat(((ExternalServiceException) error).getServiceName())
                        .isEqualTo("AI model"));

        assertThat(Duration.ofNanos(System.nanoTime() - startedAt))
                .isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void malformedChatResponseReturnsStableExternalServiceError() {
        server.createContext("/chat", exchange -> respond(exchange, "{not-json"));

        assertThatThrownBy(() -> client.chat("system", "question", 0.2, 100, true))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("AI 服务暂时不可用，请稍后重试");
    }

    @Test
    void streamWithOnlyMalformedChunksIsRejectedInsteadOfReportedAsSuccess() {
        server.createContext("/chat", exchange -> respond(
                exchange,
                "data: {not-json}\n"
                        + "data: [DONE]\n"
        ));

        assertThatThrownBy(() -> client.streamChat(
                "system",
                "question",
                0.3,
                100,
                false,
                chunk -> {
                }
        ))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("AI 服务暂时不可用，请稍后重试");
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
