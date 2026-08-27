package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CommunityNotificationMapper;
import com.ruyi.teach.mapper.CommunityReplyMapper;
import com.ruyi.teach.model.dto.CommunityNotificationQueryRequest;
import com.ruyi.teach.model.entity.CommunityFeaturedAnswer;
import com.ruyi.teach.model.entity.CommunityNotification;
import com.ruyi.teach.model.entity.CommunityPost;
import com.ruyi.teach.model.entity.CommunityReply;
import com.ruyi.teach.model.vo.CommunityNotificationVO;
import com.ruyi.teach.service.CommunityNotificationService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CommunityNotificationServiceImpl
        extends ServiceImpl<CommunityNotificationMapper, CommunityNotification>
        implements CommunityNotificationService {

    @Resource
    private CommunityNotificationMapper notificationMapper;

    @Resource
    private CommunityReplyMapper replyMapper;

    @Override
    public IPage<CommunityNotificationVO> listMyNotifications(Long userId, CommunityNotificationQueryRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        int pageNum = request != null && request.getPageNum() != null && request.getPageNum() > 0
                ? request.getPageNum() : 1;
        int pageSize = request != null && request.getPageSize() != null && request.getPageSize() > 0
                ? request.getPageSize() : 10;

        Page<CommunityNotificationVO> page = new Page<>(pageNum, pageSize);
        return notificationMapper.selectNotificationPage(
                page,
                userId,
                request != null ? request.getIsRead() : null,
                request != null ? request.getType() : null
        );
    }

    @Override
    public Boolean readNotification(Long userId, Long id) {
        if (userId == null || userId <= 0 || id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        return notificationMapper.markAsRead(id, userId) > 0;
    }

    @Override
    public Integer readAllNotifications(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        return notificationMapper.markAllAsRead(userId);
    }

    @Override
    public Integer countUnreadNotifications(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Override
    public void notifyOnPostReply(CommunityPost post, CommunityReply reply) {
        if (post == null || reply == null || reply.getUserId() == null) {
            return;
        }

        Set<Long> notifiedUserIds = new HashSet<>();
        Long actorUserId = reply.getUserId();

        if (post.getUserId() != null && !post.getUserId().equals(actorUserId)) {
            createNotification(
                    post.getUserId(),
                    "post_replied",
                    post.getId(),
                    reply.getId(),
                    "你的帖子有新回复",
                    buildReplyContent(reply.getAuthorName(), post.getTitle())
            );
            notifiedUserIds.add(post.getUserId());
        }

        List<Long> participantUserIds = replyMapper.selectParticipantUserIds(post.getId());
        for (Long userId : participantUserIds) {
            if (userId == null || userId <= 0) {
                continue;
            }
            if (userId.equals(actorUserId)) {
                continue;
            }
            if (notifiedUserIds.contains(userId)) {
                continue;
            }
            if (post.getUserId() != null && post.getUserId().equals(userId)) {
                continue;
            }

            createNotification(
                    userId,
                    "followed_discussion_updated",
                    post.getId(),
                    reply.getId(),
                    "你参与的讨论有新动态",
                    buildReplyContent(reply.getAuthorName(), post.getTitle())
            );
            notifiedUserIds.add(userId);
        }
    }

    @Override
    public void notifyOnPostResolved(CommunityPost post) {
        if (post == null || post.getUserId() == null || post.getUserId() <= 0) {
            return;
        }

        createNotification(
                post.getUserId(),
                "post_resolved",
                post.getId(),
                null,
                "你的问题已被标记为已解决",
                "《" + safeTitle(post.getTitle()) + "》已更新为已解决状态"
        );
    }

    @Override
    public void notifyOnPostFeatured(CommunityPost post, CommunityFeaturedAnswer featuredAnswer) {
        if (post == null || post.getUserId() == null || post.getUserId() <= 0) {
            return;
        }

        String teacherName = featuredAnswer != null && StringUtils.isNotBlank(featuredAnswer.getTeacherName())
                ? featuredAnswer.getTeacherName() : "老师";

        createNotification(
                post.getUserId(),
                "post_featured",
                post.getId(),
                featuredAnswer != null ? featuredAnswer.getReplyId() : null,
                "你的帖子被加入答疑精选",
                teacherName + " 已将《" + safeTitle(post.getTitle()) + "》加入答疑精选"
        );
    }

    private void createNotification(Long userId,
                                    String type,
                                    Long postId,
                                    Long replyId,
                                    String title,
                                    String content) {
        if (userId == null || userId <= 0) {
            return;
        }

        Date now = new Date();

        CommunityNotification notification = new CommunityNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setPostId(postId);
        notification.setReplyId(replyId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notification.setIsDelete(0);

        this.save(notification);
    }

    private String buildReplyContent(String authorName, String postTitle) {
        String safeAuthor = StringUtils.isNotBlank(authorName) ? authorName : "有人";
        return safeAuthor + " 回复了《" + safeTitle(postTitle) + "》";
    }

    private String safeTitle(String title) {
        if (StringUtils.isBlank(title)) {
            return "你的讨论";
        }
        return title.trim();
    }
}