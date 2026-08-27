package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@TableName(value = "course")
@Data
public class Course implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程简介
     */
    private String description;

    /**
     * 课程封面
     */
    private String coverImg;

    /**
     * 课程视频链接
     */
    private String videoUrl;

    /**
     * 课程类型：video / text
     */
    private String type;

    /**
     * 原有字段，教师端仍在使用
     */
    private Long teacherId;

    /**
     * 原有冗余字段
     */
    private String teacherName;

    /**
     * 课程来源：platform / teacher
     */
    private String sourceType;

    /**
     * 创建人 ID
     */
    private Long creatorId;

    /**
     * 创建人角色：admin / teacher
     */
    private String creatorRole;

    /**
     * 发布状态：draft / published / offline
     */
    private String publishStatus;

    /**
     * 课程分类，对应 course_category.id
     */
    private Long categoryId;

    /**
     * 价格
     */
    private Integer price;

    /**
     * 积分消耗
     */
    private Integer pointsCost;

    /**
     * 视频知识上下文
     */
    @TableField("video_context")
    private String videoContext;

    /**
     * 是否要求学生看课时开启人脸/学习状态检测
     */
    @TableField("face_detection_required")
    private Boolean faceDetectionRequired;

    /** 星火知识库 ID；一门课程绑定一个知识库。 */
    @TableField("knowledgeRepoId")
    private String knowledgeRepoId;

    /** 便于管理端回显的知识库名称。 */
    @TableField("knowledgeRepoName")
    private String knowledgeRepoName;

    /** 通用页面提问时用于自动路由课程知识库的关键词，逗号分隔。 */
    @TableField("knowledgeKeywords")
    private String knowledgeKeywords;

    /** empty / processing / ready / failed。 */
    @TableField("knowledgeSyncStatus")
    private String knowledgeSyncStatus;

    @TableField("knowledgeUpdatedAt")
    private Date knowledgeUpdatedAt;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    /**
     * 教师端排课时临时使用，不入库
     */
    @TableField(exist = false)
    private List<Long> classIds;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
