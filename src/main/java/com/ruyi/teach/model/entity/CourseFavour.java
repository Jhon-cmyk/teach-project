package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField; // 1. 务必引入这个包
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("course_favour")
public class CourseFavour {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 2. 明确指定数据库列名为 course_id
    @TableField("course_id")
    private Long courseId;

    // 3. 明确指定数据库列名为 user_id
    @TableField("user_id")
    private Long userId;

    // 4. 明确指定数据库列名为 create_time
    @TableField("create_time")
    private Date createTime;
}