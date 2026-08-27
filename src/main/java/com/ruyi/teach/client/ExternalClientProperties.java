package com.ruyi.teach.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "external-client")
public class ExternalClientProperties {

    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration defaultReadTimeout = Duration.ofSeconds(30);
    private Duration aiModelTimeout = Duration.ofMinutes(5);
    private Duration aiAgentTimeout = Duration.ofMinutes(8);
    private Duration judge0Timeout = Duration.ofSeconds(15);
    private Duration remoteResourceTimeout = Duration.ofSeconds(20);
    private int maxRetries = 1;
}
