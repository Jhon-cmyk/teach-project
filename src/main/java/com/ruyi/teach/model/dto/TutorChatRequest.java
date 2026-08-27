package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * AI 学伴聊天请求.
 */
@Data
public class TutorChatRequest {

    /** 已有会话 id; 为空则新建 */
    @Positive(message = "会话 ID 必须为正数")
    private Long sessionId;

    /** 可选, 限定检索在某课程内 */
    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    /** 用户问题 */
    @NotBlank(message = "问题内容不能为空")
    @Size(max = 10_000, message = "问题内容长度不能超过 10000 位")
    private String message;

    /** 助教模式：explain / hint / check / practice / summary / answer / debug */
    @Pattern(
            regexp = "^(explain|hint|check|practice|summary|answer|debug)?$",
            message = "助教模式不合法"
    )
    private String mode;

    /** 提问入口：avatar / ai_assistant，用于画像证据说明 */
    @Pattern(
            regexp = "^(avatar|ai_assistant)?$",
            message = "提问来源不合法"
    )
    private String source;

    /** 当前学习上下文，由学生端页面收集并做长度截断 */
    @Size(max = 50, message = "学习上下文字段不能超过 50 个")
    private Map<String, Object> context;
}
