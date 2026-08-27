package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("coding_test_case")
@Data
public class CodingTestCase implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("problem_id")
    private Long problemId;

    private String input;

    @TableField("expected_output")
    private String expectedOutput;

    @TableField("is_sample")
    private Integer isSample;

    private Integer score;

    @TableField("sort_order")
    private Integer sortOrder;

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
