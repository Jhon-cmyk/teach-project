package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.AgentIndexService;
import com.ruyi.teach.service.PlatformTeachingCaseService;
import com.ruyi.teach.service.RemoteDocumentTextService;
import com.ruyi.teach.service.TeachingCaseAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/admin/teaching-case")
@Tag(name = "Admin platform teaching case library")
public class AdminTeachingCaseController {

    private static final int PREVIEW_TEXT_LIMIT = 8000;

    @Resource
    private PlatformTeachingCaseService platformTeachingCaseService;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private AgentIndexService agentIndexService;

    @Resource
    private TeachingCaseAssetService teachingCaseAssetService;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Resource
    private RemoteDocumentTextService remoteDocumentTextService;

    @Operation(summary = "Import local platform teaching case")
    @PostMapping("/import")
    public BaseResponse<Long> importPlatformCase(@RequestBody ImportRequest req, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        if (req == null || StringUtils.isBlank(req.getTitle())) {
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

        String status = StringUtils.defaultIfBlank(req.getStatus(), PlatformTeachingCaseService.STATUS_APPROVED);
        if (!List.of(
                PlatformTeachingCaseService.STATUS_PENDING,
                PlatformTeachingCaseService.STATUS_APPROVED,
                PlatformTeachingCaseService.STATUS_REJECTED,
                PlatformTeachingCaseService.STATUS_OFFLINE
        ).contains(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "案例状态不合法");
        }

        TeachingCase teachingCase = new TeachingCase();
        teachingCase.setTeacherId(admin.getId());
        teachingCase.setTitle(req.getTitle().trim());
        teachingCase.setCategory(req.getCategory());
        teachingCase.setDifficulty(req.getDifficulty());
        teachingCase.setCourseName(StringUtils.trimToNull(req.getCourseName()));
        teachingCase.setPdfUrl(req.getPdfUrl());
        teachingCase.setScope(PlatformTeachingCaseService.SCOPE_PLATFORM);
        teachingCase.setStatus(status);
        teachingCase.setSourceUrl(StringUtils.trimToNull(req.getSourceUrl()));
        teachingCase.setSourceName(StringUtils.defaultIfBlank(req.getSourceName(), "平台上传"));
        teachingCase.setSummary(StringUtils.trimToNull(req.getSummary()));
        teachingCase.setKeywords(StringUtils.trimToNull(req.getKeywords()));
        teachingCase.setPreviewText(extractPreviewText(req.getPdfUrl()));
        teachingCase.setPreviewType("document");
        teachingCase.setIsDelete(0);
        if (PlatformTeachingCaseService.STATUS_APPROVED.equals(status)) {
            teachingCase.setReviewTime(new Date());
            teachingCase.setReviewerId(admin.getId());
        }

        teachingCaseMapper.insert(teachingCase);
        teachingCaseAssetService.rebuildCaseImages(teachingCase);
        if (PlatformTeachingCaseService.STATUS_APPROVED.equals(status)) {
            agentIndexService.upsertTeachingCase(teachingCaseMapper.selectById(teachingCase.getId()));
        }
        adminAuditLogger.log(admin, "平台案例", "导入平台案例", "teaching_case", teachingCase.getId(),
                teachingCase.getTitle(), request);
        return ResultUtils.success(teachingCase.getId());
    }

    @Operation(summary = "Crawl public teaching cases into pending review")
    @PostMapping("/crawl")
    public BaseResponse<List<TeachingCase>> crawl(@RequestBody CrawlRequest req, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        List<TeachingCase> result = platformTeachingCaseService.crawlToPending(
                req == null ? null : req.getKeyword(),
                req == null ? null : req.getSourceUrl(),
                admin.getId()
        );
        adminAuditLogger.log(admin, "平台案例", "抓取平台案例", "teaching_case", "",
                "keyword=" + (req == null ? "" : StringUtils.defaultString(req.getKeyword()))
                        + "，count=" + result.size(), request);
        return ResultUtils.success(result);
    }

    @Operation(summary = "Page platform teaching cases")
    @GetMapping("/page")
    public BaseResponse<Page<TeachingCase>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        getAdminLoginUser(request);
        return ResultUtils.success(platformTeachingCaseService.pagePlatformCases(current, size, status, keyword));
    }

