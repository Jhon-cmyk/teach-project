package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CommunityPostMapper;
import com.ruyi.teach.mapper.CommunityReplyMapper;
import com.ruyi.teach.model.dto.CommunityQueryRequest;
import com.ruyi.teach.model.dto.HomeworkHelpAddRequest;
import com.ruyi.teach.model.dto.HomeworkHelpQueryRequest;
import com.ruyi.teach.model.dto.MyCommunityPostsQueryRequest;
import com.ruyi.teach.model.dto.MyCommunityRepliesQueryRequest;
import com.ruyi.teach.model.entity.CommunityPost;
import com.ruyi.teach.model.entity.CommunityReply;
import com.ruyi.teach.model.vo.CommunityDetailVO;
import com.ruyi.teach.model.vo.CommunityOverviewVO;
import com.ruyi.teach.model.vo.CommunityPostVO;
import com.ruyi.teach.model.vo.CommunityReplyVO;
import com.ruyi.teach.model.vo.MyCommunityPostVO;
import com.ruyi.teach.model.vo.MyCommunityReplyVO;
import com.ruyi.teach.service.CommunityNotificationService;
import com.ruyi.teach.service.CommunityPostService;
import com.ruyi.teach.util.CommunityRichTextSanitizer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityPostServiceImpl extends ServiceImpl<CommunityPostMapper, CommunityPost>
        implements CommunityPostService {

    @Autowired
    private CommunityPostMapper postMapper;

    @Autowired
    private CommunityReplyMapper replyMapper;

    @Autowired
    private CommunityNotificationService communityNotificationService;

    // ==================== 首页概览 ====================

    @Override
    public CommunityOverviewVO getOverview() {
        CommunityOverviewVO vo = new CommunityOverviewVO();

        QueryWrapper<CommunityPost> qw = new QueryWrapper<>();
        qw.eq("post_type", "discussion")
                .orderByDesc("last_active_time")
                .last("LIMIT 8");
        List<CommunityPost> posts = this.list(qw);
        vo.setDiscussions(posts.stream().map(this::toPostVO).collect(Collectors.toList()));

        CommunityOverviewVO.HomeworkHelpSummary hwSummary = new CommunityOverviewVO.HomeworkHelpSummary();
        hwSummary.setTodayQuestionCount(postMapper.countTodayHomeworkQuestions());
        vo.setHomeworkHelp(hwSummary);

        CommunityOverviewVO.FeaturedAnswersSummary faSummary = new CommunityOverviewVO.FeaturedAnswersSummary();
        faSummary.setWeeklySelectedCount(postMapper.countWeeklyFeaturedAnswers());
        vo.setFeaturedAnswers(faSummary);

        return vo;
    }

    // ==================== 讨论列表 ====================

    @Override
    public IPage<CommunityPostVO> listDiscussions(CommunityQueryRequest request) {
        return listDiscussionsForCourses(request, null);
    }

    @Override
    public IPage<CommunityPostVO> listDiscussionsForCourses(CommunityQueryRequest request, List<Long> courseIds) {
        Page<CommunityPost> page = new Page<>(request.getPageNum(), request.getPageSize());

        QueryWrapper<CommunityPost> qw = new QueryWrapper<>();
        qw.eq("post_type", "discussion");

        applyCourseScope(qw, courseIds);

        if (request.getCourseId() != null) {
            qw.eq("course_id", request.getCourseId());
        }

        if (StringUtils.isNotBlank(request.getKeyword())) {
            qw.and(w -> w.like("title", request.getKeyword())
                    .or().like("course_name", request.getKeyword()));
        }

        if ("hot".equals(request.getSort())) {
            qw.orderByDesc("view_count");
        } else {
            qw.orderByDesc("last_active_time");
        }

        IPage<CommunityPost> result = this.page(page, qw);
        return result.convert(this::toPostVO);
    }

    // ==================== 讨论详情 ====================

    @Override
    public CommunityDetailVO getDiscussionDetail(Long id) {
        CommunityPost post = this.getById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "讨论不存在");
        }

        postMapper.incrementViewCount(id);

        CommunityDetailVO vo = new CommunityDetailVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCourseId(post.getCourseId());
        vo.setCourseName(post.getCourseName());
        vo.setAuthorName(post.getAuthorName());
        vo.setCreatedAt(formatDate(post.getCreateTime()));
        vo.setReplyCount(post.getReplyCount() != null ? post.getReplyCount() : 0);
        vo.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0);
        vo.setLastActiveTime(formatTimeAgo(post.getLastActiveTime()));
        vo.setIsHot(Integer.valueOf(1).equals(post.getIsHot()));
        vo.setIsTeacherAnswered(Integer.valueOf(1).equals(post.getIsTeacherAnswered()));
        vo.setPostType(post.getPostType());
        vo.setStatus(post.getStatus());

        QueryWrapper<CommunityReply> replyQw = new QueryWrapper<>();
        replyQw.eq("post_id", id).orderByAsc("create_time");
        List<CommunityReply> replies = replyMapper.selectList(replyQw);
        vo.setReplies(replies.stream().map(this::toReplyVO).collect(Collectors.toList()));

        return vo;
    }

    // ==================== 相关推荐 ====================

    @Override
    public List<CommunityPostVO> getRelatedDiscussions(Long id, Long courseId, int limit) {
        QueryWrapper<CommunityPost> qw = new QueryWrapper<>();
        qw.ne("id", id);
        if (courseId != null) {
            qw.eq("course_id", courseId);
        }
        qw.eq("post_type", "discussion")
                .orderByDesc("last_active_time")
                .last("LIMIT " + Math.min(limit, 10));
        return this.list(qw).stream().map(this::toPostVO).collect(Collectors.toList());
    }

    // ==================== 作业互助列表 ====================

    @Override
    public IPage<CommunityPostVO> listHomeworkHelp(HomeworkHelpQueryRequest request) {
        return listHomeworkHelpForCourses(request, null);
    }

    @Override
    public IPage<CommunityPostVO> listHomeworkHelpForCourses(HomeworkHelpQueryRequest request, List<Long> courseIds) {
        Page<CommunityPost> page = new Page<>(request.getPageNum(), request.getPageSize());

        QueryWrapper<CommunityPost> qw = new QueryWrapper<>();
        qw.eq("post_type", "homework");

        applyCourseScope(qw, courseIds);

        if (request.getCourseId() != null) {
            qw.eq("course_id", request.getCourseId());
        }

        String status = request.getStatus();
        if ("open".equals(status)) {
            qw.eq("status", "open");
        } else if ("resolved".equals(status)) {
            qw.eq("status", "resolved");
        } else if ("teacher".equals(status)) {
            qw.eq("is_teacher_answered", 1);
        }

        if (StringUtils.isNotBlank(request.getKeyword())) {
            qw.and(w -> w.like("title", request.getKeyword())
                    .or().like("course_name", request.getKeyword()));
        }

        qw.orderByDesc("last_active_time");

        IPage<CommunityPost> result = this.page(page, qw);
        return result.convert(this::toHomeworkVO);
    }

    // ==================== 我的提问 ====================

    @Override
    public IPage<MyCommunityPostVO> listMyPosts(Long userId, MyCommunityPostsQueryRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        int pageNum = request != null && request.getPageNum() != null && request.getPageNum() > 0
                ? request.getPageNum() : 1;
        int pageSize = request != null && request.getPageSize() != null && request.getPageSize() > 0
                ? request.getPageSize() : 10;

        Page<CommunityPost> page = new Page<>(pageNum, pageSize);

        QueryWrapper<CommunityPost> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);

        if (request != null
                && StringUtils.isNotBlank(request.getPostType())
                && !"all".equalsIgnoreCase(request.getPostType())) {
            qw.eq("post_type", request.getPostType().trim());
        }

        qw.orderByDesc("last_active_time");

        IPage<CommunityPost> result = this.page(page, qw);
        return result.convert(this::toMyPostVO);
    }

    // ==================== 我的回复 ====================

    @Override
    public IPage<MyCommunityReplyVO> listMyReplies(Long userId, MyCommunityRepliesQueryRequest request) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        int pageNum = request != null && request.getPageNum() != null && request.getPageNum() > 0
                ? request.getPageNum() : 1;
        int pageSize = request != null && request.getPageSize() != null && request.getPageSize() > 0
                ? request.getPageSize() : 10;

        long total = replyMapper.countMyRepliedPosts(userId);
        long offset = (long) (pageNum - 1) * pageSize;

        List<MyCommunityReplyVO> records = total > 0
                ? replyMapper.selectMyRepliedPosts(userId, offset, pageSize)
                : Collections.emptyList();

        Page<MyCommunityReplyVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(records);
        return page;
    }

    // ==================== 写能力：发布作业提问 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addHomeworkHelp(HomeworkHelpAddRequest request, Long userId, String authorName) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (StringUtils.isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
        }
        if (StringUtils.isBlank(request.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容不能为空");
        }
        if (request.getCourseId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "所属课程不能为空");
        }

        Date now = new Date();

        CommunityPost post = new CommunityPost();
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent().trim());
        post.setPostType("homework");
        post.setCourseId(request.getCourseId());
        post.setCourseName(StringUtils.isNotBlank(request.getCourseName())
                ? request.getCourseName().trim()
                : "课程#" + request.getCourseId());
        post.setUserId(userId);
        post.setAuthorName(StringUtils.isNotBlank(authorName) ? authorName : "当前同学");

        post.setStatus("open");
        post.setIsHot(0);
        post.setIsTeacherAnswered(0);
        post.setReplyCount(0);
        post.setViewCount(0);
        post.setLastActiveTime(now);
        post.setCreateTime(now);
        post.setUpdateTime(now);
        post.setIsDelete(0);

        boolean saved = this.save(post);
        if (!saved || post.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发布提问失败");
        }
        return post.getId();
    }

    // ==================== 写能力：标记已解决 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resolvePost(Long id) {
        return resolvePostForCourses(id, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resolvePostForCourses(Long id, List<Long> courseIds) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不合法");
        }

        CommunityPost post = this.getById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        if (!"homework".equals(post.getPostType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅作业互助帖子支持标记已解决");
        }
        if (courseIds != null && !courseIds.contains(post.getCourseId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能处理本人授课课程中的作业问题");
        }

        int rows = postMapper.resolveHomeworkPost(id, new Date());
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "标记已解决失败");
        }

        communityNotificationService.notifyOnPostResolved(post);
        return true;
    }

    private void applyCourseScope(QueryWrapper<CommunityPost> queryWrapper, List<Long> courseIds) {
        if (courseIds == null) {
            return;
        }
        if (courseIds.isEmpty()) {
            queryWrapper.apply("1 = 0");
            return;
        }
        queryWrapper.in("course_id", courseIds);
    }

    // ==================== 转换方法 ====================

    private CommunityPostVO toPostVO(CommunityPost post) {
        CommunityPostVO vo = new CommunityPostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setCourseId(post.getCourseId());
        vo.setCourseName(post.getCourseName());
        vo.setReplyCount(post.getReplyCount() != null ? post.getReplyCount() : 0);
        vo.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0);
        vo.setLastActiveTime(formatTimeAgo(post.getLastActiveTime()));
        vo.setLastActiveTimestamp(post.getLastActiveTime() != null ? post.getLastActiveTime().getTime() : null);
        vo.setIsHot(Integer.valueOf(1).equals(post.getIsHot()));
        vo.setIsTeacherAnswered(Integer.valueOf(1).equals(post.getIsTeacherAnswered()));
        return vo;
    }

    private CommunityPostVO toHomeworkVO(CommunityPost post) {
        CommunityPostVO vo = toPostVO(post);
        vo.setStatus(post.getStatus());
        vo.setAuthorName(post.getAuthorName());
        if (StringUtils.isNotBlank(post.getContent())) {
            String text = post.getContent().replaceAll("\\n+", " ");
            vo.setExcerpt(text.length() > 80 ? text.substring(0, 80) + "..." : text);
        }
        return vo;
    }

    private MyCommunityPostVO toMyPostVO(CommunityPost post) {
        MyCommunityPostVO vo = new MyCommunityPostVO();
        vo.setId(post.getId());
        vo.setTitle(post.getTitle());
        vo.setCourseId(post.getCourseId());
        vo.setCourseName(post.getCourseName());
        vo.setPostType(post.getPostType());
        vo.setStatus(post.getStatus());
        vo.setReplyCount(post.getReplyCount() != null ? post.getReplyCount() : 0);
        vo.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0);
        vo.setLastActiveTime(formatTimeAgo(post.getLastActiveTime()));
        vo.setLastActiveTimestamp(post.getLastActiveTime() != null ? post.getLastActiveTime().getTime() : null);
        vo.setTeacherAnswered(Integer.valueOf(1).equals(post.getIsTeacherAnswered()));
        return vo;
    }

    private CommunityReplyVO toReplyVO(CommunityReply reply) {
        CommunityReplyVO vo = new CommunityReplyVO();
        vo.setId(reply.getId());
        vo.setUserId(reply.getUserId());
        vo.setAuthorName(reply.getAuthorName());
        vo.setContent(CommunityRichTextSanitizer.sanitize(reply.getContent()));
        vo.setCreatedAt(formatDate(reply.getCreateTime()));
        vo.setTeacher(Integer.valueOf(1).equals(reply.getIsTeacher()));
        return vo;
    }

    // ==================== 时间格式化工具 ====================

    private String formatDate(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private String formatTimeAgo(Date date) {
        if (date == null) return "";
        long diffMs = System.currentTimeMillis() - date.getTime();
        long diffMin = diffMs / (60 * 1000);
        long diffHour = diffMs / (3600 * 1000);
        long diffDay = diffMs / (86400 * 1000L);

        if (diffMin < 1) return "刚刚";
        if (diffMin < 60) return diffMin + " 分钟前";
        if (diffHour < 24) return diffHour + " 小时前";
        if (diffDay == 1) return "昨天";
        if (diffDay < 30) return diffDay + " 天前";
        return new SimpleDateFormat("MM-dd").format(date);
    }
}
