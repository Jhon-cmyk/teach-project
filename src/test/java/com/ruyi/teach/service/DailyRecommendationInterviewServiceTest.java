package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.model.dto.learning.DailyRecommendationInterviewRequest;
import com.ruyi.teach.model.dto.learning.DailyRecommendationSubmitRequest;
import com.ruyi.teach.model.vo.DailyRecommendationInterviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.ruyi.teach.mapper.StudentLearningPreferenceMapper;

class DailyRecommendationInterviewServiceTest {

    private DeepSeekService deepSeekService;
    private DailyRecommendationInterviewService service;

    @BeforeEach
    void setUp() {
        deepSeekService = mock(DeepSeekService.class);
        StudentLearningContextService contextService = new StudentLearningContextService(
                mock(StudentLearningPreferenceMapper.class));
        service = new DailyRecommendationInterviewService(deepSeekService, new ObjectMapper(), contextService);
    }

    @Test
    void normalizesModelOutputBeforeReturningProfile() {
        when(deepSeekService.chat(anyString(), anyString(), anyInt())).thenReturn("""
                ```json
                {
                  "reply":"信息已经足够，可以生成推荐了。",
                  "ready":true,
                  "progress":99,
                  "summary":"今天重点查漏补缺",
                  "profile":{
                    "universityName":"示例大学",
                    "developmentGoal":"postgraduate",
                    "courseId":999,
                    "goal":"查漏补缺",
                    "difficultyText":"递归调用栈",
                    "learningSituation":"能听懂，但做题不稳定",
                    "personalityType":"challenge",
                    "availableMinutes":999,
                    "preferredResourceType":"video"
                  }
                }
                ```
                """);

        DailyRecommendationInterviewRequest request = requestWithTwoStudentTurns();
        DailyRecommendationInterviewVO result = service.interview(request);

        assertThat(result.isReady()).isTrue();
        assertThat(result.isDegraded()).isFalse();
        assertThat(result.getProfile().getCourseId()).isNull();
        assertThat(result.getProfile().getAvailableMinutes()).isEqualTo(180);
        assertThat(result.getProfile().getPersonalityType()).isEqualTo("challenge");
        assertThat(result.getProfile().getPreferredResourceType()).isEqualTo("video");
        assertThat(result.getProfile().getCollectionMode()).isEqualTo("ai_interview");
        assertThat(result.getProfile().getUniversityName()).isEqualTo("示例大学");
        assertThat(result.getProfile().getDevelopmentGoal()).isEqualTo("postgraduate");
        assertThat(result.getProfile().getInterviewSummary()).isEqualTo("今天重点查漏补缺");
    }

    @Test
    void doesNotFinishInterviewBeforeStudentHasAnsweredTwice() {
        when(deepSeekService.chat(anyString(), anyString(), anyInt())).thenReturn("""
                {"reply":"可以生成了","ready":true,"progress":100,
                 "profile":{"universityName":"示例大学","developmentGoal":"employment",
                 "goal":"复习巩固","difficultyText":"链表","availableMinutes":30,
                 "personalityType":"steady","preferredResourceType":"balanced"}}
                """);
        DailyRecommendationInterviewRequest request = requestWithTwoStudentTurns();
        request.setMessages(request.getMessages().subList(0, 2));

        DailyRecommendationInterviewVO result = service.interview(request);

        assertThat(result.isReady()).isFalse();
    }

    @Test
    void returnsRecoverableResponseWhenAiCallFails() {
        when(deepSeekService.chat(anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalStateException("timeout"));

        DailyRecommendationInterviewVO result = service.interview(requestWithTwoStudentTurns());

        assertThat(result.isDegraded()).isTrue();
        assertThat(result.isReady()).isFalse();
        assertThat(result.getReply()).contains("快速问卷");
        assertThat(result.getProfile()).isNotNull();
    }

    private DailyRecommendationInterviewRequest requestWithTwoStudentTurns() {
        DailyRecommendationInterviewRequest request = new DailyRecommendationInterviewRequest();
        DailyRecommendationSubmitRequest profile = new DailyRecommendationSubmitRequest();
        profile.setAvailableMinutes(30);
        profile.setPreferredResourceType("balanced");
        request.setProfile(profile);

        DailyRecommendationInterviewRequest.CourseOption course = new DailyRecommendationInterviewRequest.CourseOption();
        course.setId(1L);
        course.setName("数据结构");
        request.setCourses(List.of(course));
        request.setMessages(List.of(
                message("assistant", "今天想解决什么问题？"),
                message("user", "我想复习数据结构"),
                message("assistant", "最困惑哪部分？"),
                message("user", "递归调用栈")
        ));
        return request;
    }

    private DailyRecommendationInterviewRequest.Message message(String role, String content) {
        DailyRecommendationInterviewRequest.Message message = new DailyRecommendationInterviewRequest.Message();
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}
