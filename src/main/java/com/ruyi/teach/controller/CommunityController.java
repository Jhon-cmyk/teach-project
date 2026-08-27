package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.CommunityFeaturedAddRequest;
import com.ruyi.teach.model.dto.CommunityQueryRequest;
import com.ruyi.teach.model.dto.CommunityReplyAddRequest;
import com.ruyi.teach.model.dto.FeaturedAnswersQueryRequest;
import com.ruyi.teach.model.dto.HomeworkHelpAddRequest;
import com.ruyi.teach.model.dto.HomeworkHelpQueryRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CommunityPost;
import com.ruyi.teach.model.entity.CommunityReply;
import com.ruyi.teach.model.vo.CommunityDetailVO;
import com.ruyi.teach.model.vo.CommunityOverviewVO;
import com.ruyi.teach.model.vo.CommunityPostVO;
import com.ruyi.teach.model.vo.FeaturedAnswerVO;
import com.ruyi.teach.service.CommunityFeaturedAnswerService;
import com.ruyi.teach.service.CommunityPostService;
import com.ruyi.teach.service.CommunityReplyService;
import com.ruyi.teach.model.dto.MyCommunityPostsQueryRequest;
import com.ruyi.teach.model.dto.MyCommunityRepliesQueryRequest;
import com.ruyi.teach.model.vo.MyCommunityPostVO;
import com.ruyi.teach.model.vo.MyCommunityReplyVO;
import com.ruyi.teach.model.dto.CommunityNotificationQueryRequest;
import com.ruyi.teach.model.vo.CommunityNotificationVO;
import com.ruyi.teach.service.CommunityNotificationService;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.OssService;
import com.ruyi.teach.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学习交流社区 Controller
 * 在现有 6 个只读接口基础上，新增最小互动闭环写接口
 */
