package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.mapper.CourseFavourMapper;
import com.ruyi.teach.mapper.CourseMapper;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseFavour;
import com.ruyi.teach.model.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/favour")
@Tag(name = "收藏模块")
public class CourseFavourController {

    @Resource
    private CourseFavourMapper courseFavourMapper;

    @Resource
    private CourseMapper courseMapper;

    @Operation(summary = "收藏 / 取消收藏 (自动切换)")
    @PostMapping("/")
    public BaseResponse<Integer> doFavour(@RequestBody CourseFavour favourReq, HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);

        long courseId = favourReq.getCourseId();
        long userId = loginUser.getId();

        // 1. 检查是否已经收藏过
        QueryWrapper<CourseFavour> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("course_id", courseId);
        CourseFavour oldFavour = courseFavourMapper.selectOne(queryWrapper);

        if (oldFavour != null) {
            // 2. 如果已收藏，则取消收藏 (删除)
            courseFavourMapper.deleteById(oldFavour.getId());
            return ResultUtils.success(-1); // 返回 -1 表示取消成功
        } else {
            // 3. 如果未收藏，则添加收藏 (新增)
            CourseFavour newFavour = new CourseFavour();
            newFavour.setUserId(userId);
            newFavour.setCourseId(courseId);
            courseFavourMapper.insert(newFavour);
            return ResultUtils.success(1); // 返回 1 表示收藏成功
        }
    }

    @Operation(summary = "查询我是否收藏了某课")
    @GetMapping("/check/{courseId}")
    public BaseResponse<Boolean> checkFavour(@PathVariable Long courseId, HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) return ResultUtils.success(false); // 没登录肯定没收藏

        QueryWrapper<CourseFavour> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.eq("course_id", courseId);
        return ResultUtils.success(courseFavourMapper.selectCount(queryWrapper) > 0);
    }

    @Operation(summary = "获取我的收藏列表")
    @GetMapping("/my")
    public BaseResponse<List<Course>> listMyFavour(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);

        // 1. 先查出这个用户所有的收藏记录 (里面只有 courseId)
        QueryWrapper<CourseFavour> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.orderByDesc("create_time");
        List<CourseFavour> favourList = courseFavourMapper.selectList(queryWrapper);

        if (favourList.isEmpty()) {
            return ResultUtils.success(new ArrayList<>());
        }

        // 2. 提取出所有的 courseId
        List<Long> courseIds = favourList.stream()
                .map(CourseFavour::getCourseId)
                .collect(Collectors.toList());

        // 3. 再去查 Course 表，获取课程详细信息
        List<Course> courses = courseMapper.selectBatchIds(courseIds);
        return ResultUtils.success(courses);
    }

    @Operation(summary = "取消收藏指定课程")
    @DeleteMapping("/{courseId}")
    public BaseResponse<Boolean> removeMyFavour(@PathVariable Long courseId,
                                                HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程 ID 不合法");
        }

        QueryWrapper<CourseFavour> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", loginUser.getId())
                .eq("course_id", courseId);
        courseFavourMapper.delete(wrapper);
        return ResultUtils.success(true);
    }
}
