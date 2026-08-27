package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerParserTest {

    private final AnswerParser parser = new AnswerParser(new ObjectMapper());

    @Test
    void onlyAcceptsImagesBoundToFillOrTextAnswers() {
        HomeworkSubmitRequest request = new HomeworkSubmitRequest();
        request.setStudentAnswerJson("""
                [
                  {"num":"1","type":"radio","answer":"A"},
                  {"num":"2","imageKey":"fill-2","type":"fill","answer":""}
                ]
                """);

        HomeworkSubmitRequest.QuestionImageItem ignored = new HomeworkSubmitRequest.QuestionImageItem();
        ignored.setQuestionNo("1");
        ignored.setImageUrls(List.of("/uploads/radio.png"));
        HomeworkSubmitRequest.QuestionImageItem accepted = new HomeworkSubmitRequest.QuestionImageItem();
        accepted.setQuestionNo("fill-2");
        accepted.setImageUrls(List.of("/uploads/fill.png"));
        request.setQuestionImageItems(List.of(ignored, accepted));

        assertThat(parser.hasImagePayload(request)).isTrue();
        assertThat(parser.parseImageAnswerQuestionNos(request.getStudentAnswerJson()))
                .containsExactly("fill-2");
    }

    @Test
    void malformedAnswerTextBecomesOneExplainableFallbackItem() {
        List<Map<String, Object>> items = parser.prepareAnswerItems("not-json", Map.of());

        assertThat(items).hasSize(1);
        assertThat(items.getFirst())
                .containsEntry("num", "1")
                .containsEntry("type", "text")
                .containsEntry("answer", "not-json");
    }

    @Test
    void wholePaperImageCountsAsAnImageSubmission() {
        HomeworkSubmitRequest request = new HomeworkSubmitRequest();
        request.setWholePaperImageUrls(List.of(" ", "/uploads/whole-paper.png"));

        assertThat(parser.hasImagePayload(request)).isTrue();
    }

    @Test
    void parsesAllDetailsBeforeRepositoryStartsWriting() {
        List<HomeworkSubmissionDetail> details = parser.parseDetails(
                88L,
                """
                        [
                          {"num":"1","type":"radio","answer":"A","fullScore":10,"score":10},
                          {"num":"2","type":"text","answer":"说明","imageUrls":["/a.png"]}
                        ]
                        """,
                null
        );

        assertThat(details).hasSize(2);
        assertThat(details.getFirst().getSubmissionId()).isEqualTo(88L);
        assertThat(details.getFirst().getIsCorrect()).isEqualTo(1);
        assertThat(details.get(1).getImageUrlsJson()).isEqualTo("[\"/a.png\"]");
    }

    @Test
    void malformedOptionalScoreDoesNotDiscardTheAnswer() {
        List<HomeworkSubmissionDetail> details = parser.parseDetails(
                99L,
                "[{\"num\":\"3\",\"type\":\"fill\",\"answer\":\"x\",\"score\":\"bad\"}]",
                null
        );

        assertThat(details).singleElement().satisfies(detail -> {
            assertThat(detail.getQuestionNo()).isEqualTo("3");
            assertThat(detail.getStudentAnswer()).isEqualTo("x");
            assertThat(detail.getScore()).isNull();
        });
    }
}
