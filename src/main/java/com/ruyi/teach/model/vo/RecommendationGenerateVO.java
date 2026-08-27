package com.ruyi.teach.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class RecommendationGenerateVO {

    private List<StudentLearningProfileVO.RecommendationItem> recommendations;
}
