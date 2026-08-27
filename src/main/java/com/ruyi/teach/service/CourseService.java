package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.ResourcePreviewVO;
import com.ruyi.teach.model.vo.ResourceSearchPageVO;

public interface CourseService extends IService<Course> {

    boolean saveCourseWithClasses(Course course);

    boolean updateCourseWithClasses(Course course);

    ResourceSearchPageVO searchPublicResourcePage(String keyword, String type, long current, long pageSize, String sortMode, User loginUser);

    /**
     * 学生端资源预览详情
     */
    ResourcePreviewVO getResourcePreview(Long id, String type, User loginUser);
}
