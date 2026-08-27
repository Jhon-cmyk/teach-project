package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "community_post")
@Data
public class CommunityPost implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 帖子类型: discussion / homework */
    @TableField("post_type")
    private String postType;

    @TableField("course_id")
    private Long courseId;

    @TableField("course_name")
    private String courseName;

    @TableField("user_id")
    private Long userId;

    @TableField("author_name")
    private String authorName;

    /** 状态: open / resolved（仅 homework 有意义） */
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