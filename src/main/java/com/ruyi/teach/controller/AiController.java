package com.ruyi.teach.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.client.AiModelClient;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.AiChatRequest;
import com.ruyi.teach.model.dto.CodingProblemAddRequest;
import com.ruyi.teach.model.dto.CodingProblemGenerateRequest;
import com.ruyi.teach.model.dto.HomeworkGradeRequest;
import com.ruyi.teach.model.dto.TutorChatRequest;
import com.ruyi.teach.model.vo.CodingProblemGenerateVO;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AliyunAsrService;
import com.ruyi.teach.service.DeepSeekService;
import com.ruyi.teach.service.KnowledgeBaseService;
import com.ruyi.teach.service.OssService;
import com.ruyi.teach.service.RemoteDocumentTextService;
import com.ruyi.teach.service.StudentAiProfileService;
import com.ruyi.teach.service.TutorVisionService;
import com.ruyi.teach.util.CaseDocumentTextExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/ai")
//@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@CrossOrigin(origins = {"http://39.105.66.116"}, allowCredentials = "true")
@Tag(name = "AI \u667a\u80fd\u52a9\u6559 - DeepSeek\u7248")
@Slf4j
public class AiController {

    public static AtomicLong aiUsageCount = new AtomicLong(0);

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private AiModelClient aiModelClient;

    @Resource
    private TutorVisionService tutorVisionService;

    @Resource
    private OssService ossService;

    @Resource
    private AliyunAsrService aliyunAsrService;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private RemoteDocumentTextService remoteDocumentTextService;

    @Resource
    private StudentAiProfileService studentAiProfileService;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ====== System Prompts ======
    // \u6ce8\u610f\uff1a\u4e2d\u6587\u5f15\u53f7\u7edf\u4e00\u4f7f\u7528\u300c\u300d\u66ff\u4ee3\uff0c\u907f\u514d\u7f16\u7801\u8f6c\u6362\u5bfc\u81f4\u4e0e Java \u5b57\u7b26\u4e32\u5f15\u53f7\u51b2\u7a81

    private static final String SYSTEM_PROMPT_CHAT =
            "\u4f60\u662f\u4e00\u4f4d\u667a\u6167\u6559\u80b2\u5e73\u53f0\u7684AI\u52a9\u6559\uff0c\u56de\u7b54\u8981\u7b80\u6d01\u3001\u6709\u6761\u7406\u3001\u6709\u6559\u5b66\u573a\u666f\u611f\u3002";

    private static final String SYSTEM_PROMPT_TEACHER_ASSISTANT =
            "你是智慧教育平台教师端的通用教学助手。服务对象是教师，不是学生。"
                    + "回答必须使用简体中文，结构清晰、务实可操作，优先给出可直接用于备课、授课、答疑、评价、学情处理的建议。"
                    + "如果用户上传了文件或图片，必须先基于材料内容回答；材料信息不足时，明确说明不足并给出下一步建议。"
                    + "不要输出空泛口号，不要替教师编造文件中不存在的数据。";

    private static final String SYSTEM_PROMPT_TUTOR =
            "你是智慧教育平台的学生学习助教。你的目标不是替学生完成任务，而是围绕学生当前学习场景帮助他理解、定位问题、形成下一步行动。\n"
                    + "必须使用简体中文，结构清晰，语气鼓励但不空泛。\n"
                    + "你会收到模式 mode 和页面上下文 context。必须优先结合 context 回答；没有上下文时退化为通用学习助手。\n"
                    + "模式规则：\n"
                    + "- hint：只给第一层提示和思考方向，不直接给最终答案或完整代码。\n"
                    + "- answer：可以给完整思路、参考答案或参考代码，但仍要解释关键步骤。\n"
                    + "- explain：解释当前知识点，先讲直觉，再给例子，最后给一个检查问题。\n"
                    + "- check：检查学生答案或代码，指出问题、依据、修改建议，不直接重写整份答案，除非学生明确要求。\n"
                    + "- debug：结合代码、运行结果、错误信息定位问题，按“现象-原因-修改建议-验证方式”回答。\n"
                    + "- practice：只生成 1 道针对当前知识点的小练习，附检查点；不要默认给完整答案。\n"
                    + "- summary：总结当前章节/题目重点、易错点和下一步复习建议。\n"
                    + "编程场景中，如果学生未选择 answer 模式，不要直接输出可复制的完整解法代码。";

    private static final String SYSTEM_PROMPT_PLAN =
            "你是一位资深一线教师与教研员。请根据用户提供的教学参数，直接生成课前可用、可二次编辑的 Markdown 教案。"
                    + "严禁输出前言、开场白、解释性文字、分析过程、客套话。输出必须从教案标题或第一个正式章节开始。"
                    + "默认只输出教学主题、课时信息、学情分析、教学目标、教学重难点、教学准备、教学过程、作业/课后任务。"
                    + "只有用户明确要求板书设计时才输出板书设计；只有用户明确要求反思或复盘时才输出教学反思。"
                    + "不得默认输出教学反思、教学效果评价、改进方向、课后反思。"
                    + "如果用户选择了教学方法，必须让教学过程体现对应活动形态，而不是只写方法名称。"
                    + "如果用户提供了参考案例，请将案例中的关键情境、问题或数据融入课堂导入、任务探究、讨论或练习。"
                    + "每一处参考或借鉴案例内容的位置，在相关段落或条目开头添加标记【案例参考】。";

    private static final String SYSTEM_PROMPT_QUIZ =
            "你是一位严谨的学科命题专家与习题编辑助手。"
                    + "用户可能会给你两类任务："
                    + "第一类：根据完整习题配置生成新的习题；"
                    + "第二类：基于已有习题全文做二次优化。"
                    + "无论是哪一类任务，你都必须输出一份可直接使用、可继续编辑的标准化习题正文。"
                    + "必须使用 Markdown 输出。"
                    + "严禁输出任何前言、开场白、解释性文字、提示语、分析过程、客套话、总结语。"
                    + "不要出现“好的”“下面为您生成”“以下是”“我将”“根据你的要求”等句子。"
                    + "输出必须直接从习题标题、题型标题或第1题开始。"
                    + "如果用户指定了总题数，你必须严格保证总题数完全一致，不能多也不能少。"
                    + "整份习题必须采用“题目区”和“答案解析区”分离的结构。"
                    + "前半部分只输出题目，不得提前泄露答案。"
                    + "后半部分必须在单独一行输出 --- 作为分隔线，然后再输出 ## 参考答案与解析。"
                    + "答案解析区必须与前面的题目编号一一对应。"
                    + "每个题型必须单独成节。"
                    + "整份试卷题号必须从 1 开始连续递增，跨题型也不能重置编号。"
                    + "例如：单项选择题 1-5 题，进入判断题后必须从 6 开始，而不是重新写 1。"
                    + "答案区必须使用同一套整卷连续题号；选择题参考答案只能写选项字母，多选写字母组合，判断题只写正确或错误。"
                    + "如果用户要求优化现有习题，你必须保留“题目区与答案解析区分离”的整体结构，"
                    + "只输出优化后的完整习题正文，不要解释你做了哪些修改。"
                    + "内容要专业、清晰、可用于教师直接编辑和保存。";

    private static final String SYSTEM_PROMPT_REPORT =
            "\u4f60\u662f\u4e00\u4f4d\u6559\u5b66\u6570\u636e\u5206\u6790\u5e08\u3002"
                    + "\u8bf7\u6839\u636e\u7528\u6237\u8f93\u5165\u751f\u6210\u4e13\u4e1a\u3001\u9f13\u52b1\u5f0f\u7684\u5b66\u60c5\u5206\u6790\u62a5\u544a\uff0c"
                    + "\u5305\u542b\uff1a\u6574\u4f53\u6982\u51b5\u3001\u4f18\u52bf\u3001\u8584\u5f31\u70b9\u3001\u6539\u8fdb\u5efa\u8bae\u3002";

    private static final String SYSTEM_PROMPT_GRADE_REPORT =
            "你是一位资深教学数据分析专家。用户会提供一份班级成绩统计摘要（包含平均分、最高分、最低分、及格率、优秀率、分数段分布等）。"
                    + "请根据这些数据，生成一份专业、结构清晰的学情分析报告。"
                    + "必须使用 Markdown 格式输出。"
                    + "报告需包含以下模块：\n"
                    + "## 整体概况\n对班级整体成绩进行客观评价（优秀/良好/一般/偏低），结合平均分与及格率判断。\n"
                    + "## 成绩分布分析\n分析分数段分布是否合理，是否存在两极分化，正态分布情况。\n"
                    + "## 亮点与优势\n挖掘数据中的积极信号。\n"
                    + "## 薄弱环节\n指出需要关注的问题（如不及格率偏高、尾部学生过多等）。\n"
                    + "## 教学改进建议\n给出 3-5 条具体、可执行的教学改进措施。\n"
                    + "## 个别化关注建议\n针对不同层次学生的差异化辅导建议。\n"
                    + "语言要专业但不晦涩，对教师有实际参考价值。严禁输出开场白、客套话。直接从报告正文开始。";

