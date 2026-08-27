package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.entity.CourseMindmap;
import com.ruyi.teach.model.vo.CourseMindmapVO;

public interface CourseMindmapService extends IService<CourseMindmap> {

    /**
     * 获取课程总思维导图
     */
    CourseMindmapVO getCourseMindmap(Long courseId);

    /**
     * 强制重新生成课程总思维导图
     */
    CourseMindmapVO regenerateCourseMindmap(Long courseId);
}