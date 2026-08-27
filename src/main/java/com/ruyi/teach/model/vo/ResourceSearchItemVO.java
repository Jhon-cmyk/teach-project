package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ResourceSearchItemVO implements Serializable {

    private Long id;

    /**
     * video / plan / quiz / anim
     */
    private String type;

    private String title;

    private String desc;

    private String cover;

    private String author;

    private Long views;

    private String date;

    private String course;

    private String duration;

    private List<String> tags;

    private String previewText;

    private String link;

    private String sourceType;

    /**
     * 仅后端排序使用，前端可不关心
     */
    private Long sortTimestamp;

    private static final long serialVersionUID = 1L;
}