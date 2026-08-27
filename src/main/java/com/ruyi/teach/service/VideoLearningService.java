package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.video.VideoInterventionCheckRequest;
import com.ruyi.teach.model.dto.video.VideoLearningEventBatchRequest;
import com.ruyi.teach.model.dto.video.VideoLearningSessionStartRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.VideoLearningSession;
import com.ruyi.teach.model.vo.VideoInterventionVO;
import com.ruyi.teach.model.vo.VideoLearningProfileVO;

public interface VideoLearningService extends IService<VideoLearningSession> {

    Long startSession(VideoLearningSessionStartRequest request, User student);

    boolean saveEvents(VideoLearningEventBatchRequest request, User student);

    VideoInterventionVO checkIntervention(VideoInterventionCheckRequest request, User student);

    VideoLearningProfileVO getStudentProfile(Long teacherId, String teacherRole, Long classId, Long studentId, Integer days);
}
