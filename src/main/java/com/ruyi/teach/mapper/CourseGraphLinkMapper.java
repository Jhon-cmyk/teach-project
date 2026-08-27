package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CourseGraphLink;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CourseGraphLinkMapper extends BaseMapper<CourseGraphLink> {

    List<CourseGraphLink> selectActiveLinks();

    @Delete("DELETE FROM course_graph_link WHERE isDelete = 0")
    int deleteActivePhysical();
}