    private static final String SYSTEM_PROMPT_ANALYSIS =
            "你是课表识别助手与学业规划顾问。用户上传了一份课表文本。"
                    + "请提取课程名称，分析课程负担，并给出学业建议。"
                    + "以 json 格式返回，只返回 json，不要 Markdown。"
                    + "返回格式："
                    + "{\"currentCourses\":[\"课程名1\",\"课程名2\"],"
                    + "\"semester\":1,"
                    + "\"insights\":{"
                    + "\"workloadVerdict\":\"对本学期课程负担的整体评价，一句话总结\","
                    + "\"recommendations\":[\"学业建议1\",\"学业建议2\",\"学业建议3\"],"
                    + "\"riskWarnings\":[\"如有风险点则列出，无风险可为空数组\"]}}";

    private static final String SYSTEM_PROMPT_PSYCHO =
            "你是一位认知心理学与学习科学专家。"
                    + "用户会提供主观量表和客观疲劳监测数据。"
                    + "请综合评估，并严格以 JSON 格式返回，不要 Markdown，不要代码块，不要额外解释。"
                    + "除字段名外，所有返回内容必须使用简体中文，严禁输出英文。"
                    + "返回字段必须包含：stressLevel、energyLevel、focusLevel、"
                    + "cognitiveLoad、flowScore、emotionScore、theories、riskFlags、suggestions、verdict。"
                    + "其中 stressLevel、energyLevel、focusLevel、cognitiveLoad、flowScore、emotionScore "
                    + "必须为 1 到 10 的整数。"
                    + "theories 必须是字符串数组，数组中的每一项都必须是简体中文。"
                    + "riskFlags 必须是字符串数组，数组中的每一项都必须是简体中文。"
                    + "verdict 必须是一段简体中文总结。"
                    + "suggestions 必须是对象，格式固定为："
                    + "{\"immediate\":\"立即执行建议\",\"shortTerm\":\"本周调整建议\",\"habit\":\"长期习惯建议\"}。"
                    + "当主观自评与客观生理数据不一致时，必须在 riskFlags 和 verdict 中明确指出“主客观数据存在不一致”。"
                    + "语言风格要专业、清晰、适合学生端展示，不要出现英文标签或英文句子。";

    // ============================================================
    // \u52a8\u753b\u8bfe\u4ef6 System Prompt\uff08\u4f7f\u7528 StringBuilder \u6784\u5efa\uff0c\u907f\u514d\u5f15\u53f7\u51b2\u7a81\uff09
    // ============================================================
    private static final String SYSTEM_PROMPT_ANIM = buildAnimSystemPrompt();

    private static String buildAnimSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        // \u4f60\u662f\u4e00\u4f4d\u7cbe\u901a\u524d\u7aef\u5f00\u53d1\u7684\u6559\u5b66\u52a8\u753b\u8bfe\u4ef6\u5de5\u7a0b\u5e08...
        sb.append("\u4f60\u662f\u4e00\u4f4d\u7cbe\u901a\u524d\u7aef\u5f00\u53d1\u7684\u6559\u5b66\u52a8\u753b\u8bfe\u4ef6\u5de5\u7a0b\u5e08\u3002");
        sb.append("\u4f60\u7684\u4efb\u52a1\u662f\u751f\u6210\u4e00\u4e2a\u7528\u4e8e\u89e3\u91ca\u62bd\u8c61\u6982\u5ff5\u7684\u3001\u53ef\u8fd0\u884c\u7684\u5355\u6587\u4ef6\u6559\u5b66\u52a8\u753bHTML\u3002\n\n");

        // \u3010\u7edd\u5bf9\u89c4\u5219\u3011
        sb.append("\u3010\u7edd\u5bf9\u89c4\u5219\u3011\n");
        sb.append("- \u53ea\u8f93\u51fa\u5b8c\u6574HTML\u4ee3\u7801\uff0c\u4e0d\u8981\u4efb\u4f55\u89e3\u91ca\u3001Markdown\u6807\u8bb0\u3001\u4ee3\u7801\u5757\u6807\u8bb0\u3002\n");
        sb.append("- \u7b2c\u4e00\u4e2a\u5b57\u7b26\u5fc5\u987b\u662f < \uff0c\u6700\u540e\u4e00\u4e2a\u5b57\u7b26\u5fc5\u987b\u662f > \u3002\n");
        sb.append("- \u6240\u6709CSS\u3001JS\u5fc5\u987b\u5185\u8054\u5728\u540c\u4e00\u4e2aHTML\u6587\u4ef6\u4e2d\u3002\n");
        sb.append("- \u4e0d\u5f97\u5f15\u7528\u4efb\u4f55\u5916\u90e8CDN\u3001\u5916\u90e8\u5e93\u3001\u5916\u90e8\u56fe\u7247\u3001\u5916\u90e8\u5b57\u4f53\u3002\n");
        sb.append("- \u6240\u6709\u6587\u5b57\u5fc5\u987b\u4f7f\u7528\u4e2d\u6587\u3002\n\n");

        // \u3010\u6781\u5176\u91cd\u8981 \u2014 \u7d27\u51d1\u5c0f\u5c3a\u5bf8\u8bbe\u8ba1\u3011
        sb.append("\u3010\u6781\u5176\u91cd\u8981 \u2014 \u7d27\u51d1\u5c0f\u5c3a\u5bf8\u8bbe\u8ba1\u3011\n");
        sb.append("\u8bfe\u4ef6\u4f1a\u5d4c\u5165\u5728\u4e00\u4e2a\u5f88\u5c0f\u7684 iframe \u4e2d\u9884\u89c8\uff08\u7ea6 800x500 \u50cf\u7d20\uff09\u3002\n");
        sb.append("\u56e0\u6b64\u5fc5\u987b\u505a\u5230\uff1a\n");
        sb.append("- \u6807\u9898\u680f\uff1a\u53ea\u7528\u4e00\u884c\uff0cpadding \u6700\u591a 8px 16px\uff0c\u6807\u9898\u5b57\u53f7 15px\uff0c\u526f\u6807\u9898 11px\u3002\n");
        sb.append("- \u52a8\u753b\u533a\uff1a\u5143\u7d20\u8981\u5c0f\uff08\u65b9\u5757 36-44px\uff0c\u5706\u5708 30-40px\uff09\uff0c\u5b57\u53f7 12px\u3002\n");
        sb.append("- \u6b65\u9aa4\u8bb2\u89e3\u533a\uff1a\u653e\u5728\u52a8\u753b\u533a\u4e0b\u65b9\uff08\u4e0d\u662f\u53f3\u4fa7\uff09\uff0c\u9ad8\u5ea6\u56fa\u5b9a 80-100px\uff0c\u6807\u9898 13px\uff0c\u6b63\u6587 12px\u3002\n");
        sb.append("- \u63a7\u5236\u680f\uff1a\u6309\u94ae padding 4px 14px\uff0c\u5b57\u53f7 12px\uff0c\u6574\u4f53 padding 6px\u3002\n");
        sb.append("- \u7981\u6b62\u51fa\u73b0\u6eda\u52a8\u6761\uff0c\u6240\u6709\u5185\u5bb9\u5fc5\u987b\u5728\u4e00\u5c4f\u5185\u5b8c\u6574\u663e\u793a\u3002\n\n");

