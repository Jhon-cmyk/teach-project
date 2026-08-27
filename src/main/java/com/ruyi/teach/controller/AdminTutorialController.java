package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.TextCourseMapper;
import com.ruyi.teach.mapper.TextNodeMapper;
import com.ruyi.teach.model.entity.TextCourse;
import com.ruyi.teach.model.entity.TextNode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/admin/tutorial")
@Tag(name = "管理端图文教程管理")
public class AdminTutorialController {

    @Resource
    private TextCourseMapper textCourseMapper;

    @Resource
    private TextNodeMapper textNodeMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Operation(summary = "管理端图文教程列表")
    @GetMapping("/list")
    public BaseResponse<Page<AdminTutorialCourseVO>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {

        getAdminLoginUser(request);

        QueryWrapper<TextCourse> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like("name", name.trim());
        }
        wrapper.orderByDesc("create_time").orderByDesc("id");

        Page<TextCourse> page = textCourseMapper.selectPage(new Page<>(current, size), wrapper);
        Page<AdminTutorialCourseVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());

        List<AdminTutorialCourseVO> records = new ArrayList<>();
        for (TextCourse course : page.getRecords()) {
            records.add(toCourseVO(course));
        }
        result.setRecords(records);
        return ResultUtils.success(result);
    }

    @Operation(summary = "管理端图文教程详情")
    @GetMapping("/detail")
    public BaseResponse<AdminTutorialDetailVO> detail(@RequestParam Long id, HttpServletRequest request) {
        getAdminLoginUser(request);
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教程 ID 不能为空");
        }

        TextCourse course = textCourseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图文教程不存在");
        }

        AdminTutorialDetailVO detail = new AdminTutorialDetailVO();
        detail.setCourse(toCourseVO(course));
        detail.setNodes(listNodes(id, true));
        return ResultUtils.success(detail);
    }

    @Operation(summary = "管理端保存图文教程")
    @PostMapping("/save")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Long> save(@RequestBody AdminTutorialSaveRequest body, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        if (body == null || StringUtils.isBlank(body.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教程名称不能为空");
        }

        boolean isCreate = body.getId() == null;
        TextCourse course = isCreate ? new TextCourse() : textCourseMapper.selectById(body.getId());
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图文教程不存在");
        }

        course.setName(body.getName().trim());
        course.setDescription(StringUtils.defaultString(body.getDescription()).trim());
        course.setCoverImg(StringUtils.defaultString(body.getCoverImg()).trim());
        if (isCreate) {
            course.setCreateTime(new Date());
            textCourseMapper.insert(course);
        } else {
            textCourseMapper.updateById(course);
        }

        replaceNodes(course.getId(), body.getNodes());

        adminAuditLogger.log(admin, "图文教程", isCreate ? "新增图文教程" : "编辑图文教程",
                "text_course", course.getId(), course.getName(), request);
        return ResultUtils.success(course.getId());
    }

    @Operation(summary = "管理端删除图文教程")
    @PostMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> delete(@RequestBody IdRequest body, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        if (body == null || body.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "教程 ID 不能为空");
        }

        TextCourse course = textCourseMapper.selectById(body.getId());
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图文教程不存在");
        }

        textNodeMapper.delete(new QueryWrapper<TextNode>().eq("course_id", body.getId()));
        int deleted = textCourseMapper.deleteById(body.getId());
        if (deleted > 0) {
            adminAuditLogger.log(admin, "图文教程", "删除图文教程",
                    "text_course", body.getId(), course.getName(), request);
        }
        return ResultUtils.success(deleted > 0);
    }

    private void replaceNodes(Long courseId, List<AdminTutorialNodeRequest> nodes) {
        textNodeMapper.delete(new QueryWrapper<TextNode>().eq("course_id", courseId));
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        List<AdminTutorialNodeRequest> normalized = nodes.stream()
                .filter(item -> item != null
                        && (StringUtils.isNotBlank(item.getTitle()) || StringUtils.isNotBlank(item.getContent())))
                .sorted(Comparator.comparingInt(item -> item.getSortOrder() == null ? Integer.MAX_VALUE : item.getSortOrder()))
                .toList();

        int index = 1;
        for (AdminTutorialNodeRequest item : normalized) {
            if (StringUtils.isBlank(item.getTitle())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "章节标题不能为空");
            }

            TextNode node = new TextNode();
            node.setCourseId(courseId);
            node.setTitle(item.getTitle().trim());
            node.setContent(StringUtils.defaultString(item.getContent()));
            node.setSortOrder(index++);
            node.setCreateTime(new Date());
            textNodeMapper.insert(node);
        }
    }

    private AdminTutorialCourseVO toCourseVO(TextCourse course) {
        AdminTutorialCourseVO vo = new AdminTutorialCourseVO();
        vo.setId(course.getId());
        vo.setName(course.getName());
        vo.setDescription(course.getDescription());
        vo.setCoverImg(course.getCoverImg());
        vo.setCreateTime(course.getCreateTime());
        vo.setNodeCount(textNodeMapper.selectCount(new QueryWrapper<TextNode>().eq("course_id", course.getId())));
        return vo;
    }

    private List<AdminTutorialNodeVO> listNodes(Long courseId, boolean includeContent) {
        QueryWrapper<TextNode> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId).orderByAsc("sort_order").orderByAsc("id");
        if (!includeContent) {
            wrapper.select("id", "course_id", "title", "sort_order", "create_time");
        }

        List<TextNode> nodes = textNodeMapper.selectList(wrapper);
        List<AdminTutorialNodeVO> result = new ArrayList<>();
        for (TextNode node : nodes) {
            AdminTutorialNodeVO vo = new AdminTutorialNodeVO();
            vo.setId(node.getId());
            vo.setCourseId(node.getCourseId());
            vo.setTitle(node.getTitle());
            vo.setContent(includeContent ? node.getContent() : null);
            vo.setSortOrder(node.getSortOrder());
            vo.setCreateTime(node.getCreateTime());
            result.add(vo);
        }
        return result;
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
    public static class AdminTutorialSaveRequest {
        private Long id;
        private String name;
        private String description;
        private String coverImg;
        private List<AdminTutorialNodeRequest> nodes;
    }

    @Data
    public static class AdminTutorialNodeRequest {
        private String title;
        private String content;
        private Integer sortOrder;
    }

    @Data
    public static class IdRequest {
        private Long id;
    }

    @Data
    public static class AdminTutorialDetailVO {
        private AdminTutorialCourseVO course;
        private List<AdminTutorialNodeVO> nodes;
    }

    @Data
    public static class AdminTutorialCourseVO {
        private Long id;
        private String name;
        private String description;
        private String coverImg;
        private Date createTime;
        private Long nodeCount;
    }

    @Data
    public static class AdminTutorialNodeVO {
        private Long id;
        private Long courseId;
        private String title;
        private String content;
        private Integer sortOrder;
        private Date createTime;
    }
}
