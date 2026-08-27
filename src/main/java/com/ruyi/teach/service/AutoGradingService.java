package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owns external AI grading calls. It deliberately has no database dependency,
 * so callers cannot accidentally hold a database transaction while waiting on AI.
 */
@Service
public class AutoGradingService {

    private static final Logger log = LoggerFactory.getLogger(AutoGradingService.class);
    private static final Set<String> OBJECTIVE_TYPES = Set.of(
            "radio", "single", "choice",
            "checkbox", "multiple",
            "judge", "true_false", "truefalse",
            "fill"
    );
    private static final Pattern CHOICE_LABEL_PATTERN = Pattern.compile("[A-Z]");

    private final DeepSeekService deepSeekService;
    private final AnswerParser answerParser;
    private final ObjectMapper objectMapper;

    public AutoGradingService(DeepSeekService deepSeekService,
                              AnswerParser answerParser,
                              ObjectMapper objectMapper) {
        this.deepSeekService = deepSeekService;
        this.answerParser = answerParser;
        this.objectMapper = objectMapper;
    }

    public String applyTextAnswerJudgments(List<Map<String, Object>> answers,
                                           HomeworkAssignment assignment) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> aiItems = new ArrayList<>();
        for (Map<String, Object> item : answers) {
            boolean hasImages = answerParser.hasImageUrls(item.get("imageUrls"));
            boolean hasText = answerParser.hasTextAnswer(item.get("answer"));
            String questionType = normalizeQuestionType(item.get("type"));
            String standardAnswer = StringUtils.trimToEmpty(
                    String.valueOf(item.getOrDefault("standardAnswer", ""))
            );
            if (hasImages) {
                item.put("score", null);
                item.put("isCorrect", null);
                item.put("aiComment", "图片作答，待教师批改");
            } else if (!hasText) {
                item.put("score", 0);
                item.put("isCorrect", 0);
                item.put("aiSuggestedScore", 0);
                item.put("aiComment", "未作答，系统记为 0 分");
            } else if (StringUtils.isBlank(standardAnswer) || "null".equalsIgnoreCase(standardAnswer)) {
                item.put("score", null);
                item.put("isCorrect", null);
                item.put("aiComment", "未提供参考答案，待教师判分");
            } else if (isObjectiveQuestionType(questionType)) {
                applyObjectiveJudgment(item, questionType, standardAnswer);
            } else {
                aiItems.add(item);
            }
        }
        if (aiItems.isEmpty()) {
            return null;
        }

        String response;
        try {
            response = deepSeekService.chatJson(
                    "你是作业文字题判定助手。只判断学生答案与参考答案是否等价。"
                            + "每题只能判正确或错误，不给部分分。只输出JSON，不要输出Markdown。",
                    buildJudgmentPrompt(assignment, aiItems),
                    3000
            );
        } catch (Exception e) {
            log.warn("作业文字题AI判定失败，将交由教师判分", e);
            markUnavailable(aiItems);
            return null;
        }
        if (StringUtils.isBlank(response)) {
            markUnavailable(aiItems);
            return null;
        }

