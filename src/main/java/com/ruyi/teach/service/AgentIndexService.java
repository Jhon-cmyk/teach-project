package com.ruyi.teach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.AiAgentClient;
import com.ruyi.teach.mapper.AiResourceLiteMapper;
import com.ruyi.teach.mapper.CourseGraphLinkMapper;
import com.ruyi.teach.mapper.CourseGraphNodeMapper;
import com.ruyi.teach.mapper.CourseGraphResourceLinkMapper;
import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.AiResourceLite;
import com.ruyi.teach.model.entity.CourseGraphLink;
import com.ruyi.teach.model.entity.CourseGraphNode;
import com.ruyi.teach.model.entity.CourseGraphResourceLink;
import com.ruyi.teach.model.entity.TeachingCase;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AgentIndexService {

    private static final Logger log = LoggerFactory.getLogger(AgentIndexService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int CASE_INDEX_CONTENT_LIMIT = 120_000;

    @Resource
    private AiAgentClient aiAgentClient;

    @Resource
    private RemoteDocumentTextService remoteDocumentTextService;

    @Resource
    private AgentIndexTaskDispatcher agentIndexTaskDispatcher;

    @Resource
    private CourseGraphNodeMapper courseGraphNodeMapper;

    @Resource
    private CourseGraphLinkMapper courseGraphLinkMapper;

    @Resource
    private CourseGraphResourceLinkMapper courseGraphResourceLinkMapper;

    @Resource
    private AiResourceLiteMapper aiResourceLiteMapper;

    public void upsertAiResource(AiResource resource) {
        if (resource == null || resource.getId() == null || StringUtils.isBlank(resource.getContent())) {
            return;
        }
        ObjectNode doc = baseDoc(resource.getTeacherId(), "ai_resource", String.valueOf(resource.getId()));
        doc.put("title", StringUtils.defaultIfBlank(resource.getTitle(), "AI resource"));
        doc.put("content", StringUtils.joinWith("\n", resource.getContent(), resource.getParamsJson()));
        doc.put("resourceType", StringUtils.defaultIfBlank(resource.getType(), "ai_resource"));
        doc.put("updatedAt", dateValue(resource.getUpdateTime()));
        doc.put("scope", "mine");
        putResourceCourseMetadata(doc, resource.getParamsJson());
        upsertDocuments(doc);
    }

    public void deleteAiResource(Long teacherId, Long resourceId) {
        if (resourceId == null) {
            return;
        }
        deleteDocument(teacherId, "ai_resource", String.valueOf(resourceId));
    }

    public void upsertTeachingCase(TeachingCase teachingCase) {
        if (teachingCase == null || teachingCase.getId() == null) {
            return;
        }
        ObjectNode doc = caseDocument(teachingCase.getTeacherId(), teachingCase);
        upsertDocuments(doc);
    }

    public void deleteTeachingCase(Long teacherId, Long caseId) {
        if (caseId == null) {
            return;
        }
        deleteDocument(teacherId, "case", String.valueOf(caseId), "mine");
        deleteDocument(null, "case", String.valueOf(caseId), "platform");
    }

    public void rebuildTeachingCases(Long teacherId, Collection<TeachingCase> teachingCases) {
        deleteDocument(teacherId, "case", null);
        rebuild(teacherId, null, null, teachingCases);
    }

    public void upsertGraphNode(Long teacherId, String nodeId) {
        if (StringUtils.isBlank(nodeId)) {
            return;
        }
        CourseGraphNode node = courseGraphNodeMapper.selectById(nodeId);
        if (node == null || Objects.equals(node.getIsDelete(), 1)) {
            deleteGraphNode(teacherId, nodeId);
            return;
        }
        GraphIndexContext context = buildGraphIndexContext(teacherId, Collections.singletonList(node));
        upsertDocuments(graphNodeDocument(teacherId, node, context));
    }

    public void upsertGraphNodes(Long teacherId, Collection<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }
        List<CourseGraphNode> nodes = nodeIds.stream()
                .filter(StringUtils::isNotBlank)
                .map(courseGraphNodeMapper::selectById)
                .filter(node -> node != null && !Objects.equals(node.getIsDelete(), 1))
                .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return;
        }
        GraphIndexContext context = buildGraphIndexContext(teacherId, nodes);
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        ArrayNode docs = payload.putArray("documents");
        for (CourseGraphNode node : nodes) {
            docs.add(graphNodeDocument(teacherId, node, context));
        }
        post("/agent/index/upsert", payload);
    }

    public void deleteGraphNode(Long teacherId, String nodeId) {
        if (StringUtils.isBlank(nodeId)) {
            return;
        }
        deleteDocument(teacherId, "graph_node", nodeId);
    }

    public void deleteGraphNodes(Long teacherId, Collection<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }
        for (String nodeId : nodeIds) {
            deleteGraphNode(teacherId, nodeId);
        }
    }

    public void rebuild(Long teacherId,
                        Collection<AiResource> resources,
                        Collection<CourseGraphNode> graphNodes,
                        Collection<TeachingCase> teachingCases) {
        ArrayNode docs = OBJECT_MAPPER.createArrayNode();
        GraphIndexContext graphContext = buildGraphIndexContext(teacherId, graphNodes);
        if (graphNodes != null) {
            for (CourseGraphNode node : graphNodes) {
                if (node == null || node.getId() == null) {
                    continue;
                }
                docs.add(graphNodeDocument(teacherId, node, graphContext));
            }
        }
        if (resources != null) {
            for (AiResource resource : resources) {
                if (resource == null || resource.getId() == null || StringUtils.isBlank(resource.getContent())) {
                    continue;
                }
                ObjectNode doc = baseDoc(resource.getTeacherId(), "ai_resource", String.valueOf(resource.getId()));
                doc.put("title", StringUtils.defaultIfBlank(resource.getTitle(), "AI resource"));
                doc.put("content", StringUtils.joinWith("\n", resource.getContent(), resource.getParamsJson()));
                doc.put("resourceType", StringUtils.defaultIfBlank(resource.getType(), "ai_resource"));
                doc.put("updatedAt", dateValue(resource.getUpdateTime()));
                doc.put("scope", "mine");
                putResourceCourseMetadata(doc, resource.getParamsJson());
                docs.add(doc);
            }
        }
        if (teachingCases != null) {
            for (TeachingCase teachingCase : teachingCases) {
                if (teachingCase == null || teachingCase.getId() == null) {
                    continue;
                }
                docs.add(caseDocument(teacherId, teachingCase));
            }
        }
        if (docs.size() > 0) {
            ObjectNode payload = OBJECT_MAPPER.createObjectNode();
            payload.set("documents", docs);
            post("/agent/index/upsert", payload);
        }
    }

    private ObjectNode baseDoc(Long teacherId, String sourceType, String sourceId) {
        ObjectNode doc = OBJECT_MAPPER.createObjectNode();
        if (teacherId != null) {
            doc.put("teacherId", teacherId);
        }
        doc.put("sourceType", sourceType);
        doc.put("sourceId", sourceId);
        return doc;
    }

    private ObjectNode caseDocument(Long teacherId, TeachingCase teachingCase) {
        String documentText = remoteDocumentTextService.extractText(teachingCase.getPdfUrl());
        if (StringUtils.isBlank(documentText)) {
            documentText = teachingCase.getPreviewText();
        }
        boolean parseOk = StringUtils.isNotBlank(documentText);
        ObjectNode doc = baseDoc(teacherId, "case", String.valueOf(teachingCase.getId()));
        doc.put("title", StringUtils.defaultIfBlank(teachingCase.getTitle(), "Teaching case"));
        doc.put("content", truncate(StringUtils.joinWith("\n",
                teachingCase.getTitle(),
                teachingCase.getCategory(),
                teachingCase.getDifficulty(),
                teachingCase.getCourseName(),
                teachingCase.getSummary(),
                teachingCase.getKeywords(),
                parseOk ? documentText : ""
        ), CASE_INDEX_CONTENT_LIMIT));
        doc.put("resourceType", "case");
        doc.put("courseName", StringUtils.defaultString(teachingCase.getCourseName()));
        doc.put("updatedAt", dateValue(teachingCase.getUpdateTime()));
        doc.put("scope", StringUtils.defaultIfBlank(teachingCase.getScope(), "mine"));
        doc.put("status", StringUtils.defaultString(teachingCase.getStatus()));
        doc.put("sourceUrl", StringUtils.defaultString(teachingCase.getSourceUrl()));
        doc.put("sourceName", StringUtils.defaultString(teachingCase.getSourceName()));
        doc.put("summary", StringUtils.defaultString(teachingCase.getSummary()));
        doc.put("materialJson", StringUtils.defaultString(teachingCase.getMaterialJson()));
        doc.put("structureJson", StringUtils.defaultString(teachingCase.getStructureJson()));
        doc.put("pdfParseOk", parseOk);
        doc.put("documentParseOk", parseOk);
        doc.put("parseStatus", parseOk ? "ok" : "failed");
        return doc;
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
            // Legacy paramsJson may be invalid; indexing the resource body still has value.
        }
    }

    private ObjectNode graphNodeDocument(Long teacherId, CourseGraphNode node, GraphIndexContext context) {
        String relationText = graphRelationText(node, context);
        String mappedResourceText = mappedResourceText(node, context);
        ObjectNode doc = baseDoc(teacherId, "graph_node", node.getId());
        doc.put("title", StringUtils.defaultIfBlank(node.getName(), "Knowledge node"));
        doc.put("content", StringUtils.joinWith("\n",
                "知识点：" + StringUtils.defaultString(node.getName()),
                "分类：" + StringUtils.defaultString(node.getCategory()),
                "简介：" + StringUtils.defaultString(node.getDescription()),
                "难度：" + StringUtils.defaultString(node.getDifficulty()),
                "重要性：" + StringUtils.defaultString(node.getImportance()),
                "易错点：" + StringUtils.defaultString(node.getCommonMistakes()),
                "教学建议：" + StringUtils.defaultString(node.getTeachingTips()),
                "资源摘要：" + StringUtils.defaultString(node.getResourceSummary()),
                "学习内容：" + StringUtils.defaultString(node.getLearningContent()),
                relationText,
                mappedResourceText
        ));
        doc.put("resourceType", "graph_node");
        doc.put("graphNodeId", node.getId());
        doc.put("graphRelationText", relationText);
        doc.put("mappedResourceText", mappedResourceText);
        doc.put("updatedAt", dateValue(node.getUpdateTime()));
        doc.put("scope", teacherId == null ? "platform" : "mine");
        return doc;
    }

    private GraphIndexContext buildGraphIndexContext(Long teacherId, Collection<CourseGraphNode> targetNodes) {
        List<CourseGraphNode> allNodes = courseGraphNodeMapper.selectActiveNodes();
        Map<String, CourseGraphNode> nodeMap = allNodes == null
                ? new HashMap<>()
                : allNodes.stream()
                .filter(node -> node != null && StringUtils.isNotBlank(node.getId()))
                .collect(Collectors.toMap(CourseGraphNode::getId, item -> item, (a, b) -> a));

        List<CourseGraphLink> links = courseGraphLinkMapper.selectActiveLinks();
        Map<String, List<CourseGraphLink>> linkMap = new HashMap<>();
        if (links != null) {
            for (CourseGraphLink link : links) {
                if (link == null) {
                    continue;
                }
                if (StringUtils.isNotBlank(link.getSource())) {
                    linkMap.computeIfAbsent(link.getSource(), key -> new ArrayList<>()).add(link);
                }
                if (StringUtils.isNotBlank(link.getTarget())) {
                    linkMap.computeIfAbsent(link.getTarget(), key -> new ArrayList<>()).add(link);
                }
            }
        }

        Map<String, List<AiResourceLite>> resourceMap = buildMappedResourceMap(teacherId, targetNodes);
        return new GraphIndexContext(nodeMap, linkMap, resourceMap);
    }

    private Map<String, List<AiResourceLite>> buildMappedResourceMap(Long teacherId, Collection<CourseGraphNode> nodes) {
        if (teacherId == null || nodes == null || nodes.isEmpty()) {
            return new HashMap<>();
        }
        List<String> nodeIds = nodes.stream()
                .filter(Objects::nonNull)
                .map(CourseGraphNode::getId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (nodeIds.isEmpty()) {
            return new HashMap<>();
        }
        List<CourseGraphResourceLink> links = courseGraphResourceLinkMapper.selectActiveByTeacherAndNodeIds(teacherId, nodeIds);
        if (links == null || links.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> resourceIds = links.stream()
                .map(CourseGraphResourceLink::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (resourceIds.isEmpty()) {
            return new HashMap<>();
        }
        List<AiResourceLite> resources = aiResourceLiteMapper.selectActiveByIds(resourceIds);
        Map<Long, AiResourceLite> byId = resources == null
                ? new HashMap<>()
                : resources.stream()
                .collect(Collectors.toMap(AiResourceLite::getId, item -> item, (a, b) -> a));
        Map<String, List<AiResourceLite>> result = new HashMap<>();
        for (CourseGraphResourceLink link : links) {
            AiResourceLite resource = byId.get(link.getResourceId());
            if (resource != null) {
                result.computeIfAbsent(link.getNodeId(), key -> new ArrayList<>()).add(resource);
            }
        }
        return result;
    }

    private String graphRelationText(CourseGraphNode node, GraphIndexContext context) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        CourseGraphNode parent = context.nodeMap.get(node.getParentId());
        if (parent != null) {
            parts.add("父节点：" + parent.getName());
        }
        for (CourseGraphNode candidate : context.nodeMap.values()) {
            if (StringUtils.equals(node.getId(), candidate.getParentId())) {
                parts.add("子节点：" + candidate.getName());
            }
        }
        for (CourseGraphLink link : context.linkMap.getOrDefault(node.getId(), Collections.emptyList())) {
            String neighborId = StringUtils.equals(node.getId(), link.getSource()) ? link.getTarget() : link.getSource();
            CourseGraphNode neighbor = context.nodeMap.get(neighborId);
            if (neighbor == null) {
                continue;
            }
            String relation = StringUtils.defaultIfBlank(link.getRelationType(), "related");
            String description = StringUtils.defaultString(link.getDescription());
            parts.add("关联节点：" + neighbor.getName() + "（" + relation + "）" + description);
        }
        if (parts.isEmpty()) {
            return "";
        }
        return "图谱关系：" + String.join("；", parts);
    }

    private String mappedResourceText(CourseGraphNode node, GraphIndexContext context) {
        List<AiResourceLite> resources = context.resourceMap.getOrDefault(node.getId(), Collections.emptyList());
        if (resources.isEmpty()) {
            return "";
        }
        return "绑定资源：" + resources.stream()
                .limit(8)
                .map(resource -> StringUtils.defaultString(resource.getType()) + " " + StringUtils.defaultString(resource.getTitle()))
                .collect(Collectors.joining("；"));
    }

    private void upsertDocuments(ObjectNode doc) {
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        ArrayNode docs = payload.putArray("documents");
        docs.add(doc);
        post("/agent/index/upsert", payload);
    }

    private void deleteDocument(Long teacherId, String sourceType, String sourceId) {
        deleteDocument(teacherId, sourceType, sourceId, null);
    }

    private void deleteDocument(Long teacherId,
                                String sourceType,
                                String sourceId,
                                String scope) {
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        if (teacherId != null) {
            payload.put("teacherId", teacherId);
        }
        payload.put("sourceType", sourceType);
        if (sourceId != null) {
            payload.put("sourceId", sourceId);
        }
        if (StringUtils.isNotBlank(scope)) {
            payload.put("scope", scope);
        }
        post("/agent/index/delete", payload);
    }

    private void post(String path, ObjectNode payload) {
        ObjectNode immutablePayload = payload.deepCopy();
        agentIndexTaskDispatcher.dispatchAfterCommit(
                () -> aiAgentClient.syncIndex(path, immutablePayload)
        );
    }

    private String dateValue(Date date) {
        return date == null ? "" : String.valueOf(date.getTime());
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private record GraphIndexContext(
            Map<String, CourseGraphNode> nodeMap,
            Map<String, List<CourseGraphLink>> linkMap,
            Map<String, List<AiResourceLite>> resourceMap
    ) {
    }
}
