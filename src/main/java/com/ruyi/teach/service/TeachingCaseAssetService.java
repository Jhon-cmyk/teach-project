package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.RemoteResourceClient;
import com.ruyi.teach.mapper.TeachingCaseAssetMapper;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.TeachingCaseAsset;
import com.ruyi.teach.util.CaseDocumentImageExtractor;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TeachingCaseAssetService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_DOCX_BYTES = 50 * 1024 * 1024;

    @Resource
    private TeachingCaseAssetMapper teachingCaseAssetMapper;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private OssService ossService;

    @Resource
    private RemoteResourceClient remoteResourceClient;

    public List<TeachingCaseAsset> rebuildCaseImages(Long caseId) {
        TeachingCase teachingCase = caseId == null ? null : teachingCaseMapper.selectById(caseId);
        return rebuildCaseImages(teachingCase);
    }

    public List<TeachingCaseAsset> rebuildCaseImages(TeachingCase teachingCase) {
        if (teachingCase == null || teachingCase.getId() == null) {
            return List.of();
        }
        if (!isDocx(teachingCase.getPdfUrl())) {
            softDeleteExistingImages(teachingCase.getId());
            mergeMaterialJson(teachingCase, List.of());
            return List.of();
        }

        byte[] bytes = downloadBytes(teachingCase.getPdfUrl());
        if (bytes.length == 0) {
            return List.of();
        }

        List<CaseDocumentImageExtractor.ExtractedImage> images = CaseDocumentImageExtractor.extractDocxImages(bytes);
        softDeleteExistingImages(teachingCase.getId());
        if (images.isEmpty()) {
            mergeMaterialJson(teachingCase, List.of());
            return List.of();
        }

        List<TeachingCaseAsset> assets = new ArrayList<>();
        int index = 0;
        for (CaseDocumentImageExtractor.ExtractedImage image : images) {
            try {
                String url = ossService.uploadBytes(
                        image.bytes(),
                        image.fileName(),
                        "case/assets/" + teachingCase.getId(),
                        image.contentType()
                );
                TeachingCaseAsset asset = new TeachingCaseAsset();
                asset.setCaseId(teachingCase.getId());
                asset.setType("image");
                asset.setUrl(url);
                asset.setTitle("Case image " + (++index));
                asset.setCaption("");
                asset.setContext(StringUtils.abbreviate(StringUtils.joinWith(" ",
                        teachingCase.getTitle(),
                        teachingCase.getCourseName(),
                        teachingCase.getSummary(),
                        teachingCase.getKeywords()
                ), 1000));
                asset.setSortOrder(image.order());
                asset.setHash(image.hash());
                asset.setWidth(image.width());
                asset.setHeight(image.height());
                asset.setSource("docx");
                asset.setIsDelete(0);
                teachingCaseAssetMapper.insert(asset);
                assets.add(asset);
            } catch (Exception ignored) {
                // Keep case import usable even if a single asset upload fails.
            }
        }
        mergeMaterialJson(teachingCase, assets);
        return assets;
    }

    public List<TeachingCaseAsset> listCaseImages(Long caseId) {
        if (caseId == null) {
            return List.of();
        }
        LambdaQueryWrapper<TeachingCaseAsset> qw = new LambdaQueryWrapper<>();
        qw.eq(TeachingCaseAsset::getCaseId, caseId)
                .eq(TeachingCaseAsset::getType, "image")
                .eq(TeachingCaseAsset::getIsDelete, 0)
                .orderByAsc(TeachingCaseAsset::getSortOrder)
                .orderByAsc(TeachingCaseAsset::getId);
        return teachingCaseAssetMapper.selectList(qw);
    }

    public List<TeachingCaseAsset> ensureCaseImages(TeachingCase teachingCase) {
        if (teachingCase == null || teachingCase.getId() == null) {
            return List.of();
        }
        List<TeachingCaseAsset> existing = listCaseImages(teachingCase.getId());
        if (!existing.isEmpty()) {
            return existing;
        }
        return rebuildCaseImages(teachingCase);
    }

    public Map<Long, List<TeachingCaseAsset>> selectBestImages(List<Long> caseIds, String query, int limit) {
        if (caseIds == null || caseIds.isEmpty() || limit <= 0) {
            return Map.of();
        }
        LambdaQueryWrapper<TeachingCaseAsset> qw = new LambdaQueryWrapper<>();
        qw.in(TeachingCaseAsset::getCaseId, caseIds)
                .eq(TeachingCaseAsset::getType, "image")
                .eq(TeachingCaseAsset::getIsDelete, 0);
        List<TeachingCaseAsset> all = teachingCaseAssetMapper.selectList(qw);
        if (all == null || all.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> caseOrder = new LinkedHashMap<>();
        for (int i = 0; i < caseIds.size(); i++) {
            caseOrder.putIfAbsent(caseIds.get(i), i);
        }

        Set<String> tokens = queryTokens(query);
        List<TeachingCaseAsset> selected = all.stream()
                .filter(item -> StringUtils.isNotBlank(item.getUrl()))
                .sorted(Comparator
                        .comparingInt((TeachingCaseAsset asset) -> -assetScore(asset, tokens))
                        .thenComparingInt(asset -> caseOrder.getOrDefault(asset.getCaseId(), 999))
                        .thenComparingInt(asset -> asset.getSortOrder() == null ? 999 : asset.getSortOrder()))
                .limit(limit)
                .toList();

        Map<Long, List<TeachingCaseAsset>> grouped = new LinkedHashMap<>();
        for (TeachingCaseAsset asset : selected) {
            grouped.computeIfAbsent(asset.getCaseId(), key -> new ArrayList<>()).add(asset);
        }
        return grouped;
    }

    private int assetScore(TeachingCaseAsset asset, Set<String> tokens) {
        String haystack = StringUtils.joinWith(" ",
                asset.getTitle(),
                asset.getCaption(),
                asset.getContext()
        ).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                score += Math.min(token.length() * 3, 18);
            }
        }
        Integer order = asset.getSortOrder();
        return score + Math.max(0, 8 - (order == null ? 8 : order));
    }

    private Set<String> queryTokens(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : StringUtils.defaultString(query).toLowerCase(Locale.ROOT).split("[\\s,，。；;:：|/()（）\\[\\]{}]+")) {
            if (StringUtils.isNotBlank(token) && token.trim().length() >= 2) {
                tokens.add(token.trim());
            }
        }
        return tokens;
    }

    private void mergeMaterialJson(TeachingCase teachingCase, List<TeachingCaseAsset> assets) {
        try {
            ArrayNode merged = OBJECT_MAPPER.createArrayNode();
            JsonNode existing = OBJECT_MAPPER.readTree(StringUtils.defaultIfBlank(teachingCase.getMaterialJson(), "[]"));
            if (existing.isArray()) {
                for (JsonNode item : existing) {
                    String type = item.path("type").asText("");
                    String source = item.path("source").asText("");
                    if (!("image".equals(type) && "docx".equals(source))) {
                        merged.add(item);
                    }
                }
            }
            for (TeachingCaseAsset asset : assets) {
                ObjectNode node = merged.addObject();
                node.put("type", "image");
                node.put("title", StringUtils.defaultIfBlank(asset.getTitle(), "Case image"));
                node.put("url", asset.getUrl());
                node.put("source", StringUtils.defaultIfBlank(asset.getSource(), "docx"));
                node.put("caseId", asset.getCaseId());
                node.put("order", asset.getSortOrder() == null ? 0 : asset.getSortOrder());
                node.put("caption", StringUtils.defaultString(asset.getCaption()));
                node.put("context", StringUtils.defaultString(asset.getContext()));
            }
            TeachingCase update = new TeachingCase();
            update.setId(teachingCase.getId());
            update.setMaterialJson(merged.toString());
            teachingCaseMapper.updateById(update);
            teachingCase.setMaterialJson(merged.toString());
        } catch (Exception ignored) {
        }
    }

    private void softDeleteExistingImages(Long caseId) {
        LambdaUpdateWrapper<TeachingCaseAsset> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(TeachingCaseAsset::getIsDelete, 1)
                .eq(TeachingCaseAsset::getCaseId, caseId)
                .eq(TeachingCaseAsset::getType, "image")
                .eq(TeachingCaseAsset::getSource, "docx")
                .eq(TeachingCaseAsset::getIsDelete, 0);
        teachingCaseAssetMapper.update(null, wrapper);
    }

    private byte[] downloadBytes(String url) {
        return remoteResourceClient.downloadBytesOrEmpty(
                "teaching-case-asset",
                url,
                MAX_DOCX_BYTES,
                Duration.ofSeconds(20)
        );
    }

    private boolean isDocx(String url) {
        return StringUtils.defaultString(url).toLowerCase(Locale.ROOT).contains(".docx");
    }
}
