package com.ruyi.teach.service;

import com.ruyi.teach.model.dto.learning.RecommendationGenerateRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.RecommendationGenerateVO;

public interface RecommendationService {

    RecommendationGenerateVO generateRecommendations(RecommendationGenerateRequest request, User viewer);

    boolean updateRecommendationStatus(Long recommendationId, String status, User student);
}
