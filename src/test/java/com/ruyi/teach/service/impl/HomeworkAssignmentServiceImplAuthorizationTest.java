package com.ruyi.teach.service.impl;

import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.dto.HomeworkPublishRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeworkAssignmentServiceImplAuthorizationTest {

    private HomeworkAssignmentServiceImpl service;
    private AiResourceMapper aiResourceMapper;
    private SysClassMapper sysClassMapper;

    @BeforeEach
    void setUp() {
        service = new HomeworkAssignmentServiceImpl();
        aiResourceMapper = mock(AiResourceMapper.class);
        sysClassMapper = mock(SysClassMapper.class);
        ReflectionTestUtils.setField(service, "aiResourceMapper", aiResourceMapper);
        ReflectionTestUtils.setField(service, "sysClassMapper", sysClassMapper);
    }

    @Test
    void rejectsPublishingHomeworkToClassOutsideTeachersTeachingScope() {
        User teacher = new User();
        teacher.setId(18L);
        teacher.setUserRole("teacher");

        AiResource quiz = new AiResource();
        quiz.setId(91L);
        quiz.setTeacherId(teacher.getId());
        quiz.setType("quiz");
        quiz.setTitle("数据结构测验");
        when(aiResourceMapper.selectById(quiz.getId())).thenReturn(quiz);

        SysClass taughtClass = new SysClass();
        taughtClass.setId(7L);
        when(sysClassMapper.selectMyClasses(teacher.getId())).thenReturn(List.of(taughtClass));

        HomeworkPublishRequest request = new HomeworkPublishRequest();
        request.setQuizResourceId(quiz.getId());
        request.setClassId(8L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.publishAssignment(request, teacher)
        );

        assertEquals("只能向本人任教的班级发布作业或考试", exception.getMessage());
    }
}
