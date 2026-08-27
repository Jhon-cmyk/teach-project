package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CommunityFeaturedAnswerMapper;
import com.ruyi.teach.mapper.CommunityPostMapper;
import com.ruyi.teach.mapper.CommunityReplyMapper;
import com.ruyi.teach.model.dto.CommunityFeaturedAddRequest;
import com.ruyi.teach.model.dto.FeaturedAnswersQueryRequest;
import com.ruyi.teach.model.entity.CommunityFeaturedAnswer;
import com.ruyi.teach.model.entity.CommunityPost;
import com.ruyi.teach.model.entity.CommunityReply;
import com.ruyi.teach.model.vo.FeaturedAnswerVO;
import com.ruyi.teach.service.CommunityFeaturedAnswerService;
import com.ruyi.teach.service.CommunityNotificationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class CommunityFeaturedAnswerServiceImpl
        extends ServiceImpl<CommunityFeaturedAnswerMapper, CommunityFeaturedAnswer>
        implements CommunityFeaturedAnswerService {

    @Resource
    private CommunityFeaturedAnswerMapper featuredAnswerMapper;

    @Resource
    private CommunityPostMapper postMapper;

    @Resource
    private CommunityReplyMapper replyMapper;

    @Resource
    private CommunityNotificationService communityNotificationService;

    @Override
    public IPage<FeaturedAnswerVO> listFeaturedAnswers(FeaturedAnswersQueryRequest request) {
        Page<FeaturedAnswerVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        return featuredAnswerMapper.selectFeaturedAnswerPage(
                page,
                request.getCourseId(),
                request.getSort(),
                request.getKeyword()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFeaturedAnswer(CommunityFeaturedAddRequest request) {
        return addFeaturedAnswerForCourses(request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFeaturedAnswerForCourses(CommunityFeaturedAddRequest request, java.util.List<Long> courseIds) {
        if (request == null || request.getPostId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "postId 不能为空");
        }
        if (StringUtils.isBlank(request.getExcerpt())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "excerpt 不能为空");
        }

        CommunityPost post = postMapper.selectById(request.getPostId());
        if (post == null || Integer.valueOf(1).equals(post.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "关联帖子不存在");
        }
        if (courseIds != null && !courseIds.contains(post.getCourseId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能精选本人授课课程中的讨论");
        }

        QueryWrapper<CommunityFeaturedAnswer> qw = new QueryWrapper<>();
        qw.eq("post_id", request.getPostId());
        qw.eq("is_delete", 0);
        qw.last("LIMIT 1");

        CommunityFeaturedAnswer existing = this.getOne(qw, false);
        if (existing != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该讨论已加入答疑精选");
        }

        if (request.getReplyId() != null) {
            CommunityReply reply = replyMapper.selectById(request.getReplyId());
            if (reply == null || Integer.valueOf(1).equals(reply.getIsDelete())) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "关联回复不存在");
            }
            if (!request.getPostId().equals(reply.getPostId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "replyId 与 postId 不匹配");
            }
        }

        Date now = new Date();

        CommunityFeaturedAnswer entity = new CommunityFeaturedAnswer();
        entity.setPostId(request.getPostId());
        entity.setReplyId(request.getReplyId());
        entity.setTeacherId(request.getTeacherId());
        entity.setTeacherName(request.getTeacherName());
        entity.setExcerpt(request.getExcerpt().trim());
        entity.setIsRecommended(request.getIsRecommended() == null ? 0 : request.getIsRecommended());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setIsDelete(0);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        int rows = featuredAnswerMapper.insertFeaturedAnswer(entity);
        if (rows <= 0 || entity.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "加入精选失败");
        }

        communityNotificationService.notifyOnPostFeatured(post, entity);
        return entity.getId();
    }
}
