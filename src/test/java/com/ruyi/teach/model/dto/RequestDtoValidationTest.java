package com.ruyi.teach.model.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestDtoValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void courseCreateRejectsBlankNameAndInvalidClassId() {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setName(" ");
        request.setClassIds(List.of(-1L));

        Set<ConstraintViolation<CourseCreateRequest>> violations = validator.validate(request);

        assertHasPath(violations, "name");
        assertHasPath(violations, "classIds[0].<list element>");
    }

    @Test
    void homeworkPublishRejectsMissingRequiredResources() {
        HomeworkPublishRequest request = new HomeworkPublishRequest();
        request.setAssignmentType("invalid");

        Set<ConstraintViolation<HomeworkPublishRequest>> violations = validator.validate(request);

        assertHasPath(violations, "quizResourceId");
        assertHasPath(violations, "classId");
        assertHasPath(violations, "assignmentType");
    }

    @Test
    void nestedCodingTestCaseIsValidated() {
        CodingProblemAddRequest request = new CodingProblemAddRequest();
        request.setTitle("示例题");
        request.setDescription("描述");
        request.setLanguages(List.of("java"));
        CodingProblemAddRequest.CodingTestCaseItem testCase =
                new CodingProblemAddRequest.CodingTestCaseItem();
        testCase.setExpectedOutput(" ");
        request.setTestCases(List.of(testCase));

        Set<ConstraintViolation<CodingProblemAddRequest>> violations =
                validator.validate(request);

        assertHasPath(violations, "testCases[0].expectedOutput");
    }

    @Test
    void codingProblemUpdateAcceptsMultilineMarkdownButRejectsBlankDescription() {
        CodingProblemUpdateRequest request = new CodingProblemUpdateRequest();
        request.setId(1L);
        request.setTitle("输出九九乘法表");
        request.setDescription("""
                ## 题目描述
                输出完整的九九乘法表。

                ## 输出格式
                按行输出计算结果。
                """);
        request.setLanguages(List.of("java", "python"));

        Set<ConstraintViolation<CodingProblemUpdateRequest>> validViolations =
                validator.validate(request);
        assertTrue(
                validViolations.stream().noneMatch(
                        violation -> "description".equals(violation.getPropertyPath().toString())
                ),
                () -> "Multiline Markdown description should be valid but got " + validViolations
        );

        request.setDescription(" \n\t\n ");
        Set<ConstraintViolation<CodingProblemUpdateRequest>> blankViolations =
                validator.validate(request);
        assertHasPath(blankViolations, "description");
    }

    @Test
    void prepareAgentRejectsMissingAgentTypeAndInvalidCaseId() {
        PrepareAgentRequest request = new PrepareAgentRequest();
        request.setCaseIds(List.of(0L));

        Set<ConstraintViolation<PrepareAgentRequest>> violations =
                validator.validate(request);

        assertHasPath(violations, "agentType");
        assertHasPath(violations, "caseIds[0].<list element>");
    }

    @Test
    void microVideoRequestsRequireCorePayload() {
        MicroRenderRequest renderRequest = new MicroRenderRequest();
        MicroPublishRequest publishRequest = new MicroPublishRequest();

        Set<ConstraintViolation<MicroRenderRequest>> renderViolations =
                validator.validate(renderRequest);
        Set<ConstraintViolation<MicroPublishRequest>> publishViolations =
                validator.validate(publishRequest);

        assertHasPath(renderViolations, "scriptJson");
        assertHasPath(publishViolations, "taskId");
    }

    @Test
    void aiResourceUpdateAcceptsMultilineQuizContentButRejectsWhitespaceOnlyContent() {
        AiResourceUpdateRequest request = new AiResourceUpdateRequest();
        request.setId(1L);
        request.setContent("""
                ## 一、单项选择题
                1. 栈的特点是什么？
                A. 先进先出
                B. 后进先出
                """);

        Set<ConstraintViolation<AiResourceUpdateRequest>> validViolations =
                validator.validate(request);
        assertTrue(
                validViolations.stream().noneMatch(
                        violation -> "content".equals(violation.getPropertyPath().toString())
                ),
                () -> "Multiline quiz content should be valid but got " + validViolations
        );

        request.setContent(" \n\t\n ");
        Set<ConstraintViolation<AiResourceUpdateRequest>> blankViolations =
                validator.validate(request);
        assertHasPath(blankViolations, "content");
    }

    private void assertHasPath(Set<? extends ConstraintViolation<?>> violations,
                               String path) {
        assertTrue(
                violations.stream().anyMatch(
                        violation -> path.equals(violation.getPropertyPath().toString())
                ),
                () -> "Expected violation for " + path + " but got "
                        + violations.stream()
                        .map(violation -> violation.getPropertyPath().toString())
                        .toList()
        );
    }
}
