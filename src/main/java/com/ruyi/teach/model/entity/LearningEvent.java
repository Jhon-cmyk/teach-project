package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("learning_event")
public class LearningEvent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long classId;

    private Long courseId;

    private Long chapterId;

    private Long resourceId;

    private String resourceType;

    private String knowledgeName;

    private String eventType;

    private Integer durationSecond;

    private BigDecimal score;

    private Integer correct;

    private String extraJson;

    private Date eventTime;

    private Date createTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
