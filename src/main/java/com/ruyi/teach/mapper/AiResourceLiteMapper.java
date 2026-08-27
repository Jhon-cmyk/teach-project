package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.AiResourceLite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiResourceLiteMapper extends BaseMapper<AiResourceLite> {
    List<AiResourceLite> selectActiveByTeacherId(@Param("teacherId") Long teacherId);
    List<AiResourceLite> selectActiveByIds(@Param("ids") List<Long> ids);
}
