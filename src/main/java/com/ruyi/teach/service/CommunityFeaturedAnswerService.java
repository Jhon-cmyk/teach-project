package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.CommunityFeaturedAddRequest;
import com.ruyi.teach.model.dto.FeaturedAnswersQueryRequest;
import com.ruyi.teach.model.entity.CommunityFeaturedAnswer;
import com.ruyi.teach.model.vo.FeaturedAnswerVO;

import java.util.List;

public interface CommunityFeaturedAnswerService extends IService<CommunityFeaturedAnswer> {

    /**
     * 分页查询答疑精选列表
     */
    IPage<FeaturedAnswerVO> listFeaturedAnswers(FeaturedAnswersQueryRequest request);

    /**
     * 新增答疑精选
     */
    Long addFeaturedAnswer(CommunityFeaturedAddRequest request);

    Long addFeaturedAnswerForCourses(CommunityFeaturedAddRequest request, List<Long> courseIds);
}
