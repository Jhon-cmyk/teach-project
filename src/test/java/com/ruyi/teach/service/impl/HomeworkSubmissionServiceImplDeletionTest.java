package com.ruyi.teach.service.impl;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.HomeworkAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeworkSubmissionServiceImplDeletionTest {

    private HomeworkSubmissionServiceImpl service;
    private HomeworkAssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        service = spy(new HomeworkSubmissionServiceImpl());
        assignmentService = mock(HomeworkAssignmentService.class);
        ReflectionTestUtils.setField(service, "assignmentService", assignmentService);
    }

    @Test
    void studentCanLogicallyDeleteOwnHomeworkRecord() {
        HomeworkSubmission submission = submission(11L, 21L, 7L);
        HomeworkAssignment assignment = assignment(21L, "homework");
        doReturn(submission).when(service).getById(11L);
        doReturn(true).when(service).removeById(11L);
        when(assignmentService.getById(21L)).thenReturn(assignment);

        service.deleteStudentHomeworkHistory(11L, student(7L));

        verify(service).removeById(11L);
    }

    @Test
    void studentCannotDeleteAnotherStudentsHomeworkRecord() {
        doReturn(submission(11L, 21L, 8L)).when(service).getById(11L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteStudentHomeworkHistory(11L, student(7L))
        );

        assertEquals(40101, exception.getCode());
        verify(service, never()).removeById(anyLong());
    }

    @Test
    void homeworkEndpointCannotDeleteExamRecord() {
        doReturn(submission(11L, 21L, 7L)).when(service).getById(11L);
        when(assignmentService.getById(21L)).thenReturn(assignment(21L, "exam"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteStudentHomeworkHistory(11L, student(7L))
        );

        assertEquals(40000, exception.getCode());
        verify(service, never()).removeById(anyLong());
    }

    private HomeworkSubmission submission(long id, long assignmentId, long studentId) {
        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setId(id);
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(studentId);
        submission.setIsDelete(0);
        return submission;
    }

    private HomeworkAssignment assignment(long id, String type) {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setId(id);
        assignment.setAssignmentType(type);
        assignment.setIsDelete(0);
        return assignment;
    }

    private User student(long id) {
        User user = new User();
        user.setId(id);
        user.setUserRole("student");
        return user;
    }
}