        // \u3010HTML\u7ed3\u6784\u6a21\u677f\u3011 \u2014 \u4e0a\u4e0b\u56db\u884c\u5e03\u5c40\uff08\u6807\u9898 + \u52a8\u753b\u533a + \u8bb2\u89e3\u533a + \u63a7\u5236\u680f\uff09
        sb.append("\u3010HTML\u7ed3\u6784\u6a21\u677f \u2014 \u4e0a\u4e0b\u56db\u884c\u5e03\u5c40\u3011\n");
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang='zh-CN'>\n");
        sb.append("<head>\n");
        sb.append("  <meta charset='UTF-8'>\n");
        sb.append("  <meta name='viewport' content='width=device-width,initial-scale=1.0'>\n");
        sb.append("  <title>\u8bfe\u4ef6\u6807\u9898</title>\n");
        sb.append("  <style>\n");
        sb.append("    *{box-sizing:border-box;margin:0;padding:0}\n");
        sb.append("    html,body{width:100%;height:100%;overflow:hidden;font-family:'PingFang SC','Microsoft YaHei',sans-serif;background:#f7f9fc;color:#1a202c;font-size:13px}\n");
        sb.append("    .app{display:grid;grid-template-rows:auto 1fr auto auto;width:100%;height:100%}\n");
        sb.append("    .header{padding:8px 16px;background:#fff;border-bottom:1px solid #e2e8f0;text-align:center}\n");
        sb.append("    .header h1{font-size:15px;font-weight:700;color:#2d3748}\n");
        sb.append("    .header .subtitle{font-size:11px;color:#718096;margin-top:2px}\n");
        sb.append("    .canvas-area{position:relative;background:#fff;display:flex;align-items:center;justify-content:center;overflow:hidden;padding:12px}\n");
        sb.append("    .step-info{padding:8px 16px;background:#f7fafc;border-top:1px solid #e2e8f0;min-height:60px;max-height:90px;overflow-y:auto}\n");
        sb.append("    .step-info .step-title{font-size:13px;font-weight:700;color:#2d3748;margin-bottom:3px}\n");
        sb.append("    .step-info .step-desc{font-size:12px;line-height:1.5;color:#4a5568}\n");
        sb.append("    .step-info .progress{font-size:11px;color:#a0aec0;margin-top:4px}\n");
        sb.append("    .controls{display:flex;align-items:center;justify-content:center;gap:8px;padding:6px 16px;background:#fff;border-top:1px solid #e2e8f0}\n");
        sb.append("    .controls button{padding:4px 14px;border-radius:6px;border:1px solid #cbd5e0;background:#fff;color:#2d3748;font-size:12px;cursor:pointer;transition:all .2s}\n");
        sb.append("    .controls button:hover{background:#edf2f7}\n");
        sb.append("    .controls button.primary{background:#3182ce;color:#fff;border-color:#3182ce}\n");
        sb.append("    .controls button.primary:hover{background:#2b6cb0}\n");
        sb.append("    .controls button.success{background:#38a169;color:#fff;border-color:#38a169}\n");
        sb.append("    .controls button:disabled{opacity:.45;cursor:not-allowed}\n");
        sb.append("    .element{transition:all .4s ease}\n");
        sb.append("    .highlight{box-shadow:0 0 0 2px #f6ad55;transform:scale(1.05)}\n");
        sb.append("    .sorted{background:#c6f6d5 !important;color:#22543d !important}\n");
        sb.append("  </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("  <div class='app'>\n");
        sb.append("    <div class='header'>\n");
        sb.append("      <h1>\u8bfe\u4ef6\u6807\u9898</h1>\n");
        sb.append("      <div class='subtitle'>\u526f\u6807\u9898\u8bf4\u660e</div>\n");
        sb.append("    </div>\n");
        sb.append("    <div class='canvas-area'><!-- \u52a8\u753b\u5143\u7d20\u533a --></div>\n");
        sb.append("    <div class='step-info'><!-- \u5f53\u524d\u6b65\u9aa4\u8bb2\u89e3 + \u8fdb\u5ea6 --></div>\n");
        sb.append("    <div class='controls'><!-- \u4e0a\u4e00\u6b65 / \u4e0b\u4e00\u6b65 / \u81ea\u52a8\u64ad\u653e / \u91cd\u7f6e --></div>\n");
        sb.append("  </div>\n");
        sb.append("  <script>\n");
        sb.append("    const steps = [ /* { title, desc, action() } */ ];\n");
        sb.append("    let current = 0, timer = null;\n");
        sb.append("    function render(idx) { /* ... */ }\n");
        sb.append("    function prev() { if(current>0){current--;render(current)} }\n");
        sb.append("    function next() { if(current<steps.length-1){current++;render(current)} }\n");
        sb.append("    function togglePlay() { /* ... */ }\n");
        sb.append("    function reset() { current=0;render(0) }\n");
        sb.append("    render(0);\n");
        sb.append("  </script>\n");
        sb.append("</body>\n");
        sb.append("</html>\n\n");

        // \u3010\u5173\u952e\u6280\u672f\u8981\u6c42\u3011
        sb.append("\u3010\u5173\u952e\u6280\u672f\u8981\u6c42\u3011\n");
        sb.append("1. \u5e03\u5c40\uff1a\u4e0a\u4e0b\u56db\u884c Grid\uff08\u6807\u9898 + \u52a8\u753b\u533a + \u8bb2\u89e3\u533a + \u63a7\u5236\u680f\uff09\uff0c\u7981\u6b62\u5de6\u53f3\u5206\u680f\uff0c\u7981\u6b62 position:absolute \u505a\u5e03\u5c40\u3002\n");
        sb.append("2. \u52a8\u753b\u533a\u5360\u9875\u9762\u4e3b\u8981\u7a7a\u95f4\uff0c\u5143\u7d20\u8981\u5c0f\u800c\u7d27\u51d1\uff08\u65b9\u5757 36-44px\uff0c\u95f4\u8ddd 4-6px\uff09\u3002\n");
        sb.append("3. \u6b65\u9aa4\u8bb2\u89e3\u533a\u5728\u52a8\u753b\u533a\u4e0b\u65b9\uff0c\u663e\u793a\u6b65\u9aa4\u6807\u9898\u3001\u4e00\u53e5\u8bdd\u8bf4\u660e\u3001\u8fdb\u5ea6\uff08\u5982\u300c\u6b65\u9aa4 3/8\u300d\uff09\u3002\n");
        sb.append("4. \u52a8\u753b\u5143\u7d20\uff1a\u6392\u5e8f/\u6570\u636e\u7ed3\u6784\u7c7b\u7528\u5f69\u8272\u5c0f\u65b9\u5757\uff1b\u534f\u8bae\u7c7b\u7528\u5de6\u53f3\u4e24\u5217 + \u7bad\u5934\u3002\u5fc5\u987b\u6709\u989c\u8272\u53d8\u5316\u3001\u9ad8\u4eae\u8fb9\u6846\u7b49\u89c6\u89c9\u53cd\u9988\u3002\n");
        sb.append("5. steps \u6570\u7ec4\uff1a6-10 \u6b65\uff0c\u6bcf\u6b65\u5305\u542b title\u3001desc\u3001action()\u3002render(idx) \u91cd\u7f6e\u540e\u4f9d\u6b21\u6267\u884c action\u3002\n");
        sb.append("6. \u81ea\u52a8\u64ad\u653e\uff1a\u6bcf 1.5 \u79d2\u8c03\u7528 next()\uff0c\u6309\u94ae\u6587\u5b57\u5728\u300c\u81ea\u52a8\u64ad\u653e\u300d\u548c\u300c\u6682\u505c\u300d\u95f4\u5207\u6362\u3002\n");
        sb.append("7. \u989c\u8272\uff1a\u84dd\u7070\u8272\u7cfb\uff0c\u9ad8\u4eae\u7528\u6696\u8272\uff0c\u5b8c\u6210\u7528\u7eff\u8272\u3002\n");
        sb.append("8. html,body \u5fc5\u987b overflow:hidden\uff0c\u7981\u6b62\u6eda\u52a8\u6761\uff0c\u6240\u6709\u5185\u5bb9\u5728\u4e00\u5c4f\u5185\u5b8c\u6574\u663e\u793a\u3002\n");
        sb.append("9. \u751f\u6210\u7684HTML\u5fc5\u987b\u53ef\u76f4\u63a5\u5728 800x500 \u50cf\u7d20\u7684 iframe \u4e2d\u5b8c\u7f8e\u8fd0\u884c\u3002\n");

        return sb.toString();
    }

    // ============================================================
    // \u52a8\u753b\u8bfe\u4ef6\u4f18\u5316\u4e13\u7528 System Prompt
    // ============================================================
    private static final String SYSTEM_PROMPT_ANIM_OPTIMIZE = buildAnimOptimizePrompt();

