package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.video.VideoKnowledgeSegmentSaveRequest;
import com.ruyi.teach.model.entity.VideoKnowledgeSegment;
import com.ruyi.teach.model.entity.User;

import java.util.List;

public interface VideoKnowledgeSegmentService extends IService<VideoKnowledgeSegment> {

    List<VideoKnowledgeSegment> listByChapterId(Long chapterId);

    boolean saveChapterSegments(Long chapterId, VideoKnowledgeSegmentSaveRequest request, User loginUser);
}
