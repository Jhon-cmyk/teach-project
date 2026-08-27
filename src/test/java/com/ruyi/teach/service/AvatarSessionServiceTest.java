package com.ruyi.teach.service;

import com.ruyi.teach.config.AvatarProperties;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.vo.AvatarSessionVO;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvatarSessionServiceTest {

    @Test
    void createsShortLivedSignedUrlWithoutExposingSecret() {
        AvatarProperties properties = configuredProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-08-10T12:00:00Z"), ZoneOffset.UTC);
        AvatarSessionService service = new AvatarSessionService(properties, clock);

        AvatarSessionVO session = service.createSession();
        URI signedUri = URI.create(session.signedUrl());
        Map<String, String> query = parseQuery(signedUri.getRawQuery());
        String authorization = new String(
                Base64.getDecoder().decode(query.get("authorization")),
                StandardCharsets.UTF_8
        );

        assertEquals("avatar.cn-huadong-1.xf-yun.com", query.get("host"));
        assertEquals("Mon, 10 Aug 2026 12:00:00 GMT", query.get("date"));
        assertTrue(authorization.contains("api_key=\"test-api-key\""));
        assertTrue(authorization.contains("algorithm=\"hmac-sha256\""));
        assertTrue(authorization.contains("signature=\""));
        assertFalse(session.signedUrl().contains("test-api-secret"));
        assertEquals("朵朵", session.avatarName());
    }

    @Test
    void rejectsIncompleteConfiguration() {
        AvatarProperties properties = new AvatarProperties();
        properties.setEnabled(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new AvatarSessionService(properties).createSession()
        );

        assertTrue(exception.getMessage().contains("配置不完整"));
    }

    @Test
    void returnsCurrentStudentWelcomeTextFromBackendProfileService() {
        AvatarProperties properties = configuredProperties();
        StudentAiProfileService profileService = mock(StudentAiProfileService.class);
        com.ruyi.teach.model.entity.User student = new com.ruyi.teach.model.entity.User();
        student.setId(7L);
        when(profileService.buildWelcomeText(student)).thenReturn("小航同学您好呀，有什么可以帮您？");
        AvatarSessionService service = new AvatarSessionService(
                properties,
                Clock.fixed(Instant.parse("2026-08-10T12:00:00Z"), ZoneOffset.UTC),
                profileService
        );

        AvatarSessionVO session = service.createSession(student);

        assertEquals("小航同学您好呀，有什么可以帮您？", session.welcomeText());
    }

    private AvatarProperties configuredProperties() {
        AvatarProperties properties = new AvatarProperties();
        properties.setEnabled(true);
        properties.setAppId("test-app-id");
        properties.setApiKey("test-api-key");
        properties.setApiSecret("test-api-secret");
        properties.setSceneId("test-scene-id");
        properties.setAvatarId("test-avatar-id");
        properties.setAvatarName("朵朵");
        return properties;
    }

    private Map<String, String> parseQuery(String rawQuery) {
        return Arrays.stream(rawQuery.split("&"))
                .map(item -> item.split("=", 2))
                .collect(Collectors.toMap(
                        item -> URLDecoder.decode(item[0], StandardCharsets.UTF_8),
                        item -> URLDecoder.decode(item[1], StandardCharsets.UTF_8)
                ));
    }
}
