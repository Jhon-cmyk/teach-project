package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CourseGraphPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseGraphPreferenceMapper extends BaseMapper<CourseGraphPreference> {

    CourseGraphPreference selectByTeacherId(@Param("teacherId") Long teacherId);
}
