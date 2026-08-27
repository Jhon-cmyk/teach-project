package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 班级实体类
 */
@TableName(value = "sys_class")
@Data
public class SysClass implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 班级名称
     */
    private String name;

    /**
     * 所属专业
     */
    private String major;

    /**
     * 所属学院
     */
    private String college;

    @TableField("create_time")
    private Date createTime;

    /**
     * 班级人数（非数据库字段，用于前端展示）
     */
    @TableField(exist = false)
    private Integer studentCount;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}