package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CommunityPostLite;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CommunityPostLiteMapper extends BaseMapper<CommunityPostLite> {
    List<CommunityPostLite> selectActivePosts();
}
