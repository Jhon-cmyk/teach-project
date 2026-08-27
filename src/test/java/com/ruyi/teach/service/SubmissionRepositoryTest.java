package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionDetailMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionImageMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionMapper;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import com.ruyi.teach.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionRepositoryTest {

    @Test
    void locksAssignmentBeforeAllocatingNextAttemptNumber() {
        HomeworkAssignmentMapper assignmentMapper = mock(HomeworkAssignmentMapper.class);
        HomeworkSubmissionMapper submissionMapper = mock(HomeworkSubmissionMapper.class);
        HomeworkSubmissionDetailMapper detailMapper = mock(HomeworkSubmissionDetailMapper.class);
        HomeworkSubmissionImageMapper imageMapper = mock(HomeworkSubmissionImageMapper.class);
        SubmissionValidator validator = mock(SubmissionValidator.class);
        AnswerParser parser = new AnswerParser(new ObjectMapper());
        SubmissionRepository repository = new SubmissionRepository(
                assignmentMapper,
                submissionMapper,
                detailMapper,
                imageMapper,
                validator,
                parser
        );

        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setId(7L);
        assignment.setTeacherId(8L);
        assignment.setCourseId(9L);
        assignment.setAllowRedo(1);
        assignment.setMaxAttemptCount(3);
        when(assignmentMapper.selectByIdForUpdate(7L)).thenReturn(assignment);
        when(validator.validateHomework(any(), any(), any()))
                .thenReturn(new SubmissionValidator.ValidatedHomework(assignment, "online", "auto"));
        HomeworkSubmission previous = new HomeworkSubmission();
        previous.setAttemptNo(1);
        previous.setSubmitStatus("failed");
        when(submissionMapper.selectList(any())).thenReturn(List.of(previous));
        when(submissionMapper.insert(any(HomeworkSubmission.class))).thenAnswer(invocation -> {
            HomeworkSubmission saved = invocation.getArgument(0);
            saved.setId(101L);
            return 1;
        });

        HomeworkSubmitRequest request = new HomeworkSubmitRequest();
        request.setAssignmentId(7L);
        request.setStudentAnswerJson("[{\"num\":\"1\",\"type\":\"radio\",\"answer\":\"A\"}]");
        User student = new User();
        student.setId(10L);
        student.setClassId(11L);
        student.setUserRole("student");

        HomeworkSubmission result = repository.createHomeworkSubmission(request, student);

        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getAttemptNo()).isEqualTo(2);
        assertThat(result.getSubmitStatus()).isEqualTo("judging");
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(assignmentMapper, submissionMapper);
        order.verify(assignmentMapper).selectByIdForUpdate(7L);
        order.verify(submissionMapper).selectList(any());
    }

    @Test
    void aggregateWriteMethodsDeclareRollbackBoundary() throws Exception {
        assertRollbackBoundary("createHomeworkSubmission", HomeworkSubmitRequest.class, User.class);
        assertRollbackBoundary("replaceDetails", Long.class, List.class);
        assertRollbackBoundary("updateSubmissionAndReplaceDetails", HomeworkSubmission.class, List.class);
        assertRollbackBoundary("resetForRegrade", HomeworkSubmission.class);
        assertRollbackBoundary("markReviewPending", Long.class);
    }

    @Test
    void detailInsertFailureEscapesTheTransactionInsteadOfLeavingPartialSuccess() {
        HomeworkAssignmentMapper assignmentMapper = mock(HomeworkAssignmentMapper.class);
        HomeworkSubmissionMapper submissionMapper = mock(HomeworkSubmissionMapper.class);
        HomeworkSubmissionDetailMapper detailMapper = mock(HomeworkSubmissionDetailMapper.class);
        SubmissionRepository repository = new SubmissionRepository(
                assignmentMapper,
                submissionMapper,
                detailMapper,
                mock(HomeworkSubmissionImageMapper.class),
                mock(SubmissionValidator.class),
                new AnswerParser(new ObjectMapper())
        );
        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setId(101L);
        when(submissionMapper.updateById(submission)).thenReturn(1);
        when(detailMapper.insert(any(HomeworkSubmissionDetail.class)))
                .thenThrow(new IllegalStateException("detail insert failed"));

        HomeworkSubmissionDetail detail = new HomeworkSubmissionDetail();
        detail.setQuestionNo("1");

        assertThatThrownBy(() -> repository.updateSubmissionAndReplaceDetails(
                submission,
                List.of(detail)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("detail insert failed");
    }

    @Test
    void repeatedSubmissionAtAttemptLimitIsRejectedWithoutPartialInsert() {
        HomeworkAssignmentMapper assignmentMapper = mock(HomeworkAssignmentMapper.class);
        HomeworkSubmissionMapper submissionMapper = mock(HomeworkSubmissionMapper.class);
        SubmissionValidator validator = mock(SubmissionValidator.class);
        SubmissionRepository repository = new SubmissionRepository(
                assignmentMapper,
                submissionMapper,
                mock(HomeworkSubmissionDetailMapper.class),
                mock(HomeworkSubmissionImageMapper.class),
                validator,
                new AnswerParser(new ObjectMapper())
        );

        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setId(7L);
        assignment.setAllowRedo(0);
        assignment.setMaxAttemptCount(1);
        when(assignmentMapper.selectByIdForUpdate(7L)).thenReturn(assignment);
        when(validator.validateHomework(any(), any(), any()))
                .thenReturn(new SubmissionValidator.ValidatedHomework(assignment, "online", "auto"));
        HomeworkSubmission previous = new HomeworkSubmission();
        previous.setAttemptNo(1);
        previous.setSubmitStatus("submitted");
        when(submissionMapper.selectList(any())).thenReturn(List.of(previous));

        HomeworkSubmitRequest request = new HomeworkSubmitRequest();
        request.setAssignmentId(7L);
        request.setStudentAnswerJson("[{\"num\":\"1\",\"type\":\"radio\",\"answer\":\"A\"}]");
        User student = new User();
        student.setId(10L);
        student.setClassId(11L);
        student.setUserRole("student");

        assertThatThrownBy(() -> repository.createHomeworkSubmission(request, student))
                .isInstanceOf(com.ruyi.teach.exception.BusinessException.class)
                .hasMessageContaining("最大提交次数");
        verify(submissionMapper, never()).insert(any(HomeworkSubmission.class));
    }

    private void assertRollbackBoundary(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = SubmissionRepository.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).contains(Exception.class);
    }
}
