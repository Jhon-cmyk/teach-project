package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/ai-resource")
@Tag(name = "管理端AI资源管理")
public class AdminAiResourceController {


    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private UserService userService;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Operation(summary = "管理端 AI 资源列表")
    @GetMapping("/list")
    public BaseResponse<Page<AdminAiResourceVO>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String teacherKeyword,
            @RequestParam(required = false) Integer isPublished,
            HttpServletRequest request) {

        getAdminLoginUser(request);

        QueryWrapper<AiResource> queryWrapper = new QueryWrapper<>();

        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like("title", title.trim());
        }

        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq("type", type.trim());
        }

        if (isPublished != null) {
            queryWrapper.eq("is_published", isPublished);
        }

        if (StringUtils.isNotBlank(teacherKeyword)) {
            QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
            userQueryWrapper.select("id", "userAccount", "userName");
            userQueryWrapper.and(wrapper -> wrapper
                    .like("userAccount", teacherKeyword.trim())
                    .or()
                    .like("userName", teacherKeyword.trim()));

            List<User> matchedUsers = userService.list(userQueryWrapper);
            if (matchedUsers == null || matchedUsers.isEmpty()) {
                Page<AdminAiResourceVO> emptyPage = new Page<>(current, size, 0);
                emptyPage.setRecords(Collections.emptyList());
                return ResultUtils.success(emptyPage);
            }

            List<Long> teacherIds = matchedUsers.stream().map(User::getId).collect(Collectors.toList());
            queryWrapper.in("teacher_id", teacherIds);
        }

        queryWrapper.orderByDesc("create_time");

        Page<AiResource> page = new Page<>(current, size);
        Page<AiResource> resultPage = aiResourceMapper.selectPage(page, queryWrapper);

        List<AiResource> records = resultPage.getRecords();
        Set<Long> teacherIds = records.stream()
                .map(AiResource::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<Long, String> teacherNameMap;
        if (!teacherIds.isEmpty()) {
            List<User> userList = userService.listByIds(teacherIds);
            teacherNameMap = userList.stream().collect(Collectors.toMap(
                    User::getId,
                    user -> StringUtils.defaultIfBlank(user.getUserName(), user.getUserAccount())
            ));
        } else {
            teacherNameMap = new HashMap<>();
        }

        List<AdminAiResourceVO> voList = records.stream()
                .map(resource -> toVO(resource, teacherNameMap))
                .collect(Collectors.toList());

        Page<AdminAiResourceVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    @Operation(summary = "管理端 AI 资源详情")
    @GetMapping("/detail")
    public BaseResponse<AdminAiResourceVO> detail(@RequestParam Long id, HttpServletRequest request) {
        getAdminLoginUser(request);

        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资源 ID 不能为空");
        }

        AiResource resource = aiResourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资源不存在");
        }

        String teacherName = "未命名教师";
        if (resource.getTeacherId() != null) {
            User teacher = userService.getById(resource.getTeacherId());
            if (teacher != null) {
                teacherName = StringUtils.defaultIfBlank(teacher.getUserName(), teacher.getUserAccount());
            }
        }

        Map<Long, String> teacherNameMap = new HashMap<>();
        if (resource.getTeacherId() != null) {
            teacherNameMap.put(resource.getTeacherId(), teacherName);
        }

        return ResultUtils.success(toVO(resource, teacherNameMap));
    }

    @Operation(summary = "管理端发布 AI 资源")
    @PostMapping("/publish")
    public BaseResponse<Boolean> publish(@RequestBody IdRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "管理员只能查看和下架 AI 资源，不能发布");
    }

    @Operation(summary = "管理端取消发布 AI 资源")
    @PostMapping("/unpublish")
    public BaseResponse<Boolean> unpublish(@RequestBody IdRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);

        if (requestBody == null || requestBody.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资源 ID 不能为空");
        }

        AiResource existing = aiResourceMapper.selectById(requestBody.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资源不存在");
        }

        AiResource update = new AiResource();
        update.setId(requestBody.getId());
        update.setIsPublished(0);

        boolean result = aiResourceMapper.updateById(update) > 0;
        adminAuditLogger.log(adminUser, "AI资源管理", "下架AI资源", "ai_resource", requestBody.getId(),
                "is_published=0", request);
        return ResultUtils.success(result);
    }

    @Operation(summary = "管理端删除 AI 资源")
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody IdRequest requestBody, HttpServletRequest request) {
        User adminUser = getAdminLoginUser(request);
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "管理员只能查看和下架 AI 资源，不能删除");
    }

    private AdminAiResourceVO toVO(AiResource resource, Map<Long, String> teacherNameMap) {
        AdminAiResourceVO vo = new AdminAiResourceVO();
        vo.setId(resource.getId());
        vo.setTeacherId(resource.getTeacherId());
        vo.setTeacherName(teacherNameMap.getOrDefault(resource.getTeacherId(), "未命名教师"));
        vo.setType(resource.getType());
        vo.setTitle(resource.getTitle());
        vo.setContent(resource.getContent());
        vo.setParamsJson(resource.getParamsJson());
        vo.setIsPublished(resource.getIsPublished());
        vo.setCreateTime(resource.getCreateTime());
        vo.setUpdateTime(resource.getUpdateTime());
        return vo;
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
    public static class IdRequest {
        private Long id;
    }

    @Data
    public static class AdminAiResourceVO {
        private Long id;
        private Long teacherId;
        private String teacherName;
        private String type;
        private String title;
        private String content;
        private String paramsJson;
        private Integer isPublished;
        private Date createTime;
        private Date updateTime;
    }
}
