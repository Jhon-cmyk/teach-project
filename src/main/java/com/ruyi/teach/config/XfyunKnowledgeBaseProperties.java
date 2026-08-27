package com.ruyi.teach.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "xfyun.knowledge-base")
public class XfyunKnowledgeBaseProperties {

    private boolean enabled = false;
    private String baseUrl = "https://chatdoc.xfyun.cn";
    private String appId = "";
    private String secret = "";
    private int topN = 5;
    private int esTopN = 5;
    private double minimumScore = 45D;
    private Duration timeout = Duration.ofSeconds(60);

    public boolean isConfigured() {
        return enabled
                && appId != null && !appId.isBlank()
                && secret != null && !secret.isBlank();
    }
}
