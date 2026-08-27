package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.HomeworkChapterPracticePublishRequest;
import com.ruyi.teach.model.dto.HomeworkPublishRequest;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.User;

public interface HomeworkAssignmentService extends IService<HomeworkAssignment> {

    Long publishAssignment(HomeworkPublishRequest req, User loginUser);

    Long publishChapterPractice(HomeworkChapterPracticePublishRequest req, User loginUser);
}