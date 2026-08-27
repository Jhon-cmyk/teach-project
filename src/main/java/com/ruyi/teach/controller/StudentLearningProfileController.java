package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.StudentLearningProfileVO;
import com.ruyi.teach.service.StudentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher")
@Tag(name = "教师端学生综合画像")
public class StudentLearningProfileController {

    @Resource
    private StudentProfileService studentProfileService;

    @Operation(summary = "教师查看单个学生综合学习画像")
    @GetMapping("/student-learning-profile")
    public BaseResponse<StudentLearningProfileVO> getProfile(@RequestParam("classId") Long classId,
                                                             @RequestParam("studentId") Long studentId,
                                                             @RequestParam(value = "days", required = false) Integer days,
                                                             HttpServletRequest request) {
        return ResultUtils.success(studentProfileService.getStudentLearningProfile(
                classId,
                studentId,
                days,
                getLoginUser(request)
        ));
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return loginUser;
    }
}
