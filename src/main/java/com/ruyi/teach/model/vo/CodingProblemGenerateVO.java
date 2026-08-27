package com.ruyi.teach.model.vo;

import com.ruyi.teach.model.dto.CodingProblemAddRequest;
import lombok.Data;

import java.util.List;

@Data
public class CodingProblemGenerateVO {

    private String title;

    private String description;

    private String difficulty;

    private List<String> languages;

    private Integer timeLimitMs;

    private Integer memoryLimitKb;

    private List<CodingProblemAddRequest.CodingTestCaseItem> testCases;

    private List<CodingProblemAddRequest.CodingTemplateItem> templates;
}
