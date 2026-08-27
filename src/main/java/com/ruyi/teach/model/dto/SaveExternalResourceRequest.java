package com.ruyi.teach.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class SaveExternalResourceRequest {

    private String platform;

    private String externalId;

    private String title;

    private String summary;

    private String cover;

    private String author;

    private String url;

    private String resourceType;

    private List<String> tags;

    private String rawJson;
}
