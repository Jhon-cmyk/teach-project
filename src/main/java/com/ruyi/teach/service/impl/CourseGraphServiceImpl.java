package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AiResourceLiteMapper;
import com.ruyi.teach.mapper.ClassAnalysisRecordLiteMapper;
import com.ruyi.teach.mapper.CommunityFeaturedAnswerLiteMapper;
import com.ruyi.teach.mapper.CodingProblemMapper;
import com.ruyi.teach.mapper.CodingSubmissionMapper;
import com.ruyi.teach.mapper.CommunityPostLiteMapper;
import com.ruyi.teach.mapper.CourseGraphLinkMapper;
import com.ruyi.teach.mapper.CourseGraphNodeActivityMapper;
import com.ruyi.teach.mapper.CourseGraphNodeMapper;
import com.ruyi.teach.mapper.CourseGraphNodeProgressMapper;
import com.ruyi.teach.mapper.CourseGraphPreferenceMapper;
import com.ruyi.teach.mapper.CourseGraphResourceLinkMapper;
import com.ruyi.teach.mapper.HomeworkAssignmentMapper;
import com.ruyi.teach.mapper.HomeworkSubmissionMapper;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphLinkCreateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphNodeCreateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphNodeUpdateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphPreferenceUpdateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphResourceBindRequest;
import com.ruyi.teach.model.entity.AiResourceLite;
import com.ruyi.teach.model.entity.ClassAnalysisRecordLite;
import com.ruyi.teach.model.entity.CodingProblem;
import com.ruyi.teach.model.entity.CodingSubmission;
import com.ruyi.teach.model.entity.CommunityFeaturedAnswerLite;
import com.ruyi.teach.model.entity.CommunityPostLite;
import com.ruyi.teach.model.entity.CourseGraphLink;
import com.ruyi.teach.model.entity.CourseGraphNode;
import com.ruyi.teach.model.entity.CourseGraphPreference;
import com.ruyi.teach.model.entity.CourseGraphResourceLink;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmission;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphAnalysisFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphAnalysisMetricVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphCategoryVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphMaterialVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphQuizVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphClassFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphCommunityDeskFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphCommunityFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphCommunityPostItemVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphDataVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphFocusedResourceItemVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphLinkVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphNodeVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphPreferenceVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphResourceFocusVO;
import com.ruyi.teach.service.AgentIndexService;
import com.ruyi.teach.service.CourseGraphService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CourseGraphServiceImpl implements CourseGraphService {

    @Resource
    private CourseGraphNodeMapper courseGraphNodeMapper;

    @Resource
    private CourseGraphLinkMapper courseGraphLinkMapper;

    @Resource
    private CourseGraphPreferenceMapper courseGraphPreferenceMapper;

    @Resource
    private CourseGraphResourceLinkMapper courseGraphResourceLinkMapper;

    @Resource
    private AiResourceLiteMapper aiResourceLiteMapper;

    @Resource
    private ClassAnalysisRecordLiteMapper classAnalysisRecordLiteMapper;

    @Resource
    private CommunityPostLiteMapper communityPostLiteMapper;

    @Resource
    private CommunityFeaturedAnswerLiteMapper communityFeaturedAnswerLiteMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CourseGraphNodeActivityMapper courseGraphNodeActivityMapper;

    @Resource
    private CourseGraphNodeProgressMapper courseGraphNodeProgressMapper;

    @Resource
    private HomeworkAssignmentMapper homeworkAssignmentMapper;

    @Resource
    private HomeworkSubmissionMapper homeworkSubmissionMapper;

    @Resource
    private CodingProblemMapper codingProblemMapper;

    @Resource
    private CodingSubmissionMapper codingSubmissionMapper;

    @Resource
    private AgentIndexService agentIndexService;

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {};
    private static final long RECENT_WINDOW_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long COMMUNITY_RECENT_WINDOW_MS = 7L * 24 * 60 * 60 * 1000;
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Override
    public CourseGraphDataVO getGraphData(Long teacherId) {
        List<CourseGraphNode> nodeEntities = courseGraphNodeMapper.selectActiveNodes();
        List<CourseGraphLink> linkEntities = courseGraphLinkMapper.selectActiveLinks();
        Map<String, List<AiResourceLite>> nodeResourceMap = buildNodeResourceMap(teacherId, nodeEntities);
        List<ClassAnalysisRecordLite> recentRecords = classAnalysisRecordLiteMapper.selectRecentByTeacherId(teacherId);
        Map<String, List<ClassAnalysisRecordLite>> nodeRecordMap = buildNodeRecordMap(nodeEntities, recentRecords);

        CourseGraphDataVO dataVO = new CourseGraphDataVO();
        dataVO.setNodes(nodeEntities.stream()
                .map(node -> toNodeVO(node, nodeResourceMap.get(node.getId()), nodeRecordMap.get(node.getId())))
                .collect(Collectors.toList()));
        dataVO.setLinks(linkEntities.stream().map(this::toLinkVO).collect(Collectors.toList()));
        dataVO.setCategories(buildCategories(nodeEntities));
        return dataVO;
    }

    @Override
    public CourseGraphNodeVO getNodeDetail(Long teacherId, String nodeId) {
        if (StringUtils.isBlank(nodeId)) {
            return null;
        }

        CourseGraphNode entity = courseGraphNodeMapper.selectById(nodeId);
        if (entity == null || Objects.equals(entity.getIsDelete(), 1)) {
            return null;
        }

        List<ClassAnalysisRecordLite> recentRecords = classAnalysisRecordLiteMapper.selectRecentByTeacherId(teacherId);
        List<ClassAnalysisRecordLite> relatedRecords = filterRelatedRecords(recentRecords, entity);
        Map<String, List<AiResourceLite>> nodeResourceMap = buildNodeResourceMap(teacherId, Collections.singletonList(entity));
        return toNodeVO(entity, nodeResourceMap.get(nodeId), relatedRecords);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseGraphNodeVO updateNode(Long teacherId, CourseGraphNodeUpdateRequest updateRequest) {
        CourseGraphNode entity = courseGraphNodeMapper.selectById(updateRequest.getId());
        if (entity == null || Objects.equals(entity.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图谱节点不存在");
        }

        if (StringUtils.isNotBlank(updateRequest.getName())) {
            entity.setName(updateRequest.getName().trim());
        }
        if (updateRequest.getCategory() != null) {
            entity.setCategory(StringUtils.trimToEmpty(updateRequest.getCategory()));
        }
        if (updateRequest.getSymbolSize() != null) {
            entity.setSymbolSize(Math.max(1, Math.min(updateRequest.getSymbolSize(), 200)));
        }
        if (updateRequest.getParentId() != null) {
            String rawParent = updateRequest.getParentId().trim();
            entity.setParentId(rawParent.isEmpty() ? null : rawParent);
        }
        if (updateRequest.getLearnUrl() != null) {
            entity.setLearnUrl(StringUtils.trimToNull(updateRequest.getLearnUrl()));
        }
        if (updateRequest.getLearningContent() != null) {
            entity.setLearningContent(StringUtils.trimToNull(updateRequest.getLearningContent()));
        }
        if (updateRequest.getDescription() != null) {
            entity.setDescription(StringUtils.trimToEmpty(updateRequest.getDescription()));
        }
        if (StringUtils.isNotBlank(updateRequest.getDifficulty())) {
            entity.setDifficulty(updateRequest.getDifficulty().trim());
        }
        if (StringUtils.isNotBlank(updateRequest.getImportance())) {
            entity.setImportance(updateRequest.getImportance().trim());
        }
        if (updateRequest.getEstimatedHours() != null) {
            entity.setEstimatedHours(Math.max(updateRequest.getEstimatedHours(), 1));
        }
        if (updateRequest.getTeachingWeek() != null) {
            entity.setTeachingWeek(Math.max(updateRequest.getTeachingWeek(), 1));
        }
        if (updateRequest.getCommonMistakes() != null) {
            entity.setCommonMistakes(writeStringList(updateRequest.getCommonMistakes()));
        }
        if (updateRequest.getTeachingTips() != null) {
            entity.setTeachingTips(writeStringList(updateRequest.getTeachingTips()));
        }
        if (updateRequest.getIsCore() != null) {
            entity.setIsCore(Boolean.TRUE.equals(updateRequest.getIsCore()) ? 1 : 0);
        }
        if (updateRequest.getIsKeyPoint() != null) {
            entity.setIsKeyPoint(Boolean.TRUE.equals(updateRequest.getIsKeyPoint()) ? 1 : 0);
        }

        int rows = courseGraphNodeMapper.updateById(entity);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图谱节点保存失败");
        }

        CourseGraphNode saved = courseGraphNodeMapper.selectById(entity.getId());
        agentIndexService.upsertGraphNode(teacherId, saved.getId());
        return toNodeVO(saved, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public CourseGraphPreferenceVO getPreferences(Long teacherId) {
        CourseGraphPreference preference = courseGraphPreferenceMapper.selectByTeacherId(teacherId);
        if (preference == null) {
            CourseGraphPreferenceVO emptyVO = new CourseGraphPreferenceVO();
            emptyVO.setFocusedNodeIds(new ArrayList<>());
            emptyVO.setRecentVisitedNodeIds(new ArrayList<>());
            emptyVO.setRecentEditedNodeIds(new ArrayList<>());
            return emptyVO;
        }
        return toPreferenceVO(preference);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseGraphPreferenceVO updatePreferences(Long teacherId, CourseGraphPreferenceUpdateRequest updateRequest) {
        CourseGraphPreference preference = courseGraphPreferenceMapper.selectByTeacherId(teacherId);
        if (preference == null) {
            preference = new CourseGraphPreference();
            preference.setTeacherId(teacherId);
        }

        if (updateRequest.getFocusedNodeIds() != null) {
            preference.setFocusedNodeIds(writeStringList(trimList(updateRequest.getFocusedNodeIds(), 10)));
        }
        if (updateRequest.getRecentVisitedNodeIds() != null) {
            preference.setRecentVisitedNodeIds(writeStringList(trimList(updateRequest.getRecentVisitedNodeIds(), 10)));
        }
        if (updateRequest.getRecentEditedNodeIds() != null) {
            preference.setRecentEditedNodeIds(writeStringList(trimList(updateRequest.getRecentEditedNodeIds(), 10)));
        }

        if (courseGraphPreferenceMapper.selectByTeacherId(teacherId) == null) {
            courseGraphPreferenceMapper.insert(preference);
        } else {
            courseGraphPreferenceMapper.updateById(preference);
        }

        return toPreferenceVO(courseGraphPreferenceMapper.selectByTeacherId(teacherId));
    }

    @Override
    public CourseGraphResourceFocusVO getResourceFocus(Long teacherId, String nodeId, String resourceType) {
        CourseGraphNode node = courseGraphNodeMapper.selectById(nodeId);
        if (node == null || Objects.equals(node.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图谱节点不存在");
        }

        List<AiResourceLite> teacherResources = aiResourceLiteMapper.selectActiveByTeacherId(teacherId);
        List<CourseGraphResourceLink> links = courseGraphResourceLinkMapper.selectActiveByTeacherAndNode(teacherId, nodeId);
        Map<Long, CourseGraphResourceLink> linkMap = links.stream()
                .collect(Collectors.toMap(CourseGraphResourceLink::getResourceId, item -> item, (a, b) -> a));

        List<ScoredResource> scoredResources = teacherResources.stream()
                .filter(resource -> StringUtils.isBlank(resourceType) || StringUtils.equalsIgnoreCase(resource.getType(), resourceType))
                .map(resource -> scoreResource(node, resource, linkMap.get(resource.getId())))
                .filter(item -> item.score > 0 || item.mapped)
                .sorted(Comparator
                        .comparing(ScoredResource::isMapped).reversed()
                        .thenComparing(ScoredResource::getScore).reversed()
                        .thenComparing(ScoredResource::getSortTime, Comparator.nullsLast(Date::compareTo)).reversed())
                .limit(12)
                .collect(Collectors.toList());

        CourseGraphResourceFocusVO vo = new CourseGraphResourceFocusVO();
        vo.setNodeId(node.getId());
        vo.setNodeName(node.getName());
        vo.setCategoryName(StringUtils.defaultIfBlank(node.getCategory(), "未分类"));
        vo.setRecommendedGraphTypes(resolveRecommendedGraphTypes(node));
        vo.setRecommendedLibraryTypes(resolveRecommendedLibraryTypes(node));
        vo.setSuggestedDirection(buildSuggestedDirection(node));
        vo.setMappedResourceCount((int) links.stream()
                .filter(link -> StringUtils.isBlank(resourceType) || StringUtils.equalsIgnoreCase(link.getResourceType(), resourceType))
                .count());
        vo.setMatchedResourceCount(scoredResources.size());
        vo.setFromMapping(vo.getMappedResourceCount() > 0);
        vo.setFocusedResources(scoredResources.stream().map(this::toFocusedResourceItemVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindResource(Long teacherId, CourseGraphResourceBindRequest bindRequest) {
        CourseGraphNode node = courseGraphNodeMapper.selectById(bindRequest.getNodeId());
        if (node == null || Objects.equals(node.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图谱节点不存在");
        }

        AiResourceLite resource = aiResourceLiteMapper.selectById(bindRequest.getResourceId());
        if (resource == null || Objects.equals(resource.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "资源不存在");
        }
        if (!Objects.equals(resource.getTeacherId(), teacherId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅可绑定自己的资源");
        }

        CourseGraphResourceLink existing = courseGraphResourceLinkMapper.selectByTeacherNodeAndResource(
                teacherId, bindRequest.getNodeId(), bindRequest.getResourceId());
        if (existing == null) {
            CourseGraphResourceLink link = new CourseGraphResourceLink();
            link.setTeacherId(teacherId);
            link.setNodeId(bindRequest.getNodeId());
            link.setResourceId(bindRequest.getResourceId());
            link.setResourceType(StringUtils.defaultIfBlank(bindRequest.getResourceType(), resource.getType()));
            link.setRelevanceScore(bindRequest.getRelevanceScore() == null ? 100 : Math.max(0, bindRequest.getRelevanceScore()));
            link.setSource(StringUtils.defaultIfBlank(bindRequest.getSource(), "graph-workflow"));
            boolean inserted = courseGraphResourceLinkMapper.insert(link) > 0;
            if (inserted) {
                agentIndexService.upsertGraphNode(teacherId, bindRequest.getNodeId());
            }
            return inserted;
        }

        existing.setResourceType(StringUtils.defaultIfBlank(bindRequest.getResourceType(), existing.getResourceType()));
        existing.setRelevanceScore(bindRequest.getRelevanceScore() == null
                ? existing.getRelevanceScore()
                : Math.max(0, bindRequest.getRelevanceScore()));
        existing.setSource(StringUtils.defaultIfBlank(bindRequest.getSource(), existing.getSource()));
        boolean updated = courseGraphResourceLinkMapper.updateById(existing) > 0;
        if (updated) {
            agentIndexService.upsertGraphNode(teacherId, bindRequest.getNodeId());
        }
        return updated;
    }

    @Override
    public CourseGraphAnalysisFocusVO getAnalysisFocus(Long teacherId, String nodeId) {
        CourseGraphNode node = getRequiredNode(nodeId);

        // 基于真实题库与资料数据计算统计
        java.util.List<CourseGraphQuizVO> quizzes = listNodeQuizzes(teacherId, nodeId);
        java.util.List<CourseGraphMaterialVO> materials = listNodeMaterials(teacherId, nodeId);

        int codingProblemCount = (int) quizzes.stream()
                .filter(q -> StringUtils.equalsIgnoreCase(q.getSource(), "coding")).count();
        int quizCount = (int) quizzes.stream()
                .filter(q -> StringUtils.equalsIgnoreCase(q.getSource(), "quiz")).count();
        int totalQuizCount = codingProblemCount + quizCount;

        int planCount = (int) materials.stream()
                .filter(m -> StringUtils.equalsIgnoreCase(m.getType(), "plan")).count();
        int animCount = (int) materials.stream()
                .filter(m -> StringUtils.equalsIgnoreCase(m.getType(), "anim")).count();
        int totalMaterialCount = planCount + animCount;

        int contentCompleteness = computeContentCompleteness(node);

        int heatScore = Math.min(100,
                Math.min(codingProblemCount * 5, 20)
                        + Math.min(quizCount * 5, 20)
                        + Math.min(planCount * 5, 15)
                        + Math.min(animCount * 5, 15)
                        + (node.getIsCore() != null && node.getIsCore() == 1 ? 15 : 0)
                        + (node.getIsKeyPoint() != null && node.getIsKeyPoint() == 1 ? 10 : 0)
                        + (contentCompleteness >= 80 ? 10 : (contentCompleteness >= 50 ? 5 : 0)));

        int weaknessScore = Math.min(100,
                (int) Math.round((100 - contentCompleteness) * 0.5)
                        + (totalQuizCount == 0 ? 20 : 0)
                        + (totalMaterialCount == 0 ? 20 : 0)
                        + ("hard".equalsIgnoreCase(node.getDifficulty()) ? 10 : 0));

        int riskScore = Math.min(100,
                (int) Math.round(weaknessScore * 0.6)
                        + (node.getIsKeyPoint() != null && node.getIsKeyPoint() == 1 ? 20 : 0)
                        + (node.getIsCore() != null && node.getIsCore() == 1 ? 20 : 0));

        int quizCoverage = Math.min(100, totalQuizCount * 10);
        int materialCoverage = Math.min(100, totalMaterialCount * 10);

        CourseGraphAnalysisFocusVO vo = new CourseGraphAnalysisFocusVO();
        vo.setNodeId(node.getId());
        vo.setNodeName(node.getName());
        vo.setCategoryName(StringUtils.defaultIfBlank(node.getCategory(), "未分类"));
        vo.setSummary(buildRealAnalysisSummary(node, heatScore, weaknessScore, riskScore,
                totalQuizCount, totalMaterialCount, contentCompleteness));
        vo.setHeatLevel(scoreToLevel(heatScore));
        vo.setWeaknessLevel(scoreToLevel(weaknessScore));
        vo.setRiskLevel(scoreToLevel(riskScore));
        vo.setCodingProblemCount(codingProblemCount);
        vo.setQuizCount(quizCount);
        vo.setTotalQuizCount(totalQuizCount);
        vo.setPlanCount(planCount);
        vo.setAnimCount(animCount);
        vo.setTotalMaterialCount(totalMaterialCount);
        vo.setContentCompleteness(contentCompleteness);
        vo.setRecommendedViews(buildRealRecommendedViews(node, weaknessScore, contentCompleteness,
                totalQuizCount, totalMaterialCount));
        vo.setSuggestedActions(buildRealSuggestedActions(node, weaknessScore, riskScore,
                totalQuizCount, totalMaterialCount, contentCompleteness));
        vo.setMetricItems(Arrays.asList(
                buildMetric("heat", "节点热度", heatScore, "综合题库、资料、核心属性与内容完善度生成的关注指数"),
                buildMetric("weakness", "薄弱程度", weaknessScore, "基于内容完善度、题库/资料缺失与难度生成的薄弱指数"),
                buildMetric("quizCoverage", "题库覆盖", quizCoverage, "基于关联编程题与随堂测验数量的题库覆盖度"),
                buildMetric("materialCoverage", "资料覆盖", materialCoverage, "基于关联教案与交互课件数量的资料覆盖度")
        ));
        return vo;
    }

    private int computeContentCompleteness(CourseGraphNode node) {
        int score = 0;
        if (StringUtils.isNotBlank(node.getLearningContent())) score += 40;
        if (StringUtils.isNotBlank(node.getDescription())) score += 15;
        if (StringUtils.isNotBlank(node.getDifficulty())) score += 10;
        if (StringUtils.isNotBlank(node.getImportance())) score += 10;
        if (node.getEstimatedHours() != null && node.getEstimatedHours() > 0) score += 10;
        if (node.getTeachingWeek() != null && node.getTeachingWeek() > 0) score += 10;
        if (StringUtils.isNotBlank(node.getLearnUrl())) score += 5;
        return score;
    }

    private String buildRealAnalysisSummary(CourseGraphNode node, int heat, int weakness, int risk,
                                            int totalQuiz, int totalMaterial, int completeness) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(node.getName()).append("】当前");
        if (totalQuiz > 0 || totalMaterial > 0) {
            sb.append("已关联 ").append(totalQuiz).append(" 道题库、").append(totalMaterial).append(" 份资料；");
        } else {
            sb.append("尚未关联任何题库与资料；");
        }
        sb.append("学习内容完善度 ").append(completeness).append("%。");

        if (weakness >= 70) {
            sb.append("该节点薄弱程度较高，建议尽快补充学习内容和配套资源。");
        } else if (weakness >= 40) {
            sb.append("该节点存在一定薄弱项，可适当完善。");
        } else {
            sb.append("该节点整体较为完善。");
        }

        if (risk >= 70) {
            sb.append("风险等级高，需重点关注。");
        } else if (risk >= 40) {
            sb.append("风险等级中等。");
        } else {
            sb.append("风险等级低。");
        }
        return sb.toString();
    }

    private List<String> buildRealRecommendedViews(CourseGraphNode node, int weakness, int completeness,
                                                    int totalQuiz, int totalMaterial) {
        List<String> views = new ArrayList<>();
        views.add("从知识点名称「" + node.getName() + "」出发，重点关注题库与资料的匹配质量");
        if (totalQuiz == 0) {
            views.add("当前无关联题库，建议从编程题库或随堂测验中补充练习资源");
        } else {
            views.add("已关联 " + totalQuiz + " 道题目，可在题库 tab 中预览与调整");
        }
        if (totalMaterial == 0) {
            views.add("当前无关联教案/课件，建议在资料 tab 中补充教学材料");
        } else {
            views.add("已关联 " + totalMaterial + " 份教学材料，可在资料 tab 中预览");
        }
        if (completeness < 60) {
            views.add("学习内容元数据尚不完整，建议完善难度、学时、正文等信息");
        }
        if (node.getIsCore() != null && node.getIsCore() == 1) {
            views.add("该节点为「核心节点」，应优先保证内容质量与资源覆盖度");
        }
        return views;
    }

    private List<String> buildRealSuggestedActions(CourseGraphNode node, int weakness, int risk,
                                                   int totalQuiz, int totalMaterial, int completeness) {
        List<String> actions = new ArrayList<>();
        if (StringUtils.isBlank(node.getLearningContent())) {
            actions.add("在学习内容 tab 中补充知识点正文，提升内容完善度");
        }
        if (totalQuiz == 0) {
            actions.add("在题库 tab 中查看自动匹配的编程题/随堂测验，补充练习资源");
        }
        if (totalMaterial == 0) {
            actions.add("在资料 tab 中查看自动匹配的教案/交互课件，补充教学材料");
        }
        if (StringUtils.isBlank(node.getDifficulty()) || StringUtils.isBlank(node.getImportance())) {
            actions.add("完善难度与重要程度等元数据，帮助学生快速了解知识点定位");
        }
        if (completeness < 50) {
            actions.add("整体完善度偏低，建议系统性地补充学习内容和配套资源");
        }
        if (risk >= 60) {
            actions.add("该节点风险等级较高，建议在教学周计划中优先安排");
        }
        if (actions.isEmpty()) {
            actions.add("该节点整体完善，可继续保持并关注学生实际掌握情况");
        }
        return actions;
    }

    @Override
    public CourseGraphClassFocusVO getClassFocus(Long teacherId, String nodeId) {
        CourseGraphNode node = getRequiredNode(nodeId);
        List<AiResourceLite> mappedResources = getMappedResources(teacherId, nodeId);
        List<ClassAnalysisRecordLite> records = filterRelatedRecords(
                classAnalysisRecordLiteMapper.selectRecentByTeacherId(teacherId), node);

        int linkedQuizCount = (int) mappedResources.stream()
                .filter(item -> StringUtils.equalsIgnoreCase(item.getType(), "quiz"))
                .count();
        int attentionScore = computeClassAttentionScore(
                node,
                records.size(),
                (int) records.stream()
                        .filter(item -> item.getCreateTime() != null && System.currentTimeMillis() - item.getCreateTime().getTime() <= RECENT_WINDOW_MS)
                        .count()
        );

        CourseGraphClassFocusVO vo = new CourseGraphClassFocusVO();
        vo.setNodeId(node.getId());
        vo.setNodeName(node.getName());
        vo.setCategoryName(StringUtils.defaultIfBlank(node.getCategory(), "未分类"));
        vo.setAttentionLevel(scoreToLevel(attentionScore));
        vo.setRelatedClassRecordCount(records.size());
        vo.setSummary(buildClassFocusSummary(node, attentionScore, records.size(), linkedQuizCount));
        vo.setLatestInsight(extractLatestInsight(records));
        vo.setObservationPoints(buildObservationPoints(node));
        vo.setBehaviorSignals(buildBehaviorSignals(node, linkedQuizCount));
        vo.setRecommendedFollowups(buildClassFollowups(node, attentionScore));
        return vo;
    }

    @Override
    public CourseGraphCommunityFocusVO getCommunityFocus(Long teacherId, String nodeId) {
        CourseGraphNode node = getRequiredNode(nodeId);

        List<CommunityPostLite> relatedPosts = matchRelatedCommunityPosts(node);
        Map<Long, CommunityFeaturedAnswerLite> featuredMap = loadFeaturedMap();

        long homeworkCount = relatedPosts.stream()
                .filter(item -> "homework".equalsIgnoreCase(item.getPostType()))
                .count();

        long pendingHomeworkCount = relatedPosts.stream()
                .filter(item -> "homework".equalsIgnoreCase(item.getPostType()))
                .filter(item -> !"resolved".equalsIgnoreCase(StringUtils.defaultString(item.getStatus())))
                .count();

        int featuredCount = (int) relatedPosts.stream()
                .filter(item -> featuredMap.containsKey(item.getId()))
                .count();

        int recentActiveCount = (int) relatedPosts.stream()
                .filter(item -> item.getLastActiveTime() != null
                        && System.currentTimeMillis() - item.getLastActiveTime().getTime() <= COMMUNITY_RECENT_WINDOW_MS)
                .count();

        int hotScore = computeCommunityHotScore(relatedPosts, pendingHomeworkCount, featuredCount);

        CourseGraphCommunityFocusVO vo = new CourseGraphCommunityFocusVO();
        vo.setNodeId(node.getId());
        vo.setNodeName(node.getName());
        vo.setCategoryName(StringUtils.defaultIfBlank(node.getCategory(), "未分类"));
        vo.setSummary(buildCommunitySummary(node, relatedPosts.size(), (int) pendingHomeworkCount, featuredCount));
        vo.setHotLevel(scoreToLevel(hotScore));
        vo.setDiscussionCount(relatedPosts.size());
        vo.setHomeworkCount((int) homeworkCount);
        vo.setFeaturedCount(featuredCount);
        vo.setPendingHomeworkCount((int) pendingHomeworkCount);
        vo.setRecentActiveCount(recentActiveCount);
        vo.setShouldGoDesk(pendingHomeworkCount > 0 || hasDeskCandidate(relatedPosts, featuredMap));
        vo.setRecentItems(
                relatedPosts.stream()
                        .limit(6)
                        .map(item -> toCommunityPostItemVO(item, featuredMap.containsKey(item.getId())))
                        .collect(Collectors.toList())
        );
        vo.setFeaturedItems(
                relatedPosts.stream()
                        .filter(item -> featuredMap.containsKey(item.getId()))
                        .limit(4)
                        .map(item -> toCommunityPostItemVO(item, true))
                        .collect(Collectors.toList())
        );
        vo.setSuggestedActions(buildCommunityActions(node, (int) pendingHomeworkCount, featuredCount));
        return vo;
    }

    @Override
    public CourseGraphCommunityDeskFocusVO getCommunityDeskFocus(Long teacherId, String nodeId) {
        CourseGraphNode node = getRequiredNode(nodeId);

        List<CommunityPostLite> relatedPosts = matchRelatedCommunityPosts(node);
        Map<Long, CommunityFeaturedAnswerLite> featuredMap = loadFeaturedMap();

        List<CommunityPostLite> pendingPosts = relatedPosts.stream()
                .filter(item -> "homework".equalsIgnoreCase(item.getPostType()))
                .filter(item -> !"resolved".equalsIgnoreCase(StringUtils.defaultString(item.getStatus())))
                .collect(Collectors.toList());

        List<CommunityPostLite> candidatePosts = relatedPosts.stream()
                .filter(item -> Objects.equals(item.getIsTeacherAnswered(), 1))
                .filter(item -> !featuredMap.containsKey(item.getId()))
                .collect(Collectors.toList());

        int featuredCount = (int) relatedPosts.stream()
                .filter(item -> featuredMap.containsKey(item.getId()))
                .count();

        int resolvedCount = (int) relatedPosts.stream()
                .filter(item -> "resolved".equalsIgnoreCase(StringUtils.defaultString(item.getStatus())))
                .count();

        CourseGraphCommunityDeskFocusVO vo = new CourseGraphCommunityDeskFocusVO();
        vo.setNodeId(node.getId());
        vo.setNodeName(node.getName());
        vo.setCategoryName(StringUtils.defaultIfBlank(node.getCategory(), "未分类"));
        vo.setSummary(buildDeskSummary(node, pendingPosts.size(), candidatePosts.size(), featuredCount));
        vo.setPendingHomeworkCount(pendingPosts.size());
        vo.setResolvedCount(resolvedCount);
        vo.setFeaturedCount(featuredCount);
        vo.setCandidateFeaturedCount(candidatePosts.size());
        vo.setShouldRecommendDesk(!pendingPosts.isEmpty() || !candidatePosts.isEmpty());
        vo.setRecentItems(
                relatedPosts.stream()
                        .limit(8)
                        .map(item -> toCommunityPostItemVO(item, featuredMap.containsKey(item.getId())))
                        .collect(Collectors.toList())
        );
        vo.setSuggestedActions(buildDeskActions(node, pendingPosts.size(), candidatePosts.size()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseGraphNodeVO createNode(Long teacherId, CourseGraphNodeCreateRequest createRequest) {
        if (createRequest == null || StringUtils.isBlank(createRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点名称不能为空");
        }

        String parentId = StringUtils.trimToNull(createRequest.getParentId());
        if (parentId != null) {
            CourseGraphNode parent = courseGraphNodeMapper.selectById(parentId);
            if (parent == null || Objects.equals(parent.getIsDelete(), 1)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "父节点不存在");
            }
        }

        CourseGraphNode entity = new CourseGraphNode();
        String requestedId = StringUtils.trimToNull(createRequest.getId());
        entity.setId(requestedId != null ? requestedId : generateNodeId());
        entity.setParentId(parentId);
        entity.setName(createRequest.getName().trim());
        entity.setCategory(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(createRequest.getCategory()), "未分类"));
        entity.setSymbolSize(createRequest.getSymbolSize() == null
                ? 30
                : Math.max(1, Math.min(createRequest.getSymbolSize(), 200)));
        entity.setDescription(StringUtils.trimToEmpty(createRequest.getDescription()));
        entity.setLearnUrl(StringUtils.trimToNull(createRequest.getLearnUrl()));
        entity.setLearningContent(StringUtils.trimToNull(createRequest.getLearningContent()));
        if (StringUtils.isNotBlank(createRequest.getDifficulty())) {
            entity.setDifficulty(createRequest.getDifficulty().trim());
        }
        if (StringUtils.isNotBlank(createRequest.getImportance())) {
            entity.setImportance(createRequest.getImportance().trim());
        }
        entity.setIsCore(Boolean.TRUE.equals(createRequest.getIsCore()) ? 1 : 0);
        entity.setIsKeyPoint(Boolean.TRUE.equals(createRequest.getIsKeyPoint()) ? 1 : 0);
        entity.setIsDelete(0);

        try {
            courseGraphNodeMapper.insert(entity);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            entity.setId(generateNodeId());
            int rows = courseGraphNodeMapper.insert(entity);
            if (rows <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "节点创建失败");
            }
        }

        CourseGraphNode saved = courseGraphNodeMapper.selectById(entity.getId());
        agentIndexService.upsertGraphNode(teacherId, saved.getId());
        return toNodeVO(saved, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNodeCascade(Long teacherId, String nodeId) {
        if (StringUtils.isBlank(nodeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "节点 id 不能为空");
        }
        CourseGraphNode root = courseGraphNodeMapper.selectById(nodeId);
        if (root == null || Objects.equals(root.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "节点不存在或已删除");
        }

        // BFS 收集所有子孙 id
        LinkedHashSet<String> idsToDelete = new LinkedHashSet<>();
        idsToDelete.add(nodeId);
        List<String> currentFrontier = Collections.singletonList(nodeId);
        int safetyGuard = 0;
        while (!currentFrontier.isEmpty() && safetyGuard++ < 50) {
            List<String> children = courseGraphNodeMapper.selectChildIdsByParentIds(currentFrontier);
            if (children == null || children.isEmpty()) {
                break;
            }
            List<String> nextFrontier = new ArrayList<>();
            for (String childId : children) {
                if (childId != null && idsToDelete.add(childId)) {
                    nextFrontier.add(childId);
                }
            }
            currentFrontier = nextFrontier;
        }

        List<String> allIds = new ArrayList<>(idsToDelete);

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<CourseGraphNode> nodeWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        nodeWrapper.in("id", allIds);
        courseGraphNodeMapper.delete(nodeWrapper);

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<CourseGraphLink> linkWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        linkWrapper.in("source", allIds).or().in("target", allIds);
        courseGraphLinkMapper.delete(linkWrapper);

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<CourseGraphResourceLink> resourceWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        resourceWrapper.in("node_id", allIds);
        courseGraphResourceLinkMapper.delete(resourceWrapper);

        agentIndexService.deleteGraphNodes(null, allIds);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseGraphLinkVO createLink(Long teacherId, CourseGraphLinkCreateRequest createRequest) {
        if (createRequest == null
                || StringUtils.isBlank(createRequest.getSource())
                || StringUtils.isBlank(createRequest.getTarget())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "连线参数不合法");
        }
        String source = createRequest.getSource().trim();
        String target = createRequest.getTarget().trim();
        if (source.equals(target)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "连线源节点与目标节点不能相同");
        }

        CourseGraphNode srcNode = courseGraphNodeMapper.selectById(source);
        if (srcNode == null || Objects.equals(srcNode.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "源节点不存在");
        }
        CourseGraphNode tgtNode = courseGraphNodeMapper.selectById(target);
        if (tgtNode == null || Objects.equals(tgtNode.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标节点不存在");
        }

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CourseGraphLink> dupQuery =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        dupQuery.eq("isDelete", 0)
                .and(w -> w.and(q -> q.eq("source", source).eq("target", target))
                        .or(q -> q.eq("source", target).eq("target", source)));
        Long dupCount = courseGraphLinkMapper.selectCount(dupQuery);
        if (dupCount != null && dupCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "两节点之间的连线已存在");
        }

        CourseGraphLink link = new CourseGraphLink();
        link.setSource(source);
        link.setTarget(target);
        link.setRelationType(StringUtils.defaultIfBlank(
                StringUtils.trimToNull(createRequest.getRelationType()), "related"));
        link.setDescription(StringUtils.trimToNull(createRequest.getDescription()));
        link.setSortOrder(0);
        link.setIsDelete(0);

        int rows = courseGraphLinkMapper.insert(link);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "连线创建失败");
        }
        agentIndexService.upsertGraphNodes(teacherId, List.of(source, target));
        return toLinkVO(courseGraphLinkMapper.selectById(link.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLink(Long teacherId, Long linkId) {
        if (linkId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "连线 id 不能为空");
        }
        CourseGraphLink existing = courseGraphLinkMapper.selectById(linkId);
        if (existing == null || Objects.equals(existing.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "连线不存在或已删除");
        }
        boolean deleted = courseGraphLinkMapper.deleteById(linkId) > 0;
        if (deleted) {
            agentIndexService.upsertGraphNodes(teacherId, List.of(existing.getSource(), existing.getTarget()));
        }
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseGraphDataVO seedDefaultGraph(Long teacherId) {
        List<CourseGraphNode> existingNodes = courseGraphNodeMapper.selectActiveNodes();
        if (existingNodes != null && !existingNodes.isEmpty()) {
            // 物理清空现有数据，然后导入默认图谱
            courseGraphLinkMapper.deleteActivePhysical();
            courseGraphNodeMapper.deleteActivePhysical();
        }
        CourseGraphSeeder.seed(courseGraphNodeMapper, courseGraphLinkMapper);
        List<String> nodeIds = courseGraphNodeMapper.selectActiveNodes().stream()
                .map(CourseGraphNode::getId)
                .collect(Collectors.toList());
        agentIndexService.upsertGraphNodes(teacherId, nodeIds);
        return getGraphData(teacherId);
    }

    private String generateNodeId() {
        String suffix = Long.toString(Math.abs(java.util.concurrent.ThreadLocalRandom.current().nextLong()), 36);
        if (suffix.length() > 6) {
            suffix = suffix.substring(0, 6);
        }
        return "node-" + System.currentTimeMillis() + "-" + suffix;
    }

    private CourseGraphNode getRequiredNode(String nodeId) {
        CourseGraphNode node = courseGraphNodeMapper.selectById(nodeId);
        if (node == null || Objects.equals(node.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图谱节点不存在");
        }
        return node;
    }

    private Map<String, List<AiResourceLite>> buildNodeResourceMap(Long teacherId, List<CourseGraphNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return new HashMap<>();
        }

        List<String> nodeIds = nodes.stream()
                .map(CourseGraphNode::getId)
                .filter(StringUtils::isNotBlank)
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
        Map<Long, AiResourceLite> resourceMap = resources.stream()
                .collect(Collectors.toMap(AiResourceLite::getId, item -> item, (a, b) -> a));
        Map<String, List<AiResourceLite>> result = new HashMap<>();
        for (CourseGraphResourceLink link : links) {
            AiResourceLite resource = resourceMap.get(link.getResourceId());
            if (resource == null) {
                continue;
            }
            result.computeIfAbsent(link.getNodeId(), key -> new ArrayList<>()).add(resource);
        }
        return result;
    }

    private List<AiResourceLite> getMappedResources(Long teacherId, String nodeId) {
        Map<String, List<AiResourceLite>> map = buildNodeResourceMap(
                teacherId,
                Collections.singletonList(getRequiredNode(nodeId))
        );
        return map.getOrDefault(nodeId, Collections.emptyList());
    }

    private Map<String, List<ClassAnalysisRecordLite>> buildNodeRecordMap(
            List<CourseGraphNode> nodes,
            List<ClassAnalysisRecordLite> records
    ) {
        Map<String, List<ClassAnalysisRecordLite>> result = new HashMap<>();
        if (nodes == null || nodes.isEmpty() || records == null || records.isEmpty()) {
            return result;
        }

        for (CourseGraphNode node : nodes) {
            List<ClassAnalysisRecordLite> related = filterRelatedRecords(records, node);
            if (!related.isEmpty()) {
                result.put(node.getId(), related);
            }
        }
        return result;
    }

    private List<ClassAnalysisRecordLite> filterRelatedRecords(
            List<ClassAnalysisRecordLite> records,
            CourseGraphNode node
    ) {
        if (records == null || records.isEmpty() || node == null) {
            return new ArrayList<>();
        }

        String nodeName = lower(node.getName());
        String category = lower(node.getCategory());

        return records.stream()
                .filter(record -> {
                    String aggregateText = lower(record.getPlanText()) + " " + lower(record.getAiReport()) + " " + lower(record.getTranscriptJson());
                    boolean matchName = StringUtils.isNotBlank(nodeName) && aggregateText.contains(nodeName);
                    boolean matchCategory = StringUtils.isNotBlank(category) && category.length() > 1 && aggregateText.contains(category);
                    return matchName || matchCategory;
                })
                .collect(Collectors.toList());
    }

    private List<CommunityPostLite> matchRelatedCommunityPosts(CourseGraphNode node) {
        List<CommunityPostLite> allPosts = communityPostLiteMapper.selectActivePosts();
        String name = lower(node.getName());
        String category = lower(node.getCategory());
        return allPosts.stream()
                .filter(item -> {
                    String text = lower(item.getTitle()) + " " + lower(item.getContent()) + " " + lower(item.getCourseName());
                    boolean matchName = StringUtils.isNotBlank(name) && text.contains(name);
                    boolean matchCategory = StringUtils.isNotBlank(category) && category.length() > 1 && text.contains(category);
                    return matchName || matchCategory;
                })
                .sorted(Comparator.comparing(CommunityPostLite::getLastActiveTime, Comparator.nullsLast(Date::compareTo)).reversed())
                .collect(Collectors.toList());
    }

    private Map<Long, CommunityFeaturedAnswerLite> loadFeaturedMap() {
        return communityFeaturedAnswerLiteMapper.selectActiveFeatured().stream()
                .collect(Collectors.toMap(CommunityFeaturedAnswerLite::getPostId, item -> item, (a, b) -> a));
    }

    private boolean hasDeskCandidate(List<CommunityPostLite> posts, Map<Long, CommunityFeaturedAnswerLite> featuredMap) {
        return posts.stream()
                .anyMatch(item -> Objects.equals(item.getIsTeacherAnswered(), 1) && !featuredMap.containsKey(item.getId()));
    }

    private List<CourseGraphCategoryVO> buildCategories(List<CourseGraphNode> nodes) {
        LinkedHashMap<String, CourseGraphCategoryVO> categoryMap = new LinkedHashMap<>();
        for (CourseGraphNode node : nodes) {
            String categoryName = StringUtils.isNotBlank(node.getCategory()) ? node.getCategory().trim() : "未分类";
            if (!categoryMap.containsKey(categoryName)) {
                CourseGraphCategoryVO categoryVO = new CourseGraphCategoryVO();
                categoryVO.setId(categoryName);
                categoryVO.setName(categoryName);
                categoryMap.put(categoryName, categoryVO);
            }
        }
        return new ArrayList<>(categoryMap.values());
    }

    private CourseGraphNodeVO toNodeVO(
            CourseGraphNode entity,
            List<AiResourceLite> mappedResources,
            List<ClassAnalysisRecordLite> relatedRecords
    ) {
        CourseGraphNodeVO vo = new CourseGraphNodeVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setName(entity.getName());
        vo.setCategory(entity.getCategory());
        vo.setSymbolSize(entity.getSymbolSize());
        vo.setDescription(entity.getDescription());
        vo.setLearnUrl(entity.getLearnUrl());
        vo.setLearningContent(entity.getLearningContent());
        vo.setDifficulty(entity.getDifficulty());
        vo.setImportance(entity.getImportance());
        vo.setEstimatedHours(entity.getEstimatedHours());
        vo.setTeachingWeek(entity.getTeachingWeek());
        vo.setCommonMistakes(readStringList(entity.getCommonMistakes()));
        vo.setTeachingTips(readStringList(entity.getTeachingTips()));
        vo.setExerciseCount(entity.getExerciseCount());
        vo.setIsCore(Objects.equals(entity.getIsCore(), 1));
        vo.setIsKeyPoint(Objects.equals(entity.getIsKeyPoint(), 1));

        List<String> baseTypes = readStringList(entity.getResourceTypes());
        List<String> mergedTypes = new ArrayList<>(baseTypes);
        int mappedCount = mappedResources == null ? 0 : mappedResources.size();
        if (mappedResources != null) {
            mappedResources.stream()
                    .map(AiResourceLite::getType)
                    .filter(StringUtils::isNotBlank)
                    .map(this::mapLibraryTypeToGraphType)
                    .filter(StringUtils::isNotBlank)
                    .forEach(mergedTypes::add);
        }
        mergedTypes = trimList(mergedTypes, 8);

        Integer baseCount = entity.getResourceCount() == null ? 0 : entity.getResourceCount();
        vo.setResourceCount(Math.max(baseCount, mappedCount));
        vo.setResourceTypes(mergedTypes);
        vo.setResourceSummary(buildResourceSummary(entity.getResourceSummary(), mappedResources, mergedTypes));

        int heatScore = computeHeatScore(entity, mappedCount, relatedRecords == null ? 0 : relatedRecords.size(), 0,
                mappedResources == null ? 0 : (int) mappedResources.stream().filter(item -> Objects.equals(item.getIsPublished(), 1)).count());
        int weaknessScore = computeWeaknessScore(entity,
                computeResourceSupportScore(entity, mappedResources == null ? Collections.emptyList() : mappedResources),
                computePracticeSupportScore(entity, mappedResources == null ? 0 : (int) mappedResources.stream().filter(item -> StringUtils.equalsIgnoreCase(item.getType(), "quiz")).count()));
        int attentionScore = computeClassAttentionScore(entity, relatedRecords == null ? 0 : relatedRecords.size(), 0);

        vo.setAnalysisHeatLevel(scoreToLevel(heatScore));
        vo.setWeaknessLevel(scoreToLevel(weaknessScore));
        vo.setRecommendedForVisual(heatScore >= 45 || weaknessScore >= 50 || mappedCount > 0);
        vo.setRecommendedForAnalysis(attentionScore >= 45 || (relatedRecords != null && !relatedRecords.isEmpty()) || StringUtils.equalsIgnoreCase(entity.getDifficulty(), "high"));
        vo.setAnalysisSummary(buildNodeAnalysisSummary(entity, heatScore, weaknessScore, relatedRecords == null ? 0 : relatedRecords.size()));
        return vo;
    }

    private String buildResourceSummary(String originalSummary, List<AiResourceLite> mappedResources, List<String> mergedTypes) {
        if (mappedResources == null || mappedResources.isEmpty()) {
            return StringUtils.defaultIfBlank(originalSummary, "当前暂无额外资源摘要。");
        }
        String typeText = mergedTypes.isEmpty() ? "资源" : String.join("、", mergedTypes);
        String mappedText = String.format("当前已关联 %d 条资源库内容，覆盖类型包括：%s。", mappedResources.size(), typeText);
        if (StringUtils.isBlank(originalSummary)) {
            return mappedText;
        }
        return originalSummary + " " + mappedText;
    }

    private String buildNodeAnalysisSummary(CourseGraphNode node, int heatScore, int weaknessScore, int relatedRecordCount) {
        List<String> parts = new ArrayList<>();
        if (heatScore >= 70) {
            parts.add("当前属于高关注节点");
        } else if (heatScore >= 40) {
            parts.add("当前适合作为阶段性观察节点");
        }
        if (weaknessScore >= 70) {
            parts.add("建议优先关注薄弱点与误区分布");
        } else if (weaknessScore >= 40) {
            parts.add("建议结合练习支撑继续跟进");
        }
        if (relatedRecordCount > 0) {
            parts.add(String.format("已有 %d 条课堂分析记录与该节点存在关联", relatedRecordCount));
        }
        if (parts.isEmpty()) {
            parts.add(String.format("可围绕 %s 继续补齐数据分析与课堂观察线索。", node.getName()));
        }
        return String.join("，", parts) + "。";
    }

    private String mapLibraryTypeToGraphType(String libraryType) {
        if (StringUtils.equalsIgnoreCase(libraryType, "plan")) {
            return "plan";
        }
        if (StringUtils.equalsIgnoreCase(libraryType, "quiz")) {
            return "exercise";
        }
        if (StringUtils.equalsIgnoreCase(libraryType, "anim")) {
            return "slides";
        }
        return libraryType;
    }

    private CourseGraphLinkVO toLinkVO(CourseGraphLink entity) {
        CourseGraphLinkVO vo = new CourseGraphLinkVO();
        vo.setId(entity.getId());
        vo.setSource(entity.getSource());
        vo.setTarget(entity.getTarget());
        vo.setRelationType(entity.getRelationType());
        vo.setDescription(entity.getDescription());
        return vo;
    }

    private CourseGraphPreferenceVO toPreferenceVO(CourseGraphPreference entity) {
        CourseGraphPreferenceVO vo = new CourseGraphPreferenceVO();
        vo.setFocusedNodeIds(readStringList(entity.getFocusedNodeIds()));
        vo.setRecentVisitedNodeIds(readStringList(entity.getRecentVisitedNodeIds()));
        vo.setRecentEditedNodeIds(readStringList(entity.getRecentEditedNodeIds()));
        return vo;
    }

    private List<String> resolveRecommendedGraphTypes(CourseGraphNode node) {
        List<String> current = readStringList(node.getResourceTypes());
        if (!current.isEmpty()) {
            return current;
        }
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high")) {
            return Arrays.asList("plan", "slides", "exercise");
        }
        if (Objects.equals(node.getIsKeyPoint(), 1)) {
            return Arrays.asList("plan", "exercise", "material");
        }
        return Arrays.asList("plan", "material", "exercise");
    }

    private List<String> resolveRecommendedLibraryTypes(CourseGraphNode node) {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        for (String graphType : resolveRecommendedGraphTypes(node)) {
            if ("exercise".equalsIgnoreCase(graphType)) {
                types.add("quiz");
            } else if ("slides".equalsIgnoreCase(graphType) || "video".equalsIgnoreCase(graphType)) {
                types.add("anim");
            } else {
                types.add("plan");
            }
        }
        if (Objects.equals(node.getIsKeyPoint(), 1)) {
            types.add("quiz");
        }
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high")) {
            types.add("anim");
        }
        return new ArrayList<>(types);
    }

    private String buildSuggestedDirection(CourseGraphNode node) {
        if ((node.getResourceCount() == null ? 0 : node.getResourceCount()) <= 2) {
            return "当前节点资源仍偏少，建议优先补齐教案、互动课件与基础练习。";
        }
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high")) {
            return "当前节点难度较高，建议强化课堂讲解材料、过程性演示与分层练习。";
        }
        if (Objects.equals(node.getIsKeyPoint(), 1)) {
            return "当前节点属于重点内容，建议优先补齐可直接用于授课与检测的核心资源。";
        }
        return "建议围绕当前节点补充更贴近课堂场景的资料、教案与测验。";
    }

    private CourseGraphAnalysisMetricVO buildMetric(String key, String label, int value, String description) {
        CourseGraphAnalysisMetricVO vo = new CourseGraphAnalysisMetricVO();
        vo.setKey(key);
        vo.setLabel(label);
        vo.setValue(clampScore(value));
        vo.setDescription(description);
        return vo;
    }

    private List<String> buildRecommendedViews(
            CourseGraphNode node,
            int weaknessScore,
            int resourceSupportScore,
            int relatedClassRecordCount
    ) {
        LinkedHashSet<String> views = new LinkedHashSet<>();
        views.add("节点薄弱程度");
        if (weaknessScore >= 45) {
            views.add("高风险知识点提示");
        }
        if (resourceSupportScore < 60) {
            views.add("资源支撑情况");
        }
        if ((node.getExerciseCount() == null ? 0 : node.getExerciseCount()) > 0) {
            views.add("关联练习支撑");
        }
        if (relatedClassRecordCount > 0) {
            views.add("课堂关注度");
        }
        views.add("教学风险提示");
        return new ArrayList<>(views);
    }

    private List<String> buildAnalysisSuggestedActions(
            CourseGraphNode node,
            int weaknessScore,
            int riskScore,
            int linkedResourceCount
    ) {
        List<String> actions = new ArrayList<>();
        if (weaknessScore >= 60) {
            actions.add("优先查看该节点的薄弱指标与课堂反馈摘要。");
        }
        if (riskScore >= 60) {
            actions.add("建议回到图谱页，将该节点纳入重点复盘范围。");
        }
        if (linkedResourceCount <= 1) {
            actions.add("建议补充教案、练习或互动课件，提升节点支撑度。");
        }
        actions.add("可继续前往课堂分析页，观察围绕该节点的课堂表现与互动反馈。");
        return actions;
    }

    private String buildAnalysisSummary(
            CourseGraphNode node,
            int heatScore,
            int weaknessScore,
            int riskScore,
            int relatedClassRecordCount
    ) {
        List<String> parts = new ArrayList<>();
        if (heatScore >= 70) {
            parts.add("当前节点在整体教学链路中关注度较高");
        } else if (heatScore >= 40) {
            parts.add("当前节点适合作为阶段性分析切口");
        }
        if (weaknessScore >= 70) {
            parts.add("薄弱程度偏高，建议优先观察误区与练习支撑");
        } else if (weaknessScore >= 40) {
            parts.add("存在一定薄弱风险，适合做局部复盘");
        }
        if (riskScore >= 70) {
            parts.add("教学风险提示较强，建议与课堂反馈一起联动查看");
        }
        if (relatedClassRecordCount > 0) {
            parts.add(String.format("已有 %d 条课堂分析记录可作为参考", relatedClassRecordCount));
        }
        if (parts.isEmpty()) {
            parts.add(String.format("可围绕 %s 组织节点级分析摘要与观察指标。", node.getName()));
        }
        return String.join("，", parts) + "。";
    }

    private String buildClassFocusSummary(
            CourseGraphNode node,
            int attentionScore,
            int relatedRecordCount,
            int linkedQuizCount
    ) {
        List<String> parts = new ArrayList<>();
        if (attentionScore >= 70) {
            parts.add("建议将当前节点作为高优先课堂观察点");
        } else if (attentionScore >= 40) {
            parts.add("建议在课堂复盘时优先关注该节点表现");
        }
        if (relatedRecordCount > 0) {
            parts.add(String.format("已有 %d 条课堂记录与该节点存在关联", relatedRecordCount));
        }
        if (linkedQuizCount <= 0) {
            parts.add("当前练习支撑不足，适合重点观察学生是否真正理解");
        }
        if (parts.isEmpty()) {
            parts.add(String.format("当前可围绕 %s 组织课堂观察摘要与追踪点。", node.getName()));
        }
        return String.join("，", parts) + "。";
    }

    private List<String> buildObservationPoints(CourseGraphNode node) {
        List<String> points = readStringList(node.getCommonMistakes());
        if (points.isEmpty()) {
            points = new ArrayList<>();
        }
        if (points.size() < 3) {
            points.add(String.format("关注学生在“%s”相关概念上的回答是否准确。", node.getName()));
        }
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high")) {
            points.add("关注学生是否能完成抽象概念到具体应用的迁移。");
        }
        if (Objects.equals(node.getIsKeyPoint(), 1)) {
            points.add("关注教师是否对关键概念进行了重复强化与及时追问。");
        }
        return trimList(points, 5);
    }

    private List<String> buildBehaviorSignals(CourseGraphNode node, int linkedQuizCount) {
        List<String> list = new ArrayList<>();
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high")) {
            list.add("学生是否出现长时间停顿、跟答迟疑或重复追问。");
        }
        if (Objects.equals(node.getIsKeyPoint(), 1)) {
            list.add("关键知识点讲授后，学生能否快速给出准确回应。");
        }
        if (linkedQuizCount <= 0 || (node.getExerciseCount() == null ? 0 : node.getExerciseCount()) <= 1) {
            list.add("缺少即时练习时，需观察学生是否仅停留在表面理解。");
        }
        list.add("可关注教师讲授节奏、板书组织与学生回应质量是否匹配。");
        return trimList(list, 5);
    }

    private List<String> buildClassFollowups(CourseGraphNode node, int attentionScore) {
        List<String> list = new ArrayList<>();
        list.add("回数据分析页查看该节点的联动指标与风险提示。");
        if (attentionScore >= 60) {
            list.add("建议回 AI 备课室补充针对该节点的讲授策略或互动课件。");
        }
        list.add("可将本节点课堂观察结果回流到图谱中做后续复盘。");
        return trimList(list, 4);
    }

    private String extractLatestInsight(List<ClassAnalysisRecordLite> records) {
        if (records == null || records.isEmpty()) {
            return "当前暂无与该节点直接相关的课堂分析摘录。";
        }
        ClassAnalysisRecordLite latest = records.get(0);
        String raw = StringUtils.defaultIfBlank(latest.getAiReport(),
                StringUtils.defaultIfBlank(latest.getPlanText(), latest.getTranscriptJson()));
        raw = raw.replaceAll("\\s+", " ").trim();
        if (raw.length() > 120) {
            return raw.substring(0, 120) + "…";
        }
        return StringUtils.defaultIfBlank(raw, "当前暂无与该节点直接相关的课堂分析摘录。");
    }

    private int computeHeatScore(
            CourseGraphNode node,
            int linkedResourceCount,
            int relatedClassRecordCount,
            int recentRecordCount,
            int publishedResourceCount
    ) {
        int score = 10;
        if (Objects.equals(node.getIsKeyPoint(), 1)) score += 24;
        if (Objects.equals(node.getIsCore(), 1)) score += 16;
        if (StringUtils.equalsIgnoreCase(node.getImportance(), "high")) score += 10;
        score += Math.min(linkedResourceCount * 8, 24);
        score += Math.min(publishedResourceCount * 6, 18);
        score += Math.min(relatedClassRecordCount * 8, 24);
        score += Math.min(recentRecordCount * 6, 12);
        return clampScore(score);
    }

    private int computeResourceSupportScore(CourseGraphNode node, List<AiResourceLite> mappedResources) {
        int score = 10;
        int resourceCount = mappedResources == null ? 0 : mappedResources.size();
        int publishedCount = mappedResources == null ? 0 : (int) mappedResources.stream()
                .filter(item -> Objects.equals(item.getIsPublished(), 1))
                .count();
        long planCount = mappedResources == null ? 0 : mappedResources.stream().filter(item -> StringUtils.equalsIgnoreCase(item.getType(), "plan")).count();
        long animCount = mappedResources == null ? 0 : mappedResources.stream().filter(item -> StringUtils.equalsIgnoreCase(item.getType(), "anim")).count();

        score += Math.min(resourceCount * 18, 54);
        score += Math.min(publishedCount * 10, 20);
        if (planCount > 0) score += 10;
        if (animCount > 0) score += 6;
        return clampScore(score);
    }

    private int computePracticeSupportScore(CourseGraphNode node, int linkedQuizCount) {
        int score = 8;
        score += Math.min(linkedQuizCount * 28, 56);
        if (node.getExerciseCount() != null) {
            score += Math.min(node.getExerciseCount() * 4, 20);
        }
        return clampScore(score);
    }

    private int computeWeaknessScore(CourseGraphNode node, int resourceSupportScore, int practiceSupportScore) {
        int score = 0;
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high")) {
            score += 48;
        } else if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "medium")) {
            score += 30;
        } else {
            score += 14;
        }

        int mistakeCount = readStringList(node.getCommonMistakes()).size();
        score += Math.min(mistakeCount * 8, 24);

        if (resourceSupportScore < 40) score += 18;
        else if (resourceSupportScore < 60) score += 8;

        if (practiceSupportScore < 35) score += 16;
        else if (practiceSupportScore < 55) score += 8;

        if (Objects.equals(node.getIsKeyPoint(), 1)) score += 6;
        return clampScore(score);
    }

    private int computeRiskScore(CourseGraphNode node, int weaknessScore, int resourceSupportScore, int relatedClassRecordCount) {
        int score = (int) Math.round(weaknessScore * 0.55 + (100 - resourceSupportScore) * 0.35);
        if (Objects.equals(node.getIsKeyPoint(), 1)) score += 8;
        if (relatedClassRecordCount <= 0) score += 6;
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high")) score += 6;
        return clampScore(score);
    }

    private int computeClassAttentionScore(CourseGraphNode node, int relatedClassRecordCount, int recentRecordCount) {
        int score = 12;
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high")) score += 20;
        if (Objects.equals(node.getIsKeyPoint(), 1)) score += 18;
        if (Objects.equals(node.getIsCore(), 1)) score += 10;
        score += Math.min(relatedClassRecordCount * 12, 30);
        score += Math.min(recentRecordCount * 8, 16);
        score += Math.min(readStringList(node.getCommonMistakes()).size() * 4, 12);
        return clampScore(score);
    }

    private int computeCommunityHotScore(List<CommunityPostLite> posts, long pendingHomeworkCount, int featuredCount) {
        int base = Math.min(posts.size() * 12, 60);
        base += Math.min((int) pendingHomeworkCount * 15, 25);
        base += Math.min(featuredCount * 10, 20);
        if (posts.stream().anyMatch(item -> Objects.equals(item.getIsHot(), 1))) {
            base += 10;
        }
        return Math.min(base, 100);
    }

    private String buildCommunitySummary(CourseGraphNode node, int total, int pending, int featured) {
        if (total <= 0) {
            return String.format("当前围绕 %s 的社区问题较少，可继续观察学生提问与互助情况。", node.getName());
        }
        List<String> parts = new ArrayList<>();
        parts.add(String.format("已发现 %d 条相关社区内容", total));
        if (pending > 0) {
            parts.add(String.format("其中 %d 条仍待处理", pending));
        }
        if (featured > 0) {
            parts.add(String.format("已有 %d 条进入答疑精选", featured));
        }
        return String.join("，", parts) + "。";
    }

    private String buildDeskSummary(CourseGraphNode node, int pending, int candidate, int featured) {
        List<String> parts = new ArrayList<>();
        if (pending > 0) {
            parts.add(String.format("当前有 %d 条作业互助问题待处理", pending));
        }
        if (candidate > 0) {
            parts.add(String.format("有 %d 条讨论适合进入精选", candidate));
        }
        if (featured > 0) {
            parts.add(String.format("已累计 %d 条精选答疑", featured));
        }
        if (parts.isEmpty()) {
            parts.add(String.format("当前围绕 %s 的教师处理台任务较轻。", node.getName()));
        }
        return String.join("，", parts) + "。";
    }

    private List<String> buildCommunityActions(CourseGraphNode node, int pending, int featured) {
        List<String> list = new ArrayList<>();
        if (pending > 0) {
            list.add("优先进入教师处理台处理待解决作业问题");
        }
        if (featured <= 0) {
            list.add("可从老师已答讨论中挑选内容加入答疑精选");
        }
        list.add("可结合资源与 AI 备课结果补充该节点的答疑支撑");
        return list;
    }

    private List<String> buildDeskActions(CourseGraphNode node, int pending, int candidate) {
        List<String> list = new ArrayList<>();
        if (pending > 0) {
            list.add("优先处理待解决作业问题，并关注重复误区");
        }
        if (candidate > 0) {
            list.add("优先把高质量教师回复加入答疑精选");
        }
        list.add("处理完成后可返回课程图谱继续补资源或完善教学建议");
        return list;
    }

    private CourseGraphCommunityPostItemVO toCommunityPostItemVO(CommunityPostLite item, boolean featured) {
        CourseGraphCommunityPostItemVO vo = new CourseGraphCommunityPostItemVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setCourseName(StringUtils.defaultIfBlank(item.getCourseName(), "未标注课程"));
        vo.setPostType(StringUtils.defaultIfBlank(item.getPostType(), "discussion"));
        vo.setStatus(StringUtils.defaultIfBlank(item.getStatus(), "open"));
        vo.setReplyCount(item.getReplyCount() == null ? 0 : item.getReplyCount());
        vo.setViewCount(item.getViewCount() == null ? 0 : item.getViewCount());
        vo.setLastActiveTime(formatDate(item.getLastActiveTime()));
        vo.setAuthorName(StringUtils.defaultIfBlank(item.getAuthorName(), "匿名同学"));
        vo.setIsTeacherAnswered(Objects.equals(item.getIsTeacherAnswered(), 1));
        vo.setIsFeatured(featured);
        return vo;
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(score, 100));
    }

    private String scoreToLevel(int score) {
        if (score >= 70) return "high";
        if (score >= 40) return "medium";
        return "low";
    }

    private ScoredResource scoreResource(CourseGraphNode node, AiResourceLite resource, CourseGraphResourceLink link) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        boolean mapped = link != null;

        String nodeName = lower(node.getName());
        String category = lower(node.getCategory());
        String title = lower(resource.getTitle());
        String paramsText = lower(resource.getParamsJson());

        if (mapped) {
            score += link.getRelevanceScore() == null ? 100 : link.getRelevanceScore();
            reasons.add("已建立图谱映射");
        }
        if (StringUtils.isNotBlank(nodeName) && title.contains(nodeName)) {
            score += 42;
            reasons.add("标题命中节点名称");
        }
        if (StringUtils.isNotBlank(nodeName) && paramsText.contains(nodeName)) {
            score += 28;
            reasons.add("生成参数命中节点名称");
        }
        if (StringUtils.isNotBlank(category) && paramsText.contains(category)) {
            score += 12;
            reasons.add("生成参数命中节点分类");
        }
        if (Objects.equals(node.getIsKeyPoint(), 1) && ("plan".equalsIgnoreCase(resource.getType()) || "quiz".equalsIgnoreCase(resource.getType()))) {
            score += 8;
            reasons.add("重点节点优先补教案/试题");
        }
        if (StringUtils.equalsIgnoreCase(node.getDifficulty(), "high") && ("plan".equalsIgnoreCase(resource.getType()) || "anim".equalsIgnoreCase(resource.getType()))) {
            score += 8;
            reasons.add("高难节点优先补教案/互动课件");
        }

        String reason = reasons.isEmpty() ? "围绕当前节点的资源聚焦推荐" : String.join("；", reasons);
        return new ScoredResource(resource, score, reason, mapped);
    }

    private CourseGraphFocusedResourceItemVO toFocusedResourceItemVO(ScoredResource item) {
        CourseGraphFocusedResourceItemVO vo = new CourseGraphFocusedResourceItemVO();
        vo.setId(item.resource.getId());
        vo.setTitle(item.resource.getTitle());
        vo.setType(item.resource.getType());
        vo.setTypeText(mapLibraryTypeText(item.resource.getType()));
        vo.setIsPublished(item.resource.getIsPublished());
        vo.setMatchReason(item.reason);
        vo.setMatchScore(item.score);
        vo.setIsMapped(item.mapped);
        return vo;
    }

    private String mapLibraryTypeText(String type) {
        if ("plan".equalsIgnoreCase(type)) return "教案";
        if ("quiz".equalsIgnoreCase(type)) return "试题";
        if ("anim".equalsIgnoreCase(type)) return "交互课件";
        return StringUtils.defaultIfBlank(type, "资源");
    }

    private List<String> readStringList(String rawJson) {
        if (StringUtils.isBlank(rawJson)) {
            return new ArrayList<>();
        }

        try {
            List<String> list = objectMapper.readValue(rawJson, STRING_LIST_TYPE);
            return trimList(list, 20);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeStringList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(trimList(list, 20));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图谱数据序列化失败");
        }
    }

    private List<String> trimList(List<String> list, int limit) {
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String lower(String value) {
        return StringUtils.defaultString(value).toLowerCase();
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "刚刚";
        }
        return SIMPLE_DATE_FORMAT.format(date);
    }

    private static class ScoredResource {
        private final AiResourceLite resource;
        private final int score;
        private final String reason;
        private final boolean mapped;

        private ScoredResource(AiResourceLite resource, int score, String reason, boolean mapped) {
            this.resource = resource;
            this.score = score;
            this.reason = reason;
            this.mapped = mapped;
        }

        public int getScore() {
            return score;
        }

        public boolean isMapped() {
            return mapped;
        }

        public Date getSortTime() {
            return resource.getUpdateTime() == null ? resource.getCreateTime() : resource.getUpdateTime();
        }
    }

    // ═══════════════════════════════════════════════════
    //  知识点-学习活动绑定
    // ═══════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindNodeActivity(Long teacherId, String nodeId, String activityType, Long activityId) {
        if (StringUtils.isBlank(nodeId) || StringUtils.isBlank(activityType) || activityId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不合法");
        }
        CourseGraphNode node = courseGraphNodeMapper.selectById(nodeId);
        if (node == null || Objects.equals(node.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "节点不存在");
        }
        // 查重
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.ruyi.teach.model.entity.CourseGraphNodeActivity> dupQuery =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        dupQuery.eq("nodeId", nodeId).eq("activityType", activityType).eq("activityId", activityId).eq("isDelete", 0);
        if (courseGraphNodeActivityMapper.selectCount(dupQuery) > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该活动已绑定到当前知识点");
        }
        // 获取活动标题
        String activityTitle = resolveActivityTitle(activityType, activityId);
        com.ruyi.teach.model.entity.CourseGraphNodeActivity activity = new com.ruyi.teach.model.entity.CourseGraphNodeActivity();
        activity.setNodeId(nodeId);
        activity.setTeacherId(teacherId);
        activity.setActivityType(activityType.trim());
        activity.setActivityId(activityId);
        activity.setActivityTitle(activityTitle);
        activity.setWeight(1);
        activity.setIsDelete(0);
        return courseGraphNodeActivityMapper.insert(activity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindNodeActivity(Long teacherId, Long activityId) {
        if (activityId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "绑定记录ID不能为空");
        }
        com.ruyi.teach.model.entity.CourseGraphNodeActivity activity = courseGraphNodeActivityMapper.selectById(activityId);
        if (activity == null || Objects.equals(activity.getIsDelete(), 1)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "绑定记录不存在");
        }
        return courseGraphNodeActivityMapper.deleteById(activityId) > 0;
    }

    @Override
    public java.util.List<com.ruyi.teach.model.entity.CourseGraphNodeActivity> listNodeActivities(String nodeId) {
        if (StringUtils.isBlank(nodeId)) {
            return new java.util.ArrayList<>();
        }
        return courseGraphNodeActivityMapper.selectActiveByNodeId(nodeId);
    }

    @Override
    public java.util.List<CourseGraphMaterialVO> listNodeMaterials(Long teacherId, String nodeId) {
        if (teacherId == null || StringUtils.isBlank(nodeId)) {
            return new java.util.ArrayList<>();
        }
        CourseGraphNode node = courseGraphNodeMapper.selectById(nodeId);
        if (node == null || Objects.equals(node.getIsDelete(), 1)) {
            return new java.util.ArrayList<>();
        }
        String nodeName = StringUtils.trimToNull(node.getName());
        if (nodeName == null) {
            return new java.util.ArrayList<>();
        }

        java.util.List<CourseGraphMaterialVO> result = new java.util.ArrayList<>();
        String keyword = nodeName.toLowerCase();

        QueryWrapper<AiResourceLite> resourceWrapper = new QueryWrapper<>();
        resourceWrapper.eq("teacher_id", teacherId)
                .eq("is_delete", 0)
                .in("type", "plan", "anim")
                .and(w -> w.like("title", nodeName).or().like("content", nodeName));
        java.util.List<AiResourceLite> resources = aiResourceLiteMapper.selectList(resourceWrapper);
        for (AiResourceLite res : resources) {
            CourseGraphMaterialVO vo = new CourseGraphMaterialVO();
            vo.setId(res.getId());
            vo.setTitle(res.getTitle());
            vo.setType(res.getType());
            vo.setTypeText("anim".equalsIgnoreCase(res.getType()) ? "交互课件" : "教案");
            vo.setContent(res.getContent());
            vo.setMatchScore(computeQuizMatchScore(keyword, res.getTitle(), res.getContent()));
            result.add(vo);
        }

        result.sort((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()));
        return result;
    }

    @Override
    public java.util.List<CourseGraphQuizVO> listNodeQuizzes(Long teacherId, String nodeId) {
        if (teacherId == null || StringUtils.isBlank(nodeId)) {
            return new java.util.ArrayList<>();
        }
        CourseGraphNode node = courseGraphNodeMapper.selectById(nodeId);
        if (node == null || Objects.equals(node.getIsDelete(), 1)) {
            return new java.util.ArrayList<>();
        }
        String nodeName = StringUtils.trimToNull(node.getName());
        if (nodeName == null) {
            return new java.util.ArrayList<>();
        }

        java.util.List<CourseGraphQuizVO> result = new java.util.ArrayList<>();
        String keyword = nodeName.toLowerCase();

        // 1. 查询编程题库（标题或描述包含节点名称）
        QueryWrapper<CodingProblem> codingWrapper = new QueryWrapper<>();
        codingWrapper.eq("creator_id", teacherId)
                .eq("is_delete", 0)
                .and(w -> w.like("title", nodeName).or().like("description", nodeName));
        java.util.List<CodingProblem> codingProblems = codingProblemMapper.selectList(codingWrapper);
        for (CodingProblem cp : codingProblems) {
            CourseGraphQuizVO vo = new CourseGraphQuizVO();
            vo.setId(cp.getId());
            vo.setTitle(cp.getTitle());
            vo.setSource("coding");
            vo.setSourceText("编程题");
            vo.setDifficulty(cp.getDifficulty());
            vo.setContent(cp.getDescription());
            vo.setMatchScore(computeQuizMatchScore(keyword, cp.getTitle(), cp.getDescription()));
            result.add(vo);
        }

        // 2. 查询随堂测验资源（标题或内容包含节点名称）
        QueryWrapper<AiResourceLite> resourceWrapper = new QueryWrapper<>();
        resourceWrapper.eq("teacher_id", teacherId)
                .eq("is_delete", 0)
                .eq("type", "quiz")
                .and(w -> w.like("title", nodeName).or().like("content", nodeName));
        java.util.List<AiResourceLite> resources = aiResourceLiteMapper.selectList(resourceWrapper);
        for (AiResourceLite res : resources) {
            CourseGraphQuizVO vo = new CourseGraphQuizVO();
            vo.setId(res.getId());
            vo.setTitle(res.getTitle());
            vo.setSource("quiz");
            vo.setSourceText("随堂测验");
            vo.setContent(res.getContent());
            vo.setMatchScore(computeQuizMatchScore(keyword, res.getTitle(), res.getContent()));
            result.add(vo);
        }

        // 按匹配分数降序排列
        result.sort((a, b) -> b.getMatchScore().compareTo(a.getMatchScore()));
        return result;
    }

    private int computeQuizMatchScore(String keyword, String title, String content) {
        int score = 0;
        String t = StringUtils.defaultString(title).toLowerCase();
        String c = StringUtils.defaultString(content).toLowerCase();
        if (t.contains(keyword)) {
            score += 50;
        }
        if (c.contains(keyword)) {
            score += 20;
        }
        return score;
    }

    private String resolveActivityTitle(String activityType, Long activityId) {
        try {
            if ("homework".equalsIgnoreCase(activityType) || "practice".equalsIgnoreCase(activityType)) {
                com.ruyi.teach.model.entity.HomeworkAssignment assignment = homeworkAssignmentMapper.selectById(activityId);
                return assignment != null ? assignment.getTitle() : "作业";
            }
            if ("coding".equalsIgnoreCase(activityType)) {
                com.ruyi.teach.model.entity.CodingProblem problem = codingProblemMapper.selectById(activityId);
                return problem != null ? problem.getTitle() : "编程题";
            }
        } catch (Exception ignored) {
        }
        return "未知活动";
    }

    // ═══════════════════════════════════════════════════
    //  知识点进度计算
    // ═══════════════════════════════════════════════════

    @Override
    public java.util.Map<String, Object> computeNodeProgress(String nodeId, java.util.List<Long> studentIds) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        java.util.List<com.ruyi.teach.model.entity.CourseGraphNodeActivity> activities = listNodeActivities(nodeId);
        if (activities == null || activities.isEmpty()) {
            result.put("totalActivities", 0);
            result.put("studentProgress", new java.util.ArrayList<>());
            return result;
        }
        int totalActivities = activities.size();
        result.put("totalActivities", totalActivities);
        java.util.List<java.util.Map<String, Object>> progressList = new java.util.ArrayList<>();
        for (Long studentId : studentIds) {
            if (studentId == null) continue;
            int completedCount = 0;
            int totalScore = 0;
            int scoredCount = 0;
            for (com.ruyi.teach.model.entity.CourseGraphNodeActivity act : activities) {
                Integer score = queryStudentActivityScore(studentId, act.getActivityType(), act.getActivityId());
                if (score != null && score >= 0) {
                    completedCount++;
                    totalScore += score;
                    scoredCount++;
                }
            }
            int completionRate = totalActivities > 0 ? (int) Math.round((double) completedCount / totalActivities * 100) : 0;
            int masteryRate = scoredCount > 0 ? (int) Math.round((double) totalScore / scoredCount) : 0;
            java.util.Map<String, Object> sp = new java.util.HashMap<>();
            sp.put("studentId", studentId);
            sp.put("completionRate", completionRate);
            sp.put("masteryRate", masteryRate);
            sp.put("completedCount", completedCount);
            progressList.add(sp);
        }
        result.put("studentProgress", progressList);
        return result;
    }

    private Integer queryStudentActivityScore(Long studentId, String activityType, Long activityId) {
        try {
            if ("homework".equalsIgnoreCase(activityType) || "practice".equalsIgnoreCase(activityType)) {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.ruyi.teach.model.entity.HomeworkSubmission> query =
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                query.eq("student_id", studentId)
                        .eq("assignment_id", activityId)
                        .eq("is_delete", 0)
                        .eq("submit_status", "completed")
                        .orderByDesc("total_score")
                        .last("LIMIT 1");
                com.ruyi.teach.model.entity.HomeworkSubmission submission = homeworkSubmissionMapper.selectOne(query);
                if (submission != null && submission.getTotalScore() != null) {
                    return submission.getTotalScore();
                }
            }
            if ("coding".equalsIgnoreCase(activityType)) {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.ruyi.teach.model.entity.CodingSubmission> query =
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                query.eq("student_id", studentId)
                        .eq("problem_id", activityId)
                        .eq("is_delete", 0)
                        .eq("status", "judged")
                        .orderByDesc("final_score")
                        .last("LIMIT 1");
                com.ruyi.teach.model.entity.CodingSubmission submission = codingSubmissionMapper.selectOne(query);
                if (submission != null && submission.getFinalScore() != null) {
                    return submission.getFinalScore();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
