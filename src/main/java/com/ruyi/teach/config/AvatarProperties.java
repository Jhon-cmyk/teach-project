package com.ruyi.teach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xfyun.avatar")
public class AvatarProperties {

    private boolean enabled = false;
    private String serverUrl = "wss://avatar.cn-huadong-1.xf-yun.com/v1/interact";
    private String appId = "";
    private String apiKey = "";
    private String apiSecret = "";
    private String sceneId = "";
    private String avatarId = "";
    private String avatarName = "朵朵";
    private String voiceName = "x4_lingxiaoqi_oral";
}
