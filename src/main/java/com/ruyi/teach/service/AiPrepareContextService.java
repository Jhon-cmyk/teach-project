package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.mapper.AiResourceMapper;
import com.ruyi.teach.mapper.CourseGraphLinkMapper;
import com.ruyi.teach.mapper.CourseGraphNodeMapper;
import com.ruyi.teach.mapper.CourseGraphResourceLinkMapper;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.dto.PrepareAgentRequest;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.CourseGraphLink;
import com.ruyi.teach.model.entity.CourseGraphNode;
import com.ruyi.teach.model.entity.CourseGraphResourceLink;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.model.entity.TeachingCaseAsset;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.stereotype.Service
public class AiPrepareContextService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MIN_RELEVANCE_SCORE = 4;
    private static final int PLAN_GRAPH_NODE_LIMIT = 8;
    private static final int DEFAULT_GRAPH_NODE_LIMIT = 12;
    private static final int PLAN_RESOURCE_CANDIDATE_LIMIT = 20;
    private static final int DEFAULT_RESOURCE_CANDIDATE_LIMIT = 40;
    private static final int PLAN_RESOURCE_LIMIT = 6;
    private static final int DEFAULT_RESOURCE_LIMIT = 10;
    private static final int PLAN_CASE_IMAGE_LIMIT = 4;
    private static final int CASE_DIRECT_CONTENT_LIMIT = 12000;
    private static final int CASE_CHUNK_SIZE = 2600;
    private static final String GRAPH_POLICY_OFF = "off";
    private static final String GRAPH_POLICY_GRAPH_FIRST = "graphFirst";
    private static final String GRAPH_POLICY_RESOURCE_FIRST = "resourceFirst";
    private static final Map<String, String> CASE_PDF_TEXT_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WEAK_QUERY_TERMS = Set.of(
            "基础知识", "新授课", "复习课", "习题课", "实验课",
            "本科一年级", "本科二年级", "本科三年级", "本科四年级",
            "较弱", "一般", "较好", "标准版", "标准规范", "简洁实用", "详细展开",
            "讲授演示法", "案例教学法", "项目驱动法", "任务驱动法", "探究式学习", "合作学习",
            "课堂提问设计", "板书设计", "随堂练习", "分层任务", "概念抽象", "理解困难", "迁移应用弱", "计算易错",
            "无", "无特别要求"
    );
    private static final Set<String> AGENT_TYPES_WITHOUT_RETRIEVAL = Set.of(
            "anim", "anim_repair", "anim_optimize", "coding"
    );

    @Resource
    private AiResourceMapper aiResourceMapper;

    @Resource
    private CourseGraphNodeMapper courseGraphNodeMapper;

    @Resource
    private CourseGraphLinkMapper courseGraphLinkMapper;

    @Resource
    private CourseGraphResourceLinkMapper courseGraphResourceLinkMapper;

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private PlatformTeachingCaseService platformTeachingCaseService;

    @Resource
    private TeachingCaseAssetService teachingCaseAssetService;

    @Resource
    private RemoteDocumentTextService remoteDocumentTextService;

    public ObjectNode buildAgentPayload(Long teacherId, PrepareAgentRequest req) {
        req.setForm(normalizeForm(req));

        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("agentType", req.getAgentType());
        root.put("teacherId", teacherId);
        if (req.getCourseId() != null) {
            root.put("courseId", req.getCourseId());
        }
        root.set("form", OBJECT_MAPPER.valueToTree(req.getForm()));
        root.set("retrievalOptions", OBJECT_MAPPER.valueToTree(req.getRetrievalOptions() == null ? Map.of() : req.getRetrievalOptions()));
        if (StringUtils.isNotBlank(req.getSourceContent())) {
            root.put("sourceContent", req.getSourceContent());
        }

        ObjectNode context = root.putObject("context");
        ArrayNode graphNodes = context.putArray("graphNodes");
        ArrayNode resources = context.putArray("resources");
        ArrayNode cases = context.putArray("cases");
        if (!skipRetrieval(req.getAgentType()) && !isRetrievalOff(req)) {
            fillGraphNodes(graphNodes, teacherId, req);
            fillResources(resources, teacherId, req);
            fillCases(cases, teacherId, req);
        }
        return root;
    }

    private boolean skipRetrieval(String agentType) {
        return AGENT_TYPES_WITHOUT_RETRIEVAL.contains(StringUtils.defaultString(agentType).toLowerCase());
    }

    private boolean isRetrievalOff(PrepareAgentRequest req) {
        Map<String, Object> options = req.getRetrievalOptions();
        if (options == null) {
            return false;
        }
        return GRAPH_POLICY_OFF.equalsIgnoreCase(String.valueOf(options.getOrDefault("graphPolicy", "")))
                || GRAPH_POLICY_OFF.equalsIgnoreCase(String.valueOf(options.getOrDefault("mode", "")));
    }

    private boolean isPlanAgent(PrepareAgentRequest req) {
        return "plan".equalsIgnoreCase(StringUtils.defaultString(req.getAgentType()));
    }

    private void fillGraphNodes(ArrayNode target, Long teacherId, PrepareAgentRequest req) {
        List<CourseGraphNode> nodes;
        if (StringUtils.isNotBlank(req.getGraphNodeId())) {
            CourseGraphNode node = courseGraphNodeMapper.selectById(req.getGraphNodeId());
            nodes = node == null ? List.of() : List.of(node);
        } else {
            nodes = courseGraphNodeMapper.selectActiveNodes();
        }

        String query = buildQueryText(req);
        String graphPolicy = graphPolicy(req);
        int resultLimit = isPlanAgent(req) ? PLAN_GRAPH_NODE_LIMIT : DEFAULT_GRAPH_NODE_LIMIT;
        if (GRAPH_POLICY_GRAPH_FIRST.equals(graphPolicy)) {
            resultLimit += isPlanAgent(req) ? 4 : 6;
        } else if (GRAPH_POLICY_RESOURCE_FIRST.equals(graphPolicy)) {
            resultLimit = Math.max(2, resultLimit / 2);
        }
        int learningContentLimit = isPlanAgent(req) ? 1200 : 1800;
        List<CourseGraphNode> selectedNodes = nodes.stream()
                .filter(node -> node != null && !Objects.equals(node.getIsDelete(), 1))
                .map(node -> new ScoredNode(node, scoreNode(node, query)))
                .filter(item -> StringUtils.isNotBlank(req.getGraphNodeId()) || item.score >= MIN_RELEVANCE_SCORE)
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(resultLimit)
                .map(item -> item.node)
                .toList();

        Map<String, CourseGraphNode> nodeMap = buildNodeMap(nodes);
        Map<String, List<CourseGraphLink>> linkMap = buildGraphLinkMap();
        Map<String, List<AiResource>> mappedResourceMap = buildMappedResourceMap(teacherId, selectedNodes);
        selectedNodes
                .stream()
                .forEach(node -> {
                    ObjectNode item = target.addObject();
                    item.put("id", node.getId());
                    item.put("name", node.getName());
                    item.put("category", node.getCategory());
                    item.put("description", node.getDescription());
                    item.put("difficulty", node.getDifficulty());
                    item.put("importance", node.getImportance());
                    item.put("commonMistakes", node.getCommonMistakes());
                    item.put("teachingTips", node.getTeachingTips());
                    item.put("resourceSummary", node.getResourceSummary());
                    item.put("learningContent", truncate(node.getLearningContent(), learningContentLimit));
                    item.put("graphRelationText", buildGraphRelationText(node, nodeMap, linkMap));
                    item.put("mappedResourceText", buildMappedResourceText(mappedResourceMap.get(node.getId())));
                });
    }

    private String graphPolicy(PrepareAgentRequest req) {
        Map<String, Object> options = req.getRetrievalOptions();
        if (options == null) {
            return "auto";
        }
        String policy = String.valueOf(options.getOrDefault("graphPolicy", "auto"));
        if (GRAPH_POLICY_GRAPH_FIRST.equals(policy) || GRAPH_POLICY_RESOURCE_FIRST.equals(policy) || GRAPH_POLICY_OFF.equals(policy)) {
            return policy;
        }
        return "auto";
    }

    private Map<String, CourseGraphNode> buildNodeMap(List<CourseGraphNode> nodes) {
        Map<String, CourseGraphNode> result = new HashMap<>();
        if (nodes == null) {
            return result;
        }
        for (CourseGraphNode node : nodes) {
            if (node != null && StringUtils.isNotBlank(node.getId())) {
                result.put(node.getId(), node);
            }
        }
        return result;
    }

    private Map<String, List<CourseGraphLink>> buildGraphLinkMap() {
        Map<String, List<CourseGraphLink>> result = new HashMap<>();
        List<CourseGraphLink> links = courseGraphLinkMapper.selectActiveLinks();
        if (links == null) {
            return result;
        }
        for (CourseGraphLink link : links) {
            if (link == null) {
                continue;
            }
            if (StringUtils.isNotBlank(link.getSource())) {
                result.computeIfAbsent(link.getSource(), key -> new ArrayList<>()).add(link);
            }
            if (StringUtils.isNotBlank(link.getTarget())) {
                result.computeIfAbsent(link.getTarget(), key -> new ArrayList<>()).add(link);
            }
        }
        return result;
    }

    private Map<String, List<AiResource>> buildMappedResourceMap(Long teacherId, List<CourseGraphNode> nodes) {
        Map<String, List<AiResource>> result = new HashMap<>();
        if (teacherId == null || nodes == null || nodes.isEmpty()) {
            return result;
        }
        List<String> nodeIds = nodes.stream()
                .map(CourseGraphNode::getId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (nodeIds.isEmpty()) {
            return result;
        }
        List<CourseGraphResourceLink> links = courseGraphResourceLinkMapper.selectActiveByTeacherAndNodeIds(teacherId, nodeIds);
        if (links == null || links.isEmpty()) {
            return result;
        }
        List<Long> resourceIds = links.stream()
                .map(CourseGraphResourceLink::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (resourceIds.isEmpty()) {
            return result;
        }
        Map<Long, AiResource> resourceMap = new HashMap<>();
        for (AiResource resource : aiResourceMapper.selectBatchIds(resourceIds)) {
            if (resource != null && !Objects.equals(resource.getIsDelete(), 1)) {
                resourceMap.put(resource.getId(), resource);
            }
        }
        for (CourseGraphResourceLink link : links) {
            AiResource resource = resourceMap.get(link.getResourceId());
            if (resource != null) {
                result.computeIfAbsent(link.getNodeId(), key -> new ArrayList<>()).add(resource);
            }
        }
        return result;
    }

    private String buildGraphRelationText(CourseGraphNode node, Map<String, CourseGraphNode> nodeMap, Map<String, List<CourseGraphLink>> linkMap) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        CourseGraphNode parent = nodeMap.get(node.getParentId());
        if (parent != null) {
            parts.add("父节点：" + parent.getName());
        }
        for (CourseGraphNode candidate : nodeMap.values()) {
            if (StringUtils.equals(node.getId(), candidate.getParentId())) {
                parts.add("子节点：" + candidate.getName());
            }
        }
        for (CourseGraphLink link : linkMap.getOrDefault(node.getId(), List.of())) {
            String neighborId = StringUtils.equals(node.getId(), link.getSource()) ? link.getTarget() : link.getSource();
            CourseGraphNode neighbor = nodeMap.get(neighborId);
            if (neighbor == null) {
                continue;
            }
            parts.add("关联节点：" + neighbor.getName()
                    + "（" + StringUtils.defaultIfBlank(link.getRelationType(), "related") + "）"
                    + StringUtils.defaultString(link.getDescription()));
        }
        return parts.isEmpty() ? "" : String.join("；", parts);
    }

    private String buildMappedResourceText(List<AiResource> resources) {
        if (resources == null || resources.isEmpty()) {
            return "";
        }
        return resources.stream()
                .limit(8)
                .map(resource -> StringUtils.defaultString(resource.getType()) + " " + StringUtils.defaultString(resource.getTitle()))
                .reduce((a, b) -> a + "；" + b)
                .orElse("");
    }

    private void fillResources(ArrayNode target, Long teacherId, PrepareAgentRequest req) {
        String query = buildQueryText(req);
        int candidateLimit = isPlanAgent(req) ? PLAN_RESOURCE_CANDIDATE_LIMIT : DEFAULT_RESOURCE_CANDIDATE_LIMIT;
        int resultLimit = isPlanAgent(req) ? PLAN_RESOURCE_LIMIT : DEFAULT_RESOURCE_LIMIT;
        String graphPolicy = graphPolicy(req);
        if (GRAPH_POLICY_RESOURCE_FIRST.equals(graphPolicy)) {
            candidateLimit += isPlanAgent(req) ? 12 : 20;
            resultLimit += isPlanAgent(req) ? 2 : 4;
        } else if (GRAPH_POLICY_GRAPH_FIRST.equals(graphPolicy)) {
            resultLimit = Math.max(3, resultLimit - 2);
        }
        int contentLimit = isPlanAgent(req) ? 1600 : 2400;
        int paramsLimit = isPlanAgent(req) ? 800 : 1200;
        LambdaQueryWrapper<AiResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiResource::getTeacherId, teacherId)
                .eq(AiResource::getIsDelete, 0)
                .in(AiResource::getType, "plan", "quiz", "anim", "coding", "micro_video")
                .orderByDesc(AiResource::getUpdateTime)
                .last("limit " + candidateLimit);

        List<AiResource> resources = aiResourceMapper.selectList(wrapper);
        resources.stream()
                .map(resource -> new ScoredResource(resource, scoreText(
                        query,
                        resource.getTitle() + "\n" + resource.getContent() + "\n" + resource.getParamsJson()
                )))
                .filter(item -> item.score >= MIN_RELEVANCE_SCORE)
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(resultLimit)
                .map(item -> item.resource)
                .forEach(resource -> {
                    ObjectNode item = target.addObject();
                    item.put("id", resource.getId());
                    item.put("type", resource.getType());
                    item.put("title", resource.getTitle());
                    item.put("content", truncate(resource.getContent(), contentLimit));
                    item.put("paramsJson", truncate(resource.getParamsJson(), paramsLimit));
                    putResourceCourseMetadata(item, resource.getParamsJson());
                });
    }

    private void putResourceCourseMetadata(ObjectNode target, String paramsJson) {
        if (StringUtils.isBlank(paramsJson)) {
            return;
        }
        try {
            JsonNode params = OBJECT_MAPPER.readTree(paramsJson);
            if (params == null || !params.isObject()) {
                return;
            }
            JsonNode courseId = params.get("courseId");
            if (courseId != null && courseId.canConvertToLong() && courseId.asLong() > 0) {
                target.put("courseId", courseId.asLong());
            }
            String courseName = params.path("courseName").asText("");
            if (StringUtils.isBlank(courseName)) {
                courseName = params.path("subject").asText("");
            }
            if (StringUtils.isNotBlank(courseName)) {
                target.put("courseName", truncate(courseName, 200));
            }
        } catch (Exception ignored) {
            // paramsJson is optional legacy data; invalid JSON must not break preparation.
        }
    }

    private void fillCases(ArrayNode target, Long teacherId, PrepareAgentRequest req) {
        List<Long> caseIds = requestedCaseIds(req);
        if (caseIds.isEmpty() && isPlanAgent(req) && autoCaseEnabled(req)) {
            caseIds = platformTeachingCaseService.autoRecommendCaseIds(buildCaseRecommendRequest(req), teacherId);
        }
        if (caseIds.isEmpty()) {
            return;
        }

        List<TeachingCase> usableCases = new ArrayList<>();
        for (Long caseId : caseIds) {
            TeachingCase teachingCase = teachingCaseMapper.selectById(caseId);
            if (!canUseCase(teachingCase, teacherId)) {
                continue;
            }
            usableCases.add(teachingCase);
        }

        if (isPlanAgent(req)) {
            for (TeachingCase teachingCase : usableCases) {
                teachingCaseAssetService.ensureCaseImages(teachingCase);
            }
        }

        Map<Long, List<TeachingCaseAsset>> imageMap = teachingCaseAssetService.selectBestImages(
                usableCases.stream().map(TeachingCase::getId).toList(),
                buildQueryText(req),
                isPlanAgent(req) ? PLAN_CASE_IMAGE_LIMIT : 0
        );

        for (TeachingCase teachingCase : usableCases) {
            String caseText = getCachedCaseText(teachingCase);
            boolean parseOk = StringUtils.isNotBlank(caseText);

            ObjectNode item = target.addObject();
            item.put("id", teachingCase.getId());
            item.put("title", teachingCase.getTitle());
            item.put("category", teachingCase.getCategory());
            item.put("difficulty", teachingCase.getDifficulty());
            item.put("courseName", teachingCase.getCourseName());
            item.put("scope", StringUtils.defaultIfBlank(teachingCase.getScope(), PlatformTeachingCaseService.SCOPE_MINE));
            item.put("sourceName", StringUtils.defaultString(teachingCase.getSourceName()));
            item.put("sourceUrl", StringUtils.defaultString(teachingCase.getSourceUrl()));
            item.put("summary", StringUtils.defaultString(teachingCase.getSummary()));
            item.put("materialJson", StringUtils.defaultString(teachingCase.getMaterialJson()));
            item.put("structureJson", StringUtils.defaultString(teachingCase.getStructureJson()));
            item.put("pdfParseOk", parseOk);
            item.put("documentParseOk", parseOk);
            item.put("parseStatus", parseOk ? "ok" : "failed");
            item.put("content", truncate(caseText, CASE_DIRECT_CONTENT_LIMIT));
            item.put("contentLength", caseText == null ? 0 : caseText.length());
            ArrayNode chunks = item.putArray("contentChunks");
            if (parseOk) {
                for (String chunk : splitText(caseText, CASE_CHUNK_SIZE)) {
                    chunks.add(chunk);
                }
            }
            item.put("chunkCount", chunks.size());
            ArrayNode imageMaterials = item.putArray("imageMaterials");
            for (TeachingCaseAsset asset : imageMap.getOrDefault(teachingCase.getId(), List.of())) {
                ObjectNode image = imageMaterials.addObject();
                image.put("id", asset.getId());
                image.put("caseId", asset.getCaseId());
                image.put("type", StringUtils.defaultString(asset.getType()));
                image.put("url", asset.getUrl());
                image.put("title", StringUtils.defaultString(asset.getTitle()));
                image.put("caption", StringUtils.defaultString(asset.getCaption()));
                image.put("context", truncate(asset.getContext(), 600));
                image.put("sortOrder", asset.getSortOrder() == null ? 0 : asset.getSortOrder());
                image.put("width", asset.getWidth() == null ? 0 : asset.getWidth());
                image.put("height", asset.getHeight() == null ? 0 : asset.getHeight());
                image.put("source", StringUtils.defaultString(asset.getSource()));
            }
        }
    }

    private List<Long> requestedCaseIds(PrepareAgentRequest req) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (req.getCaseIds() != null) {
            for (Long id : req.getCaseIds()) {
                if (id != null) {
                    ids.add(id);
                }
                if (ids.size() >= 3) {
                    break;
                }
            }
        }
        if (ids.isEmpty() && req.getCaseId() != null) {
            ids.add(req.getCaseId());
        }
        return new ArrayList<>(ids);
    }

    private boolean autoCaseEnabled(PrepareAgentRequest req) {
        Map<String, Object> options = req.getRetrievalOptions();
        if (options == null || !options.containsKey("autoCase")) {
            return true;
        }
        Object value = options.get("autoCase");
        return !Boolean.FALSE.equals(value) && !"false".equalsIgnoreCase(String.valueOf(value));
    }

    private PlatformTeachingCaseService.RecommendRequest buildCaseRecommendRequest(PrepareAgentRequest req) {
        PlatformTeachingCaseService.RecommendRequest recommendRequest = new PlatformTeachingCaseService.RecommendRequest();
        Map<String, Object> form = req.getForm() == null ? Map.of() : req.getForm();
        recommendRequest.setSubject(stringValue(form.get("subject")));
        recommendRequest.setGrade(stringValue(form.get("grade")));
        recommendRequest.setTopic(stringValue(form.get("topic")));
        recommendRequest.setLessonType(stringValue(form.get("lessonType")));
        recommendRequest.setCourseName(stringValue(firstNonNull(form.get("courseName"), form.get("subject"))));
        return recommendRequest;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean canUseCase(TeachingCase teachingCase, Long teacherId) {
        if (teachingCase == null || Objects.equals(teachingCase.getIsDelete(), 1)) {
            return false;
        }
        String scope = StringUtils.defaultIfBlank(teachingCase.getScope(), PlatformTeachingCaseService.SCOPE_MINE);
        if (PlatformTeachingCaseService.SCOPE_PLATFORM.equals(scope)) {
            return PlatformTeachingCaseService.STATUS_APPROVED.equals(StringUtils.defaultString(teachingCase.getStatus()));
        }
        return Objects.equals(teachingCase.getTeacherId(), teacherId);
    }

    private String getCachedCaseText(TeachingCase teachingCase) {
        String cacheKey = String.join(":",
                String.valueOf(teachingCase.getId()),
                StringUtils.defaultString(teachingCase.getPdfUrl()),
                teachingCase.getUpdateTime() == null ? "" : String.valueOf(teachingCase.getUpdateTime().getTime())
        );
        return CASE_PDF_TEXT_CACHE.computeIfAbsent(
                cacheKey,
                key -> remoteDocumentTextService.extractText(teachingCase.getPdfUrl())
        );
    }

    private Map<String, Object> normalizeForm(PrepareAgentRequest req) {
        Map<String, Object> form = new LinkedHashMap<>(req.getForm() == null ? Map.of() : req.getForm());
        if (!"plan".equals(req.getAgentType())) {
            return form;
        }

        Set<String> selectedMethods = toStringSet(firstNonNull(form.get("selectedMethods"), form.get("methods")));
        Set<String> selectedActivities = toStringSet(firstNonNull(form.get("selectedActivities"), form.get("activities")));
        Set<String> excludedSections = toStringSet(form.get("excludedSections"));

        if (!selectedActivities.contains("板书设计")) {
            excludedSections.add("板书设计");
        }

        String extraRequirements = String.valueOf(form.getOrDefault("extraRequirements", ""));
        if (!extraRequirements.matches(".*(教学反思|课后反思|反思|复盘).*")) {
            excludedSections.addAll(Arrays.asList("教学反思", "教学效果评价", "改进方向", "课后反思"));
        }

        form.put("selectedMethods", new ArrayList<>(selectedMethods));
        form.put("selectedActivities", new ArrayList<>(selectedActivities));
        form.put("excludedSections", new ArrayList<>(excludedSections));
        return form;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private Set<String> toStringSet(Object value) {
        Set<String> result = new LinkedHashSet<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
            for (String item : String.valueOf(value).split("[,，、/|\\s]+")) {
                if (StringUtils.isNotBlank(item)) {
                    result.add(item.trim());
                }
            }
        }
        return result;
    }

    private String buildQueryText(PrepareAgentRequest req) {
        List<String> parts = new ArrayList<>();
        if (req.getForm() != null) {
            addQueryField(parts, req.getForm().get("subject"));
            addQueryField(parts, req.getForm().get("topic"));
            addQueryField(parts, req.getForm().get("knowledgePoints"));
            addQueryField(parts, req.getForm().get("description"));
            addQueryField(parts, req.getForm().get("concept"));
            addQueryField(parts, req.getForm().get("emphasis"));
            addQueryField(parts, req.getForm().get("teachingGoal"));
            addQueryField(parts, req.getForm().get("extraRequirements"));
        }
        if (StringUtils.isNotBlank(req.getSourceContent())) {
            parts.add(req.getSourceContent());
        }
        return String.join(" ", parts);
    }

    private void addQueryField(List<String> parts, Object value) {
        if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
            parts.add(String.valueOf(value));
        }
    }

    private int scoreNode(CourseGraphNode node, String query) {
        return scoreText(query, String.join("\n",
                StringUtils.defaultString(node.getName()),
                StringUtils.defaultString(node.getDescription()),
                StringUtils.defaultString(node.getCommonMistakes()),
                StringUtils.defaultString(node.getTeachingTips()),
                StringUtils.defaultString(node.getLearningContent())
        ));
    }

    private int scoreText(String query, String text) {
        if (StringUtils.isBlank(query) || StringUtils.isBlank(text)) {
            return 0;
        }
        String normalizedText = text.toLowerCase();
        int score = 0;
        for (String token : queryTokens(query)) {
            if (normalizedText.contains(token)) {
                score += Math.min(token.length() * 2, 16);
            }
        }
        return score;
    }

    private Set<String> queryTokens(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        if (StringUtils.isBlank(query)) {
            return tokens;
        }
        for (String token : query.toLowerCase().split("[\\s,，、;；:：\\[\\]{}()（）\"'|/]+")) {
            token = token.trim();
            if (token.length() >= 2 && !WEAK_QUERY_TERMS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private List<String> splitText(String text, int chunkSize) {
        if (StringUtils.isBlank(text)) {
            return List.of();
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= chunkSize) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int overlap = Math.min(220, Math.max(80, chunkSize / 10));
        while (start < normalized.length()) {
            int end = Math.min(start + chunkSize, normalized.length());
            int cut = findSentenceCut(normalized, start, end, chunkSize);
            if (cut > start) {
                end = cut;
            }

            String chunk = normalized.substring(start, end).trim();
            if (StringUtils.isNotBlank(chunk)) {
                chunks.add(chunk);
            }

            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private int findSentenceCut(String text, int start, int end, int chunkSize) {
        if (end >= text.length()) {
            return end;
        }

        int minCut = start + (int) (chunkSize * 0.58);
        int best = -1;
        for (String mark : List.of("。", "！", "？", ";", "；", ".", "\n")) {
            int index = text.lastIndexOf(mark, end - 1);
            if (index >= minCut && index > best) {
                best = index + mark.length();
            }
        }
        return best;
    }

    private record ScoredNode(CourseGraphNode node, int score) {
    }

    private record ScoredResource(AiResource resource, int score) {
    }

}
