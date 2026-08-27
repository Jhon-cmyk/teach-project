package com.ruyi.teach.model.dto;

import lombok.Data;

@Data
public class CodingRunRequest {

    private Long problemId;

    private String language;

    private String code;
}
