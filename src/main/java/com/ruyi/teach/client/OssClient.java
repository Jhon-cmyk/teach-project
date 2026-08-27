package com.ruyi.teach.client;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.ruyi.teach.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class OssClient {

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucketName;

    public OssClient(@Value("${aliyun.oss.endpoint}") String endpoint,
                     @Value("${aliyun.oss.access-key-id}") String accessKeyId,
                     @Value("${aliyun.oss.access-key-secret}") String accessKeySecret,
                     @Value("${aliyun.oss.bucket-name}") String bucketName) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
    }

    public String upload(InputStream inputStream,
                         String objectName,
                         String contentType,
                         long contentLength) {
        OSS client = null;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (contentLength > 0) {
                metadata.setContentLength(contentLength);
            }
            if (contentType != null && !contentType.isBlank()) {
                metadata.setContentType(contentType);
            }

            client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            client.putObject(bucketName, objectName, inputStream, metadata);
            return buildFileUrl(objectName);
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "Aliyun OSS",
                    "文件上传服务暂时不可用",
                    e
            );
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }

    private String buildFileUrl(String objectName) {
        String normalizedEndpoint = endpoint
                .replaceFirst("^https?://", "")
                .replaceAll("/+$", "");
        return "https://" + bucketName + "." + normalizedEndpoint + "/" + objectName;
    }
}
