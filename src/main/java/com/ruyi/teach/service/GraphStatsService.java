package com.ruyi.teach.service;

import java.util.List;
import java.util.Map;

public interface GraphStatsService {

    Map<String, Object> getOverview(Long teacherId, Long classId);

    Map<String, Object> getCompare(Long classA, Long classB);

    Map<String, Object> getStudentProfile(Long teacherId, Long classId);

    Map<String, Object> getBuildStats();

    Map<String, List<Map<String, Object>>> getActivityCandidates(Long teacherId, Long courseId);
}
