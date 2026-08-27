package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.courseanalysis.SaveScheduleAnalysisRequest;
import com.ruyi.teach.model.entity.StudentScheduleAnalysisRecord;
import com.ruyi.teach.model.vo.ScheduleAnalysisRecordVO;

public interface StudentScheduleAnalysisRecordService extends IService<StudentScheduleAnalysisRecord> {

    ScheduleAnalysisRecordVO saveRecord(SaveScheduleAnalysisRequest request);

    ScheduleAnalysisRecordVO getLatestRecord(Long userId, String semesterLabel);
}