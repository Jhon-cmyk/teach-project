package com.ruyi.teach.client;

import com.ruyi.teach.common.TraceContext;
import com.ruyi.teach.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@Component
public class ExternalHttpClient {

    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(502, 503, 504);

    private final ExternalClientProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public ExternalHttpClient(ExternalClientProperties properties) {
        this(
                properties,
                HttpClient.newBuilder()
                        .connectTimeout(properties.getConnectTimeout())
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build()
        );
    }

    ExternalHttpClient(ExternalClientProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    public String getString(String serviceName,
                            URI uri,
                            Map<String, String> headers,
                            Duration timeout,
                            RetryPolicy retryPolicy,
                            String publicMessage) {
        HttpRequest request = requestBuilder(uri, headers, timeout).GET().build();
        return send(
                serviceName,
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8),
                retryPolicy,
                publicMessage
        ).body();
    }

    public String postJson(String serviceName,
                           URI uri,
                           String body,
                           Map<String, String> headers,
                           Duration timeout,
                           RetryPolicy retryPolicy,
                           String publicMessage) {
        HttpRequest request = requestBuilder(uri, headers, timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return send(
                serviceName,
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8),
                retryPolicy,
                publicMessage
        ).body();
    }

    public void postJsonLines(String serviceName,
                              URI uri,
                              String body,
                              Map<String, String> headers,
                              Duration timeout,
                              String publicMessage,
                              Consumer<String> lineConsumer) {
        HttpRequest request = requestBuilder(uri, headers, timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<Stream<String>> response = send(
                serviceName,
                request,
                HttpResponse.BodyHandlers.ofLines(),
                RetryPolicy.NEVER,
                publicMessage
        );
        try (Stream<String> lines = response.body()) {
            lines.forEach(lineConsumer);
        }
    }

    public <T> T getStream(String serviceName,
                           URI uri,
                           Map<String, String> headers,
                           Duration timeout,
                           RetryPolicy retryPolicy,
                           String publicMessage,
                           StreamHandler<T> handler) {
        HttpRequest request = requestBuilder(uri, headers, timeout).GET().build();
        HttpResponse<InputStream> response = send(
                serviceName,
                request,
                HttpResponse.BodyHandlers.ofInputStream(),
                retryPolicy,
                publicMessage
        );
        try (InputStream inputStream = response.body()) {
            return handler.handle(new StreamResponse(
                    inputStream,
                    response.headers().firstValue("Content-Type").orElse(""),
                    response.headers().firstValueAsLong("Content-Length").orElse(-1L)
            ));
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(serviceName, publicMessage, e);
        }
    }

    private HttpRequest.Builder requestBuilder(URI uri,
                                               Map<String, String> headers,
                                               Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(timeout == null ? properties.getDefaultReadTimeout() : timeout);
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && value != null && !value.isBlank()) {
                    builder.header(name, value);
                }
            });
        }
        String traceId = TraceContext.currentTraceId();
        if (traceId != null && !traceId.isBlank() && !containsTraceHeader(headers)) {
            builder.header(TraceContext.HEADER_NAME, traceId);
        }
        return builder;
    }

    private boolean containsTraceHeader(Map<String, String> headers) {
        return headers != null && headers.keySet().stream()
                .anyMatch(TraceContext.HEADER_NAME::equalsIgnoreCase);
    }

    private <T> HttpResponse<T> send(String serviceName,
                                     HttpRequest request,
                                     HttpResponse.BodyHandler<T> bodyHandler,
                                     RetryPolicy retryPolicy,
                                     String publicMessage) {
        int maxAttempts = retryPolicy == RetryPolicy.TRANSIENT
                ? Math.max(1, properties.getMaxRetries() + 1)
                : 1;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startedAt = System.nanoTime();
            try {
                HttpResponse<T> response = httpClient.send(request, bodyHandler);
                long durationMs = elapsedMillis(startedAt);
                logResult(serviceName, request, response.statusCode(), durationMs, attempt);

                if (isSuccessful(response.statusCode())) {
                    return response;
                }
                if (attempt < maxAttempts && RETRYABLE_STATUS_CODES.contains(response.statusCode())) {
                    closeBody(response.body());
                    continue;
                }
                closeBody(response.body());
                throw new ExternalServiceException(serviceName, publicMessage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logFailure(serviceName, request, elapsedMillis(startedAt), attempt, e);
                throw new ExternalServiceException(serviceName, publicMessage, e);
            } catch (IOException e) {
                logFailure(serviceName, request, elapsedMillis(startedAt), attempt, e);
                if (attempt >= maxAttempts) {
                    throw new ExternalServiceException(serviceName, publicMessage, e);
                }
            } catch (ExternalServiceException e) {
                throw e;
            } catch (RuntimeException e) {
                logFailure(serviceName, request, elapsedMillis(startedAt), attempt, e);
                throw new ExternalServiceException(serviceName, publicMessage, e);
            }
        }
        throw new ExternalServiceException(serviceName, publicMessage);
    }

    private void logResult(String serviceName,
                           HttpRequest request,
                           int status,
                           long durationMs,
                           int attempt) {
        log.info(
                "External request completed, trace_id={}, service={}, method={}, target={}, status={}, durationMs={}, attempt={}",
                traceId(),
                serviceName,
                request.method(),
                safeTarget(request.uri()),
                status,
                durationMs,
                attempt
        );
    }

    private void logFailure(String serviceName,
                            HttpRequest request,
                            long durationMs,
                            int attempt,
                            Exception exception) {
        log.warn(
                "External request failed, trace_id={}, service={}, method={}, target={}, durationMs={}, attempt={}, cause={}",
                traceId(),
                serviceName,
                request.method(),
                safeTarget(request.uri()),
                durationMs,
                attempt,
                exception.getClass().getSimpleName()
        );
    }

    private String safeTarget(URI uri) {
        String host = uri.getHost() == null ? "-" : uri.getHost();
        String path = uri.getPath() == null ? "" : uri.getPath();
        return host + path;
    }

    private String traceId() {
        String traceId = TraceContext.currentTraceId();
        return traceId == null ? "-" : traceId;
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private void closeBody(Object body) {
        if (body instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    public enum RetryPolicy {
        NEVER,
        TRANSIENT
    }

    @FunctionalInterface
    public interface StreamHandler<T> {
        T handle(StreamResponse response) throws Exception;
    }

    public record StreamResponse(InputStream body, String contentType, long contentLength) {
    }
}
