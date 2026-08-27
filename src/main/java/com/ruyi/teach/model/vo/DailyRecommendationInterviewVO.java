package com.ruyi.teach.model.vo;

import com.ruyi.teach.model.dto.learning.DailyRecommendationSubmitRequest;
import lombok.Data;

@Data
public class DailyRecommendationInterviewVO {

    private String reply;

    private boolean ready;

    private int progress;

    private DailyRecommendationSubmitRequest profile;

    private boolean degraded;
}
