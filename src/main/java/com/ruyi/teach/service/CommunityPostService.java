package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.CommunityQueryRequest;
import com.ruyi.teach.model.dto.HomeworkHelpAddRequest;
import com.ruyi.teach.model.dto.HomeworkHelpQueryRequest;
import com.ruyi.teach.model.dto.MyCommunityPostsQueryRequest;
import com.ruyi.teach.model.dto.MyCommunityRepliesQueryRequest;
import com.ruyi.teach.model.entity.CommunityPost;
import com.ruyi.teach.model.vo.CommunityDetailVO;
import com.ruyi.teach.model.vo.CommunityOverviewVO;
import com.ruyi.teach.model.vo.CommunityPostVO;
import com.ruyi.teach.model.vo.MyCommunityPostVO;
import com.ruyi.teach.model.vo.MyCommunityReplyVO;

import java.util.List;

public interface CommunityPostService extends IService<CommunityPost> {

    CommunityOverviewVO getOverview();

    IPage<CommunityPostVO> listDiscussions(CommunityQueryRequest request);

    IPage<CommunityPostVO> listDiscussionsForCourses(CommunityQueryRequest request, List<Long> courseIds);

    CommunityDetailVO getDiscussionDetail(Long id);

    List<CommunityPostVO> getRelatedDiscussions(Long id, Long courseId, int limit);

    IPage<CommunityPostVO> listHomeworkHelp(HomeworkHelpQueryRequest request);

    IPage<CommunityPostVO> listHomeworkHelpForCourses(HomeworkHelpQueryRequest request, List<Long> courseIds);

    Long addHomeworkHelp(HomeworkHelpAddRequest request, Long userId, String authorName);

    Boolean resolvePost(Long id);

    Boolean resolvePostForCourses(Long id, List<Long> courseIds);

    /**
     * 我的提问
     */
    IPage<MyCommunityPostVO> listMyPosts(Long userId, MyCommunityPostsQueryRequest request);

    /**
     * 我的回复
     */
    IPage<MyCommunityReplyVO> listMyReplies(Long userId, MyCommunityRepliesQueryRequest request);
}
