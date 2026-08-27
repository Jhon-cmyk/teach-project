package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("community_post")
public class CommunityPostLite implements Serializable {
    @TableId("id")
    private Long id;
    private String title;
    private String content;
    @TableField("post_type")
    private String postType;
    @TableField("course_name")
    private String courseName;
    @TableField("author_name")
    private String authorName;
    private String status;
    @TableField("is_hot")
    private Integer isHot;
    @TableField("is_teacher_answered")
    private Integer isTeacherAnswered;
    @TableField("view_count")
    private Integer viewCount;
    @TableField("reply_count")
    private Integer replyCount;
    @TableField("last_active_time")
    private Date lastActiveTime;
    @TableField("is_delete")
    private Integer isDelete;
}
