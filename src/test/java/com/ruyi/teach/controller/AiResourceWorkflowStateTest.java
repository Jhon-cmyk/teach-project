package com.ruyi.teach.controller;

import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.model.dto.AiResourceCreateRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AgentIndexService;
import com.ruyi.teach.service.AgentWorkflowStateSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiResourceWorkflowStateTest {

    private static final String REQUEST_ID =
            "0123456789abcdef0123456789abcdef";

    private final AiResourceMapper resourceMapper = mock(AiResourceMapper.class);
    private final AgentIndexService indexService = mock(AgentIndexService.class);
    private final AgentWorkflowStateSyncService workflowStateSyncService =
            mock(AgentWorkflowStateSyncService.class);
    private final AiResourceController controller = new AiResourceController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "aiResourceMapper", resourceMapper);
        ReflectionTestUtils.setField(controller, "agentIndexService", indexService);
        ReflectionTestUtils.setField(
                controller,
                "agentWorkflowStateSyncService",
                workflowStateSyncService
        );
        when(resourceMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            AiResource resource = invocation.getArgument(0);
            resource.setId(88L);
            return 1;
        }).when(resourceMapper).insert(any(AiResource.class));
    }

    @Test
    void marksWorkflowSavedOnlyAfterResourceInsertSucceeds() {
        AiResourceController.SaveResult result = controller
                .save(createRequest(), loggedInTeacherRequest())
                .getData();

        assertThat(result.getId()).isEqualTo(88L);
        verify(workflowStateSyncService).markSaved(REQUEST_ID, 1001L, 88L);
    }

    @Test
    void resourceSaveRemainsSuccessfulWhenStateSyncIsUnavailable() {
        doThrow(new RuntimeException("executor rejected"))
                .when(workflowStateSyncService)
                .markSaved(REQUEST_ID, 1001L, 88L);

        AiResourceController.SaveResult result = controller
                .save(createRequest(), loggedInTeacherRequest())
                .getData();

        assertThat(result.getId()).isEqualTo(88L);
    }

    private AiResourceCreateRequest createRequest() {
        AiResourceCreateRequest request = new AiResourceCreateRequest();
        request.setType("plan");
        request.setTitle("二叉树遍历教案");
        request.setContent("# 教学目标");
        request.setAgentRequestId(REQUEST_ID);
        return request;
    }

    private MockHttpServletRequest loggedInTeacherRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        User teacher = new User();
        teacher.setId(1001L);
        teacher.setUserRole("teacher");
        SessionUserContext.login(request, teacher);
        return request;
    }
}
