package com.ruyi.teach.controller;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.model.dto.FatigueReportRequest;
import com.ruyi.teach.model.entity.FatigueRecord;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.FatigueRecordService;
import com.ruyi.teach.service.RoleAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FatigueRecordControllerAuthorizationTest {

    private FatigueRecordController controller;
    private FatigueRecordService fatigueRecordService;

    @BeforeEach
    void setUp() {
        controller = new FatigueRecordController();
        fatigueRecordService = mock(FatigueRecordService.class);
        ReflectionTestUtils.setField(controller, "fatigueRecordService", fatigueRecordService);
        ReflectionTestUtils.setField(
                controller,
                "roleAuthorizationService",
                new RoleAuthorizationService()
        );
    }

    @Test
    void reportAlwaysUsesAuthenticatedStudentId() {
        FatigueReportRequest body = new FatigueReportRequest();
        body.setMonitorSeconds(30);
        when(fatigueRecordService.saveOrUpdateToday(eq(7L), any(FatigueRecord.class)))
                .thenReturn(new FatigueRecord());

        controller.report(body, requestFor(7L, "student"));

        verify(fatigueRecordService)
                .saveOrUpdateToday(eq(7L), any(FatigueRecord.class));
    }

    @Test
    void nonStudentCannotReadFatigueData() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> controller.getToday(requestFor(7L, "teacher"))
        );

        assertEquals(40101, exception.getCode());
    }

    private MockHttpServletRequest requestFor(Long userId, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        User user = new User();
        user.setId(userId);
        user.setUserRole(role);
        SessionUserContext.login(request, user);
        return request;
    }
}
