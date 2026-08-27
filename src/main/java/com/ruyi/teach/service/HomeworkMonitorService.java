package com.ruyi.teach.service;

import com.ruyi.teach.model.dto.HomeworkTeacherMonitorReportRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.HomeworkTeacherMonitorItemVO;
import com.ruyi.teach.model.vo.HomeworkTeacherMonitorReportVO;
import com.ruyi.teach.model.vo.HomeworkTeacherMonitorStudentVO;

import java.util.List;

public interface HomeworkMonitorService {

    /**
     * 教师作业学情列表
     */
    List<HomeworkTeacherMonitorItemVO> listTeacherMonitor(User loginUser);

    /**
     * 单个作业学生详情
     */
    List<HomeworkTeacherMonitorStudentVO> getTeacherMonitorDetail(Long assignmentId, User loginUser);

    /**
     * 生成并保存诊断报告
     */
    HomeworkTeacherMonitorReportVO generateTeacherMonitorReport(HomeworkTeacherMonitorReportRequest request,
                                                                User loginUser);

    /**
     * 报告历史
     */
    List<HomeworkTeacherMonitorReportVO> listTeacherMonitorReportHistory(User loginUser);

    /**
     * 报告详情
     */
    HomeworkTeacherMonitorReportVO getTeacherMonitorReportDetail(Long reportId, User loginUser);


    /**
     * 删除诊断报告（逻辑删除）
     */
    Boolean deleteTeacherMonitorReport(Long reportId, User loginUser);

    /**
     * 教师删除自己发布的作业（逻辑删除）
     * 软删除作业本身，不级联删除学生提交记录
     */
    Boolean deleteTeacherAssignment(Long assignmentId, User loginUser);

}