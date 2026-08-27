package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("homework_submission_image")
@Data
public class HomeworkSubmissionImage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private String questionNo;

    private String imageUrl;

    private Integer imageOrder;

    private String recognizedText;

    private String visionJson;

    private String status;

    private String errorMessage;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
