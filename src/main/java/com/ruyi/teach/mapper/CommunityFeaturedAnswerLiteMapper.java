package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CommunityFeaturedAnswerLite;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CommunityFeaturedAnswerLiteMapper extends BaseMapper<CommunityFeaturedAnswerLite> {
    List<CommunityFeaturedAnswerLite> selectActiveFeatured();
}
