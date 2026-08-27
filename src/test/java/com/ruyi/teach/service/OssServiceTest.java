package com.ruyi.teach.service;

import com.ruyi.teach.client.OssClient;
import com.ruyi.teach.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OssServiceTest {

    @Test
    void uploadFailureReturnsStableMessageAndKeepsServiceCauseForLogs() {
        OssClient ossClient = mock(OssClient.class);
        when(ossClient.upload(
                any(InputStream.class),
                anyString(),
                anyString(),
                anyLong()
        )).thenThrow(new IllegalStateException("provider-internal-detail"));
        OssService ossService = new OssService(ossClient);

        assertThatThrownBy(() -> ossService.uploadBytes(
                "content".getBytes(),
                "lesson.txt",
                "qa",
                "text/plain"
        ))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("文件上传服务暂时不可用")
                .satisfies(error -> {
                    ExternalServiceException externalError =
                            (ExternalServiceException) error;
                    assertThat(externalError.getServiceName()).isEqualTo("Aliyun OSS");
                    assertThat(externalError.getCause())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("provider-internal-detail");
                });
    }
}
