package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "community_featured_answer")
@Data
public class CommunityFeaturedAnswer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("reply_id")
    private Long replyId;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("teacher_name")
    private String teacherName;

    private String excerpt;

    @TableField("is_recommended")
    private Integer isRecommended;

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
