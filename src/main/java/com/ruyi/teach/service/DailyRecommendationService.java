package com.ruyi.teach.service;

import com.ruyi.teach.model.dto.learning.DailyRecommendationSubmitRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.DailyRecommendationTodayVO;

public interface DailyRecommendationService {

    DailyRecommendationTodayVO getToday(User student);

    DailyRecommendationTodayVO getTodayCached(User student);

    DailyRecommendationTodayVO dismissToday(User student);

    DailyRecommendationTodayVO submitToday(DailyRecommendationSubmitRequest request, User student);

    DailyRecommendationTodayVO refreshToday(User student);
}
