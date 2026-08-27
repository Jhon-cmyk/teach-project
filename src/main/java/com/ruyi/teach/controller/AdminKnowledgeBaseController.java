package com.ruyi.teach.controller;

import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.knowledge.KnowledgeFileVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeRepositoryVO;
import com.ruyi.teach.model.vo.knowledge.KnowledgeUploadVO;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/knowledge-base")
@Tag(name = "管理端星火知识库")
public class AdminKnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final AdminAuditLogger adminAuditLogger;

    public AdminKnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                        AdminAuditLogger adminAuditLogger) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.adminAuditLogger = adminAuditLogger;
    }

    @GetMapping("/status")
    @Operation(summary = "查询星火知识库配置状态")
    public BaseResponse<KnowledgeBaseService.ConfigurationStatus> status(HttpServletRequest request) {
        requireAdmin(request);
        return ResultUtils.success(knowledgeBaseService.configurationStatus());
    }

    @GetMapping("/repos")
    @Operation(summary = "查询星火知识库列表")
    public BaseResponse<List<KnowledgeRepositoryVO>> repositories(HttpServletRequest request) {
        requireAdmin(request);
        return ResultUtils.success(knowledgeBaseService.listRepositories());
    }

    @PostMapping("/repo/create")
    @Operation(summary = "创建星火知识库")
    public BaseResponse<KnowledgeRepositoryVO> createRepository(@RequestBody RepositoryCreateRequest body,
                                                                HttpServletRequest request) {
        User admin = requireAdmin(request);
        KnowledgeRepositoryVO created = knowledgeBaseService.createRepository(
                body == null ? null : body.getName(),
                body == null ? null : body.getDescription(),
                body == null ? null : body.getTags());
        adminAuditLogger.log(admin, "星火知识库", "创建知识库", "xfyun_repo",
                created.repoId(), created.repoName(), request);
        return ResultUtils.success(created);
    }

    @GetMapping("/courses")
    @Operation(summary = "查询课程与知识库绑定关系")
    public BaseResponse<List<KnowledgeBaseService.CourseBinding>> courseBindings(HttpServletRequest request) {
        requireAdmin(request);
        return ResultUtils.success(knowledgeBaseService.listCourseBindings());
    }

    @PostMapping("/course/bind")
    @Operation(summary = "绑定课程知识库")
    public BaseResponse<KnowledgeBaseService.CourseBinding> bindCourse(@RequestBody CourseBindRequest body,
                                                                       HttpServletRequest request) {
        User admin = requireAdmin(request);
        if (body == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "绑定参数不能为空");
        KnowledgeBaseService.CourseBinding binding = knowledgeBaseService.bindCourse(
                body.getCourseId(), body.getRepoId(), body.getRepoName(), body.getKeywords());
        adminAuditLogger.log(admin, "星火知识库", "绑定课程", "course", body.getCourseId(),
                binding.courseName() + " -> " + binding.repoName(), request);
        return ResultUtils.success(binding);
    }

    @PostMapping("/course/unbind")
    @Operation(summary = "解除课程知识库绑定")
    public BaseResponse<KnowledgeBaseService.CourseBinding> unbindCourse(@RequestBody CourseIdRequest body,
                                                                         HttpServletRequest request) {
        User admin = requireAdmin(request);
        if (body == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择课程");
        KnowledgeBaseService.CourseBinding binding = knowledgeBaseService.unbindCourse(body.getCourseId());
        adminAuditLogger.log(admin, "星火知识库", "解除课程绑定", "course", body.getCourseId(),
                binding.courseName(), request);
        return ResultUtils.success(binding);
    }

    @GetMapping("/files")
    @Operation(summary = "查询知识库文档")
    public BaseResponse<List<KnowledgeFileVO>> files(@RequestParam String repoId,
                                                     HttpServletRequest request) {
        requireAdmin(request);
        return ResultUtils.success(knowledgeBaseService.listFilesAndRefresh(repoId));
    }

    @PostMapping("/file/upload")
    @Operation(summary = "上传文档到知识库")
    public BaseResponse<KnowledgeUploadVO> upload(@RequestParam String repoId,
                                                  @RequestParam("file") MultipartFile file,
                                                  HttpServletRequest request) {
        User admin = requireAdmin(request);
        KnowledgeUploadVO uploaded = knowledgeBaseService.upload(repoId, file);
        adminAuditLogger.log(admin, "星火知识库", "上传知识文档", "xfyun_file",
                uploaded.fileId(), uploaded.fileName(), request);
        return ResultUtils.success(uploaded);
    }

    @PostMapping("/starter-pack/upload")
    @Operation(summary = "上传内置数据结构初始资料")
    public BaseResponse<List<KnowledgeUploadVO>> uploadStarterPack(@RequestBody RepositoryIdRequest body,
                                                                   HttpServletRequest request) {
        User admin = requireAdmin(request);
        if (body == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择知识库");
        List<KnowledgeUploadVO> uploads = knowledgeBaseService.uploadStarterPack(body.getRepoId());
        adminAuditLogger.log(admin, "星火知识库", "导入数据结构初始资料", "xfyun_repo",
                body.getRepoId(), "上传 " + uploads.size() + " 份内置资料", request);
        return ResultUtils.success(uploads);
    }

    @PostMapping("/file/status")
    @Operation(summary = "刷新文档向量化状态")
    public BaseResponse<Map<String, String>> refreshStatuses(@RequestBody FileStatusRequest body,
                                                             HttpServletRequest request) {
        requireAdmin(request);
        if (body == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件参数不能为空");
        return ResultUtils.success(knowledgeBaseService.refreshFileStatuses(body.getRepoId(), body.getFileIds()));
    }

    @PostMapping("/file/delete")
    @Operation(summary = "删除知识库文档及向量")
    public BaseResponse<Boolean> deleteFile(@RequestBody FileDeleteRequest body,
                                            HttpServletRequest request) {
        User admin = requireAdmin(request);
        if (body == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要删除的文件");
        knowledgeBaseService.deleteFile(body.getRepoId(), body.getFileId());
        adminAuditLogger.log(admin, "星火知识库", "删除知识文档", "xfyun_file",
                body.getFileId(), body.getFileName(), request);
        return ResultUtils.success(true);
    }

    private User requireAdmin(HttpServletRequest request) {
        User user = SessionUserContext.getOptional(request);
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        if (!"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可管理星火知识库");
        }
        return user;
    }

    @Data
    public static class RepositoryCreateRequest {
        private String name;
        private String description;
        private String tags;
    }

    @Data
    public static class RepositoryIdRequest {
        private String repoId;
    }

    @Data
    public static class CourseBindRequest {
        private Long courseId;
        private String repoId;
        private String repoName;
        private String keywords;
    }

    @Data
    public static class CourseIdRequest {
        private Long courseId;
    }

    @Data
    public static class FileStatusRequest {
        private String repoId;
        private List<String> fileIds;
    }

    @Data
    public static class FileDeleteRequest {
        private String repoId;
        private String fileId;
        private String fileName;
    }
}
