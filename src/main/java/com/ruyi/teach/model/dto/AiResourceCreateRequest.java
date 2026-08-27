package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiResourceCreateRequest {

    @NotBlank(message = "资源类型不能为空")
    @Size(max = 32, message = "资源类型长度不能超过 32 位")
    private String type;

    @NotBlank(message = "资源标题不能为空")
    @Size(max = 200, message = "资源标题长度不能超过 200 位")
    private String title;

    @NotBlank(message = "资源内容不能为空")
    @Size(max = 5_000_000, message = "资源内容过长")
    private String content;

    @Size(max = 1_000_000, message = "资源参数过长")
    private String paramsJson;

    @jakarta.validation.constraints.Min(value = 0, message = "发布状态只能为 0 或 1")
    @Max(value = 1, message = "发布状态只能为 0 或 1")
    private Integer isPublished;

    @Positive(message = "来源资源 ID 必须为正数")
    private Long sourceId;

    @Size(max = 32, message = "来源类型长度不能超过 32 位")
    private String sourceType;

    @Pattern(regexp = "^[a-f0-9]{32}$", message = "Agent 请求 ID 格式不正确")
    private String agentRequestId;
}
