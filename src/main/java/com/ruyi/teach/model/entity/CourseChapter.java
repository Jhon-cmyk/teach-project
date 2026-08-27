package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 课程章节（选集）实体类
 */
@TableName(value = "course_chapter")
@Data
public class CourseChapter implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属课程ID
     */
    @TableField("course_id")
    private Long courseId;

    /**
     * 章节/选集标题
     */
    private String title;

    /**
     * 视频真实URL
     */
    @TableField("video_url")
    private String videoUrl;

    /**
     * 播放顺序（升序排）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("anim_html")
    private String animHtml;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}