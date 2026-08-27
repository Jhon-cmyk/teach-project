package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("homework_assignment")
@Data
public class HomeworkAssignment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private Long classId;

    /**
     * 个性化练习的目标学生；普通教师作业为空。
     */
    private Long targetStudentId;

    private Long courseId;

    /**
     * 章节ID（章节练习用）
     */
    private Long chapterId;

    /**
     * 章节标题快照
     */
    private String chapterTitleSnapshot;

    private Long quizResourceId;

    private String title;

    private String quizTitleSnapshot;

    private String contentSnapshot;

    private String paramsSnapshot;

    private String teacherNote;

    /**
     * online / image / mixed
     */
    private String answerMode;

    /**
     * whole / per_question / both
     */
    private String imageGranularity;

    /**
     * auto / ai_review
     */
    private String gradingMode;

    /**
     * homework / chapter_practice
     */
    private String assignmentType;

    /**
     * teacher_bank / platform_bank / ai_generated
     */
    private String sourceType;

    /**
     * draft / published / closed
     */
    private String status;

    private Date deadline;

    /**
     * 考试时长（分钟），仅 assignmentType='exam' 使用
     */
    private Integer durationMinutes;

    private Integer allowRedo;

    private Integer maxAttemptCount;

    private Integer questionCount;

    private Integer totalScore;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
