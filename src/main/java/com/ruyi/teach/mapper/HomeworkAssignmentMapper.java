package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HomeworkAssignmentMapper extends BaseMapper<HomeworkAssignment> {

    /**
     * Serializes attempt-number allocation for one assignment.
     * Must only be called from an active transaction.
     */
    @Select("SELECT * FROM homework_assignment WHERE id = #{id} AND isDelete = 0 FOR UPDATE")
    HomeworkAssignment selectByIdForUpdate(@Param("id") Long id);
}