    private static String buildAnimOptimizePrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("\u4f60\u662f\u4e00\u4f4d\u7cbe\u901a\u524d\u7aef\u5f00\u53d1\u7684\u6559\u5b66\u52a8\u753b\u8bfe\u4ef6\u4f18\u5316\u5e08\u3002");
        sb.append("\u7528\u6237\u4f1a\u63d0\u4f9b\u4e00\u4efd\u5df2\u6709\u7684\u6559\u5b66\u52a8\u753bHTML\u8bfe\u4ef6\u548c\u4f18\u5316\u6307\u4ee4\u3002\n\n");
        sb.append("\u3010\u4f60\u7684\u4efb\u52a1\u3011\n");
        sb.append("\u6839\u636e\u7528\u6237\u7684\u4f18\u5316\u6307\u4ee4\uff0c\u5728\u5df2\u6709HTML\u7684\u57fa\u7840\u4e0a\u8fdb\u884c\u6539\u8fdb\uff0c\u8f93\u51fa\u4f18\u5316\u540e\u7684\u5b8c\u6574HTML\u3002\n\n");
        sb.append("\u3010\u7edd\u5bf9\u89c4\u5219\u3011\n");
        sb.append("- \u53ea\u8f93\u51fa\u5b8c\u6574HTML\u4ee3\u7801\uff0c\u4e0d\u8981\u4efb\u4f55\u89e3\u91ca\u6216Markdown\u6807\u8bb0\u3002\n");
        sb.append("- \u7b2c\u4e00\u4e2a\u5b57\u7b26\u5fc5\u987b\u662f < \uff0c\u6700\u540e\u4e00\u4e2a\u5b57\u7b26\u5fc5\u987b\u662f > \u3002\n");
        sb.append("- \u4fdd\u6301\u539f\u6709\u8bfe\u4ef6\u7684\u4e3b\u9898\u548c\u6838\u5fc3\u903b\u8f91\uff0c\u53ea\u505a\u9488\u5bf9\u6027\u4f18\u5316\u3002\n");
        sb.append("- \u4fdd\u7559\u4e09\u884cGrid\u5e03\u5c40\u7ed3\u6784\u3002\n");
        sb.append("- \u6240\u6709CSS\u3001JS\u5fc5\u987b\u5185\u8054\u3002\u4e0d\u5f97\u5f15\u7528\u4efb\u4f55\u5916\u90e8\u8d44\u6e90\u3002\n");
        sb.append("- \u6240\u6709\u6587\u5b57\u4f7f\u7528\u4e2d\u6587\u3002\n");
        sb.append("- \u786e\u4fdd\u8f93\u51fa\u7684HTML\u53ef\u76f4\u63a5\u5728 iframe \u4e2d\u8fd0\u884c\u3002\n");
        sb.append("- \u8bfe\u4ef6\u5fc5\u987b\u9002\u914d 800x500 \u50cf\u7d20\u7684\u5c0f\u5c3a\u5bf8\u9884\u89c8\uff0c\u7981\u6b62\u6eda\u52a8\u6761\uff0c\u6240\u6709\u5185\u5bb9\u4e00\u5c4f\u663e\u793a\u3002\n");
        sb.append("- \u4f7f\u7528\u4e0a\u4e0b\u56db\u884c\u5e03\u5c40\uff08\u6807\u9898 + \u52a8\u753b\u533a + \u8bb2\u89e3\u533a + \u63a7\u5236\u680f\uff09\uff0c\u4e0d\u8981\u5de6\u53f3\u5206\u680f\u3002\n");
        return sb.toString();
    }

    private static final String SYSTEM_PROMPT_ANIM_JSON = buildAnimJsonSystemPrompt();
    private static final String SYSTEM_PROMPT_ANIM_OPTIMIZE_JSON = buildAnimOptimizeJsonPrompt();


    private static final String SYSTEM_PROMPT_WRITING_POLISH =
            "你是一位专业的中文写作助手。用户会给你一段文字，请对其进行润色优化，使其更加流畅、准确、优美。只输出润色后的文字，不要解释，不要输出开场白。";

    private static final String SYSTEM_PROMPT_WRITING_CONTINUE =
            "你是一位专业的中文写作助手。用户会给你一段文字，请自然地续写下去，保持文风和语境一致。只输出续写内容，不要重复原文，不要解释，不要输出开场白。";

    private static final String SYSTEM_PROMPT_WRITING_REWRITE =
            "你是一位专业的中文写作助手。用户会给你一段文字，请用不同的表达方式重新改写，保持原意不变但更生动有力。只输出改写后的文字，不要解释，不要输出开场白。";

    private static final String SYSTEM_PROMPT_WRITING_EXPAND =
            "你是一位专业的中文写作助手。用户会给你一段文字，请在保持原意的基础上扩充细节和论述，使内容更丰富。只输出扩写后的文字，不要解释，不要输出开场白。";

    private static final String SYSTEM_PROMPT_WRITING_SUMMARIZE =
            "你是一位专业的中文写作助手。用户会给你一段文字，请用精炼的语言进行总结概括。只输出总结内容，不要解释，不要输出开场白。";

    private static final String SYSTEM_PROMPT_ARTICLE =
            "你是一位资深的教育行业写作专家。请根据用户提供的文章类型、主题和目录结构，生成一篇专业、完整、可直接使用的文章。"
                    + "必须使用 Markdown 格式输出。严禁输出任何前言、开场白、解释性文字、客套话。"
                    + "输出必须直接从文章标题或第一个章节开始。内容要具体、专业、有深度，避免空泛套话。";

    private static final String SYSTEM_PROMPT_HOMEWORK_GRADE =
            "你是一位严谨、耐心的学科批改老师。"
                    + "用户会提供试卷原文和学生的作答记录。"
                    + "请逐题批改，给出每题得分与简明评语，最后给出总分与整体学习建议。"
                    + "必须使用 Markdown 输出，结构清晰。"
                    + "批改风格：客观公正，语气鼓励，指出错误原因并给出正确解法。"
                    + "严禁输出开场白、客套话，直接从批改正文开始。";

    private static final String SYSTEM_PROMPT_CODING_GENERATE =
            "你是一位编程教育专家和算法竞赛命题人。请根据教师的需求生成一道完整的在线编程练习题。\n"
                    + "【要求】\n"
                    + "1. 返回必须是合法的 JSON 对象，不要包含任何 Markdown 代码块标记（如 ```json），不要包含任何解释性文字或开场白。\n"
                    + "2. JSON 结构必须严格如下：\n"
                    + "{\n"
                    + "  \"title\": \"题目标题（简洁明了，不超过30字）\",\n"
                    + "  \"description\": \"题目描述，使用 Markdown 格式。包含：题目背景、输入格式、输出格式、样例说明、数据范围、注意事项。\",\n"
                    + "  \"difficulty\": \"easy|medium|hard\",\n"
                    + "  \"languages\": [\"java\", \"python\", \"cpp\", \"javascript\"],\n"
                    + "  \"timeLimitMs\": 5000,\n"
                    + "  \"memoryLimitKb\": 262144,\n"
                    + "  \"testCases\": [\n"
                    + "    {\n"
                    + "      \"input\": \"标准输入内容\",\n"
                    + "      \"expectedOutput\": \"标准输出内容\",\n"
                    + "      \"isSample\": 1,\n"
                    + "      \"score\": 0\n"
                    + "    }\n"
                    + "  ],\n"
                    + "  \"templates\": [\n"
                    + "    {\n"
                    + "      \"language\": \"java\",\n"
                    + "      \"starterCode\": \"public class Main { public static void main(String[] args) { // 请在此处完成代码 } }\",\n"
                    + "      \"referenceSolution\": \"public class Main { ... }\"\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}\n"
                    + "3. 测试用例要求：\n"
                    + "   - 至少包含 1 个样例测试用例（isSample=1，score=0）和 2 个隐藏测试用例（isSample=0，score>0）。\n"
                    + "   - 隐藏测试用例的 score 建议设为 10-30 分。\n"
                    + "   - expectedOutput 必须精确，不要有多余空格、空行或解释性文字。\n"
                    + "   - 如果输入为空，input 字段设为空字符串。\n"
                    + "4. 代码模板要求：\n"
                    + "   - starterCode：学生打开编辑器时看到的初始代码，必须包含 main 函数（或对应语言的入口）和读取输入的框架，核心逻辑处留注释提示。\n"
                    + "   - referenceSolution：完整的参考实现，必须能通过所有测试用例。\n"
                    + "   - 每种支持的语言都必须提供对应的模板。\n"
                    + "5. 题目描述要求：\n"
                    + "   - 清晰说明输入格式和输出格式。\n"
                    + "   - 提供具体的样例（输入、输出、解释）。\n"
                    + "   - 数据范围要合理。\n";

    private static String buildAnimJsonSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位教学动画课件结构设计师。");
        sb.append("你的任务不是生成HTML，而是把抽象概念转换为严格可解析的JSON课件数据。\n\n");

        sb.append("【绝对规则】\n");
        sb.append("- 只能输出合法JSON对象，不要输出Markdown，不要输出解释。\n");
        sb.append("- 不要输出代码块标记。\n");
        sb.append("- 所有字段名必须严格使用约定名称。\n");
        sb.append("- 所有说明文字使用中文。\n");
        sb.append("- steps必须是数组，且至少3步；concept模板建议5到7步。\n");
        sb.append("- 只允许输出七类模板：sort、protocol、stack、queue、tree、graph、concept。\n");
        sb.append("- 排序/数组比较/交换类，输出 templateType=sort。\n");
        sb.append("- TCP/HTTP/握手/请求响应/消息传递类，输出 templateType=protocol。\n");
        sb.append("- 栈/入栈/出栈/后进先出/LIFO类，输出 templateType=stack。\n");
        sb.append("- 队列/入队/出队/先进先出/FIFO类，输出 templateType=queue。\n");
        sb.append("- 二叉树/BST/树遍历/堆结构类，输出 templateType=tree。\n");
        sb.append("- 图/BFS/DFS/最短路径/拓扑关系类，输出 templateType=graph。\n");
        sb.append("- 链表、哈希、递归、条件分支、循环、通用抽象概念，输出 templateType=concept，并为多数步骤提供visual视觉原语。\n\n");
        sb.append("- 动画流程是主解释，文字只做短提示；不要用大段文字解释代替动画。\n");

        sb.append("【统一外层结构】\n");
        sb.append("{\n");
        sb.append("  \"templateType\": \"sort 或 protocol 或 stack 或 queue 或 tree 或 graph 或 concept\",\n");
        sb.append("  \"title\": \"标题\",\n");
        sb.append("  \"subtitle\": \"副标题\",\n");
        sb.append("  \"targetGroup\": \"适用对象\",\n");
        sb.append("  \"teachingGoal\": \"教学目标\",\n");
        sb.append("  \"steps\": []\n");
        sb.append("}\n\n");

        sb.append("【所有 step 通用动画字段】\n");
        sb.append("- stageCaption：图上短提示，最多一句话，建议不超过25个汉字。\n");
        sb.append("- motion：可选对象，用来描述可见动作。type 只能取 observe、compare、swap、send、push、pop、enqueue、dequeue、peek、visit、insert-node、delete-node、branch、call、return、flow、done。\n");
        sb.append("- motion 可包含 indexes、fromIndex、toIndex、value、from、to、path、branch 等辅助字段。\n");
        sb.append("- 每一步必须先能被画出来，再写一句 desc；desc 不要超过40个汉字。\n\n");

        sb.append("【sort 模板结构】\n");
        sb.append("{\n");
        sb.append("  \"templateType\": \"sort\",\n");
        sb.append("  \"title\": \"冒泡排序算法分步推演\",\n");
        sb.append("  \"subtitle\": \"通过相邻元素比较与交换，将最大元素逐步移动到末尾\",\n");
        sb.append("  \"targetGroup\": \"本科一年级\",\n");
        sb.append("  \"teachingGoal\": \"帮助学生理解冒泡排序如何逐轮把最大值移动到末尾\",\n");
        sb.append("  \"initialData\": [5,1,4,2,8],\n");
        sb.append("  \"steps\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"比较第1对元素\",\n");
        sb.append("      \"desc\": \"比较5和1，因为5更大，所以交换它们。\",\n");
        sb.append("      \"stageCaption\": \"5比1大，两个方块交换\",\n");
        sb.append("      \"motion\": {\"type\":\"swap\",\"indexes\":[0,1],\"fromIndex\":0,\"toIndex\":1},\n");
        sb.append("      \"array\": [1,5,4,2,8],\n");
        sb.append("      \"highlight\": [0,1],\n");
        sb.append("      \"swap\": [0,1],\n");
        sb.append("      \"sortedTailStart\": 5\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("【stack 模板结构】\n");
        sb.append("{\n");
        sb.append("  \"templateType\": \"stack\",\n");
        sb.append("  \"title\": \"栈的深入浅出\",\n");
        sb.append("  \"subtitle\": \"通过入栈与出栈理解后进先出\",\n");
        sb.append("  \"targetGroup\": \"本科一年级\",\n");
        sb.append("  \"teachingGoal\": \"帮助学生理解栈顶操作与后进先出\",\n");
        sb.append("  \"initialStack\": [],\n");
        sb.append("  \"steps\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"入栈 A\",\n");
        sb.append("      \"desc\": \"把 A 放入栈中，A 成为当前栈顶。\",\n");
        sb.append("      \"stageCaption\": \"A 从栈外压到栈顶\",\n");
        sb.append("      \"motion\": {\"type\":\"push\",\"value\":\"A\"},\n");
        sb.append("      \"stack\": [\"A\"],\n");
        sb.append("      \"operation\": \"push\",\n");
        sb.append("      \"activeValue\": \"A\",\n");
        sb.append("      \"poppedValue\": null\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("【queue 模板结构】\n");
        sb.append("{\n");
        sb.append("  \"templateType\": \"queue\",\n");
        sb.append("  \"title\": \"队列入队出队演示\",\n");
        sb.append("  \"subtitle\": \"从队尾进入，从队头离开，理解先进先出\",\n");
        sb.append("  \"targetGroup\": \"本科一年级\",\n");
        sb.append("  \"teachingGoal\": \"帮助学生理解队头、队尾与先进先出规则\",\n");
        sb.append("  \"initialQueue\": [],\n");
        sb.append("  \"steps\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"A 入队\",\n");
        sb.append("      \"desc\": \"A 从队尾进入，成为当前队头。\",\n");
        sb.append("      \"stageCaption\": \"A 从队尾进入\",\n");
        sb.append("      \"motion\": {\"type\":\"enqueue\",\"value\":\"A\"},\n");
        sb.append("      \"queue\": [\"A\"],\n");
        sb.append("      \"operation\": \"enqueue\",\n");
        sb.append("      \"activeValue\": \"A\",\n");
        sb.append("      \"removedValue\": null\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("【tree 模板结构】\n");
        sb.append("{\n");
        sb.append("  \"templateType\": \"tree\",\n");
        sb.append("  \"title\": \"二叉搜索树查找\",\n");
        sb.append("  \"subtitle\": \"沿着比较结果逐层缩小查找范围\",\n");
        sb.append("  \"targetGroup\": \"本科一年级\",\n");
        sb.append("  \"teachingGoal\": \"帮助学生理解 BST 如何选择左右子树\",\n");
        sb.append("  \"root\": {\"value\":7,\"left\":{\"value\":3,\"right\":{\"value\":5}},\"right\":{\"value\":10}},\n");
        sb.append("  \"steps\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"访问节点 3\",\n");
        sb.append("      \"desc\": \"目标值比 7 小，转向左子树。\",\n");
        sb.append("      \"stageCaption\": \"沿路径来到节点 3\",\n");
        sb.append("      \"motion\": {\"type\":\"visit\",\"path\":[7,3]},\n");
        sb.append("      \"currentNode\": 3,\n");
        sb.append("      \"path\": [7,3],\n");
        sb.append("      \"visited\": [7,3],\n");
        sb.append("      \"operation\": \"go-left\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("【graph 模板结构】\n");
        sb.append("{\n");
        sb.append("  \"templateType\": \"graph\",\n");
        sb.append("  \"title\": \"BFS 图遍历\",\n");
        sb.append("  \"subtitle\": \"用队列按层访问图中的节点\",\n");
        sb.append("  \"targetGroup\": \"本科一年级\",\n");
        sb.append("  \"teachingGoal\": \"帮助学生理解 BFS 的 frontier 和 visited\",\n");
        sb.append("  \"nodes\": [\"A\",\"B\",\"C\",\"D\"],\n");
        sb.append("  \"edges\": [{\"from\":\"A\",\"to\":\"B\",\"directed\":false}],\n");
        sb.append("  \"steps\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"访问 A\",\n");
        sb.append("      \"desc\": \"访问 A，并把相邻节点加入 frontier。\",\n");
        sb.append("      \"stageCaption\": \"A 扩展到 B\",\n");
        sb.append("      \"motion\": {\"type\":\"visit\",\"value\":\"A\"},\n");
        sb.append("      \"activeNode\": \"A\",\n");
        sb.append("      \"visited\": [\"A\"],\n");
        sb.append("      \"frontier\": [\"B\"],\n");
        sb.append("      \"activeEdges\": [{\"from\":\"A\",\"to\":\"B\"}],\n");
        sb.append("      \"operation\": \"visit\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("【protocol 模板结构】\n");
        sb.append("{\n");
        sb.append("  \"templateType\": \"protocol\",\n");
        sb.append("  \"title\": \"TCP 三次握手\",\n");
        sb.append("  \"subtitle\": \"建立可靠连接并同步初始序列号\",\n");
        sb.append("  \"targetGroup\": \"本科一年级\",\n");
        sb.append("  \"teachingGoal\": \"帮助学生理解客户端与服务器如何建立连接\",\n");
        sb.append("  \"actors\": [\"客户端\", \"服务器\"],\n");
        sb.append("  \"steps\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"第一次握手\",\n");
        sb.append("      \"desc\": \"客户端向服务器发送 SYN，请求建立连接。\",\n");
        sb.append("      \"stageCaption\": \"SYN 飞向服务器\",\n");
        sb.append("      \"motion\": {\"type\":\"send\",\"from\":\"客户端\",\"to\":\"服务器\",\"value\":\"SYN\"},\n");
        sb.append("      \"from\": \"客户端\",\n");
        sb.append("      \"to\": \"服务器\",\n");
        sb.append("      \"message\": \"SYN=1, seq=x\",\n");
        sb.append("      \"clientState\": \"SYN-SENT\",\n");
        sb.append("      \"serverState\": \"LISTEN\",\n");
        sb.append("      \"messageType\": \"request\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("【concept 模板结构】\n");
        sb.append("{\n");
        sb.append("  \"templateType\": \"concept\",\n");
        sb.append("  \"title\": \"链表插入删除\",\n");
        sb.append("  \"subtitle\": \"通过节点连接变化理解链表操作\",\n");
        sb.append("  \"targetGroup\": \"本科一年级\",\n");
        sb.append("  \"teachingGoal\": \"帮助学生理解链表节点、指针和插入删除过程\",\n");
        sb.append("  \"mainTerm\": \"链表\",\n");
        sb.append("  \"coreIdea\": \"链表通过节点和指针把离散数据连接成一条逻辑链。\",\n");
        sb.append("  \"steps\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"插入节点 X\",\n");
        sb.append("      \"desc\": \"先找到插入位置，再让前驱指向 X，X 指向原来的后继。\",\n");
        sb.append("      \"stageCaption\": \"X 插入到 A 和 B 中间\",\n");
        sb.append("      \"motion\": {\"type\":\"insert-node\",\"fromIndex\":0,\"toIndex\":1,\"value\":\"X\"},\n");
        sb.append("      \"focus\": \"指针重连\",\n");
        sb.append("      \"keyPoints\": [\"找到前驱节点\", \"改变两条连接\"],\n");
        sb.append("      \"visual\": {\"type\":\"nodes-chain\",\"nodes\":[\"A\",\"X\",\"B\",\"C\"],\"linked\":true,\"highlight\":[1],\"action\":\"insert\",\"actionIndex\":1,\"actionValue\":\"X\"}\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("【concept visual 视觉原语】\n");
        sb.append("- nodes-chain：线性表、链表、数组、队列。字段：type,nodes,linked,highlight,action,actionIndex,actionValue。\n");
        sb.append("- tree：二叉树、BST、递归调用树。字段：type,root,highlight,path；root使用value/left/right递归结构。\n");
        sb.append("- branching：if/switch条件分支。字段：type,condition,trueLabel,falseLabel,activeBranch。\n");
        sb.append("- comparison：左右对比。字段：type,leftTitle,leftItems,rightTitle,rightItems,winner。\n");
        sb.append("- highlight-card：核心定义、复杂度、提醒。字段：type,mainValue,label,tone。\n");
        sb.append("- flow：流程、循环、递归过程。字段：type,boxes,activeIndex,loopBack。\n");
        sb.append("- concept模板至少4步带visual；链表优先nodes-chain且linked=true；树/BST优先tree；递归优先tree或flow。\n\n");

        sb.append("【额外要求】\n");
        sb.append("- 输出内容必须适合教学分步播放。\n");
        sb.append("- 每一步只表达一个核心动作，并优先用 stageCaption 和 motion 表达当前动作。\n");
        sb.append("- stack 模板中，stack 数组表示从底到顶的顺序。\n");
        sb.append("- queue 模板中，queue 数组表示从队头到队尾的顺序，operation 只能为 init/enqueue/dequeue/peek/done。\n");
        sb.append("- tree 模板中，root 必须使用 value/left/right 递归结构，operation 只能为 init/visit/compare/go-left/go-right/backtrack/done。\n");
        sb.append("- graph 模板中，nodes 与 edges 必须自洽，edges 只能引用已有节点，operation 只能为 init/visit/enqueue/dequeue/push/pop/relax/done。\n");
        sb.append("- sort 模板中，每步 array 长度必须与 initialData 一致，并使用 highlight、swap、sortedTailStart 表示比较、交换、已排序边界。\n");
        sb.append("- protocol 模板中，actors 固定两个，每步 from/to 必须来自 actors，并写清 clientState/serverState。\n");
        sb.append("- step.title 简洁明确。\n");
        sb.append("- step.desc 口语化、便于学生理解。\n");
        sb.append("- 不要出现空字段。\n");
        return sb.toString();
    }

    private static String buildAnimOptimizeJsonPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位教学动画课件JSON优化器。\n\n");
        sb.append("【任务】\n");
        sb.append("用户会提供已有课件JSON和优化目标。\n");
        sb.append("你需要输出优化后的完整JSON对象。\n\n");

        sb.append("【绝对规则】\n");
        sb.append("- 只能输出合法JSON对象。\n");
        sb.append("- 保持原有 templateType 不变，可为 sort、protocol、stack、queue、tree、graph 或 concept。\n");
        sb.append("- 保持原有步骤顺序和整体结构稳定。\n");
        sb.append("- 优先只优化 subtitle、teachingGoal、steps[].title、steps[].stageCaption、steps[].motion、steps[].desc；concept模板可补充或修正 steps[].visual。\n");
        sb.append("- 除非用户明确要求，否则不要重写 initialData、initialStack、initialQueue、root、nodes、edges、actors、from/to、array、swap、stack、queue、operation 等结构字段。\n");
        sb.append("- 若concept模板缺少视觉表达，可补充 nodes-chain、tree、branching、comparison、highlight-card、flow 六类visual原语。\n");
        sb.append("- 若步骤缺少动画字段，必须补充 stageCaption 和 motion；desc 只保留一句短说明。\n");
        sb.append("- 不要输出解释。\n");
        return sb.toString();
    }

    @Operation(summary = "\u6d41\u5f0f\u95ee\u7b54 (\u5168\u573a\u666f\u901a\u7528)")
    @PostMapping("/stream")
    public void chatStream(@Valid @RequestBody AiChatRequest aiRequest, HttpServletResponse response) {
        aiUsageCount.incrementAndGet();

        String userQuestion = aiRequest.getQuestion();
        String type = aiRequest.getType();

        String systemPrompt = SYSTEM_PROMPT_CHAT;
        boolean jsonMode = false;
        int maxTokens = 4000;

        if ("teacher_assistant".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_TEACHER_ASSISTANT;
        } else if ("plan".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_PLAN;
            // 如果用户选择了参考案例，提取PDF文本并拼接到prompt中
            if (aiRequest.getCaseId() != null) {
                try {
                    TeachingCase teachingCase = teachingCaseMapper.selectById(aiRequest.getCaseId());
                    if (teachingCase != null && teachingCase.getIsDelete() == 0) {
                        String caseText = remoteDocumentTextService.extractText(teachingCase.getPdfUrl());
                        if (caseText != null && !caseText.isEmpty()) {
                            if (caseText.length() > 6000) {
                                caseText = caseText.substring(0, 6000) + "\n...（案例内容已截断）";
                            }
                            userQuestion = userQuestion + "\n\n【参考案例】\n"
                                    + "案例标题：" + teachingCase.getTitle() + "\n"
                                    + "案例分类：" + teachingCase.getCategory() + "\n"
                                    + "难度等级：" + teachingCase.getDifficulty() + "\n"
                                    + "适用课程：" + (teachingCase.getCourseName() != null ? teachingCase.getCourseName() : "未指定") + "\n"
                                    + "案例内容：\n" + caseText + "\n"
                                    + "\n请结合以上参考案例设计教案，将案例中的关键情境、问题或数据融入教学过程。"
                            + "\n重要：你必须在每一处参考或借鉴了案例具体内容的位置，使用【案例参考】标记。"
                            + "\n请将【案例参考】标记放在相关段落或条目的开头，使教案阅读者能清晰看到案例的影响。";
                        }
                    }
                } catch (Exception e) {
                    // 提取失败时不阻断生成，仅记录日志
                    log.warn("Teaching case file extraction failed, caseId={}", aiRequest.getCaseId(), e);
                }
            }
        } else if ("quiz".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_QUIZ;
        } else if ("report".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_REPORT;
        } else if ("grade_report".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_GRADE_REPORT;
            maxTokens = 4000;
        } else if ("analysis".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_ANALYSIS;
            jsonMode = true;
            maxTokens = 1500;
        } else if ("psycho".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_PSYCHO;
            jsonMode = true;
            maxTokens = 1500;
        } else if ("anim".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_ANIM;
            maxTokens = 8192;
        } else if ("anim_optimize".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_ANIM_OPTIMIZE;
            maxTokens = 8192;
        } else if ("anim_json".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_ANIM_JSON;
            jsonMode = true;
            maxTokens = 3500;
        } else if ("anim_optimize_json".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_ANIM_OPTIMIZE_JSON;
            jsonMode = true;
            maxTokens = 3500;
        } else if ("article".equals(type)) {
            systemPrompt = SYSTEM_PROMPT_ARTICLE;
            maxTokens = 4000;
        } else if ("writing".equals(type)) {
            String op = aiRequest.getOperation();
            if ("continue".equals(op)) systemPrompt = SYSTEM_PROMPT_WRITING_CONTINUE;
            else if ("rewrite".equals(op)) systemPrompt = SYSTEM_PROMPT_WRITING_REWRITE;
            else if ("expand".equals(op)) systemPrompt = SYSTEM_PROMPT_WRITING_EXPAND;
            else if ("summarize".equals(op)) systemPrompt = SYSTEM_PROMPT_WRITING_SUMMARIZE;
            else systemPrompt = SYSTEM_PROMPT_WRITING_POLISH;
            maxTokens = 2000;
        }

        callDeepSeekStream(userQuestion, systemPrompt, response, jsonMode, maxTokens);
    }

    @Operation(summary = "\u5b66\u751f\u7aef\u573a\u666f\u5316 AI \u52a9\u6559\u6d41\u5f0f\u95ee\u7b54")
    @PostMapping("/tutor/stream")
    public void tutorStream(@Valid @RequestBody TutorChatRequest request,
                            HttpServletRequest httpRequest,
                            HttpServletResponse response) {
        aiUsageCount.incrementAndGet();

        User student = SessionUserContext.require(httpRequest);
        KnowledgeBaseService.RetrievalContext knowledgeContext =
                knowledgeBaseService.retrieveForStudent(student, request);
        studentAiProfileService.recordQuestion(student, request, knowledgeContext.eventMetadata());
        Map<String, Object> serverProfile = studentAiProfileService.buildTutorProfileContext(student);
        String prompt = buildTutorUserPrompt(request, serverProfile);
        if (StringUtils.isNotBlank(knowledgeContext.promptContext())) {
            prompt = prompt + "\n\n" + knowledgeContext.promptContext();
        }
        callDeepSeekStream(prompt, SYSTEM_PROMPT_TUTOR, response, false, 3000);
    }

    @Operation(summary = "学生端 AI 助手图片题目解析")
    @PostMapping(value = "/tutor/vision/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void tutorVisionStream(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "message", required = false) String message,
                                  @RequestParam(value = "context", required = false) String context,
                                  HttpServletResponse response) {
        aiUsageCount.incrementAndGet();
        tutorVisionService.streamQuestionExplanation(file, buildTutorVisionPrompt(message, context), response);
    }

    @Operation(summary = "教师端通用助手图片解析")
    @PostMapping(value = "/teacher/vision/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void teacherVisionStream(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "message", required = false) String message,
                                    HttpServletResponse response) {
        aiUsageCount.incrementAndGet();
        tutorVisionService.streamQuestionExplanation(file, buildTeacherVisionPrompt(message), response);
    }

    @Operation(summary = "教师端通用助手文件解析")
    @PostMapping(value = "/teacher/file/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void teacherFileStream(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "message", required = false) String message,
                                  HttpServletResponse response) {
        aiUsageCount.incrementAndGet();
        try {
            String fileText = extractTeacherAssistantFileText(file);
            callDeepSeekStream(buildTeacherFilePrompt(file, message, fileText), SYSTEM_PROMPT_TEACHER_ASSISTANT, response, false, 4000);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Teacher assistant file parsing failed", e);
            try {
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("文件解析失败，请检查文件格式后重试");
                response.getWriter().flush();
            } catch (IOException ignored) {
            }
        }
    }

    @PostMapping(value = "/tutor/speech-to-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> tutorSpeechToText(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先录制语音");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("audio/")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持音频文件");
        }

        String audioUrl = ossService.uploadFile(file, "tutor-audio");
        String text = aliyunAsrService.transcribeAudioToText(audioUrl);
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未识别到有效语音内容");
        }

        return ResultUtils.success(text.trim());
    }

    @Operation(summary = "教师端通用助手语音转文字")
    @PostMapping(value = "/teacher/speech-to-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> teacherSpeechToText(@RequestParam("file") MultipartFile file) {
        return tutorSpeechToText(file);
    }

    private String buildTeacherVisionPrompt(String message) {
        String safeMessage = truncateText(message == null ? "" : message.trim(), 2000);
        StringBuilder prompt = new StringBuilder();
        prompt.append("【teacher_message】\n");
        if (safeMessage == null || safeMessage.isBlank()) {
            prompt.append("请解析这张图片，并从教师备课、课堂讲解、题目讲评或学情处理角度给出可操作建议。");
        } else {
            prompt.append(safeMessage);
        }
        prompt.append("\n\n请注意：回答对象是教师，请不要使用面向学生的口吻。");
        return prompt.toString();
    }

    private String buildTeacherFilePrompt(MultipartFile file, String message, String fileText) {
        String safeMessage = truncateText(message == null ? "" : message.trim(), 2000);
        String safeFileText = truncateText(fileText == null ? "" : fileText.trim(), 12000);
        String fileName = file == null ? "未命名文件" : String.valueOf(file.getOriginalFilename());

        StringBuilder prompt = new StringBuilder();
        prompt.append("【教师问题】\n");
        prompt.append(safeMessage == null || safeMessage.isBlank()
                ? "请解析这份文件，提炼重点，并给出适合教师使用的教学建议。"
                : safeMessage);
        prompt.append("\n\n【上传文件】\n");
        prompt.append("文件名：").append(fileName).append("\n\n");
        prompt.append("【文件正文】\n").append(safeFileText);
        return prompt.toString();
    }

    private String extractTeacherAssistantFileText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先上传文件");
        }
        if (file.getSize() > 20L * 1024L * 1024L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能超过 20MB");
        }

        String fileName = String.valueOf(file.getOriginalFilename());
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        String contentType = String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT);
        byte[] bytes = file.getBytes();

        String text = CaseDocumentTextExtractor.extractText(bytes, fileName);
        if ((text == null || text.isBlank()) && isPlainTextFile(lowerName, contentType)) {
            text = new String(bytes, StandardCharsets.UTF_8);
        }

        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "暂不支持解析该文件，请上传 PDF、Word 或文本类文件");
        }
        return text.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]+", " ").trim();
    }

    private boolean isPlainTextFile(String lowerName, String contentType) {
        if (contentType.startsWith("text/") || contentType.contains("json") || contentType.contains("xml")) {
            return true;
        }
        return lowerName.endsWith(".txt")
                || lowerName.endsWith(".md")
                || lowerName.endsWith(".markdown")
                || lowerName.endsWith(".csv")
                || lowerName.endsWith(".json")
                || lowerName.endsWith(".xml")
                || lowerName.endsWith(".html")
                || lowerName.endsWith(".css")
                || lowerName.endsWith(".js")
                || lowerName.endsWith(".ts")
                || lowerName.endsWith(".java")
                || lowerName.endsWith(".py")
                || lowerName.endsWith(".sql")
                || lowerName.endsWith(".yml")
                || lowerName.endsWith(".yaml")
                || lowerName.endsWith(".vue");
    }

    private String buildTutorVisionPrompt(String message, String context) {
        String safeMessage = truncateText(message == null ? "" : message.trim(), 2000);
        String safeContext = truncateText(context == null ? "{}" : context.trim(), 8000);
        StringBuilder prompt = new StringBuilder();
        prompt.append("【context】\n").append(safeContext == null || safeContext.isBlank() ? "{}" : safeContext).append("\n\n");
        prompt.append("【student_message】\n");
        if (safeMessage == null || safeMessage.isBlank()) {
            prompt.append("请解析这张题目截图，并给出适合学生理解的讲解。");
        } else {
            prompt.append(safeMessage);
        }
        return prompt.toString();
    }

    private String buildTutorUserPrompt(TutorChatRequest request, Map<String, Object> serverProfile) {
        String mode = normalizeTutorMode(request == null ? null : request.getMode());
        String message = request == null ? "" : truncateText(request.getMessage(), 2000);
        String contextJson = "{}";
        String profileJson = "{}";

        if (serverProfile != null && !serverProfile.isEmpty()) {
            try {
                profileJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(serverProfile);
                profileJson = truncateText(profileJson, 6000);
            } catch (Exception e) {
                profileJson = "{\"profileError\":\"学生画像解析失败\"}";
            }
        }

        if (request != null && request.getContext() != null && !request.getContext().isEmpty()) {
            try {
                contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request.getContext());
                contextJson = truncateText(contextJson, 8000);
            } catch (Exception e) {
                contextJson = "{\"contextError\":\"上下文解析失败\"}";
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("【mode】\n").append(mode).append("\n\n");
        prompt.append("【student_profile_from_server】\n").append(profileJson).append("\n\n");
        prompt.append("【context】\n").append(contextJson).append("\n\n");
        prompt.append("【student_message】\n");
        if (message == null || message.trim().isEmpty()) {
            prompt.append("学生点击了快捷助教任务，请根据 mode 和 context 主动给出帮助。");
        } else {
            prompt.append(message.trim());
        }
        return prompt.toString();
    }

    private String normalizeTutorMode(String mode) {
        if (mode == null) {
            return "hint";
        }
        return switch (mode) {
            case "explain", "hint", "check", "practice", "summary", "answer", "debug" -> mode;
            default -> "hint";
        };
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...（内容已截断）";
    }

    @Operation(summary = "\u4e0a\u4f20\u8bfe\u8868\u8fdb\u884c\u5206\u6790")
    @PostMapping("/analyze/file")
    public void analyzeFileStream(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        aiUsageCount.incrementAndGet();
        try {
            String pdfContent = extractTextFromPdf(file);
            if (pdfContent.length() > 8000) {
                pdfContent = pdfContent.substring(0, 8000);
            }
            callDeepSeekStream(pdfContent, SYSTEM_PROMPT_ANALYSIS, response, true, 1500);
        } catch (Exception e) {
            log.error("Schedule file analysis failed", e);
            try {
                response.getWriter().write("解析失败，请检查文件格式后重试");
            } catch (IOException ignored) {
            }
        }
    }


    @Operation(summary = "流式批改作业")
    @PostMapping("/grade-homework")
    public void gradeHomeworkStream(@Valid @RequestBody HomeworkGradeRequest req, HttpServletResponse response) {
        aiUsageCount.incrementAndGet();
        String prompt = "「试卷原文」\n" + req.getPaperContent()
                + "\n\n「学生作答（JSON）」\n" + req.getStudentAnswers();
        callDeepSeekStream(prompt, SYSTEM_PROMPT_HOMEWORK_GRADE, response, false, 4000);
    }

    private void callDeepSeekStream(String userQuestion, String systemPrompt,
                                    HttpServletResponse response, boolean jsonMode,
                                    int maxTokens) {
        response.setContentType("text/plain;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            response.getWriter().flush();
            aiModelClient.streamChat(
                    systemPrompt,
                    userQuestion,
                    jsonMode ? 0.2 : 0.7,
                    maxTokens,
                    jsonMode,
                    chunk -> {
                        try {
                            response.getWriter().write(chunk);
                            response.getWriter().flush();
                        } catch (IOException e) {
                            throw new IllegalStateException("Client stream disconnected", e);
                        }
                    }
            );

        } catch (Exception e) {
            log.error("DeepSeek stream request failed", e);
            try {
                response.getWriter().write("\n[AI_SERVICE_ERROR] AI 服务暂时不可用，请稍后重试");
                response.getWriter().flush();
            } catch (Exception ignored) {
            }
        }
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    @Operation(summary = "AI生成编程题")
    @PostMapping("/coding/generate")
    public BaseResponse<CodingProblemGenerateVO> generateCodingProblem(@Valid @RequestBody CodingProblemGenerateRequest req) {
        if (req.getDescription() == null || req.getDescription().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "需求描述不能为空");
        }

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("【教师需求】\n").append(req.getDescription().trim()).append("\n\n");
        if (req.getLanguages() != null && !req.getLanguages().isEmpty()) {
            userPrompt.append("【支持语言】\n").append(String.join(", ", req.getLanguages())).append("\n\n");
        }
        if (req.getDifficulty() != null && !req.getDifficulty().isEmpty()) {
            userPrompt.append("【难度要求】\n").append(req.getDifficulty()).append("\n");
        }

        String aiResponse = deepSeekService.chat(SYSTEM_PROMPT_CODING_GENERATE, userPrompt.toString(), 4000);
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成失败，请稍后重试");
        }

        // 清理可能的 markdown 代码块标记
        String jsonStr = aiResponse.trim();
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substring(7);
        } else if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.substring(3);
        }
        if (jsonStr.endsWith("```")) {
            jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
        }
        jsonStr = jsonStr.trim();

        try {
            JsonNode root = objectMapper.readTree(jsonStr);
            CodingProblemGenerateVO vo = new CodingProblemGenerateVO();

            // 基本信息
            vo.setTitle(getTextNode(root, "title"));
            vo.setDescription(getTextNode(root, "description"));
            vo.setDifficulty(getTextNode(root, "difficulty"));
            vo.setTimeLimitMs(getIntNode(root, "timeLimitMs", 5000));
            vo.setMemoryLimitKb(getIntNode(root, "memoryLimitKb", 262144));

            // languages
            JsonNode languagesNode = root.path("languages");
            if (languagesNode.isArray()) {
                List<String> languages = new ArrayList<>();
                languagesNode.forEach(n -> {
                    if (n.isTextual()) languages.add(n.asText());
                });
                vo.setLanguages(languages);
            }

            // testCases
            JsonNode testCasesNode = root.path("testCases");
            List<CodingProblemAddRequest.CodingTestCaseItem> testCases = new ArrayList<>();
            if (testCasesNode.isArray()) {
                int order = 0;
                for (JsonNode tc : testCasesNode) {
                    CodingProblemAddRequest.CodingTestCaseItem item = new CodingProblemAddRequest.CodingTestCaseItem();
                    item.setInput(getTextNode(tc, "input", ""));
                    item.setExpectedOutput(getTextNode(tc, "expectedOutput"));
                    item.setIsSample(getIntNode(tc, "isSample", 0));
                    item.setScore(getIntNode(tc, "score", 0));
                    item.setSortOrder(order++);
                    testCases.add(item);
                }
            }
            vo.setTestCases(testCases);

            // templates
            JsonNode templatesNode = root.path("templates");
            List<CodingProblemAddRequest.CodingTemplateItem> templates = new ArrayList<>();
            if (templatesNode.isArray()) {
                for (JsonNode tpl : templatesNode) {
                    CodingProblemAddRequest.CodingTemplateItem item = new CodingProblemAddRequest.CodingTemplateItem();
                    item.setLanguage(getTextNode(tpl, "language"));
                    item.setStarterCode(getTextNode(tpl, "starterCode", ""));
                    item.setReferenceSolution(getTextNode(tpl, "referenceSolution", ""));
                    templates.add(item);
                }
            }
            vo.setTemplates(templates);

            // 校验
            if (vo.getTitle() == null || vo.getTitle().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成内容缺少标题");
            }
            if (vo.getDescription() == null || vo.getDescription().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成内容缺少题目描述");
            }
            if (testCases.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成内容缺少测试用例");
            }
            boolean hasNonSample = testCases.stream().anyMatch(tc -> tc.getIsSample() == null || tc.getIsSample() == 0);
            if (!hasNonSample) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成内容缺少隐藏测试用例（至少需要一个非样例用例）");
            }
            for (CodingProblemAddRequest.CodingTestCaseItem tc : testCases) {
                if (tc.getExpectedOutput() == null || tc.getExpectedOutput().isEmpty()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成内容中测试用例的期望输出不能为空");
                }
            }
            if (templates.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成内容缺少代码模板");
            }

            return ResultUtils.success(vo);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成内容格式异常，请重试");
        }
    }

    private String getTextNode(JsonNode parent, String field) {
        return getTextNode(parent, field, null);
    }

    private String getTextNode(JsonNode parent, String field, String defaultValue) {
        JsonNode node = parent.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        return node.asText(defaultValue);
    }

    private int getIntNode(JsonNode parent, String field, int defaultValue) {
        JsonNode node = parent.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        return node.asInt(defaultValue);
    }

}
