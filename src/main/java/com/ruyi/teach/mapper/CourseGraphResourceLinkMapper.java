package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CourseGraphResourceLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseGraphResourceLinkMapper extends BaseMapper<CourseGraphResourceLink> {
    List<CourseGraphResourceLink> selectActiveByTeacherAndNode(@Param("teacherId") Long teacherId, @Param("nodeId") String nodeId);
    List<CourseGraphResourceLink> selectActiveByTeacherAndNodeIds(@Param("teacherId") Long teacherId, @Param("nodeIds") List<String> nodeIds);
    CourseGraphResourceLink selectByTeacherNodeAndResource(@Param("teacherId") Long teacherId, @Param("nodeId") String nodeId, @Param("resourceId") Long resourceId);
}