@RestController
@RequestMapping("/community")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CommunityController {

    private static final long MAX_REPLY_IMAGE_SIZE = 5L * 1024 * 1024;

    @Autowired
    private CommunityPostService postService;

    @Autowired
    private CommunityReplyService replyService;

    @Autowired
    private CommunityFeaturedAnswerService featuredAnswerService;

    @Autowired
    private CommunityNotificationService communityNotificationService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private OssService ossService;

    private CommunityUserContext requireTeacherCommunityUser(HttpServletRequest request) {
        CommunityUserContext ctx = getRequiredCommunityUser(request);
        if (ctx.getIsTeacher() == null || ctx.getIsTeacher() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅教师可执行该操作");
        }
        return ctx;
    }

    /**
     * 1. 首页学习交流概览
     * GET /community/overview
     */
    @GetMapping("/overview")
    public BaseResponse<CommunityOverviewVO> getOverview() {
        CommunityOverviewVO overview = postService.getOverview();
        return ResultUtils.success(overview);
    }

    /**
     * 2. 学习交流讨论列表（分页）
     * GET /community/discussions
     */
    @GetMapping("/discussions")
    public BaseResponse<IPage<CommunityPostVO>> listDiscussions(CommunityQueryRequest request) {
        IPage<CommunityPostVO> page = postService.listDiscussions(request);
        return ResultUtils.success(page);
    }

    /**
     * 3. 讨论详情
     * GET /community/discussions/{id}
     */
    @GetMapping("/discussions/{id}")
    public BaseResponse<CommunityDetailVO> getDiscussionDetail(@PathVariable("id") Long id) {
        CommunityDetailVO detail = postService.getDiscussionDetail(id);
        return ResultUtils.success(detail);
    }

    /**
     * 4. 相关推荐讨论
     * GET /community/discussions/{id}/related
     */
    @GetMapping("/discussions/{id}/related")
    public BaseResponse<List<CommunityPostVO>> getRelatedDiscussions(
            @PathVariable("id") Long id,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "limit", defaultValue = "4") int limit
    ) {
        List<CommunityPostVO> list = postService.getRelatedDiscussions(id, courseId, limit);
        return ResultUtils.success(list);
    }

    /**
     * 5. 作业互助列表（分页）
     * GET /community/homework-help
     */
    @GetMapping("/homework-help")
    public BaseResponse<IPage<CommunityPostVO>> listHomeworkHelp(HomeworkHelpQueryRequest request) {
        IPage<CommunityPostVO> page = postService.listHomeworkHelp(request);
        return ResultUtils.success(page);
    }

    /**
     * 教师处理台讨论列表：仅返回当前教师授课课程中的数据。
     */
    @GetMapping("/teacher/discussions")
    public BaseResponse<IPage<CommunityPostVO>> listTeacherDiscussions(CommunityQueryRequest request,
                                                                       HttpServletRequest httpServletRequest) {
        CommunityUserContext teacher = requireTeacherCommunityUser(httpServletRequest);
        return ResultUtils.success(postService.listDiscussionsForCourses(request, getTeacherCourseIds(teacher.getUserId())));
    }

    /**
     * 教师处理台作业问题：open 与 resolved 均可分页查看，但范围始终限定为本人授课课程。
     */
    @GetMapping("/teacher/homework-help")
    public BaseResponse<IPage<CommunityPostVO>> listTeacherHomeworkHelp(HomeworkHelpQueryRequest request,
                                                                        HttpServletRequest httpServletRequest) {
        CommunityUserContext teacher = requireTeacherCommunityUser(httpServletRequest);
        return ResultUtils.success(postService.listHomeworkHelpForCourses(request, getTeacherCourseIds(teacher.getUserId())));
    }

    /**
     * 6. 答疑精选列表（分页）
     * GET /community/featured-answers
     */
    @GetMapping("/featured-answers")
    public BaseResponse<IPage<FeaturedAnswerVO>> listFeaturedAnswers(FeaturedAnswersQueryRequest request) {
        IPage<FeaturedAnswerVO> page = featuredAnswerService.listFeaturedAnswers(request);
        return ResultUtils.success(page);
    }

    /**
     * 7. 发布作业互助提问
     * POST /community/homework-help/add
     */
    @PostMapping("/homework-help/add")
    public BaseResponse<Long> addHomeworkHelp(@RequestBody HomeworkHelpAddRequest addRequest,
                                              HttpServletRequest request) {
        CommunityUserContext user = getRequiredCommunityUser(request);
        Course course = requireWritableCourse(addRequest.getCourseId(), user);
        addRequest.setCourseName(course.getName());
        Long postId = postService.addHomeworkHelp(addRequest, user.getUserId(), user.getAuthorName());
        return ResultUtils.success(postId);
    }

    /**
     * 8. 提交回复
     * POST /community/reply/add
     */
    @PostMapping("/reply/add")
    public BaseResponse<Long> addReply(@RequestBody CommunityReplyAddRequest addRequest,
                                       HttpServletRequest request) {
        CommunityUserContext user = getRequiredCommunityUser(request);
        if (Integer.valueOf(1).equals(user.getIsTeacher())) {
            requireTeacherPostAccess(addRequest != null ? addRequest.getPostId() : null, user.getUserId());
        }
        Long replyId = replyService.addReply(
                addRequest,
                user.getUserId(),
                user.getAuthorName(),
                user.getIsTeacher()
        );
        return ResultUtils.success(replyId);
    }

    /**
     * 教师社区富文本回复图片上传。图片文件进入 OSS，回复正文只保存返回的 URL。
     */
    @PostMapping("/teacher/reply/image")
    public BaseResponse<String> uploadReplyImage(@RequestParam("file") MultipartFile file,
                                                  HttpServletRequest request) {
        requireTeacherCommunityUser(request);
        validateReplyImage(file);
        return ResultUtils.success(ossService.uploadFile(file, "community/replies"));
    }

    /**
     * 教师删除自己发布的社区回复。
     * POST /community/teacher/reply/delete/{replyId}
     */
    @PostMapping("/teacher/reply/delete/{replyId}")
    public BaseResponse<Boolean> deleteTeacherReply(@PathVariable("replyId") Long replyId,
                                                    HttpServletRequest request) {
        CommunityUserContext teacher = requireTeacherCommunityUser(request);
        CommunityReply reply = replyService.getById(replyId);
        if (reply == null || Integer.valueOf(1).equals(reply.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "回复不存在");
        }
        if (!teacher.getUserId().equals(reply.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能删除自己发布的回复");
        }
        if (!Integer.valueOf(1).equals(reply.getIsTeacher())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能删除教师回复");
        }
        requireTeacherPostAccess(reply.getPostId(), teacher.getUserId());
        return ResultUtils.success(replyService.deleteOwnTeacherReply(replyId, teacher.getUserId()));
    }

    /**
     * 9. 标记作业问题已解决
     * POST /community/post/resolve/{id}
     */
    @PostMapping("/post/resolve/{id}")
    public BaseResponse<Boolean> resolvePost(@PathVariable("id") Long id,
                                             HttpServletRequest request) {
        CommunityUserContext teacher = requireTeacherCommunityUser(request);
        return ResultUtils.success(postService.resolvePostForCourses(id, getTeacherCourseIds(teacher.getUserId())));
    }

    /**
     * 10. 加入答疑精选
     * POST /community/featured/add
     */
    @PostMapping("/featured/add")
    public BaseResponse<Long> addFeaturedAnswer(@RequestBody CommunityFeaturedAddRequest addRequest,
                                                HttpServletRequest request) {
        CommunityUserContext teacher = requireTeacherCommunityUser(request);

        addRequest.setTeacherId(teacher.getUserId());
        addRequest.setTeacherName(teacher.getAuthorName());

        Long featuredId = featuredAnswerService.addFeaturedAnswerForCourses(
                addRequest,
                getTeacherCourseIds(teacher.getUserId())
        );
        return ResultUtils.success(featuredId);
    }

    /**
     * 11. 我的提问
     * GET /community/my/posts
     */
    @GetMapping("/my/posts")
    public BaseResponse<IPage<MyCommunityPostVO>> listMyPosts(MyCommunityPostsQueryRequest request,
                                                              HttpServletRequest httpServletRequest) {
        CommunityUserContext user = getRequiredCommunityUser(httpServletRequest);
        IPage<MyCommunityPostVO> page = postService.listMyPosts(user.getUserId(), request);
        return ResultUtils.success(page);
    }

    /**
     * 12. 我的回复
     * GET /community/my/replies
     */
    @GetMapping("/my/replies")
    public BaseResponse<IPage<MyCommunityReplyVO>> listMyReplies(MyCommunityRepliesQueryRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        CommunityUserContext user = getRequiredCommunityUser(httpServletRequest);
        IPage<MyCommunityReplyVO> page = postService.listMyReplies(user.getUserId(), request);
        return ResultUtils.success(page);
    }

    /**
     * 当前社区用户上下文
     */
    @Data
    private static class CommunityUserContext {
        private Long userId;
        private String authorName;
        private Integer isTeacher;
    }

    private CommunityUserContext getRequiredCommunityUser(HttpServletRequest request) {
        User loginUser = null;
        try {
            loginUser = SessionUserContext.getOptional(request);
        } catch (Exception ignored) {
        }

        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }

        CommunityUserContext ctx = new CommunityUserContext();
        ctx.setUserId(loginUser.getId());
        ctx.setAuthorName(StringUtils.isNotBlank(loginUser.getUserName()) ? loginUser.getUserName() : "当前用户");
        ctx.setIsTeacher("teacher".equals(loginUser.getUserRole()) ? 1 : 0);
        return ctx;
    }

    private List<Long> getTeacherCourseIds(Long teacherId) {
        QueryWrapper<Course> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teacherId", teacherId).select("id");
        return courseService.list(queryWrapper).stream()
                .map(Course::getId)
                .collect(Collectors.toList());
    }

    private void requireTeacherPostAccess(Long postId, Long teacherId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不能为空");
        }
        CommunityPost post = postService.getById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        if (post.getCourseId() == null || !getTeacherCourseIds(teacherId).contains(post.getCourseId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能回复本人授课课程中的问题");
        }
    }

    private void validateReplyImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_REPLY_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复图片不能超过5MB");
        }
        String contentType = StringUtils.defaultString(file.getContentType()).toLowerCase();
        if (!("image/jpeg".equals(contentType)
                || "image/png".equals(contentType)
                || "image/gif".equals(contentType))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 JPG、PNG、GIF 图片");
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件无效");
            }
            if (image.getWidth() > 8000 || image.getHeight() > 8000) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片尺寸不能超过8000×8000");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片读取失败");
        }
    }

    private Course requireWritableCourse(Long courseId, CommunityUserContext user) {
        if (courseId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "所属课程不能为空");
        }
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }
        if (Integer.valueOf(1).equals(user.getIsTeacher())) {
            if (!user.getUserId().equals(course.getTeacherId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能在本人授课课程中提问");
            }
            return course;
        }

        boolean enrolled = courseMapper.selectStudentCourses(user.getUserId()).stream()
                .anyMatch(item -> courseId.equals(item.getId()));
        if (!enrolled) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能选择本班已开设课程");
        }
        return course;
    }

    /**
     * 13. 我的社区动态
     * GET /community/notifications
     */
    @GetMapping("/notifications")
    public BaseResponse<IPage<CommunityNotificationVO>> listNotifications(CommunityNotificationQueryRequest request,
                                                                          HttpServletRequest httpServletRequest) {
        CommunityUserContext user = getRequiredCommunityUser(httpServletRequest);
        IPage<CommunityNotificationVO> page = communityNotificationService.listMyNotifications(user.getUserId(), request);
        return ResultUtils.success(page);
    }

    /**
     * 14. 标记单条已读
     * POST /community/notifications/read/{id}
     */
    @PostMapping("/notifications/read/{id}")
    public BaseResponse<Boolean> readNotification(@PathVariable("id") Long id,
                                                  HttpServletRequest httpServletRequest) {
        CommunityUserContext user = getRequiredCommunityUser(httpServletRequest);
        return ResultUtils.success(communityNotificationService.readNotification(user.getUserId(), id));
    }

    /**
     * 15. 全部标记已读
     * POST /community/notifications/read-all
     */
    @PostMapping("/notifications/read-all")
    public BaseResponse<Integer> readAllNotifications(HttpServletRequest httpServletRequest) {
        CommunityUserContext user = getRequiredCommunityUser(httpServletRequest);
        return ResultUtils.success(communityNotificationService.readAllNotifications(user.getUserId()));
    }

    /**
     * 16. 获取未读数
     * GET /community/notifications/unread-count
     */
    @GetMapping("/notifications/unread-count")
    public BaseResponse<Integer> getUnreadNotificationCount(HttpServletRequest httpServletRequest) {
        CommunityUserContext user = getRequiredCommunityUser(httpServletRequest);
        return ResultUtils.success(communityNotificationService.countUnreadNotifications(user.getUserId()));
    }
}
