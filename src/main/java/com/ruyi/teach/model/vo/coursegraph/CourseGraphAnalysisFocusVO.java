package com.ruyi.teach.model.vo.coursegraph;

import lombok.Data;

import java.util.List;

@Data
public class CourseGraphAnalysisFocusVO {
    private String nodeId;
    private String nodeName;
    private String categoryName;
    private String summary;
    private String heatLevel;
    private String weaknessLevel;
    private String riskLevel;

    /** 编程题数量 */
    private Integer codingProblemCount;
    /** 随堂测验数量 */
    private Integer quizCount;
    /** 题库总数量 = 编程题 + 随堂测验 */
    private Integer totalQuizCount;
    /** 教案数量 */
    private Integer planCount;
    /** 交互课件数量 */
    private Integer animCount;
    /** 资料总数量 = 教案 + 交互课件 */
    private Integer totalMaterialCount;
    /** 学习内容完善度 0-100 */
    private Integer contentCompleteness;

    private List<String> recommendedViews;
    private List<String> suggestedActions;
    private List<CourseGraphAnalysisMetricVO> metricItems;
}
