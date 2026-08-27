package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PrepareAgentRequest {

    @NotBlank(message = "智能体类型不能为空")
    @Size(max = 32, message = "智能体类型长度不能超过 32 位")
    private String agentType;

    @Size(max = 100, message = "表单字段不能超过 100 个")
    private Map<String, Object> form;

    @Size(max = 128, message = "知识图谱节点 ID 长度不能超过 128 位")
    private String graphNodeId;

    @Positive(message = "课程 ID 必须为正数")
    private Long courseId;

    @Positive(message = "案例 ID 必须为正数")
    private Long caseId;

    @Size(max = 20, message = "参考案例不能超过 20 个")
    private List<@Positive(message = "案例 ID 必须为正数") Long> caseIds;

    @Size(max = 2_000_000, message = "来源内容过长")
    private String sourceContent;

    @Size(max = 50, message = "检索选项不能超过 50 个")
    private Map<String, Object> retrievalOptions;
}
