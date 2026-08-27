package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.CommunityNotificationQueryRequest;
import com.ruyi.teach.model.entity.CommunityFeaturedAnswer;
import com.ruyi.teach.model.entity.CommunityNotification;
import com.ruyi.teach.model.entity.CommunityPost;
import com.ruyi.teach.model.entity.CommunityReply;
import com.ruyi.teach.model.vo.CommunityNotificationVO;

public interface CommunityNotificationService extends IService<CommunityNotification> {

    IPage<CommunityNotificationVO> listMyNotifications(Long userId, CommunityNotificationQueryRequest request);

    Boolean readNotification(Long userId, Long id);

    Integer readAllNotifications(Long userId);

    Integer countUnreadNotifications(Long userId);

    void notifyOnPostReply(CommunityPost post, CommunityReply reply);

    void notifyOnPostResolved(CommunityPost post);

    void notifyOnPostFeatured(CommunityPost post, CommunityFeaturedAnswer featuredAnswer);
}