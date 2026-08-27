package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("community_featured_answer")
public class CommunityFeaturedAnswerLite implements Serializable {
    @TableId("id")
    private Long id;
    @TableField("post_id")
    private Long postId;
    private String excerpt;
    @TableField("teacher_name")
    private String teacherName;
    @TableField("is_recommended")
    private Integer isRecommended;
    @TableField("update_time")
    private Date updateTime;
    @TableField("is_delete")
    private Integer isDelete;
}
