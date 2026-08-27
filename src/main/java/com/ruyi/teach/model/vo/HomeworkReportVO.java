package com.ruyi.teach.model.vo;

import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.HomeworkSubmissionImage;
import lombok.Data;

import java.util.List;

/**
 * 单次作答报告详情
 */
@Data
public class HomeworkReportVO {
    /** 是否考试模式（前端判断需要展示 teacherRemark） */
    private Boolean examMode;

    private HomeworkSubmission submission;
    private String assignmentTitle;
    private String contentSnapshot;
    private List<HomeworkSubmissionDetail> details;
    private List<HomeworkSubmissionImage> images;
}
