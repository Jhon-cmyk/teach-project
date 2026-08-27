package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.StudentScheduleAnalysisRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentScheduleAnalysisRecordMapper extends BaseMapper<StudentScheduleAnalysisRecord> {

    StudentScheduleAnalysisRecord selectLatestByUserId(@Param("userId") Long userId);

    StudentScheduleAnalysisRecord selectByUserIdAndSemester(@Param("userId") Long userId,
                                                            @Param("semesterLabel") String semesterLabel);

    int insertRecord(StudentScheduleAnalysisRecord record);

    int updateRecord(StudentScheduleAnalysisRecord record);
}