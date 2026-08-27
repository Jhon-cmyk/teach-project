package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CourseGraphNodeProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseGraphNodeProgressMapper extends BaseMapper<CourseGraphNodeProgress> {

    List<CourseGraphNodeProgress> selectByStudentId(@Param("studentId") Long studentId);

    List<CourseGraphNodeProgress> selectByNodeId(@Param("nodeId") String nodeId);

    List<CourseGraphNodeProgress> selectByStudentIdsAndNodeId(
            @Param("studentIds") List<Long> studentIds,
            @Param("nodeId") String nodeId);

    List<CourseGraphNodeProgress> selectByStudentIdsAndNodeIds(
            @Param("studentIds") List<Long> studentIds,
            @Param("nodeIds") List<String> nodeIds);

    int batchUpsertProgress(@Param("list") List<CourseGraphNodeProgress> list);
}
