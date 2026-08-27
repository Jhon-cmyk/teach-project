package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

@Data
public class CourseGraphQuizVO {

    private Long id;

    private String title;

    /**
     * 来源类型：coding / quiz
     */
    private String source;

    /**
     * 来源显示文本：编程题 / 随堂测验
     */
    private String sourceText;

    /**
     * 难度：easy / medium / hard（仅编程题有）
     */
    private String difficulty;

    /**
     * 描述/内容摘要
     */
    private String content;

    /**
     * 匹配分数（越高越相关）
     */
    private Integer matchScore;
}
