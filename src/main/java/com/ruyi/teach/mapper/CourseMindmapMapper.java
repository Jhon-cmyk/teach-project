package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CourseMindmap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseMindmapMapper extends BaseMapper<CourseMindmap> {

    CourseMindmap selectByCourseId(@Param("courseId") Long courseId);

    int upsert(CourseMindmap entity);
}