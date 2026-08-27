package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CourseGraphNode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CourseGraphNodeMapper extends BaseMapper<CourseGraphNode> {

    List<CourseGraphNode> selectActiveNodes();

    List<String> selectChildIdsByParentIds(@Param("parentIds") Collection<String> parentIds);

    @Delete("DELETE FROM course_graph_node WHERE isDelete = 0")
    int deleteActivePhysical();
}
