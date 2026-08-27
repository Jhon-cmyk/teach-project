package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.client.RemoteResourceClient;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.exception.ExternalServiceException;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.TeachingCaseAsset;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AgentIndexService;
import com.ruyi.teach.service.PlatformTeachingCaseService;
import com.ruyi.teach.service.RemoteDocumentTextService;
import com.ruyi.teach.service.TeachingCaseAssetService;
import com.ruyi.teach.util.CaseDocumentPreviewExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/teaching-case")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Tag(name = "教学案例管理")
public class TeachingCaseController {

    private static final int PREVIEW_TEXT_LIMIT = 8000;
    private static final int PREVIEW_DOCX_MAX_BYTES = 50 * 1024 * 1024;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private AgentIndexService agentIndexService;

    @Resource
    private PlatformTeachingCaseService platformTeachingCaseService;

    @Resource
    private TeachingCaseAssetService teachingCaseAssetService;

    @Resource
    private RemoteResourceClient remoteResourceClient;

    @Resource
    private RemoteDocumentTextService remoteDocumentTextService;

    private Long getLoginTeacherId(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        return loginUser.getId();
    }

    private User getLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "not logged in");
        }
        return loginUser;
    }

    @Data
    public static class ImportRequest {
        private String title;
        private String category;
        private String difficulty;
        private String courseName;
        private String pdfUrl;
    }

    @Data
    public static class SavePlatformCaseVO {
        private Long id;
        private Boolean alreadySaved;
    }

    @Operation(summary = "查询当前教师的案例列表")
    @GetMapping("/list")
    public BaseResponse<List<TeachingCase>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        applyTeacherVisibleCaseFilter(qw, teacherId)
                .eq(StringUtils.isNotBlank(category), TeachingCase::getCategory, category)
                .and(StringUtils.isNotBlank(keyword), wrapper ->
                        wrapper.like(TeachingCase::getTitle, keyword)
                                .or()
                                .like(TeachingCase::getCourseName, keyword)
                                .or()
                                .like(TeachingCase::getSummary, keyword)
                                .or()
                                .like(TeachingCase::getKeywords, keyword)
                )
                .orderByDesc(TeachingCase::getCreateTime);
        return ResultUtils.success(teachingCaseMapper.selectList(qw));
    }

    @Operation(summary = "分页查询当前教师的案例列表")
    @GetMapping("/page")
    public BaseResponse<Page<TeachingCase>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "8") long pageSize,
            HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        long safeCurrent = Math.max(current, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 50);

        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        applyTeacherVisibleCaseFilter(qw, teacherId)
                .eq(StringUtils.isNotBlank(category), TeachingCase::getCategory, category)
                .eq(StringUtils.isNotBlank(difficulty), TeachingCase::getDifficulty, difficulty)
                .and(StringUtils.isNotBlank(keyword), wrapper ->
                        wrapper.like(TeachingCase::getTitle, keyword)
                                .or()
                                .like(TeachingCase::getCourseName, keyword)
                                .or()
                                .like(TeachingCase::getSummary, keyword)
                                .or()
                                .like(TeachingCase::getKeywords, keyword)
                )
                .orderByDesc(TeachingCase::getCreateTime);

        Page<TeachingCase> page = new Page<>(safeCurrent, safePageSize);
        return ResultUtils.success(teachingCaseMapper.selectPage(page, qw));
    }

    @Operation(summary = "导入案例")
    @PostMapping("/import")
    public BaseResponse<Long> importCase(@RequestBody ImportRequest req, HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);

        if (StringUtils.isBlank(req.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "案例标题不能为空");
        }
        if (StringUtils.isBlank(req.getCategory())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "案例分类不能为空");
        }
        if (StringUtils.isBlank(req.getDifficulty())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "难度等级不能为空");
        }
        if (StringUtils.isBlank(req.getPdfUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "案例文件不能为空");
        }
        if (!isSupportedCaseFile(req.getPdfUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 PDF、Word(.doc/.docx) 文件");
        }

        TeachingCase teachingCase = new TeachingCase();
        teachingCase.setTeacherId(teacherId);
        teachingCase.setTitle(req.getTitle().trim());
        teachingCase.setCategory(req.getCategory());
        teachingCase.setDifficulty(req.getDifficulty());
        teachingCase.setCourseName(StringUtils.isBlank(req.getCourseName()) ? null : req.getCourseName().trim());
        teachingCase.setPdfUrl(req.getPdfUrl());
        teachingCase.setScope(PlatformTeachingCaseService.SCOPE_MINE);
        teachingCase.setStatus(PlatformTeachingCaseService.STATUS_APPROVED);
        teachingCase.setPreviewType("document");
        teachingCase.setPreviewText(extractPreviewText(req.getPdfUrl()));
        teachingCase.setIsDelete(0);

        teachingCaseMapper.insert(teachingCase);
        teachingCaseAssetService.rebuildCaseImages(teachingCase);
        agentIndexService.upsertTeachingCase(teachingCaseMapper.selectById(teachingCase.getId()));
        return ResultUtils.success(teachingCase.getId());
    }

    @Operation(summary = "推荐当前教师可用教学案例")
    @PostMapping("/recommend")
    public BaseResponse<List<PlatformTeachingCaseService.RecommendCaseVO>> recommend(
            @RequestBody PlatformTeachingCaseService.RecommendRequest req,
            HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        return ResultUtils.success(platformTeachingCaseService.recommend(req, teacherId));
    }

    @Operation(summary = "保存平台案例到当前教师案例库")
    @PostMapping("/save-platform/{id}")
    public BaseResponse<SavePlatformCaseVO> savePlatformCase(@PathVariable Long id, HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        TeachingCase platformCase = teachingCaseMapper.selectById(id);
        if (platformCase == null
                || Objects.equals(platformCase.getIsDelete(), 1)
                || !PlatformTeachingCaseService.SCOPE_PLATFORM.equals(StringUtils.defaultString(platformCase.getScope()))
                || !PlatformTeachingCaseService.STATUS_APPROVED.equals(StringUtils.defaultString(platformCase.getStatus()))) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "平台案例不存在或未发布");
        }

        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        qw.eq(TeachingCase::getTeacherId, teacherId)
                .eq(TeachingCase::getScope, PlatformTeachingCaseService.SCOPE_MINE)
                .eq(TeachingCase::getSourceCaseId, id)
                .eq(TeachingCase::getIsDelete, 0)
                .last("LIMIT 1");
        TeachingCase existing = teachingCaseMapper.selectOne(qw);
        SavePlatformCaseVO vo = new SavePlatformCaseVO();
        if (existing != null) {
            vo.setId(existing.getId());
            vo.setAlreadySaved(true);
            return ResultUtils.success(vo);
        }
        // Platform cases are already shown in Case Management as shared cases.
        // Treat them as saved instead of copying another personal case row.
        vo.setId(platformCase.getId());
        vo.setAlreadySaved(true);
        return ResultUtils.success(vo);
    }

    @Operation(summary = "获取当前教师已保存的平台案例 ID")
    @GetMapping("/saved-platform-ids")
    public BaseResponse<List<Long>> savedPlatformIds(HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        LambdaQueryWrapper<TeachingCase> copyQw = new LambdaQueryWrapper<>();
        copyQw.eq(TeachingCase::getTeacherId, teacherId)
                .eq(TeachingCase::getScope, PlatformTeachingCaseService.SCOPE_MINE)
                .isNotNull(TeachingCase::getSourceCaseId)
                .eq(TeachingCase::getIsDelete, 0);
        List<Long> ids = new java.util.ArrayList<>(teachingCaseMapper.selectList(copyQw).stream()
                .map(TeachingCase::getSourceCaseId)
                .filter(Objects::nonNull)
                .toList());

        LambdaQueryWrapper<TeachingCase> platformQw = new LambdaQueryWrapper<>();
        platformQw.eq(TeachingCase::getScope, PlatformTeachingCaseService.SCOPE_PLATFORM)
                .eq(TeachingCase::getStatus, PlatformTeachingCaseService.STATUS_APPROVED)
                .eq(TeachingCase::getIsDelete, 0);
        ids.addAll(teachingCaseMapper.selectList(platformQw).stream()
                .map(TeachingCase::getId)
                .filter(Objects::nonNull)
                .toList());

        ids = ids.stream()
                .distinct()
                .toList();
        return ResultUtils.success(ids);
    }

    @Operation(summary = "Rebuild teaching case vector index")
    @PostMapping("/rebuild-index")
    public BaseResponse<Boolean> rebuildIndex(HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        qw.eq(TeachingCase::getTeacherId, teacherId)
                .eq(TeachingCase::getIsDelete, 0);
        List<TeachingCase> teachingCases = teachingCaseMapper.selectList(qw);
        agentIndexService.rebuildTeachingCases(teacherId, teachingCases);
        return ResultUtils.success(true);
    }

    @Operation(summary = "删除案例(逻辑删除)")
    @PostMapping("/delete/{id}")
    public BaseResponse<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        Long teacherId = getLoginTeacherId(request);
        TeachingCase existing = teachingCaseMapper.selectById(id);
        if (existing == null || existing.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "案例不存在");
        }
        if (!existing.getTeacherId().equals(teacherId)
                || !PlatformTeachingCaseService.SCOPE_MINE.equals(StringUtils.defaultIfBlank(existing.getScope(), PlatformTeachingCaseService.SCOPE_MINE))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权操作该案例");
        }

        teachingCaseMapper.deleteById(id);
        agentIndexService.deleteTeachingCase(teacherId, id);
        return ResultUtils.success(true);
    }

    @Operation(summary = "获取案例详情")
    @GetMapping("/detail")
    public BaseResponse<TeachingCase> detail(@RequestParam Long id, HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        TeachingCase existing = teachingCaseMapper.selectById(id);
        if (existing == null || existing.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "案例不存在");
        }
        if (!canViewCase(existing, loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权查看该案例");
        }
        return ResultUtils.success(existing);
    }

    @Operation(summary = "Get teaching case preview metadata")
    @GetMapping("/preview-detail")
    public BaseResponse<PreviewDetailVO> previewDetail(@RequestParam Long id, HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        TeachingCase existing = teachingCaseMapper.selectById(id);
        if (existing == null || existing.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "case not found");
        }
        if (!canViewCase(existing, loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "no permission to view this case");
        }
        ensurePreviewText(existing);
        if (StringUtils.defaultString(existing.getPdfUrl()).toLowerCase(Locale.ROOT).contains(".docx")) {
            teachingCaseAssetService.ensureCaseImages(existing);
        }
        return ResultUtils.success(toPreviewDetail(existing));
    }

    @Operation(summary = "预览案例文件（代理流）")
    @GetMapping("/preview/{id}")
    public void previewFile(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        User loginUser = getLoginUser(request);
        TeachingCase existing = teachingCaseMapper.selectById(id);
        if (existing == null || existing.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "案例不存在");
        }
        if (!canViewCase(existing, loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权查看该案例");
        }

        try {
            String fileUrl = existing.getPdfUrl();
            remoteResourceClient.withStream(
                    "teaching-case-file",
                    fileUrl,
                    Duration.ofSeconds(30),
                    remote -> {
                        String upstreamType = StringUtils.defaultString(remote.contentType()).toLowerCase(Locale.ROOT);
                        if (upstreamType.contains("text/html") || upstreamType.contains("application/json")) {
                            throw new ExternalServiceException(
                                    "teaching-case-file",
                                    "案例文件格式无效或暂时不可用"
                            );
                        }

                        try (PushbackInputStream in = new PushbackInputStream(remote.body(), 16)) {
                            byte[] header = in.readNBytes(8);
                            if (header.length > 0) {
                                in.unread(header);
                            }
                            if (!isValidPreviewHeader(fileUrl, header)) {
                                throw new ExternalServiceException(
                                        "teaching-case-file",
                                        "案例文件格式无效或暂时不可用"
                                );
                            }

                            response.setContentType(resolveContentType(fileUrl));
                            response.setHeader("Content-Disposition", resolveDisposition(fileUrl));
                            try (OutputStream out = response.getOutputStream()) {
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, len);
                                }
                                out.flush();
                            }
                        }
                        return null;
                    }
            );
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "teaching-case-file",
                    "案例文件服务暂时不可用",
                    e
            );
        }
    }

    private boolean isSupportedCaseFile(String url) {
        String lower = StringUtils.defaultString(url).toLowerCase(Locale.ROOT);
        return lower.contains(".pdf") || lower.contains(".docx") || lower.contains(".doc");
    }

    private PreviewDetailVO toPreviewDetail(TeachingCase teachingCase) {
        PreviewDetailVO vo = new PreviewDetailVO();
        vo.setId(teachingCase.getId());
        vo.setTitle(teachingCase.getTitle());
        vo.setCategory(teachingCase.getCategory());
        vo.setDifficulty(teachingCase.getDifficulty());
        vo.setCourseName(teachingCase.getCourseName());
        vo.setSummary(teachingCase.getSummary());
        vo.setPreviewText(StringUtils.defaultIfBlank(teachingCase.getPreviewText(), teachingCase.getSummary()));
        vo.setPreviewType(StringUtils.defaultIfBlank(teachingCase.getPreviewType(), "document"));
        vo.setSourceName(teachingCase.getSourceName());
        vo.setSourceUrl(teachingCase.getSourceUrl());
        vo.setMaterialJson(teachingCase.getMaterialJson());
        vo.setPdfUrl(teachingCase.getPdfUrl());
        vo.setCanOpenDocument(isSupportedCaseFile(teachingCase.getPdfUrl()));
        List<TeachingCaseAsset> imageMaterials = teachingCaseAssetService.listCaseImages(teachingCase.getId());
        vo.setImageMaterials(imageMaterials);
        vo.setPreviewHtml(buildPreviewHtml(teachingCase, imageMaterials));
        return vo;
    }

    private String buildPreviewHtml(TeachingCase teachingCase, List<TeachingCaseAsset> imageMaterials) {
        if (teachingCase == null || !StringUtils.defaultString(teachingCase.getPdfUrl()).toLowerCase(Locale.ROOT).contains(".docx")) {
            return "";
        }
        byte[] bytes = downloadPreviewBytes(teachingCase.getPdfUrl());
        if (bytes.length == 0) {
            return "";
        }
        return CaseDocumentPreviewExtractor.extractDocxHtml(bytes, imageMaterials);
    }

    private byte[] downloadPreviewBytes(String url) {
        return remoteResourceClient.downloadBytesOrEmpty(
                "teaching-case-preview",
                url,
                PREVIEW_DOCX_MAX_BYTES,
                Duration.ofSeconds(20)
        );
    }

    private void ensurePreviewText(TeachingCase teachingCase) {
        if (teachingCase == null || StringUtils.isNotBlank(teachingCase.getPreviewText())) {
            return;
        }
        String previewText = extractPreviewText(teachingCase.getPdfUrl());
        if (StringUtils.isBlank(previewText)) {
            return;
        }
        TeachingCase update = new TeachingCase();
        update.setId(teachingCase.getId());
        update.setPreviewText(previewText);
        update.setPreviewType("document");
        teachingCaseMapper.updateById(update);
        teachingCase.setPreviewText(previewText);
        teachingCase.setPreviewType("document");
    }

    private String extractPreviewText(String fileUrl) {
        String text = remoteDocumentTextService.extractText(fileUrl);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String normalized = text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t\\f]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n\n")
                .trim();
        return StringUtils.abbreviate(normalized, PREVIEW_TEXT_LIMIT);
    }

    private String resolveContentType(String url) {
        String lower = StringUtils.defaultString(url).toLowerCase(Locale.ROOT);
        if (lower.contains(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.contains(".doc")) {
            return "application/msword";
        }
        if (lower.contains(".htm") || lower.contains(".html")) {
            return "text/html;charset=UTF-8";
        }
        return "application/pdf";
    }

    private String resolveDisposition(String url) {
        String lower = StringUtils.defaultString(url).toLowerCase(Locale.ROOT);
        return lower.contains(".pdf") ? "inline" : "attachment";
    }

    private boolean isValidPreviewHeader(String url, byte[] header) {
        String lower = StringUtils.defaultString(url).toLowerCase(Locale.ROOT);
        if (lower.contains(".pdf")) {
            return header != null
                    && header.length >= 4
                    && header[0] == '%'
                    && header[1] == 'P'
                    && header[2] == 'D'
                    && header[3] == 'F';
        }
        if (lower.contains(".docx")) {
            return header != null
                    && header.length >= 2
                    && header[0] == 'P'
                    && header[1] == 'K';
        }
        if (lower.contains(".doc")) {
            return header != null
                    && header.length >= 8
                    && (header[0] & 0xFF) == 0xD0
                    && (header[1] & 0xFF) == 0xCF
                    && (header[2] & 0xFF) == 0x11
                    && (header[3] & 0xFF) == 0xE0
                    && (header[4] & 0xFF) == 0xA1
                    && (header[5] & 0xFF) == 0xB1
                    && (header[6] & 0xFF) == 0x1A
                    && (header[7] & 0xFF) == 0xE1;
        }
        return false;
    }

    private boolean canViewCase(TeachingCase teachingCase, Long teacherId) {
        if (teachingCase == null || Objects.equals(teachingCase.getIsDelete(), 1)) {
            return false;
        }
        String scope = StringUtils.defaultIfBlank(teachingCase.getScope(), PlatformTeachingCaseService.SCOPE_MINE);
        if (PlatformTeachingCaseService.SCOPE_PLATFORM.equals(scope)) {
            return PlatformTeachingCaseService.STATUS_APPROVED.equals(StringUtils.defaultString(teachingCase.getStatus()));
        }
        return PlatformTeachingCaseService.SCOPE_MINE.equals(scope)
                && Objects.equals(teachingCase.getTeacherId(), teacherId);
    }

    private boolean canViewCase(TeachingCase teachingCase, User loginUser) {
        if (teachingCase == null || Objects.equals(teachingCase.getIsDelete(), 1) || loginUser == null) {
            return false;
        }
        if ("admin".equals(loginUser.getUserRole())) {
            return true;
        }
        return canViewCase(teachingCase, loginUser.getId());
    }

    private LambdaQueryWrapper<TeachingCase> applyTeacherVisibleCaseFilter(LambdaQueryWrapper<TeachingCase> qw, Long teacherId) {
        return qw.eq(TeachingCase::getIsDelete, 0)
                .and(wrapper -> wrapper
                        .and(mine -> mine
                                .eq(TeachingCase::getTeacherId, teacherId)
                                .and(scope -> scope
                                        .eq(TeachingCase::getScope, PlatformTeachingCaseService.SCOPE_MINE)
                                        .or()
                                        .isNull(TeachingCase::getScope)
                                        .or()
                                        .eq(TeachingCase::getScope, "")))
                        .or(platform -> platform
                                .eq(TeachingCase::getScope, PlatformTeachingCaseService.SCOPE_PLATFORM)
                                .eq(TeachingCase::getStatus, PlatformTeachingCaseService.STATUS_APPROVED)));
    }

    @Data
    public static class PreviewDetailVO {
        private Long id;
        private String title;
        private String category;
        private String difficulty;
        private String courseName;
        private String summary;
        private String previewText;
        private String previewHtml;
        private String previewType;
        private String sourceName;
        private String sourceUrl;
        private String materialJson;
        private String pdfUrl;
        private Boolean canOpenDocument;
        private List<TeachingCaseAsset> imageMaterials;
    }
}
