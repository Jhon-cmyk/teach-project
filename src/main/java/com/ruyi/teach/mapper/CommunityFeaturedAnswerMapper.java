package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.model.entity.CommunityFeaturedAnswer;
import com.ruyi.teach.model.vo.FeaturedAnswerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityFeaturedAnswerMapper extends BaseMapper<CommunityFeaturedAnswer> {

    /**
     * 精选列表分页
     */
    IPage<FeaturedAnswerVO> selectFeaturedAnswerPage(Page<FeaturedAnswerVO> page,
                                                     @Param("courseId") Long courseId,
                                                     @Param("sort") String sort,
                                                     @Param("keyword") String keyword);

    /**
     * 新增精选
     */
    int insertFeaturedAnswer(CommunityFeaturedAnswer entity);
}