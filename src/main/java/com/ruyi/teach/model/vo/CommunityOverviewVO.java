package com.ruyi.teach.model.vo;

import lombok.Data;
import java.util.List;

/**
 * 首页学习交流概览 VO
 * 对齐前端 CommunityOverview 类型
 */
@Data
public class CommunityOverviewVO {

    /** 最新讨论列表（前 5 条） */
    private List<CommunityPostVO> discussions;

    /** 作业互助摘要 */
    private HomeworkHelpSummary homeworkHelp;

    /** 答疑精选摘要 */
    private FeaturedAnswersSummary featuredAnswers;

    @Data
    public static class HomeworkHelpSummary {
        /** 今日新增提问数 */
        private int todayQuestionCount;
    }

    @Data
    public static class FeaturedAnswersSummary {
        /** 本周精选回答数 */
        private int weeklySelectedCount;
    }
}
