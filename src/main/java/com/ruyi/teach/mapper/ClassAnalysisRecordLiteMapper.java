package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.ClassAnalysisRecordLite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClassAnalysisRecordLiteMapper extends BaseMapper<ClassAnalysisRecordLite> {

    List<ClassAnalysisRecordLite> selectRecentByTeacherId(@Param("teacherId") Long teacherId);
}
