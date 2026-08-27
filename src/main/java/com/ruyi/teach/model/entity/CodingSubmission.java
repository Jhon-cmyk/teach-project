package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("coding_submission")
@Data
public class CodingSubmission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("problem_id")
    private Long problemId;

    @TableField("publish_id")
    private Long publishId;

    @TableField("student_id")
    private Long studentId;

    private String language;

    private String code;

    private String status;

    @TableField("passed_count")
    private Integer passedCount;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("test_score")
    private Integer testScore;

    @TableField("ai_score")
    private Integer aiScore;

    @TableField("final_score")
    private Integer finalScore;

    @TableField("ai_review_md")
    private String aiReviewMd;

    @TableField("runtime_ms")
    private Integer runtimeMs;

    @TableField("memory_kb")
    private Integer memoryKb;

    @TableField("judge_detail")
    private String judgeDetail;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
