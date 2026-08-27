package com.ruyi.teach.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class RemoteResourceClient {

    private static final String USER_AGENT = "TeachPlatform/1.0";

    private final ExternalHttpClient httpClient;
    private final ExternalClientProperties properties;
    private final ObjectMapper objectMapper;

    public RemoteResourceClient(ExternalHttpClient httpClient,
                                ExternalClientProperties properties,
                                ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode getJsonOrEmpty(String url) {
        try {
            String body = httpClient.getString(
                    "external-resource",
                    URI.create(url),
                    Map.of("User-Agent", USER_AGENT),
                    properties.getRemoteResourceTimeout(),
                    ExternalHttpClient.RetryPolicy.TRANSIENT,
                    "外部资源检索服务暂时不可用"
            );
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.debug("External resource ignored, cause={}", e.getClass().getSimpleName());
            return objectMapper.createObjectNode();
        }
    }

    public byte[] downloadBytesOrEmpty(String serviceName,
                                       String url,
                                       int maxBytes,
                                       Duration timeout) {
        try {
            return download(serviceName, url, maxBytes, timeout).bytes();
        } catch (Exception e) {
            log.debug("Remote resource download ignored, service={}, cause={}",
                    serviceName, e.getClass().getSimpleName());
            return new byte[0];
        }
    }

    public DownloadedResource download(String serviceName,
                                       String url,
                                       int maxBytes,
                                       Duration timeout) {
        return withStream(serviceName, url, timeout, response -> {
            byte[] bytes = response.body().readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new ExternalServiceException(serviceName, "远程资源超过大小限制");
            }
            return new DownloadedResource(bytes, response.contentType(), response.contentLength());
        });
    }

    public <T> T withStream(String serviceName,
                            String url,
                            Duration timeout,
                            ExternalHttpClient.StreamHandler<T> handler) {
        if (url == null || url.isBlank()) {
            throw new ExternalServiceException(serviceName, "远程资源地址无效");
        }
        return httpClient.getStream(
                serviceName,
                URI.create(url),
                Map.of("User-Agent", USER_AGENT),
                timeout == null ? properties.getRemoteResourceTimeout() : timeout,
                ExternalHttpClient.RetryPolicy.NEVER,
                "远程资源服务暂时不可用",
                handler
        );
    }

    public record DownloadedResource(byte[] bytes, String contentType, long contentLength) {
    }
}
