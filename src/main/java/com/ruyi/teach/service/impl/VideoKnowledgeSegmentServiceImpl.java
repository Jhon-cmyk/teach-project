package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.VideoKnowledgeSegmentMapper;
import com.ruyi.teach.model.dto.video.VideoKnowledgeSegmentSaveRequest;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.VideoKnowledgeSegment;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.service.VideoKnowledgeSegmentService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class VideoKnowledgeSegmentServiceImpl
        extends ServiceImpl<VideoKnowledgeSegmentMapper, VideoKnowledgeSegment>
        implements VideoKnowledgeSegmentService {

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private CourseService courseService;

    @Override
    public List<VideoKnowledgeSegment> listByChapterId(Long chapterId) {
        if (chapterId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "chapterId不能为空");
        }
        LambdaQueryWrapper<VideoKnowledgeSegment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoKnowledgeSegment::getChapterId, chapterId)
                .eq(VideoKnowledgeSegment::getIsDelete, 0)
                .orderByAsc(VideoKnowledgeSegment::getSortOrder)
                .orderByAsc(VideoKnowledgeSegment::getStartSecond);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveChapterSegments(Long chapterId, VideoKnowledgeSegmentSaveRequest request, User loginUser) {
        CourseChapter chapter = requireChapter(chapterId);
        requireTeacherOwner(chapter, loginUser);

        List<VideoKnowledgeSegmentSaveRequest.SegmentItem> items =
                request == null || request.getSegments() == null ? List.of() : request.getSegments();
        List<VideoKnowledgeSegment> segments = normalizeAndValidate(chapterId, items);

        LambdaQueryWrapper<VideoKnowledgeSegment> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(VideoKnowledgeSegment::getChapterId, chapterId);
        remove(removeWrapper);

        if (segments.isEmpty()) {
            return true;
        }
        return saveBatch(segments);
    }

    private CourseChapter requireChapter(Long chapterId) {
        if (chapterId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "chapterId不能为空");
        }
        CourseChapter chapter = courseChapterService.getById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "章节不存在");
        }
        return chapter;
    }

    private void requireTeacherOwner(CourseChapter chapter, User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if ("admin".equals(loginUser.getUserRole())) {
            return;
        }
        if (!"teacher".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可维护知识点时间轴");
        }
        Course course = courseService.getById(chapter.getCourseId());
        if (course == null || course.getTeacherId() == null || !course.getTeacherId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能维护自己课程的知识点时间轴");
        }
    }

    private List<VideoKnowledgeSegment> normalizeAndValidate(
            Long chapterId,
            List<VideoKnowledgeSegmentSaveRequest.SegmentItem> items
    ) {
        List<VideoKnowledgeSegment> result = new ArrayList<>();
        Date now = new Date();
        int fallbackOrder = 1;

        for (VideoKnowledgeSegmentSaveRequest.SegmentItem item : items) {
            if (item == null) {
                continue;
            }
            String name = trim(item.getKnowledgeName());
            if (name.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识点名称不能为空");
            }
            Integer start = item.getStartSecond();
            Integer end = item.getEndSecond();
            if (start == null || end == null || start < 0 || end <= start) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识点时间段不合法");
            }

            VideoKnowledgeSegment segment = new VideoKnowledgeSegment();
            segment.setChapterId(chapterId);
            segment.setStartSecond(start);
            segment.setEndSecond(end);
            segment.setKnowledgeName(name);
            segment.setDescription(trim(item.getDescription()));
            segment.setDifficulty(trim(item.getDifficulty()).isEmpty() ? "中" : trim(item.getDifficulty()));
            segment.setSortOrder(item.getSortOrder() == null ? fallbackOrder : item.getSortOrder());
            segment.setCreateTime(now);
            segment.setUpdateTime(now);
            segment.setIsDelete(0);
            result.add(segment);
            fallbackOrder++;
        }

        result.sort(Comparator.comparing(VideoKnowledgeSegment::getStartSecond)
                .thenComparing(VideoKnowledgeSegment::getEndSecond));
        for (int i = 1; i < result.size(); i++) {
            VideoKnowledgeSegment prev = result.get(i - 1);
            VideoKnowledgeSegment cur = result.get(i);
            if (cur.getStartSecond() < prev.getEndSecond()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "同一章节的知识点时间段不能重叠");
            }
        }
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setSortOrder(i + 1);
        }
        return result;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
