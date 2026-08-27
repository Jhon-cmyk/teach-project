package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.entity.ClassAnalysisRecord;

public interface ClassAnalysisService extends IService<ClassAnalysisRecord> {
    Long submitAnalysisTask(Long teacherId, String audioUrl, Long planId, String planText);
    void asyncTranscription(Long recordId);
    void generateAndSaveReport(Long recordId);
}