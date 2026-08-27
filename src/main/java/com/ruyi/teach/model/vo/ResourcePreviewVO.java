package com.ruyi.teach.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResourcePreviewVO implements Serializable {

    private Long id;

    /**
     * video / plan / quiz / anim / micro_video
     */
    private String type;

    private String title;

    private String author;

    private String cover;

    /**
     * 视频链接（video 用）
     */
    private String videoUrl;

    /**
     * 文本/HTML 原始内容（plan / quiz / anim 用）
     */
    private String content;

    /**
     * 预览简介
     */
    private String summary;

    private String createTime;

    private static final long serialVersionUID = 1L;
}
