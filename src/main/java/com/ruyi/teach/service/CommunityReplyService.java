package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.CommunityReplyAddRequest;
import com.ruyi.teach.model.entity.CommunityReply;

public interface CommunityReplyService extends IService<CommunityReply> {

    /**
     * 提交回复
     */
    Long addReply(CommunityReplyAddRequest request, Long userId, String authorName, Integer isTeacher);

    /**
     * 教师删除自己发布的社区回复（软删除）。
     */
    boolean deleteOwnTeacherReply(Long replyId, Long teacherId);
}
