package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CourseGraphNodeActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseGraphNodeActivityMapper extends BaseMapper<CourseGraphNodeActivity> {

    List<CourseGraphNodeActivity> selectActiveByNodeId(@Param("nodeId") String nodeId);

    List<CourseGraphNodeActivity> selectActiveByNodeIds(@Param("nodeIds") List<String> nodeIds);
}
