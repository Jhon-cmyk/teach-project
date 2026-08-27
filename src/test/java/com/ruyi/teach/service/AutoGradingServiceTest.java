package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AutoGradingServiceTest {

    @Test
    void externalFailureLeavesQuestionsPendingForTeacherReview() {
        DeepSeekService deepSeekService = mock(DeepSeekService.class);
        when(deepSeekService.chatJson(anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalStateException("temporary outage"));
        AutoGradingService service = new AutoGradingService(
                deepSeekService,
                new AnswerParser(new ObjectMapper()),
                new ObjectMapper()
        );

        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("num", "1");
        answer.put("type", "text");
        answer.put("answer", "A");
        answer.put("standardAnswer", "A");
        answer.put("fullScore", 10);
        List<Map<String, Object>> answers = new ArrayList<>(List.of(answer));

        String response = service.applyTextAnswerJudgments(answers, new HomeworkAssignment());

        assertThat(response).isNull();
        assertThat(answer.get("score")).isNull();
        assertThat(answer.get("isCorrect")).isNull();
        assertThat(answer.get("aiComment")).isEqualTo("AI判定暂不可用，待教师判分");
    }

    @Test
    void validAiJudgmentProducesSuggestedScore() {
        DeepSeekService deepSeekService = mock(DeepSeekService.class);
        when(deepSeekService.chatJson(anyString(), anyString(), anyInt()))
                .thenReturn("{\"judgments\":[{\"questionNo\":\"1\",\"isCorrect\":1,\"comment\":\"答案等价\"}]}");
        AutoGradingService service = new AutoGradingService(
                deepSeekService,
                new AnswerParser(new ObjectMapper()),
                new ObjectMapper()
        );

        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("num", "1");
        answer.put("type", "text");
        answer.put("answer", "A");
        answer.put("standardAnswer", "A");
        answer.put("fullScore", 10);

        service.applyTextAnswerJudgments(new ArrayList<>(List.of(answer)), new HomeworkAssignment());

        assertThat(answer)
                .containsEntry("score", 10)
                .containsEntry("isCorrect", 1)
                .containsEntry("aiSuggestedScore", 10);
    }

    @Test
    void objectiveQuestionsAreGradedLocallyWhenAiIsUnavailable() {
        DeepSeekService deepSeekService = mock(DeepSeekService.class);
        AutoGradingService service = new AutoGradingService(
                deepSeekService,
                new AnswerParser(new ObjectMapper()),
                new ObjectMapper()
        );

        Map<String, Object> single = answer("1", "radio", "A", "A", 9);
        Map<String, Object> multiple = answer("2", "checkbox", List.of("C", "A"), "A, C", 8);
        Map<String, Object> judge = answer("3", "judge", "对", "正确", 7);
        Map<String, Object> fill = answer("4", "fill", "O(n)", "O(n)", 6);
        Map<String, Object> wrong = answer("5", "radio", "B", "A", 5);

        String response = service.applyTextAnswerJudgments(
                new ArrayList<>(List.of(single, multiple, judge, fill, wrong)),
                new HomeworkAssignment()
        );

        assertThat(response).isNull();
        assertThat(single).containsEntry("score", 9).containsEntry("isCorrect", 1);
        assertThat(multiple).containsEntry("score", 8).containsEntry("isCorrect", 1);
        assertThat(judge).containsEntry("score", 7).containsEntry("isCorrect", 1);
        assertThat(fill).containsEntry("score", 6).containsEntry("isCorrect", 1);
        assertThat(wrong).containsEntry("score", 0).containsEntry("isCorrect", 0);
        verifyNoInteractions(deepSeekService);
    }

    private Map<String, Object> answer(String num,
                                       String type,
                                       Object studentAnswer,
                                       String standardAnswer,
                                       int fullScore) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("num", num);
        item.put("type", type);
        item.put("answer", studentAnswer);
        item.put("standardAnswer", standardAnswer);
        item.put("fullScore", fullScore);
        return item;
    }
}
