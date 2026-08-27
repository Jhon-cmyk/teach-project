package com.ruyi.teach.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExportPlanRequest {

    @Pattern(regexp = "^(docx|pdf)?$", message = "导出格式只支持 docx 或 pdf")
    private String format;

    @Size(max = 200, message = "文档标题长度不能超过 200 位")
    private String title;

    @NotBlank(message = "教案内容不能为空")
    @Size(max = 2_000_000, message = "教案内容过长")
    private String contentMarkdown;
}
