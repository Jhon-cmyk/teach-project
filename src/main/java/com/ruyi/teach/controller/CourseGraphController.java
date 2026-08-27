package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphLinkCreateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphLinkDeleteRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphNodeCreateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphNodeDeleteRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphNodeUpdateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphPreferenceUpdateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphResourceBindRequest;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphAnalysisFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphClassFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphCommunityDeskFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphCommunityFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphDataVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphLinkVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphNodeVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphPreferenceVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphMaterialVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphQuizVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphResourceFocusVO;
import com.ruyi.teach.service.CourseGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher/course-graph")
@Tag(name = "教师端课程图谱")
public class CourseGraphController {

    @Resource
    private CourseGraphService courseGraphService;

    @Operation(summary = "获取课程图谱主数据")
    @GetMapping("/data")
    public BaseResponse<CourseGraphDataVO> getGraphData(HttpServletRequest request) {
        User loginUser = getLoginTeacher(request);
        return ResultUtils.success(courseGraphService.getGraphData(loginUser.getId()));
    }

    @Operation(summary = "获取单个图谱节点教学信息")
    @GetMapping("/node/{id}")
    public BaseResponse<CourseGraphNodeVO> getNodeDetail(@PathVariable String id, HttpServletRequest request) {
        User loginUser = getLoginTeacher(request);

        if (StringUtils.isBlank(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }

        CourseGraphNodeVO nodeVO = courseGraphService.getNodeDetail(loginUser.getId(), id);
        if (nodeVO == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到对应的图谱节点");
        }

        return ResultUtils.success(nodeVO);
    }

    @Operation(summary = "更新单个图谱节点教学信息")
    @PostMapping("/node/update")
    public BaseResponse<CourseGraphNodeVO> updateNode(
            @RequestBody CourseGraphNodeUpdateRequest updateRequest,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (updateRequest == null || StringUtils.isBlank(updateRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点更新参数不合法");
        }

        CourseGraphNodeVO saved = courseGraphService.updateNode(loginUser.getId(), updateRequest);
        return ResultUtils.success(saved);
    }

    @Operation(summary = "获取当前教师的图谱偏好")
    @GetMapping("/preferences")
    public BaseResponse<CourseGraphPreferenceVO> getPreferences(HttpServletRequest request) {
        User loginUser = getLoginTeacher(request);
        return ResultUtils.success(courseGraphService.getPreferences(loginUser.getId()));
    }

    @Operation(summary = "更新当前教师的图谱偏好")
    @PostMapping("/preferences/update")
    public BaseResponse<CourseGraphPreferenceVO> updatePreferences(
            @RequestBody CourseGraphPreferenceUpdateRequest updateRequest,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "偏好更新参数不能为空");
        }

        CourseGraphPreferenceVO preferenceVO =
                courseGraphService.updatePreferences(loginUser.getId(), updateRequest);
        return ResultUtils.success(preferenceVO);
    }

    @Operation(summary = "获取当前节点资源聚焦")
    @GetMapping("/resource-focus")
    public BaseResponse<CourseGraphResourceFocusVO> getResourceFocus(
            @RequestParam("nodeId") String nodeId,
            @RequestParam(value = "resourceType", required = false) String resourceType,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }

