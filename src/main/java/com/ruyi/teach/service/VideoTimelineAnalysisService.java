package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.VideoTimelineAnalysisTask;
import com.ruyi.teach.model.vo.VideoTimelineAnalysisTaskVO;

public interface VideoTimelineAnalysisService extends IService<VideoTimelineAnalysisTask> {

    Long startAnalysis(Long chapterId, User loginUser);

    Long startAutoAnalysis(Long chapterId, User loginUser);

    VideoTimelineAnalysisTaskVO getLatestTask(Long chapterId, User loginUser);

    VideoTimelineAnalysisTaskVO getTask(Long chapterId, Long taskId, User loginUser);
}
