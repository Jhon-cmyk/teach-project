package com.ruyi.teach.service;

import com.ruyi.teach.client.AiModelClient;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 最小可复用的 DeepSeek 调用服务，供作业判题使用。
 * 不影响现有 AiController 的流式接口。
 */
@Service
public class DeepSeekService {

    private final AiModelClient aiModelClient;

    public DeepSeekService(AiModelClient aiModelClient) {
        this.aiModelClient = aiModelClient;
    }

    /**
     * 同步调用 DeepSeek，返回完整文本响应。
     *
     * @param systemPrompt 系统提示词
     * @param userContent  用户内容
     * @param maxTokens    最大 token 数
     * @return AI 返回的文本内容
     */
    public String chat(String systemPrompt, String userContent, int maxTokens) {
        return aiModelClient.chat(systemPrompt, userContent, 0.3, maxTokens, false);
    }

    /**
     * 调用模型并要求返回严格 JSON，适用于自动批改等机器解析场景。
     */
    public String chatJson(String systemPrompt, String userContent, int maxTokens) {
        return aiModelClient.chat(systemPrompt, userContent, 0.3, maxTokens, true);
    }

    public String streamChat(String systemPrompt, String userContent, int maxTokens, Consumer<String> onChunk) {
        return aiModelClient.streamChat(
                systemPrompt,
                userContent,
                0.3,
                maxTokens,
                false,
                onChunk
        );
    }
}
