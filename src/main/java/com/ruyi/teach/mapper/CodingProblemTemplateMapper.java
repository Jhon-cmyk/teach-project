package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CodingProblemTemplate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CodingProblemTemplateMapper extends BaseMapper<CodingProblemTemplate> {

    @Delete("DELETE FROM coding_problem_template WHERE problem_id = #{problemId}")
    int deleteByProblemIdPhysical(Long problemId);
}
