package com.ruyi.teach.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XfyunKnowledgeBaseClientTest {

    @Test
    void createsOfficialMd5ThenHmacSha1Signature() {
        String signature = XfyunKnowledgeBaseClient.signature(
                "test-app",
                "test-secret",
                1_700_000_000L
        );

        assertThat(signature).isEqualTo("8wjxrxZbXf4XSSSzU0OTCfNHnFw=");
    }
}
