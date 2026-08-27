package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField; // 1. 引入这个包
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("study_plan")
public class StudyPlan {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 2. 加上 @TableField 指定数据库里的列名
    @TableField("user_id")
    private Long userId;

    private String title;

    // 3. 同理，isCompleted 也要指定对应 is_completed
    @TableField("is_completed")
    private Integer isCompleted;

    @TableField("create_time")
    private Date createTime;
}