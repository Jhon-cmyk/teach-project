package com.ruyi.teach.controller;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.mapper.HomeworkReminderMapper;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.dto.SendReminderRequest;
import com.ruyi.teach.model.entity.HomeworkReminder;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.RoleAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerAuthorizationTest {

    private NotificationController controller;
    private HomeworkReminderMapper reminderMapper;
    private SysClassMapper sysClassMapper;

    @BeforeEach
    void setUp() {
        controller = new NotificationController();
        reminderMapper = mock(HomeworkReminderMapper.class);
        sysClassMapper = mock(SysClassMapper.class);
        ReflectionTestUtils.setField(controller, "homeworkReminderMapper", reminderMapper);
        ReflectionTestUtils.setField(controller, "sysClassMapper", sysClassMapper);
        ReflectionTestUtils.setField(
                controller,
                "roleAuthorizationService",
                new RoleAuthorizationService()
        );
    }

    @Test
    void teacherBroadcastIsLimitedToOwnClassesAndUsesSessionIdentity() {
        when(sysClassMapper.selectMyClasses(7L)).thenReturn(List.of(sysClass(11L), sysClass(12L)));
        when(reminderMapper.insert(any(HomeworkReminder.class))).thenReturn(1);

        SendReminderRequest body = new SendReminderRequest();
        body.setMessage("请完成作业");
        controller.sendHomeworkReminder(body, requestFor(7L, "teacher"));

        ArgumentCaptor<HomeworkReminder> captor = ArgumentCaptor.forClass(HomeworkReminder.class);
        verify(reminderMapper, times(2)).insert(captor.capture());
        assertEquals(List.of(11L, 12L), captor.getAllValues().stream()
                .map(HomeworkReminder::getClassId)
                .sorted()
                .toList());
        captor.getAllValues().forEach(reminder -> assertEquals(7L, reminder.getTeacherId()));
    }

    @Test
    void teacherCannotSendToAnotherClass() {
        when(sysClassMapper.selectMyClasses(7L)).thenReturn(List.of(sysClass(11L)));
        SendReminderRequest body = new SendReminderRequest();
        body.setClassId(99L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> controller.sendHomeworkReminder(body, requestFor(7L, "teacher"))
        );

        assertEquals(40101, exception.getCode());
        verify(reminderMapper, never()).insert(any(HomeworkReminder.class));
    }

    @Test
    void studentPollingUsesAuthenticatedStudentId() {
        controller.checkHomeworkReminder(0L, requestFor(9L, "student"));

        verify(reminderMapper).selectLatestForStudent(
                org.mockito.ArgumentMatchers.eq(9L),
                any(java.util.Date.class)
        );
    }

    private SysClass sysClass(Long id) {
        SysClass sysClass = new SysClass();
        sysClass.setId(id);
        return sysClass;
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
