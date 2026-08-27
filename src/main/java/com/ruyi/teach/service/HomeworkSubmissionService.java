package com.ruyi.teach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruyi.teach.model.dto.ExamGradeRequest;
import com.ruyi.teach.model.dto.HomeworkSubmissionReviewRequest;
import com.ruyi.teach.model.dto.HomeworkSubmitRequest;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.HomeworkHistoryVO;
import com.ruyi.teach.model.vo.HomeworkReportVO;

import java.util.List;

public interface HomeworkSubmissionService extends IService<HomeworkSubmission> {

    /**
     * 学生提交作业（含全部校验、判题、写detail）
     *
     * @param req       提交请求
     * @param loginUser 当前登录学生
     * @return submissionId
     */
    Long submitHomework(HomeworkSubmitRequest req, User loginUser);

    /**
     * 学生历史作答列表
     */
    List<HomeworkHistoryVO> getStudentHistory(User loginUser);

    /**
     * 学生删除自己的单条作业提交记录（逻辑删除，不适用于考试记录）
     */
    void deleteStudentHomeworkHistory(Long submissionId, User loginUser);

    /**
     * 单次报告详情
     */
    HomeworkReportVO getStudentReport(Long submissionId, User loginUser);

    Long submitHomeworkAsync(HomeworkSubmitRequest req, User loginUser);

    String streamGradeSubmission(Long submissionId, User loginUser, java.util.function.Consumer<String> onChunk);


    HomeworkReportVO getTeacherSubmissionReport(Long submissionId, User loginUser);

    void teacherReviewHomeworkSubmission(HomeworkSubmissionReviewRequest req, User loginUser);

    String generateHomeworkReviewComment(HomeworkSubmissionReviewRequest req, User loginUser);

    void teacherRegradeHomeworkSubmission(Long submissionId, User loginUser);

    /**
     * 学生提交考试（无AI批改，状态直接设为 submitted）
     */
    Long submitExam(HomeworkSubmitRequest req, User loginUser);

    /**
     * 学生考试历史
     */
    List<HomeworkHistoryVO> getExamHistory(User loginUser);

    /**
     * 教师批阅考试提交
     */
    void teacherGradeExam(ExamGradeRequest req, User loginUser);

    /**
     * 教师生成考试批阅评语
     */
    String generateExamReviewComment(ExamGradeRequest req, User loginUser);

    /**
     * 教师AI自动批阅单个学生答卷
     */
    void teacherAutoGradeExam(Long submissionId, User loginUser);
}
