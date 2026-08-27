package com.ruyi.teach.model.vo;

public record AvatarSessionVO(
        String signedUrl,
        String appId,
        String sceneId,
        String avatarId,
        String avatarName,
        String voiceName,
        String welcomeText
) {
}
