package com.ruyi.teach.client;

import com.ruyi.teach.common.TraceContext;
import com.ruyi.teach.exception.ExternalServiceException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalHttpClientTest {

    private HttpServer server;
    private ExternalHttpClient client;
    private URI baseUri;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

        ExternalClientProperties properties = new ExternalClientProperties();
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setDefaultReadTimeout(Duration.ofSeconds(2));
        properties.setMaxRetries(1);
        client = new ExternalHttpClient(properties);
    }

    @AfterEach
    void tearDown() {
        TraceContext.clear();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retriesTransientGetAndPropagatesTraceId() {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> traceHeader = new AtomicReference<>();
        server.createContext("/retry", exchange -> {
            traceHeader.set(exchange.getRequestHeaders().getFirst(TraceContext.HEADER_NAME));
            int status = requests.incrementAndGet() == 1 ? 503 : 200;
            respond(exchange, status, status == 200 ? "ok" : "busy");
        });
        TraceContext.bind("trace-test-001");

        String body = client.getString(
                "test-service",
                baseUri.resolve("/retry"),
                Map.of(),
                Duration.ofSeconds(2),
                ExternalHttpClient.RetryPolicy.TRANSIENT,
                "服务不可用"
        );

        assertThat(body).isEqualTo("ok");
        assertThat(requests).hasValue(2);
        assertThat(traceHeader).hasValue("trace-test-001");
    }

    @Test
    void neverRetriesNonIdempotentPost() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/post", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 503, "busy");
        });

        assertThatThrownBy(() -> client.postJson(
                "test-service",
                baseUri.resolve("/post"),
                "{}",
                Map.of(),
                Duration.ofSeconds(2),
                ExternalHttpClient.RetryPolicy.NEVER,
                "服务不可用"
        )).isInstanceOf(ExternalServiceException.class)
                .hasMessage("服务不可用");
        assertThat(requests).hasValue(1);
    }

    @Test
    void streamsResponseLinesThroughOneClientBoundary() {
        server.createContext("/stream", exchange ->
                respond(exchange, 200, "first\nsecond\n"));
        StringBuilder lines = new StringBuilder();

        client.postJsonLines(
                "test-service",
                baseUri.resolve("/stream"),
                "{}",
                Map.of(),
                Duration.ofSeconds(2),
                "服务不可用",
                line -> lines.append(line).append('|')
        );

        assertThat(lines).hasToString("first|second|");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
