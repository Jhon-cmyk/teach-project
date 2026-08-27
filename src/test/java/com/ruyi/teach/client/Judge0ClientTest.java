package com.ruyi.teach.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class Judge0ClientTest {

    private HttpServer server;
    private Judge0Client client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();

        ExternalClientProperties properties = new ExternalClientProperties();
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setJudge0Timeout(Duration.ofSeconds(2));
        client = new Judge0Client(
                new ExternalHttpClient(properties),
                properties,
                new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "sandbox-token",
                5000,
                262144
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void waitsForAndReturnsPlainTextCompletedResult() {
        AtomicReference<String> submitAuth = new AtomicReference<>();
        AtomicReference<String> submitBody = new AtomicReference<>();
        AtomicReference<String> submitQuery = new AtomicReference<>();
        server.createContext("/submissions", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                submitAuth.set(exchange.getRequestHeaders().getFirst("X-Auth-Token"));
                submitBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                submitQuery.set(exchange.getRequestURI().getQuery());
                respond(
                        exchange,
                        201,
                        "{\"token\":\"submission-1\",\"status\":{\"id\":3,\"description\":\"Accepted\"},"
                                + "\"stdout\":\"McOXMT0xCg==\\n\",\"time\":\"0.01\",\"memory\":1024}"
                );
                return;
            }
            respond(exchange, 405, "{}");
        });

        Judge0Client.JudgeResult result = client.submitAndWait(
                "java",
                "class Main { String value = \"×\"; }",
                "",
                1000,
                65536
        );

        assertThat(result.accepted).isTrue();
        assertThat(result.statusId).isEqualTo(3);
        assertThat(result.stdout).isEqualTo("1×1=1\n");
        assertThat(submitAuth).hasValue("sandbox-token");
        assertThat(submitQuery).hasValue("base64_encoded=true&wait=true");
        String encodedSource = Base64.getEncoder().encodeToString(
                "class Main { String value = \"×\"; }".getBytes(StandardCharsets.UTF_8)
        );
        assertThat(submitBody.get()).contains("\"source_code\":\"" + encodedSource + "\"");
        assertThat(submitBody.get()).doesNotContain("×");
        assertThat(submitBody.get()).doesNotContain("max_processes_and_or_threads");
    }

    @Test
    void rejectsUnsupportedLanguageWithoutNetworkCall() {
        Judge0Client.JudgeResult result = client.submitAndWait(
                "rust",
                "fn main() {}",
                "",
                1000,
                65536
        );

        assertThat(result.accepted).isFalse();
        assertThat(result.statusDescription).contains("不支持的语言");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
