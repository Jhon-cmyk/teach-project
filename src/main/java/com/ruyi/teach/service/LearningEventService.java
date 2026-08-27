package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.learning.LearningEventBatchRequest;
import com.ruyi.teach.model.entity.LearningEvent;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.LearningHeatmapDayVO;

import java.util.List;

public interface LearningEventService extends IService<LearningEvent> {

    boolean saveBatchEvents(LearningEventBatchRequest request, User student);

    List<LearningHeatmapDayVO> getLearningHeatmap(User student, Integer days);
}
