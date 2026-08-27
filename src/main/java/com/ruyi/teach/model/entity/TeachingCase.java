package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("teaching_case")
public class TeachingCase {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("title")
    private String title;

    @TableField("category")
    private String category;

    @TableField("difficulty")
    private String difficulty;

    @TableField("course_name")
    private String courseName;

    @TableField("pdf_url")
    private String pdfUrl;

    @TableField("scope")
    private String scope;

    @TableField("status")
    private String status;

    @TableField("source_url")
    private String sourceUrl;

    @TableField("source_case_id")
    private Long sourceCaseId;

    @TableField("source_name")
    private String sourceName;

    @TableField("summary")
    private String summary;

    @TableField("keywords")
    private String keywords;

    @TableField("material_json")
    private String materialJson;

    @TableField("structure_json")
    private String structureJson;

    @TableField("preview_text")
    private String previewText;

    @TableField("preview_type")
    private String previewType;

    @TableField("relevance_score")
    private Integer relevanceScore;

    @TableField("crawl_keyword")
    private String crawlKeyword;

    @TableField("crawl_time")
    private Date crawlTime;

    @TableField("review_time")
    private Date reviewTime;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("is_delete")
    private Integer isDelete;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
