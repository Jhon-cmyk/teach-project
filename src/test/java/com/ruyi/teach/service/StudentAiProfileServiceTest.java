package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.mapper.LearningEventMapper;
import com.ruyi.teach.mapper.StudentKnowledgeMasteryMapper;
import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;
import com.ruyi.teach.model.dto.TutorChatRequest;
import com.ruyi.teach.model.entity.LearningEvent;
import com.ruyi.teach.model.entity.StudentLearningPreference;
import com.ruyi.teach.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentAiProfileServiceTest {

    private LearningEventMapper learningEventMapper;
    private StudentLearningPreferenceMapper preferenceMapper;
    private StudentLearningContextService contextService;
    private StudentAiProfileService service;

    @BeforeEach
    void setUp() {
        learningEventMapper = mock(LearningEventMapper.class);
        preferenceMapper = mock(StudentLearningPreferenceMapper.class);
        StudentKnowledgeMasteryMapper masteryMapper = mock(StudentKnowledgeMasteryMapper.class);
        contextService = mock(StudentLearningContextService.class);
        service = new StudentAiProfileService(
                learningEventMapper,
                preferenceMapper,
                masteryMapper,
                contextService,
                new ObjectMapper()
        );
    }

    @Test
    void recordsAvatarQuestionAgainstCurrentStudentAndUpdatesSingleProfile() {
        StudentLearningPreference preference = new StudentLearningPreference();
        preference.setId(31L);
        preference.setStudentId(7L);
        preference.setAiQuestionCount(0);
        when(contextService.getOrCreateGeneralPreference(7L)).thenReturn(preference);

        TutorChatRequest request = new TutorChatRequest();
        request.setMessage("数据结构里的队列为什么是先进先出？");
        request.setMode("explain");
        request.setSource("avatar");
        request.setContext(Map.of("courseId", 64L, "chapterId", 12L, "courseName", "数据结构"));

        service.recordQuestion(student(), request);

        ArgumentCaptor<LearningEvent> eventCaptor = ArgumentCaptor.forClass(LearningEvent.class);
        verify(learningEventMapper).insert(eventCaptor.capture());
        LearningEvent event = eventCaptor.getValue();
        assertThat(event.getStudentId()).isEqualTo(7L);
        assertThat(event.getClassId()).isEqualTo(1L);
        assertThat(event.getCourseId()).isEqualTo(64L);
        assertThat(event.getChapterId()).isEqualTo(12L);
        assertThat(event.getEventType()).isEqualTo("ai_question");
        assertThat(event.getResourceType()).isEqualTo("avatar");
        assertThat(event.getKnowledgeName()).isEqualTo("数据结构");
        assertThat(event.getExtraJson()).contains("先进先出");

        verify(preferenceMapper).updateById(preference);
        assertThat(preference.getAiQuestionCount()).isEqualTo(1);
        assertThat(preference.getAiProfileSummary()).contains("数据结构（1次）");
        assertThat(preference.getAiProfileJson()).contains("recentQuestions", "avatar");
        assertThat(preference.getLastAiQuestionTime()).isNotNull();
    }

    @Test
    void buildsWelcomeFromAuthenticatedUserInsteadOfFrontendConstant() {
        User student = student();
        student.setUserName("小航");

        assertThat(service.buildWelcomeText(student))
                .isEqualTo("小航同学您好呀，欢迎使用智慧教学平台呀，有什么可以帮您？");
    }

    private User student() {
        User student = new User();
        student.setId(7L);
        student.setClassId(1L);
        student.setUserRole("student");
        return student;
    }
}
