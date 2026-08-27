package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.AiModelClient;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.ClassAnalysisRecordMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.ClassAnalysisRecord;
import com.ruyi.teach.service.AliyunAsrService;
import com.ruyi.teach.service.ClassAnalysisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ClassAnalysisServiceImpl extends ServiceImpl<ClassAnalysisRecordMapper, ClassAnalysisRecord>
        implements ClassAnalysisService {

    @Resource
    private AliyunAsrService aliyunAsrService;

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private AiModelClient aiModelClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * AI 输出的统计标记格式（前端用同一个 pattern 解析）：
     *   <!--STATS:{"interaction":"良好","coverage":"低"}-->
     * 存入 aiReport 的第一行，前端负责解析后剔除再渲染。
     */
    private static final String STATS_MARKER_PREFIX = "<!--STATS:";

    // ============================================================
    // System Prompts
    // ============================================================

    /**
     * 有教案：对照分析，AI 需根据实际情况如实输出 coverage 评级
     */
    private static final String SYSTEM_PROMPT_WITH_PLAN =
            "你是一位资深教学督导专家，同时具备课程论与教学评价的深厚背景。\n"
                    + "用户会同时提供「教案正文」和「课堂实录转写」。\n"
                    + "你的核心任务：以教案为基准，逐项诊断实际课堂的执行情况，给出有依据、可落地的改进建议。\n"
                    + "必须使用 Markdown 格式输出。严禁输出任何开场白、客套话。\n\n"
                    + "【输出格式——必须严格遵守】\n"
                    + "第 1 行：统计标记，格式固定，只改两个值，不得有任何多余字符：\n"
                    + "<!--STATS:{\"interaction\":\"良好\",\"coverage\":\"高\"}-->\n"
                    + "  interaction 取值：良好 / 一般 / 较差（根据师生互动实际情况如实填）\n"
                    + "  coverage   取值：高 / 中 / 低（根据教案知识点实际覆盖情况如实填，若课堂内容与教案无关则填 低）\n"
                    + "第 2 行起：报告正文，直接从第一个 ### 标题开始，不得有任何多余前缀或空行。\n\n"
                    + "报告正文必须包含以下五个模块，每个模块都必须引用转写内容中的具体语句作为佐证：\n"
                    + "### 整体评价\n"
                    + "对本次课堂的整体质量做出综合性判断（100 字以内）。\n\n"
                    + "### 教案执行度\n"
                    + "对照教案中的「教学目标」「教学重难点」「教学环节」，逐项评估执行情况，\n"
                    + "每项标注：✅ 完整执行 / ⚠️ 部分执行 / ❌ 未涉及，并注明原因。\n\n"
                    + "### 知识点覆盖分析\n"
                    + "列出教案核心知识点，标注哪些被充分讲解，哪些被一带而过或遗漏，\n"
                    + "最后给出覆盖率定性评估（高/中/低）。\n\n"
                    + "### 师生互动质量\n"
                    + "分析互动频次、提问类型（开放式/封闭式），引用 1-2 处具体对话片段。\n\n"
                    + "### 改进建议\n"
                    + "给出 3-5 条改进措施，每条遵循「问题来源 → 改进方向 → 参考做法」结构。";

    /**
     * 无教案：通用观察评估，coverage 固定为 N/A
     */
    private static final String SYSTEM_PROMPT_WITHOUT_PLAN =
            "你是一位资深教学督导专家，擅长课堂观察与通用教学质量评估。\n"
                    + "用户未提供教案，仅提供课堂实录转写记录。\n"
                    + "依据布卢姆认知目标分类法、FIAS 课堂互动分析框架等通用教学理论进行评估。\n"
                    + "注意：不得评价「教案执行度」和「知识点覆盖率」，聚焦课堂行为本身可观察的维度。\n"
                    + "必须使用 Markdown 格式输出。严禁输出任何开场白、客套话。\n\n"
                    + "【输出格式——必须严格遵守】\n"
                    + "第 1 行：统计标记，格式固定（coverage 无教案时必须填 N/A）：\n"
                    + "<!--STATS:{\"interaction\":\"良好\",\"coverage\":\"N/A\"}-->\n"
                    + "  interaction 取值：良好 / 一般 / 较差（根据师生互动实际情况如实填）\n"
                    + "  coverage   固定为 N/A\n"
                    + "第 2 行起：报告正文，直接从第一个 ### 标题开始，不得有任何多余前缀或空行。\n\n"
                    + "报告正文必须包含以下五个模块，每个模块都必须引用转写内容中的具体语句作为佐证：\n"
                    + "### 整体评价\n"
                    + "对本次课堂的整体教学风格和质量做出综合性判断（100 字以内）。\n\n"
                    + "### 教学逻辑与节奏\n"
                    + "评估内容组织逻辑脉络、各环节衔接自然度、节奏合理性。\n\n"
                    + "### 师生互动质量\n"
                    + "分析互动频次、提问类型（开放式/封闭式），引用 1-2 处具体对话片段。\n\n"
                    + "### 教学亮点\n"
                    + "挖掘至少 2 处值得肯定的具体教学行为，说明其价值。\n\n"
                    + "### 改进建议\n"
                    + "给出 3-5 条通用的、可立即执行的改进措施。\n"
                    + "> 若关联教案后重新生成报告，可获得教案执行度与知识点覆盖率的精准诊断。";

    // ============================================================
    // 核心业务方法
    // ============================================================

    @Override
    public Long submitAnalysisTask(Long teacherId, String audioUrl, Long planId, String planText) {
        ClassAnalysisRecord record = new ClassAnalysisRecord();
        record.setTeacherId(teacherId);
        record.setAudioUrl(audioUrl);
        record.setPlanText(planText);
        record.setStatus("transcribing");

        if (planId != null) {
            record.setPlanResourceId(planId);
            AiResource resource = aiResourceMapper.selectById(planId);
            if (resource != null && resource.getTitle() != null) {
                record.setPlanTitleSnapshot(resource.getTitle());
            }
        }

        this.save(record);
        return record.getId();
    }

    @Async
    @Override
    public void asyncTranscription(Long recordId) {
        ClassAnalysisRecord record = this.getById(recordId);
        if (record == null) return;

        try {
            String transcriptJson = aliyunAsrService.transcribeAudio(record.getAudioUrl());
            record.setTranscriptJson(transcriptJson);
            record.setStatus("transcribed");
        } catch (Exception e) {
            log.error("转写异常, recordId={}", recordId, e);
            record.setStatus("failed");
        }
        this.updateById(record);
    }

    @Override
    public void generateAndSaveReport(Long recordId) {
        ClassAnalysisRecord record = this.getById(recordId);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }
        if (record.getTranscriptJson() == null || "[]".equals(record.getTranscriptJson())) {
            throw new RuntimeException("转写内容为空，无法生成报告");
        }

        record.setStatus("analyzing");
        this.updateById(record);

        try {
            String readableTranscript = formatTranscriptForAi(record.getTranscriptJson());

            boolean hasPlan = record.getPlanResourceId() != null;
            String systemPrompt;
            String userPrompt;

            if (hasPlan) {
                String planTitle   = record.getPlanTitleSnapshot() != null ? record.getPlanTitleSnapshot() : "未命名教案";
                String planContent = record.getPlanText()          != null ? record.getPlanText()          : "（教案内容未提供）";
                systemPrompt = SYSTEM_PROMPT_WITH_PLAN;
                userPrompt   = "【教案标题】\n" + planTitle + "\n\n"
                        + "【教案正文】\n" + planContent + "\n\n"
                        + "【课堂实录转写】\n" + readableTranscript;
            } else {
                systemPrompt = SYSTEM_PROMPT_WITHOUT_PLAN;
                userPrompt   = "【课堂实录转写】\n" + readableTranscript;
            }

            String rawReport = callDeepSeekSync(userPrompt, systemPrompt);

            // 若 AI 偶发性遗漏了 STATS 标记，补一个默认值，保证前端不崩
            String reportToSave = rawReport;
            if (!rawReport.trim().startsWith(STATS_MARKER_PREFIX)) {
                String fallback = hasPlan
                        ? "<!--STATS:{\"interaction\":\"一般\",\"coverage\":\"待评估\"}-->\n"
                        : "<!--STATS:{\"interaction\":\"一般\",\"coverage\":\"N/A\"}-->\n";
                reportToSave = fallback + rawReport;
                log.warn("AI 未按格式输出 STATS 标记，已补充默认值, recordId={}", recordId);
            }

            record.setAiReport(reportToSave);
            record.setStatus("completed");

        } catch (Exception e) {
            log.error("AI 报告生成失败, recordId={}", recordId, e);
            record.setAiReport(null);
            record.setStatus("failed");
        }

        this.updateById(record);
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /**
     * 将 transcriptJson 数组转成可读对话文本，方便 AI 理解。
     * 输入: [{"role":"teacher","time":"00:01","content":"同学们好"}]
     * 输出: [00:01] 教师：同学们好
     */
    private String formatTranscriptForAi(String transcriptJson) {
        try {
            List<Map<String, Object>> lines = objectMapper.readValue(
                    transcriptJson, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> line : lines) {
                String role    = "student".equals(line.get("role")) ? "学生" : "教师";
                Object time    = line.getOrDefault("time", "");
                Object content = line.getOrDefault("content", "");
                sb.append("[").append(time).append("] ")
                        .append(role).append("：")
                        .append(content).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("转写 JSON 格式化失败，fallback 到原始字符串, error={}", e.getMessage());
            return transcriptJson;
        }
    }

    private String callDeepSeekSync(String userMessage, String systemPrompt) throws Exception {
        return aiModelClient.chat(systemPrompt, userMessage, 0.6, 4000, false);
    }
}