    @Operation(summary = "Update platform teaching case metadata")
    @PostMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody UpdateRequest req, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        if (req == null || req.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id is required");
        }
        TeachingCase existing = requirePlatformCase(req.getId());
        TeachingCase update = new TeachingCase();
        update.setId(existing.getId());
        update.setTitle(StringUtils.defaultIfBlank(req.getTitle(), existing.getTitle()).trim());
        update.setCategory(StringUtils.defaultIfBlank(req.getCategory(), existing.getCategory()));
        update.setDifficulty(StringUtils.defaultIfBlank(req.getDifficulty(), existing.getDifficulty()));
        update.setCourseName(StringUtils.defaultIfBlank(req.getCourseName(), existing.getCourseName()));
        update.setPdfUrl(StringUtils.defaultIfBlank(req.getPdfUrl(), existing.getPdfUrl()));
        update.setSourceUrl(StringUtils.defaultIfBlank(req.getSourceUrl(), existing.getSourceUrl()));
        update.setSourceName(StringUtils.defaultIfBlank(req.getSourceName(), existing.getSourceName()));
        update.setSummary(req.getSummary());
        update.setKeywords(req.getKeywords());
        update.setMaterialJson(req.getMaterialJson());
        update.setStructureJson(req.getStructureJson());
        if (StringUtils.isNotBlank(req.getPdfUrl()) && !Objects.equals(req.getPdfUrl(), existing.getPdfUrl())) {
            update.setPreviewText(extractPreviewText(req.getPdfUrl()));
            update.setPreviewType("document");
        }
        teachingCaseMapper.updateById(update);
        if (StringUtils.isNotBlank(req.getPdfUrl()) && !Objects.equals(req.getPdfUrl(), existing.getPdfUrl())) {
            teachingCaseAssetService.rebuildCaseImages(existing.getId());
        }
        TeachingCase fresh = teachingCaseMapper.selectById(existing.getId());
        if (fresh != null && PlatformTeachingCaseService.STATUS_APPROVED.equals(StringUtils.defaultString(fresh.getStatus()))) {
            agentIndexService.upsertTeachingCase(fresh);
        }
        adminAuditLogger.log(admin, "平台案例", "更新平台案例", "teaching_case", existing.getId(),
                StringUtils.defaultIfBlank(update.getTitle(), existing.getTitle()), request);
        return ResultUtils.success(true);
    }

    @PostMapping("/approve/{id}")
    public BaseResponse<Boolean> approve(@PathVariable Long id, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        TeachingCase existing = requirePlatformCase(id);
        platformTeachingCaseService.approve(id, admin.getId());
        adminAuditLogger.log(admin, "平台案例", "审核通过平台案例", "teaching_case", id,
                existing.getTitle(), request);
        return ResultUtils.success(true);
    }

    @PostMapping("/reject/{id}")
    public BaseResponse<Boolean> reject(@PathVariable Long id, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        TeachingCase existing = requirePlatformCase(id);
        platformTeachingCaseService.reject(id, admin.getId());
        adminAuditLogger.log(admin, "平台案例", "驳回平台案例", "teaching_case", id,
                existing.getTitle(), request);
        return ResultUtils.success(true);
    }

    @PostMapping("/offline/{id}")
    public BaseResponse<Boolean> offline(@PathVariable Long id, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        TeachingCase existing = requirePlatformCase(id);
        platformTeachingCaseService.offline(id, admin.getId());
        adminAuditLogger.log(admin, "平台案例", "下架平台案例", "teaching_case", id,
                existing.getTitle(), request);
        return ResultUtils.success(true);
    }

    @PostMapping("/rebuild-index")
    public BaseResponse<Boolean> rebuildIndex(HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        platformTeachingCaseService.rebuildApprovedPlatformIndex();
        adminAuditLogger.log(admin, "平台案例", "重建案例索引", "teaching_case", "",
                "重建已通过平台案例索引", request);
        return ResultUtils.success(true);
    }

    @PostMapping("/rebuild-assets/{id}")
    public BaseResponse<Integer> rebuildAssets(@PathVariable Long id, HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        TeachingCase existing = requirePlatformCase(id);
        int count = teachingCaseAssetService.rebuildCaseImages(id).size();
        TeachingCase fresh = teachingCaseMapper.selectById(id);
        if (fresh != null && PlatformTeachingCaseService.STATUS_APPROVED.equals(StringUtils.defaultString(fresh.getStatus()))) {
            agentIndexService.upsertTeachingCase(fresh);
        }
        adminAuditLogger.log(admin, "平台案例", "重建案例资源", "teaching_case", id,
                existing.getTitle() + "，资源数=" + count, request);
        return ResultUtils.success(count);
    }

    @PostMapping("/rebuild-assets")
    public BaseResponse<Integer> rebuildApprovedAssets(HttpServletRequest request) {
        User admin = getAdminLoginUser(request);
        int total = 0;
        long current = 1;
        Page<TeachingCase> page;
        do {
            page = platformTeachingCaseService.pagePlatformCases(current++, 50, PlatformTeachingCaseService.STATUS_APPROVED, null);
            for (TeachingCase teachingCase : page.getRecords()) {
                total += teachingCaseAssetService.rebuildCaseImages(teachingCase).size();
                TeachingCase fresh = teachingCaseMapper.selectById(teachingCase.getId());
                if (fresh != null) {
                    agentIndexService.upsertTeachingCase(fresh);
                }
            }
        } while (page.getCurrent() < page.getPages());
        adminAuditLogger.log(admin, "平台案例", "批量重建案例资源", "teaching_case", "",
                "重建资源数=" + total, request);
        return ResultUtils.success(total);
    }

    private TeachingCase requirePlatformCase(Long id) {
        TeachingCase existing = teachingCaseMapper.selectById(id);
        if (existing == null || Objects.equals(existing.getIsDelete(), 1)
                || !PlatformTeachingCaseService.SCOPE_PLATFORM.equals(StringUtils.defaultString(existing.getScope()))) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "platform teaching case not found");
        }
        return existing;
    }

    private User getAdminLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "admin only");
        }
        return loginUser;
    }

    @Data
    public static class CrawlRequest {
        private String keyword;
        private String sourceUrl;
    }

    @Data
    public static class ImportRequest {
        private String title;
        private String category;
        private String difficulty;
        private String courseName;
        private String pdfUrl;
        private String sourceUrl;
        private String sourceName;
        private String summary;
        private String keywords;
        private String status;
    }

    @Data
    public static class UpdateRequest {
        private Long id;
        private String title;
        private String category;
        private String difficulty;
        private String courseName;
        private String pdfUrl;
        private String sourceUrl;
        private String sourceName;
        private String summary;
        private String keywords;
        private String materialJson;
        private String structureJson;
    }

    private boolean isSupportedCaseFile(String url) {
        String lower = StringUtils.defaultString(url).toLowerCase();
        return lower.contains(".pdf") || lower.contains(".docx") || lower.contains(".doc");
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
}
