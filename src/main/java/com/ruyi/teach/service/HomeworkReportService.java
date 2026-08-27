package com.ruyi.teach.service;

import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.HomeworkSubmissionImage;
import com.ruyi.teach.model.vo.HomeworkReportVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeworkReportService {

    public HomeworkReportVO build(HomeworkSubmission submission,
                                  HomeworkAssignment assignment,
                                  List<HomeworkSubmissionDetail> details,
                                  List<HomeworkSubmissionImage> images) {
        HomeworkReportVO report = new HomeworkReportVO();
        report.setSubmission(submission);
        report.setAssignmentTitle(assignment == null ? "" : assignment.getTitle());
        report.setContentSnapshot(assignment == null ? "" : assignment.getContentSnapshot());
        report.setDetails(details);
        report.setImages(images);
        report.setExamMode(assignment != null && "exam".equals(assignment.getAssignmentType()));
        return report;
    }
}
