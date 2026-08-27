package com.ruyi.teach.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CourseGraphLinkMapper;
import com.ruyi.teach.mapper.CourseGraphNodeMapper;
import com.ruyi.teach.model.entity.CourseGraphLink;
import com.ruyi.teach.model.entity.CourseGraphNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程图谱默认数据加载器
 * 将 resources/seed/default-course-graph.json 中的数据批量导入数据库
 */
class CourseGraphSeeder {

    private static final String SEED_RESOURCE = "seed/default-course-graph.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    static void seed(CourseGraphNodeMapper nodeMapper, CourseGraphLinkMapper linkMapper) {
        ClassPathResource resource = new ClassPathResource(SEED_RESOURCE);
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "默认图谱数据文件不存在: " + SEED_RESOURCE);
        }

        try {
            Map<String, Object> data = MAPPER.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, Object>>() {});

            List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) data.get("nodes");
            List<Map<String, Object>> linkMaps = (List<Map<String, Object>>) data.get("links");

            if (nodeMaps == null || nodeMaps.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "默认图谱数据为空");
            }

            for (Map<String, Object> map : nodeMaps) {
                CourseGraphNode node = toNode(map);
                nodeMapper.insert(node);
            }

            if (linkMaps != null) {
                for (Map<String, Object> map : linkMaps) {
                    CourseGraphLink link = toLink(map);
                    linkMapper.insert(link);
                }
            }

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "默认图谱数据解析失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static CourseGraphNode toNode(Map<String, Object> map) {
        CourseGraphNode node = new CourseGraphNode();
        node.setId((String) map.get("id"));
        String parentId = (String) map.get("parentId");
        node.setParentId(StringUtils.isNotBlank(parentId) ? parentId : null);
        node.setName((String) map.get("name"));
        node.setCategory(StringUtils.defaultIfBlank((String) map.get("category"), "未分类"));

        Number symbolSize = (Number) map.get("symbolSize");
        node.setSymbolSize(symbolSize == null ? 30 : symbolSize.intValue());

        Number sortOrder = (Number) map.get("sortOrder");
        node.setSortOrder(sortOrder == null ? 0 : sortOrder.intValue());

        node.setDescription((String) map.get("description"));
        node.setLearnUrl((String) map.get("learnUrl"));
        node.setDifficulty((String) map.get("difficulty"));
        node.setImportance((String) map.get("importance"));

        Number estimatedHours = (Number) map.get("estimatedHours");
        node.setEstimatedHours(estimatedHours == null ? null : estimatedHours.intValue());

        Number teachingWeek = (Number) map.get("teachingWeek");
        node.setTeachingWeek(teachingWeek == null ? null : teachingWeek.intValue());

        Object isCoreVal = map.get("isCore");
        if (isCoreVal instanceof Boolean) {
            node.setIsCore((Boolean) isCoreVal ? 1 : 0);
        }

        Object isKeyPointVal = map.get("isKeyPoint");
        if (isKeyPointVal instanceof Boolean) {
            node.setIsKeyPoint((Boolean) isKeyPointVal ? 1 : 0);
        }

        node.setLearningContent((String) map.get("learningContent"));
        node.setIsDelete(0);
        return node;
    }

    @SuppressWarnings("unchecked")
    private static CourseGraphLink toLink(Map<String, Object> map) {
        CourseGraphLink link = new CourseGraphLink();
        link.setSource((String) map.get("source"));
        link.setTarget((String) map.get("target"));
        link.setRelationType((String) map.get("relationType"));
        link.setDescription((String) map.get("description"));

        Number sortOrder = (Number) map.get("sortOrder");
        link.setSortOrder(sortOrder == null ? 0 : sortOrder.intValue());

        link.setIsDelete(0);
        return link;
    }
}
