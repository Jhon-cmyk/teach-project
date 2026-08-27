package com.ruyi.teach.model.vo;

import lombok.Data;

@Data
public class CaptchaVO {
    /** 验证码唯一标识，登录时需原样回传 */
    private String captchaId;
    /** 图形验证码图片 (data:image/png;base64,...) */
    private String imageBase64;
    /** Development convenience: captcha text for local auto-fill. */
    private String captchaCode;
}