        Map<String, AiJudgment> judgments = parseJudgments(response);
        for (Map<String, Object> item : aiItems) {
            AiJudgment judgment = judgments.get(String.valueOf(item.getOrDefault("num", "")));
            if (judgment == null) {
                markUnavailable(item);
                continue;
            }
            int score = judgment.correct ? fullScoreOf(item) : 0;
            item.put("score", score);
            item.put("isCorrect", judgment.correct ? 1 : 0);
            item.put("aiSuggestedScore", score);
            item.put(
                    "aiComment",
                    "AI判定：" + (judgment.correct ? "正确" : "错误")
                            + (StringUtils.isBlank(judgment.comment) ? "" : "。" + judgment.comment)
            );
        }
        return response;
    }

    private boolean isObjectiveQuestionType(String type) {
        return OBJECTIVE_TYPES.contains(type)
                || type.contains("单选")
                || type.contains("多选")
                || type.contains("判断")
                || type.contains("填空");
    }

    private void applyObjectiveJudgment(Map<String, Object> item,
                                        String questionType,
                                        String standardAnswer) {
        boolean correct = objectiveAnswersMatch(questionType, item.get("answer"), standardAnswer);
        int score = correct ? fullScoreOf(item) : 0;
        item.put("score", score);
        item.put("isCorrect", correct ? 1 : 0);
        item.put("aiSuggestedScore", score);
        item.put(
                "aiComment",
                "系统自动判定：" + (correct ? "正确" : "错误")
                        + (correct ? "，答案与参考答案一致" : "，答案与参考答案不一致")
        );
    }

    private boolean objectiveAnswersMatch(String type, Object studentAnswer, String standardAnswer) {
        if (isMultipleChoiceType(type)) {
            Set<String> student = choiceLabels(studentAnswer);
            Set<String> standard = choiceLabels(standardAnswer);
            return !student.isEmpty() && student.equals(standard);
        }
        if (isSingleChoiceType(type)) {
            String student = singleChoiceLabel(studentAnswer);
            String standard = singleChoiceLabel(standardAnswer);
            return StringUtils.isNotBlank(student) && student.equals(standard);
        }
        if (isJudgeType(type)) {
            String student = normalizeJudgeAnswer(studentAnswer);
            String standard = normalizeJudgeAnswer(standardAnswer);
            return StringUtils.isNotBlank(student) && student.equals(standard);
        }
        String student = normalizeComparableAnswer(studentAnswer);
        String standard = normalizeComparableAnswer(standardAnswer);
        return StringUtils.isNotBlank(student) && student.equals(standard);
    }

    private boolean isSingleChoiceType(String type) {
        return "radio".equals(type)
                || "single".equals(type)
                || "choice".equals(type)
                || type.contains("单选");
    }

    private boolean isMultipleChoiceType(String type) {
        return "checkbox".equals(type)
                || "multiple".equals(type)
                || type.contains("多选");
    }

    private boolean isJudgeType(String type) {
        return "judge".equals(type)
                || "true_false".equals(type)
                || "truefalse".equals(type)
                || type.contains("判断");
    }

    private String normalizeQuestionType(Object rawType) {
        return StringUtils.trimToEmpty(rawType == null ? "" : String.valueOf(rawType))
                .toLowerCase(Locale.ROOT);
    }

    private String singleChoiceLabel(Object answer) {
        Set<String> labels = choiceLabels(answer);
        if (labels.size() == 1) {
            return labels.iterator().next();
        }
        return normalizeComparableAnswer(answer);
    }

    private Set<String> choiceLabels(Object answer) {
        Set<String> labels = new TreeSet<>();
        if (answer instanceof Collection<?> collection) {
            collection.forEach(item -> labels.addAll(choiceLabels(item)));
            return labels;
        }
        String text = normalizeAnswerText(answer);
        Matcher matcher = CHOICE_LABEL_PATTERN.matcher(text);
        while (matcher.find()) {
            labels.add(matcher.group());
        }
        return labels;
    }

    private String normalizeJudgeAnswer(Object answer) {
        String text = normalizeAnswerText(answer);
        if (Set.of("正确", "对", "TRUE", "T", "√", "1").contains(text)) {
            return "TRUE";
        }
        if (Set.of("错误", "错", "FALSE", "F", "×", "X", "0").contains(text)) {
            return "FALSE";
        }
        return text;
    }

    private String normalizeComparableAnswer(Object answer) {
        if (answer instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::normalizeComparableAnswer)
                    .sorted()
                    .reduce((left, right) -> left + "|" + right)
                    .orElse("");
        }
        return normalizeAnswerText(answer)
                .replaceAll("<[^>]*>", "")
                .replaceAll("[,，、;；。\\.!！?？\\s]+", "");
    }

    private String normalizeAnswerText(Object answer) {
        return StringUtils.trimToEmpty(answer == null ? "" : String.valueOf(answer))
                .toUpperCase(Locale.ROOT);
    }

    public String gradeHomeworkPaper(String paperContent, List<Map<String, Object>> answers) {
        String prompt = "【试卷与标准答案】：\n\"\"\"\n" + paperContent + "\n\"\"\"\n\n"
                + "【学生的作答(JSON)】：\n\"\"\"\n" + answerParser.toJson(answers) + "\n\"\"\"";
        return deepSeekService.chat(
                "你是一位极其严格的阅卷机器。请根据试卷与标准答案批改学生作答，输出Markdown格式的批改报告。\n"
                        + "报告必须包含：# 综合评分：XX分、## 答题分析（逐题列出判卷过程）、## 辅导建议。\n"
                        + "在报告末尾单独一行输出JSON格式的统计："
                        + "<!--STATS:{\"totalScore\":XX,\"correctCount\":XX,\"wrongCount\":XX}-->",
                prompt,
                4000
        );
    }

    public String gradeExamPaper(String systemPrompt, String gradingPrompt) {
        return deepSeekService.chat(systemPrompt, gradingPrompt, 4000);
    }

    private String buildJudgmentPrompt(HomeworkAssignment assignment, List<Map<String, Object>> items) {
        return "请根据题干、参考答案和学生答案逐题判断。\n"
                + "规则：选择题、判断题、填空题必须答案等价才算正确；简答题只要核心含义覆盖参考答案即可算正确。\n"
                + "作业标题：" + (assignment == null ? "" : StringUtils.defaultString(assignment.getTitle())) + "\n\n"
                + "题目JSON：\n" + answerParser.toJson(items) + "\n\n"
                + "返回格式：{\"judgments\":[{\"questionNo\":\"1\",\"isCorrect\":1,\"comment\":\"简短原因\"}]}";
    }

    private Map<String, AiJudgment> parseJudgments(String response) {
        Map<String, AiJudgment> result = new HashMap<>();
        String json = StringUtils.defaultString(response)
                .replaceAll("(?s)^```(?:json)?\\s*", "")
                .replaceAll("(?s)\\s*```$", "")
                .trim();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.isArray() ? root : root.path("judgments");
            if (!items.isArray()) {
                items = root.path("details");
            }
            if (items.isArray()) {
                for (JsonNode node : items) {
                    String no = node.path("questionNo").asText(node.path("num").asText(""));
                    if (StringUtils.isBlank(no)) {
                        continue;
                    }
                    boolean correct = node.path("isCorrect").asInt(0) == 1
                            || "true".equalsIgnoreCase(node.path("correct").asText(""));
                    result.put(no, new AiJudgment(correct, node.path("comment").asText("")));
                }
            }
        } catch (Exception e) {
            log.warn("解析作业AI判定JSON失败", e);
        }
        return result;
    }

    private int fullScoreOf(Map<String, Object> item) {
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(item.get("fullScore"))));
        } catch (Exception ignore) {
            return 0;
        }
    }

    private void markUnavailable(List<Map<String, Object>> items) {
        items.forEach(this::markUnavailable);
    }

    private void markUnavailable(Map<String, Object> item) {
        item.put("score", null);
        item.put("isCorrect", null);
        item.put("aiComment", "AI判定暂不可用，待教师判分");
    }

    private record AiJudgment(boolean correct, String comment) {
    }
}
