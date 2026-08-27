package com.ruyi.teach.service;

import com.ruyi.teach.config.AvatarProperties;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.AvatarSessionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class AvatarSessionService {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String AUTH_HEADERS = "host date request-line";

    private final AvatarProperties properties;
    private final Clock clock;
    private final StudentAiProfileService studentAiProfileService;

    @Autowired
    public AvatarSessionService(AvatarProperties properties,
                                StudentAiProfileService studentAiProfileService) {
        this(properties, Clock.systemUTC(), studentAiProfileService);
    }

    AvatarSessionService(AvatarProperties properties) {
        this(properties, Clock.systemUTC(), null);
    }

    AvatarSessionService(AvatarProperties properties, Clock clock) {
        this(properties, clock, null);
    }

    AvatarSessionService(AvatarProperties properties,
                         Clock clock,
                         StudentAiProfileService studentAiProfileService) {
        this.properties = properties;
        this.clock = clock;
        this.studentAiProfileService = studentAiProfileService;
    }

    public AvatarSessionVO createSession() {
        return createSession(null);
    }

    public AvatarSessionVO createSession(User user) {
        validateConfiguration();
        String welcomeText = studentAiProfileService == null
                ? "同学您好呀，欢迎使用智慧教学平台呀，有什么可以帮您？"
                : studentAiProfileService.buildWelcomeText(user);
        return new AvatarSessionVO(
                createSignedUrl(),
                properties.getAppId(),
                properties.getSceneId(),
                properties.getAvatarId(),
                properties.getAvatarName(),
                properties.getVoiceName(),
                welcomeText
        );
    }

    String createSignedUrl() {
        URI serverUri;
        try {
            serverUri = URI.create(properties.getServerUrl());
        } catch (IllegalArgumentException exception) {
            throw configurationError("数字人服务地址格式不正确");
        }

        String host = serverUri.getHost();
        String path = StringUtils.hasText(serverUri.getRawPath()) ? serverUri.getRawPath() : "/";
        if (!StringUtils.hasText(host)
                || !("wss".equalsIgnoreCase(serverUri.getScheme()) || "ws".equalsIgnoreCase(serverUri.getScheme()))) {
            throw configurationError("数字人服务地址必须是有效的 WebSocket 地址");
        }

        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock));
        String signatureOrigin = "host: " + host
                + "\ndate: " + date
                + "\nGET " + path + " HTTP/1.1";
        String signature = sign(signatureOrigin, properties.getApiSecret());
        String authorizationOrigin = "api_key=\"" + properties.getApiKey()
                + "\", algorithm=\"hmac-sha256\", headers=\"" + AUTH_HEADERS
                + "\", signature=\"" + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(
                authorizationOrigin.getBytes(StandardCharsets.UTF_8)
        );

        String authority = serverUri.getRawAuthority();
        return serverUri.getScheme() + "://" + authority + path
                + "?authorization=" + encodeQuery(authorization)
                + "&date=" + encodeQuery(date)
                + "&host=" + encodeQuery(host);
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw configurationError("数字人服务尚未启用");
        }
        if (!StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getApiSecret())
                || !StringUtils.hasText(properties.getSceneId())
                || !StringUtils.hasText(properties.getAvatarId())) {
            throw configurationError("数字人服务配置不完整，请配置 APPID、APIKey、APISecret、接口服务ID和数字人形象ID");
        }
    }

    private String sign(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数字人鉴权签名生成失败");
        }
    }

    private String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private BusinessException configurationError(String message) {
        return new BusinessException(ErrorCode.OPERATION_ERROR, message);
    }
}
