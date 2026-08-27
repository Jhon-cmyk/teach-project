package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("student_daily_recommendation_session")
public class StudentDailyRecommendationSession implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private LocalDate recommendDate;

    private String status;

    private Long courseId;

    private String goal;

    private String difficultyText;

    private Integer availableMinutes;

    private String preferredResourceType;

    private String answersJson;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
