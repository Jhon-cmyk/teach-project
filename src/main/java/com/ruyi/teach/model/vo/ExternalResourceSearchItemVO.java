package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExternalResourceSearchItemVO implements Serializable {

    private String id;

    private String platform;

    private String externalId;

    private String title;

    private String desc;

    private String cover;

    private String author;

    private String url;

    private String resourceType;

    private String date;

    private List<String> tags = new ArrayList<>();

    private String rawJson;

    private static final long serialVersionUID = 1L;
}
