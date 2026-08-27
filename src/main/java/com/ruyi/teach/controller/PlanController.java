package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.mapper.StudyPlanMapper;
import com.ruyi.teach.mapper.UserMapper;
import com.ruyi.teach.mapper.PointsRecordMapper; // 新增引入流水 Mapper
import com.ruyi.teach.model.entity.StudyPlan;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.entity.PointsRecord; // 新增引入流水实体
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/plan")
@Tag(name = "学习计划 & 积分")
public class PlanController {

    @Resource
    private StudyPlanMapper studyPlanMapper;

    @Resource
    private UserMapper userMapper;

    // 🌟 注入积分流水 Mapper
    @Resource
    private PointsRecordMapper pointsRecordMapper;

    @Operation(summary = "添加今日计划")
    @PostMapping("/add")
    public BaseResponse<Long> addPlan(@RequestBody StudyPlan plan, HttpServletRequest request) {
        User loginUser = SessionUserContext.require(request);

        plan.setUserId(loginUser.getId());
        plan.setIsCompleted(0); // 默认未完成
        studyPlanMapper.insert(plan);
        return ResultUtils.success(plan.getId());
    }

    @Operation(summary = "完成计划(+10积分)")
    @PostMapping("/complete/{id}")
    @Transactional(rollbackFor = Exception.class) // 🌟 必须显式指定 rollbackFor，确保发生任何异常都回滚
    public BaseResponse<Integer> completePlan(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = SessionUserContext.require(request);

        // 1. 检查计划是否存在
        StudyPlan plan = studyPlanMapper.selectById(id);
        if (plan == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "计划不存在");

        // 2. 严谨校验：如果已经完成了，直接返回当前积分，防止通过重复请求接口无限刷分
        if (plan.getIsCompleted() == 1) {
            return ResultUtils.success(loginUser.getPoints());
        }

        // 3. 更新计划状态为已完成
        plan.setIsCompleted(1);
        studyPlanMapper.updateById(plan);

        // 4. 更新用户总积分表
        User user = userMapper.selectById(loginUser.getId());
        user.setPoints(user.getPoints() + 10);
        userMapper.updateById(user);

        // 🌟 5. 【核心补充】写入积分流水表，作为排行榜的数据源
        PointsRecord record = new PointsRecord();
        record.setUserId(user.getId());
        record.setType("plan"); // 类型标识为“完成计划”
        record.setPoints(10);
        // 将具体的计划标题写入描述，方便日后排查和用户查看明细
        record.setDescription("完成学习计划：" + plan.getTitle());
        pointsRecordMapper.insert(record);

        // 6. 更新 Session 里的用户信息，防止前端页面积分不同步
        SessionUserContext.replace(request, user);

        return ResultUtils.success(user.getPoints());
    }

    @Operation(summary = "获取我的今日计划")
    @GetMapping("/my/today")
    public BaseResponse<List<StudyPlan>> getMyTodayPlan(HttpServletRequest request) {
        User loginUser = SessionUserContext.require(request);

        QueryWrapper<StudyPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("limit 10");

        return ResultUtils.success(studyPlanMapper.selectList(queryWrapper));
    }

    @Operation(summary = "每日签到(+5积分)")
    @PostMapping("/checkin")
    @Transactional(rollbackFor = Exception.class) // 🌟 同样必须开启强事务
    public BaseResponse<Integer> checkIn(HttpServletRequest request) {
        User loginUser = SessionUserContext.require(request);

        // 🌟 1. 【核心补充】防刷校验：查询流水表，检查今天是否已经有签到记录
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startTime = calendar.getTime(); // 今天的 00:00:00

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endTime = calendar.getTime(); // 今天的 23:59:59

        QueryWrapper<PointsRecord> checkQuery = new QueryWrapper<>();
        checkQuery.eq("user_id", loginUser.getId())
                .eq("type", "checkin") // 限定查询类型为签到
                .between("create_time", startTime, endTime); // 限定时间在今天之内

        Long count = pointsRecordMapper.selectCount(checkQuery);
        if (count > 0) {
            // 如果查到大于0条，说明今天已经签到过，直接抛出业务异常拦截
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到，请勿重复操作");
        }

        // 2. 更新用户总积分表
        User user = userMapper.selectById(loginUser.getId());
        user.setPoints(user.getPoints() + 5);
        userMapper.updateById(user);

        // 🌟 3. 【核心补充】写入积分流水表
        PointsRecord record = new PointsRecord();
        record.setUserId(user.getId());
        record.setType("checkin");
        record.setPoints(5);
        record.setDescription("每日签到奖励");
        pointsRecordMapper.insert(record);

        // 4. 更新 Session
        SessionUserContext.replace(request, user);

        return ResultUtils.success(user.getPoints());
    }

    @Operation(summary = "查询今日是否已签到")
    @GetMapping("/checkin/status")
    public BaseResponse<Boolean> checkInStatus(HttpServletRequest request) {
        User loginUser = SessionUserContext.require(request);

        // 获取今天的时间边界
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startTime = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endTime = calendar.getTime();

        // 去流水表里查今天有没有签到记录
        QueryWrapper<PointsRecord> checkQuery = new QueryWrapper<>();
        checkQuery.eq("user_id", loginUser.getId())
                .eq("type", "checkin")
                .between("create_time", startTime, endTime);

        Long count = pointsRecordMapper.selectCount(checkQuery);
        // 如果大于0，说明今天已经签到过了，返回 true
        return ResultUtils.success(count > 0);
    }
}
