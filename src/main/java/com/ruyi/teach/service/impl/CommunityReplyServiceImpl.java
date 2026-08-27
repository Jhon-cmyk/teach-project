package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CommunityFeaturedAnswerMapper;
import com.ruyi.teach.mapper.CommunityPostMapper;
import com.ruyi.teach.mapper.CommunityReplyMapper;
import com.ruyi.teach.model.dto.CommunityReplyAddRequest;
import com.ruyi.teach.model.entity.CommunityFeaturedAnswer;
import com.ruyi.teach.model.entity.CommunityPost;
import com.ruyi.teach.model.entity.CommunityReply;
import com.ruyi.teach.service.CommunityNotificationService;
import com.ruyi.teach.service.CommunityReplyService;
import com.ruyi.teach.util.CommunityRichTextSanitizer;
import com.ruyi.teach.service.CommunityNotificationService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class CommunityReplyServiceImpl extends ServiceImpl<CommunityReplyMapper, CommunityReply>
        implements CommunityReplyService {

    @Resource
    private CommunityPostMapper postMapper;

    @Resource
    private CommunityFeaturedAnswerMapper featuredAnswerMapper;

    @Resource
    private CommunityNotificationService communityNotificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addReply(CommunityReplyAddRequest request, Long userId, String authorName, Integer isTeacher) {
        if (request == null || request.getPostId() == null || request.getPostId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不能为空");
        }
        if (StringUtils.isBlank(request.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复内容不能为空");
        }

        String safeContent = CommunityRichTextSanitizer.sanitize(request.getContent());
        if (!CommunityRichTextSanitizer.hasMeaningfulContent(safeContent)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复内容不能为空");
        }
        if (safeContent.length() > 60000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复内容过长");
        }

        CommunityPost post = postMapper.selectById(request.getPostId());
        if (post == null || Integer.valueOf(1).equals(post.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }

        Date now = new Date();

        CommunityReply reply = new CommunityReply();
        reply.setPostId(request.getPostId());
        reply.setUserId(userId);
        reply.setAuthorName(StringUtils.isNotBlank(authorName) ? authorName : "当前用户");
        reply.setContent(safeContent);
        reply.setIsTeacher(Integer.valueOf(1).equals(isTeacher) ? 1 : 0);
        reply.setCreateTime(now);
        reply.setUpdateTime(now);
        reply.setIsDelete(0);

        boolean saved = this.save(reply);
        if (!saved || reply.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "回复失败");
        }

        int rows = postMapper.updatePostAfterReply(request.getPostId(), reply.getIsTeacher(), now);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步帖子统计失败");
        }
        communityNotificationService.notifyOnPostReply(post, reply);

        return reply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOwnTeacherReply(Long replyId, Long teacherId) {
        if (replyId == null || replyId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复ID不能为空");
        }
        if (teacherId == null || teacherId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }

        CommunityReply reply = this.getById(replyId);
        if (reply == null || Integer.valueOf(1).equals(reply.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "回复不存在");
        }
        if (!teacherId.equals(reply.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能删除自己发布的回复");
        }
        if (!Integer.valueOf(1).equals(reply.getIsTeacher())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能删除教师回复");
        }

        Date now = new Date();
        LambdaUpdateWrapper<CommunityReply> replyWrapper = new LambdaUpdateWrapper<>();
        replyWrapper.eq(CommunityReply::getId, replyId)
                .eq(CommunityReply::getUserId, teacherId)
                .eq(CommunityReply::getIsTeacher, 1)
                .eq(CommunityReply::getIsDelete, 0)
                .set(CommunityReply::getIsDelete, 1)
                .set(CommunityReply::getUpdateTime, now);
        boolean updated = this.update(replyWrapper);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除回复失败");
        }

        LambdaUpdateWrapper<CommunityFeaturedAnswer> featuredWrapper = new LambdaUpdateWrapper<>();
        featuredWrapper.eq(CommunityFeaturedAnswer::getReplyId, replyId)
                .eq(CommunityFeaturedAnswer::getIsDelete, 0)
                .set(CommunityFeaturedAnswer::getIsDelete, 1)
                .set(CommunityFeaturedAnswer::getUpdateTime, now);
        featuredAnswerMapper.update(null, featuredWrapper);

        int rows = postMapper.refreshPostAfterReplyDeleted(reply.getPostId(), now);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步帖子统计失败");
        }
        return true;
    }
}
