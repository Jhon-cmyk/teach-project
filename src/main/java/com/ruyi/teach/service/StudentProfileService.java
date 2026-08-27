package com.ruyi.teach.service;

import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.StudentLearningProfileVO;

public interface StudentProfileService {

    StudentLearningProfileVO getStudentLearningProfile(Long classId, Long studentId, Integer days, User viewer);

    StudentLearningProfileVO getSelfLearningProfile(Integer days, Long courseId, Long chapterId, User student);
}
