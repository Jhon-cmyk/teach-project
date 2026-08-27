package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.MajorCurriculumCourseMapper;
import com.ruyi.teach.model.entity.MajorCurriculumCourse;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/admin/major-curriculum")
public class AdminMajorCurriculumController {


    @Resource
    private MajorCurriculumCourseMapper majorCurriculumCourseMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @GetMapping("/list")
    public BaseResponse<List<MajorCurriculumCourse>> list(@RequestParam String major,
                                                          @RequestParam(required = false) Integer semesterNo,
                                                          HttpServletRequest request) {
        getAdminLoginUser(request);
        return ResultUtils.success(listCurriculum(major, semesterNo));
    }

    @PostMapping("/save")
    public BaseResponse<Boolean> save(@RequestBody CurriculumSaveRequest body, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        validate(body);

        MajorCurriculumCourse entity = new MajorCurriculumCourse();
        entity.setMajor(body.getMajor().trim());
        entity.setSemesterNo(body.getSemesterNo());
        entity.setCourseName(body.getCourseName().trim());
        entity.setCourseType(StringUtils.defaultIfBlank(body.getCourseType(), "required"));
        entity.setCredits(body.getCredits());
        entity.setHours(body.getHours());
        entity.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        entity.setUpdateTime(new Date());

        boolean result;
        String action;
        if (body.getId() == null) {
            entity.setCreateTime(new Date());
            entity.setIsDelete(0);
            result = majorCurriculumCourseMapper.insert(entity) > 0;
            action = "新增专业课程";
        } else {
            MajorCurriculumCourse old = majorCurriculumCourseMapper.selectById(body.getId());
            if (old == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "专业课程不存在");
            }
            entity.setId(body.getId());
            result = majorCurriculumCourseMapper.updateById(entity) > 0;
            action = "修改专业课程";
        }

        adminAuditLogger.log(admin, "班级专业管理", action, "major_curriculum_course", entity.getId(),
                entity.getMajor() + " / 第" + entity.getSemesterNo() + "学期 / " + entity.getCourseName(), request);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody IdRequest body, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        if (body == null || body.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程 ID 不能为空");
        }
        MajorCurriculumCourse old = majorCurriculumCourseMapper.selectById(body.getId());
        if (old == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "专业课程不存在");
        }
        boolean result = majorCurriculumCourseMapper.deleteById(body.getId()) > 0;
        adminAuditLogger.log(admin, "班级专业管理", "删除专业课程", "major_curriculum_course", body.getId(),
                old.getMajor() + " / " + old.getCourseName(), request);
        return ResultUtils.success(result);
    }

    private List<MajorCurriculumCourse> listCurriculum(String major, Integer semesterNo) {
        if (StringUtils.isBlank(major)) {
            return List.of();
        }
        QueryWrapper<MajorCurriculumCourse> wrapper = new QueryWrapper<>();
        wrapper.eq("major", major.trim())
                .eq("is_delete", 0);
        if (semesterNo != null) {
            wrapper.eq("semester_no", semesterNo);
        }
        wrapper.orderByAsc("semester_no").orderByAsc("sort_order").orderByAsc("id");
        return majorCurriculumCourseMapper.selectList(wrapper);
    }

    private void validate(CurriculumSaveRequest body) {
        if (body == null || StringUtils.isBlank(body.getMajor())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "专业不能为空");
        }
        if (body.getSemesterNo() == null || body.getSemesterNo() < 1 || body.getSemesterNo() > 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学期必须是 1 到 8");
        }
        if (StringUtils.isBlank(body.getCourseName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程名称不能为空");
        }
    }

    private User getAdminLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可访问");
        }
        return loginUser;
    }

    @Data
    public static class CurriculumSaveRequest {
        private Long id;
        private String major;
        private Integer semesterNo;
        private String courseName;
        private String courseType;
        private BigDecimal credits;
        private Integer hours;
        private Integer sortOrder;
    }

    @Data
    public static class IdRequest {
        private Long id;
    }
}
