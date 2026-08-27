package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {

    @NotBlank(message = "问题内容不能为空")
    @Size(max = 100_000, message = "问题内容过长")
    private String question;

    @Size(max = 32, message = "场景类型长度不能超过 32 位")
    private String type;

    @Pattern(
            regexp = "^(continue|rewrite|expand|summarize|polish)?$",
            message = "写作操作类型不合法"
    )
    private String operation;

    @Positive(message = "案例 ID 必须为正数")
    private Long caseId;

    @Size(max = 20, message = "参考案例不能超过 20 个")
    private List<@Positive(message = "案例 ID 必须为正数") Long> caseIds;
}
