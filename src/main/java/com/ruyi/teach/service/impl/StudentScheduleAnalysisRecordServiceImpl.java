package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.StudentScheduleAnalysisRecordMapper;
import com.ruyi.teach.model.dto.courseanalysis.SaveScheduleAnalysisRequest;
import com.ruyi.teach.model.entity.StudentScheduleAnalysisRecord;
import com.ruyi.teach.model.vo.ScheduleAnalysisRecordVO;
import com.ruyi.teach.service.StudentScheduleAnalysisRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class StudentScheduleAnalysisRecordServiceImpl
        extends ServiceImpl<StudentScheduleAnalysisRecordMapper, StudentScheduleAnalysisRecord>
        implements StudentScheduleAnalysisRecordService {

    @Resource
    private StudentScheduleAnalysisRecordMapper recordMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleAnalysisRecordVO saveRecord(SaveScheduleAnalysisRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户信息不能为空");
        }
        if (CollectionUtils.isEmpty(request.getMatchedCourses())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "匹配课程不能为空");
        }

        String semesterLabel = StringUtils.hasText(request.getSemesterLabel())
                ? request.getSemesterLabel()
                : "default";

        StudentScheduleAnalysisRecord existing = recordMapper.selectByUserIdAndSemester(
                request.getUserId(),
                semesterLabel
        );

        StudentScheduleAnalysisRecord record = existing != null ? existing : new StudentScheduleAnalysisRecord();
        record.setUserId(request.getUserId());
        record.setSourceFileName(request.getSourceFileName());
        record.setSourceFileUrl(request.getSourceFileUrl());
        record.setExtractedJson(toJson(request.getExtractedCourses()));
        record.setMatchedJson(toJson(request.getMatchedCourses()));
        record.setInsightsJson(toJson(request.getInsights()));
        record.setSemesterLabel(semesterLabel);
        record.setStatus("completed");

        if (existing == null) {
            recordMapper.insertRecord(record);
        } else {
            recordMapper.updateRecord(record);
        }

        return getLatestRecord(request.getUserId(), semesterLabel);
    }

    @Override
    public ScheduleAnalysisRecordVO getLatestRecord(Long userId, String semesterLabel) {
        if (userId == null) {
            return null;
        }

        StudentScheduleAnalysisRecord record;
        if (StringUtils.hasText(semesterLabel)) {
            record = recordMapper.selectByUserIdAndSemester(userId, semesterLabel);
        } else {
            record = recordMapper.selectLatestByUserId(userId);
        }

        if (record == null) {
            return null;
        }

        ScheduleAnalysisRecordVO vo = new ScheduleAnalysisRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setSemesterLabel(record.getSemesterLabel());
        vo.setSourceFileName(record.getSourceFileName());
        vo.setExtractedCourses(parseList(record.getExtractedJson()));
        vo.setMatchedCourses(parseList(record.getMatchedJson()));
        vo.setInsights(parseMap(record.getInsightsJson()));
        vo.setStatus(record.getStatus());
        vo.setUpdateTime(
                record.getUpdateTime() == null
                        ? ""
                        : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getUpdateTime())
        );
        return vo;
    }

    private String toJson(Object value) {
        try {
            if (value == null) {
                return null;
            }
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "JSON 序列化失败");
        }
    }

    private List<String> parseList(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseMap(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Collections.emptyMap();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}