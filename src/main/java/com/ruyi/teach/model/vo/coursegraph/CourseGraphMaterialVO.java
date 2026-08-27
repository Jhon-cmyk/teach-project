package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

@Data
public class CourseGraphMaterialVO {

    private Long id;

    private String title;

    /**
     * 资源类型：plan / anim
     */
    private String type;

    /**
     * 类型显示文本
     */
    private String typeText;

    /**
     * 内容（plan 为 Markdown/HTML，anim 为 JSON）
     */
    private String content;

    /**
     * 匹配分数
     */
    private Integer matchScore;
}
