package com.ruyi.teach.service.impl;

import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.vo.HomeworkTeacherMonitorItemVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeworkMonitorServiceImplCompletionTest {

    private final HomeworkMonitorServiceImpl service = new HomeworkMonitorServiceImpl();

    @Test
    void reviewedHomeworkCountsAsCompleted() {
        HomeworkSubmission submission = reviewedHomework(1L, 1L, 86);

        Boolean completed = ReflectionTestUtils.invokeMethod(
                service,
                "countsAsCompleted",
                submission,
                "homework"
        );

        assertTrue(Boolean.TRUE.equals(completed));
    }

    @Test
    void submittedHomeworkWithoutApprovedFinalScoreDoesNotCountAsCompleted() {
        HomeworkSubmission unreviewed = new HomeworkSubmission();
        unreviewed.setSubmitStatus("submitted");
        unreviewed.setReviewStatus("pending");
        unreviewed.setTotalScore(86);

        Boolean pendingReview = ReflectionTestUtils.invokeMethod(
                service,
                "countsAsCompleted",
                unreviewed,
                "homework"
        );
        unreviewed.setReviewStatus("approved");
        unreviewed.setTotalScore(null);
        Boolean missingFinalScore = ReflectionTestUtils.invokeMethod(
                service,
                "countsAsCompleted",
                unreviewed,
                "homework"
        );

        assertFalse(Boolean.TRUE.equals(pendingReview));
        assertFalse(Boolean.TRUE.equals(missingFinalScore));
    }

    @Test
    void monitorCardShowsNineReviewedStudentsAsFullyCompleted() {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setId(75L);
        assignment.setAssignmentType("homework");
        assignment.setStatus("published");

        List<HomeworkSubmission> submissions = new ArrayList<>();
        for (long studentId = 1; studentId <= 9; studentId++) {
            submissions.add(reviewedHomework(studentId, studentId, 59 + (int) studentId));
        }

        HomeworkTeacherMonitorItemVO item = ReflectionTestUtils.invokeMethod(
                service,
                "buildMonitorItemVO",
                assignment,
                submissions,
                9
        );

        assertEquals(9, item.getCompletedCount());
        assertEquals(0, item.getPendingCount());
        assertEquals(0, item.getReviewPendingCount());
        assertEquals(100.0D, item.getCompletionRate());
        assertEquals(64.0D, item.getAvgScore());
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatedReportAlsoIncludesReviewedHomework() {
        HomeworkSubmission reviewed = reviewedHomework(8L, 22L, 78);

        Map<Long, HomeworkSubmission> result = ReflectionTestUtils.invokeMethod(
                service,
                "pickLatestCompletedByStudent",
                List.of(reviewed),
                "homework"
        );

        assertEquals(1, result.size());
        assertEquals(reviewed, result.get(8L));
    }

    private HomeworkSubmission reviewedHomework(Long studentId, Long submissionId, int score) {
        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setId(submissionId);
        submission.setStudentId(studentId);
        submission.setSubmitStatus("submitted");
        submission.setReviewStatus("approved");
        submission.setTotalScore(score);
        return submission;
    }
}