        return ResultUtils.success(
                courseGraphService.getResourceFocus(loginUser.getId(), nodeId, resourceType)
        );
    }

    @Operation(summary = "绑定资源到图谱节点")
    @PostMapping("/resource-link/bind")
    public BaseResponse<Boolean> bindResource(
            @RequestBody CourseGraphResourceBindRequest bindRequest,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (bindRequest == null
                || StringUtils.isBlank(bindRequest.getNodeId())
                || bindRequest.getResourceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "资源绑定参数不合法");
        }

        return ResultUtils.success(
                courseGraphService.bindResource(loginUser.getId(), bindRequest)
        );
    }

    @Operation(summary = "获取当前节点分析聚焦")
    @GetMapping("/analysis-focus")
    public BaseResponse<CourseGraphAnalysisFocusVO> getAnalysisFocus(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }

        return ResultUtils.success(
                courseGraphService.getAnalysisFocus(loginUser.getId(), nodeId)
        );
    }

    @Operation(summary = "获取当前节点课堂分析聚焦")
    @GetMapping("/class-focus")
    public BaseResponse<CourseGraphClassFocusVO> getClassFocus(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }

        return ResultUtils.success(
                courseGraphService.getClassFocus(loginUser.getId(), nodeId)
        );
    }

    @Operation(summary = "获取当前节点社区聚焦")
    @GetMapping("/community-focus")
    public BaseResponse<CourseGraphCommunityFocusVO> getCommunityFocus(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }

        return ResultUtils.success(
                courseGraphService.getCommunityFocus(loginUser.getId(), nodeId)
        );
    }

    @Operation(summary = "获取当前节点教师处理台聚焦")
    @GetMapping("/community-desk-focus")
    public BaseResponse<CourseGraphCommunityDeskFocusVO> getCommunityDeskFocus(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }

        return ResultUtils.success(
                courseGraphService.getCommunityDeskFocus(loginUser.getId(), nodeId)
        );
    }

    @Operation(summary = "创建图谱节点")
    @PostMapping("/node/create")
    public BaseResponse<CourseGraphNodeVO> createNode(
            @RequestBody CourseGraphNodeCreateRequest createRequest,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (createRequest == null || StringUtils.isBlank(createRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点名称不能为空");
        }

        return ResultUtils.success(courseGraphService.createNode(loginUser.getId(), createRequest));
    }

    @Operation(summary = "删除图谱节点（级联删除子孙节点和关联连线）")
    @PostMapping("/node/delete")
    public BaseResponse<Boolean> deleteNode(
            @RequestBody CourseGraphNodeDeleteRequest deleteRequest,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (deleteRequest == null || StringUtils.isBlank(deleteRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }

        return ResultUtils.success(courseGraphService.deleteNodeCascade(loginUser.getId(), deleteRequest.getId()));
    }

    @Operation(summary = "创建图谱连线")
    @PostMapping("/link/create")
    public BaseResponse<CourseGraphLinkVO> createLink(
            @RequestBody CourseGraphLinkCreateRequest createRequest,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (createRequest == null
                || StringUtils.isBlank(createRequest.getSource())
                || StringUtils.isBlank(createRequest.getTarget())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "连线源节点与目标节点不能为空");
        }

        return ResultUtils.success(courseGraphService.createLink(loginUser.getId(), createRequest));
    }

    @Operation(summary = "删除图谱连线")
    @PostMapping("/link/delete")
    public BaseResponse<Boolean> deleteLink(
            @RequestBody CourseGraphLinkDeleteRequest deleteRequest,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);

        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "连线 id 不能为空");
        }

        return ResultUtils.success(courseGraphService.deleteLink(loginUser.getId(), deleteRequest.getId()));
    }

    @Operation(summary = "导入默认图谱数据")
    @PostMapping("/seed-default")
    public BaseResponse<CourseGraphDataVO> seedDefaultGraph(HttpServletRequest request) {
        User loginUser = getLoginTeacher(request);
        return ResultUtils.success(courseGraphService.seedDefaultGraph(loginUser.getId()));
    }

    @Operation(summary = "绑定学习活动到知识点")
    @PostMapping("/node/bind-activity")
    public BaseResponse<Boolean> bindNodeActivity(
            @RequestParam("nodeId") String nodeId,
            @RequestParam("activityType") String activityType,
            @RequestParam("activityId") Long activityId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);
        if (StringUtils.isBlank(nodeId) || StringUtils.isBlank(activityType) || activityId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不合法");
        }
        return ResultUtils.success(courseGraphService.bindNodeActivity(loginUser.getId(), nodeId, activityType, activityId));
    }

    @Operation(summary = "解绑学习活动")
    @PostMapping("/node/unbind-activity")
    public BaseResponse<Boolean> unbindNodeActivity(
            @RequestParam("activityId") Long activityId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);
        if (activityId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "绑定记录ID不能为空");
        }
        return ResultUtils.success(courseGraphService.unbindNodeActivity(loginUser.getId(), activityId));
    }

    @Operation(summary = "查询知识点绑定的学习活动")
    @GetMapping("/node/activities")
    public BaseResponse<java.util.List> listNodeActivities(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request
    ) {
        getLoginTeacher(request);
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }
        return ResultUtils.success(courseGraphService.listNodeActivities(nodeId));
    }

    @Operation(summary = "查询知识点关联的题库（编程题+随堂测验）")
    @GetMapping("/node/quizzes")
    public BaseResponse<java.util.List<CourseGraphQuizVO>> listNodeQuizzes(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }
        return ResultUtils.success(courseGraphService.listNodeQuizzes(loginUser.getId(), nodeId));
    }

    @Operation(summary = "查询知识点关联的资料（教案+交互课件）")
    @GetMapping("/node/materials")
    public BaseResponse<java.util.List<CourseGraphMaterialVO>> listNodeMaterials(
            @RequestParam("nodeId") String nodeId,
            HttpServletRequest request
    ) {
        User loginUser = getLoginTeacher(request);
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }
        return ResultUtils.success(courseGraphService.listNodeMaterials(loginUser.getId(), nodeId));
    }

    @Operation(summary = "计算知识点学生进度")
    @GetMapping("/node/progress")
    public BaseResponse<java.util.Map> computeNodeProgress(
            @RequestParam("nodeId") String nodeId,
            @RequestParam("studentIds") java.util.List<Long> studentIds,
            HttpServletRequest request
    ) {
        getLoginTeacher(request);
        if (StringUtils.isBlank(nodeId) || studentIds == null || studentIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不合法");
        }
        return ResultUtils.success(courseGraphService.computeNodeProgress(nodeId, studentIds));
    }

    private User getLoginTeacher(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"teacher".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师或管理员可访问课程图谱接口");
        }
        return loginUser;
    }
}
