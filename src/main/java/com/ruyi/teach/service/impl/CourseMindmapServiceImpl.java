package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.AiModelClient;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.CourseMindmapMapper;
import com.ruyi.teach.model.entity.Course;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.model.entity.CourseMindmap;
import com.ruyi.teach.model.vo.CourseMindmapVO;
import com.ruyi.teach.service.CourseChapterService;
import com.ruyi.teach.service.CourseMindmapService;
import com.ruyi.teach.service.CourseService;
import com.ruyi.teach.utils.CourseMindmapRuleHelper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CourseMindmapServiceImpl extends ServiceImpl<CourseMindmapMapper, CourseMindmap>
        implements CourseMindmapService {

    @Resource
    private CourseMindmapMapper courseMindmapMapper;

    @Resource
    private CourseService courseService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private AiModelClient aiModelClient;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseMindmapVO getCourseMindmap(Long courseId) {
        return buildCourseMindmap(courseId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseMindmapVO regenerateCourseMindmap(Long courseId) {
        return buildCourseMindmap(courseId, true);
    }

    private CourseMindmapVO buildCourseMindmap(Long courseId, boolean forceRegenerate) {
        if (courseId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "courseId 不能为空");
        }

        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }

        String courseName = normalizeText(course.getName());
        if (StringUtils.isBlank(courseName)) {
            courseName = "未命名课程";
        }

        String courseDescription = normalizeText(course.getDescription());
        List<CourseChapter> chapterList = listChapters(courseId);
        List<String> chapterTitles = listChapterTitles(chapterList);
        String sourceHash = buildSourceHash(course, chapterList, courseName, courseDescription);
        CourseMindmapVO enhancedFallback = buildEnhancedFallbackMindmap(courseName, chapterTitles);

        if (!forceRegenerate) {
            CourseMindmap cached = courseMindmapMapper.selectByCourseId(courseId);
            if (cached != null
                    && StringUtils.equals(sourceHash, cached.getSourceHash())
                    && StringUtils.isNotBlank(cached.getMindmapJson())) {

                CourseMindmapVO cachedMindmap = parseAndValidateMindmap(
                        cached.getMindmapJson(),
                        courseName,
                        chapterTitles,
                        enhancedFallback
                );

                if (cachedMindmap != null) {
                    return attachMeta(
                            cachedMindmap,
                            StringUtils.defaultIfBlank(cached.getStatus(), "ready"),
                            cached.getSourceHash(),
                            cached.getUpdatedAt()
                    );
                }
            }
        }

        CourseMindmapVO resultMindmap = null;
        String status = "fallback";

        if (!chapterTitles.isEmpty()) {
            try {
                String aiRaw = requestMindmapJsonFromAi(course, chapterList, courseName, courseDescription, chapterTitles);
                resultMindmap = parseAndValidateMindmap(aiRaw, courseName, chapterTitles, enhancedFallback);
                if (resultMindmap != null) {
                    status = "ready";
                }
            } catch (Exception ignored) {
            }
        }

        if (resultMindmap == null) {
            resultMindmap = enhancedFallback;
            status = "fallback";
        }

        Date now = new Date();
        saveMindmapCache(courseId, resultMindmap, sourceHash, status);
        return attachMeta(resultMindmap, status, sourceHash, now);
    }

    private List<CourseChapter> listChapters(Long courseId) {
        QueryWrapper<CourseChapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                .eq("is_delete", 0)
                .orderByAsc("sort_order");

        List<CourseChapter> chapterList = courseChapterService.list(queryWrapper);
        return chapterList != null ? chapterList : new ArrayList<>();
    }

    private List<String> listChapterTitles(List<CourseChapter> chapterList) {
        List<String> titles = new ArrayList<>();
        for (CourseChapter chapter : chapterList) {
            if (chapter == null) {
                continue;
            }
            String title = normalizeText(chapter.getTitle());
            if (StringUtils.isNotBlank(title)) {
                titles.add(title);
            }
        }
        return CourseMindmapRuleHelper.normalizeChapterTitles(titles);
    }

    private String buildSourceHash(Course course,
                                   List<CourseChapter> chapterList,
                                   String courseName,
                                   String courseDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.defaultString(courseName)).append("||");
        sb.append(StringUtils.defaultString(courseDescription)).append("||");

        String optionalCourseHints = collectOptionalCourseHints(course);
        sb.append(optionalCourseHints).append("||");

        for (CourseChapter chapter : chapterList) {
            if (chapter == null) {
                continue;
            }
            sb.append(CourseMindmapRuleHelper.cleanChapterTitle(chapter.getTitle())).append("##");
            sb.append(collectOptionalChapterHint(chapter)).append("##");
        }

        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String requestMindmapJsonFromAi(Course course,
                                            List<CourseChapter> chapterList,
                                            String courseName,
                                            String courseDescription,
                                            List<String> chapterTitles) throws Exception {
        String content = aiModelClient.chat(
                buildMindmapSystemPrompt(),
                buildMindmapUserPrompt(course, chapterList, courseName, courseDescription, chapterTitles),
                0.15,
                2200,
                true
        );
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException("AI 返回内容为空");
        }
        return content;
    }

    private String buildMindmapSystemPrompt() {
        return "你是一名课程知识结构设计助手。"
                + "你的目标不是把章节标题直接拼成树，而是把课程内容整理成适合学生理解的教学结构。"
                + "你必须先归纳课程主模块，再把章节归入模块，再提炼每个模块下的核心知识点。"
                + "根节点必须是课程名。"
                + "一级节点只能表示课程模块，不能只是章节标题重命名。"
                + "二级节点只能表示模块下的关键知识点，不要继续下钻。"
                + "一级节点建议控制在 5 到 6 个；若课程章节较少，可降到 4 个，但不能失去主线。"
                + "每个一级节点下建议 2 到 4 个子节点。"
                + "模块顺序要符合教学路径：先基础与导入，再核心原理，再关键技术，再框架或工程应用，最后项目实践与部署总结。"
                + "如果不是纯技术课，也要保持从基础到核心到应用的渐进结构。"
                + "禁止使用模块一、第一部分、第二部分、核心内容、主要知识、课程重点、知识体系等空泛名称。"
                + "避免重复节点、混层级节点、过碎节点、同义反复节点。"
                + "输出必须覆盖主要章节，但不能机械照抄章节顺序。"
                + "只能输出合法 JSON 对象，不要输出 Markdown，不要输出解释，不要输出代码块。"
                + "输出结构必须严格为："
                + "{"
                + "\"title\":\"课程思维导图标题\","
                + "\"root\":{"
                + "\"name\":\"课程名\","
                + "\"children\":["
                + "{\"name\":\"课程模块\",\"children\":[{\"name\":\"核心知识点\"}]}"
                + "]"
                + "}"
                + "}";
    }

    private String buildMindmapUserPrompt(Course course,
                                          List<CourseChapter> chapterList,
                                          String courseName,
                                          String courseDescription,
                                          List<String> chapterTitles) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程名称：").append(courseName).append("\n");

        if (StringUtils.isNotBlank(courseDescription)) {
            sb.append("课程简介：").append(courseDescription).append("\n");
        }

        String optionalCourseHints = collectOptionalCourseHints(course);
        if (StringUtils.isNotBlank(optionalCourseHints)) {
            sb.append("课程补充信息：").append(optionalCourseHints).append("\n");
        }

        List<String> suggestedModules = CourseMindmapRuleHelper.suggestModuleLabels(courseName, chapterTitles);
        if (!suggestedModules.isEmpty()) {
            sb.append("可参考的模块风格（仅供归纳方向参考，不要机械照搬）：")
                    .append(String.join("、", suggestedModules))
                    .append("\n");
        }

        sb.append("章节信息（已做基础清洗）：\n");
        int index = 1;
        for (CourseChapter chapter : chapterList) {
            if (chapter == null) {
                continue;
            }
            String title = CourseMindmapRuleHelper.cleanChapterTitle(chapter.getTitle());
            if (StringUtils.isBlank(title)) {
                continue;
            }

            sb.append(index++).append(". 标题：").append(title);
            String chapterHint = collectOptionalChapterHint(chapter);
            if (StringUtils.isNotBlank(chapterHint)) {
                sb.append("；补充：").append(chapterHint);
            }
            sb.append("\n");
        }

        sb.append("\n输出要求：\n")
                .append("1. 先归纳课程主模块，再组织知识点，不能只是章节标题平铺。\n")
                .append("2. 一级节点代表模块，二级节点代表模块下的核心知识点。\n")
                .append("3. 一级节点优先保持 5~6 个，子节点优先保持 2~4 个。\n")
                .append("4. 模块命名要专业、具体、适合学生端展示。\n")
                .append("5. 要覆盖主要章节内容，但不能出现空泛名称和重复节点。\n")
                .append("6. 如果章节中同时出现基础知识、专题技术、框架、项目部署等内容，要分层组织，不要混在同一模块里。\n")
                .append("7. 请直接输出严格 JSON。\n");

        return sb.toString();
    }

    private CourseMindmapVO parseAndValidateMindmap(String raw,
                                                    String courseName,
                                                    List<String> chapterTitles,
                                                    CourseMindmapVO fallbackMindmap) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }

        try {
            String json = extractJsonObject(raw);
            CourseMindmapVO parsed = OBJECT_MAPPER.readValue(json, CourseMindmapVO.class);
            CourseMindmapVO sanitized = sanitizeMindmap(parsed, courseName, chapterTitles, fallbackMindmap);
            return isValidMindmap(sanitized, chapterTitles.size()) ? sanitized : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractJsonObject(String raw) {
        String content = StringUtils.trimToEmpty(raw);

        if (content.startsWith("```")) {
            content = content
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();
        }

        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private CourseMindmapVO sanitizeMindmap(CourseMindmapVO raw,
                                            String courseName,
                                            List<String> chapterTitles,
                                            CourseMindmapVO fallbackMindmap) {
        if (raw == null) {
            return null;
        }

        CourseMindmapVO safeFallback = fallbackMindmap != null
                ? cloneMindmap(fallbackMindmap)
                : buildEnhancedFallbackMindmap(courseName, chapterTitles);

        CourseMindmapVO result = new CourseMindmapVO();
        result.setTitle(StringUtils.defaultIfBlank(
                normalizeText(raw.getTitle()),
                courseName + "课程思维导图"
        ));

        CourseMindmapVO.Node root = new CourseMindmapVO.Node();
        String rootName = courseName;
        if (raw.getRoot() != null && StringUtils.isNotBlank(normalizeText(raw.getRoot().getName()))) {
            rootName = normalizeText(raw.getRoot().getName());
        }
        root.setName(StringUtils.defaultIfBlank(rootName, courseName));

        int minFirstLevel = CourseMindmapRuleHelper.resolveMinFirstLevelCount(chapterTitles.size());
        int minSecondLevel = CourseMindmapRuleHelper.resolveMinSecondLevelCount(chapterTitles.size());

        List<CourseMindmapVO.Node> firstLevel = new ArrayList<>();
        LinkedHashSet<String> firstLevelKeys = new LinkedHashSet<>();
        LinkedHashSet<String> globalSecondLevelKeys = new LinkedHashSet<>();
        int lowQualityModuleCount = 0;

        if (raw.getRoot() != null && raw.getRoot().getChildren() != null) {
            for (CourseMindmapVO.Node rawFirst : raw.getRoot().getChildren()) {
                if (rawFirst == null) {
                    continue;
                }

                String firstName = CourseMindmapRuleHelper.cleanChapterTitle(rawFirst.getName());
                if (!CourseMindmapRuleHelper.isMeaningfulModuleName(firstName)) {
                    lowQualityModuleCount++;
                    continue;
                }
                if (CourseMindmapRuleHelper.isLowQualityModuleName(firstName)) {
                    lowQualityModuleCount++;
                    continue;
                }

                String firstKey = normalizeNodeKey(firstName);
                if (StringUtils.isBlank(firstKey) || !firstLevelKeys.add(firstKey)) {
                    continue;
                }

                CourseMindmapVO.Node first = new CourseMindmapVO.Node();
                first.setName(firstName);

                List<CourseMindmapVO.Node> secondLevel = buildSanitizedSecondLevel(
                        rawFirst,
                        firstName,
                        chapterTitles,
                        safeFallback,
                        globalSecondLevelKeys,
                        minSecondLevel
                );

                if (secondLevel.size() < minSecondLevel) {
                    lowQualityModuleCount++;
                }
                if (secondLevel.isEmpty()) {
                    continue;
                }

                first.setChildren(secondLevel);
                firstLevel.add(first);

                if (firstLevel.size() >= CourseMindmapRuleHelper.MAX_FIRST_LEVEL) {
                    break;
                }
            }
        }

        appendFallbackModules(firstLevel, firstLevelKeys, globalSecondLevelKeys, safeFallback, minFirstLevel);

        if (firstLevel.size() > CourseMindmapRuleHelper.MAX_FIRST_LEVEL) {
            firstLevel = new ArrayList<>(firstLevel.subList(0, CourseMindmapRuleHelper.MAX_FIRST_LEVEL));
        }

        if (firstLevel.size() < minFirstLevel) {
            return safeFallback;
        }

        int totalSecondCount = countSecondLevelNodes(firstLevel);
        int minimumExpectedChildren = Math.max(minFirstLevel, Math.min(chapterTitles.size(), minFirstLevel * 2));
        boolean poorQuality = lowQualityModuleCount > Math.max(1, firstLevel.size() / 2)
                || totalSecondCount < minimumExpectedChildren;
        if (poorQuality) {
            return safeFallback;
        }

        root.setChildren(firstLevel);
        result.setRoot(root);
        return result;
    }

    private List<CourseMindmapVO.Node> buildSanitizedSecondLevel(CourseMindmapVO.Node rawFirst,
                                                                 String moduleName,
                                                                 List<String> chapterTitles,
                                                                 CourseMindmapVO fallbackMindmap,
                                                                 LinkedHashSet<String> globalSecondLevelKeys,
                                                                 int minSecondLevel) {
        List<CourseMindmapVO.Node> secondLevel = new ArrayList<>();
        LinkedHashSet<String> localSecondLevelKeys = new LinkedHashSet<>();

        if (rawFirst.getChildren() != null) {
            for (CourseMindmapVO.Node rawSecond : rawFirst.getChildren()) {
                if (rawSecond == null) {
                    continue;
                }
                String secondName = CourseMindmapRuleHelper.cleanChapterTitle(rawSecond.getName());
                String secondKey = normalizeNodeKey(secondName);
                if (!CourseMindmapRuleHelper.isMeaningfulKnowledgeName(secondName)
                        || StringUtils.isBlank(secondKey)
                        || secondKey.equals(normalizeNodeKey(moduleName))
                        || !localSecondLevelKeys.add(secondKey)
                        || !globalSecondLevelKeys.add(secondKey)) {
                    continue;
                }

                CourseMindmapVO.Node second = new CourseMindmapVO.Node();
                second.setName(secondName);
                second.setChildren(new ArrayList<>());
                secondLevel.add(second);

                if (secondLevel.size() >= CourseMindmapRuleHelper.MAX_SECOND_LEVEL) {
                    break;
                }
            }
        }

        if (secondLevel.size() < minSecondLevel) {
            List<CourseMindmapVO.Node> fallbackChildren = findBestFallbackChildren(moduleName, fallbackMindmap);
            for (CourseMindmapVO.Node fallbackChild : fallbackChildren) {
                if (fallbackChild == null) {
                    continue;
                }
                String name = CourseMindmapRuleHelper.cleanChapterTitle(fallbackChild.getName());
                String key = normalizeNodeKey(name);
                if (!CourseMindmapRuleHelper.isMeaningfulKnowledgeName(name)
                        || StringUtils.isBlank(key)
                        || localSecondLevelKeys.contains(key)
                        || globalSecondLevelKeys.contains(key)) {
                    continue;
                }

                CourseMindmapVO.Node child = new CourseMindmapVO.Node();
                child.setName(name);
                child.setChildren(new ArrayList<>());
                secondLevel.add(child);
                localSecondLevelKeys.add(key);
                globalSecondLevelKeys.add(key);

                if (secondLevel.size() >= minSecondLevel) {
                    break;
                }
            }
        }

        if (secondLevel.size() < minSecondLevel) {
            for (String chapterTitle : chapterTitles) {
                String name = CourseMindmapRuleHelper.cleanChapterTitle(chapterTitle);
                String key = normalizeNodeKey(name);
                if (!CourseMindmapRuleHelper.isMeaningfulKnowledgeName(name)
                        || StringUtils.isBlank(key)
                        || localSecondLevelKeys.contains(key)
                        || globalSecondLevelKeys.contains(key)) {
                    continue;
                }

                CourseMindmapVO.Node child = new CourseMindmapVO.Node();
                child.setName(name);
                child.setChildren(new ArrayList<>());
                secondLevel.add(child);
                localSecondLevelKeys.add(key);
                globalSecondLevelKeys.add(key);

                if (secondLevel.size() >= minSecondLevel) {
                    break;
                }
            }
        }

        if (secondLevel.size() > CourseMindmapRuleHelper.MAX_SECOND_LEVEL) {
            return new ArrayList<>(secondLevel.subList(0, CourseMindmapRuleHelper.MAX_SECOND_LEVEL));
        }
        return secondLevel;
    }

    private List<CourseMindmapVO.Node> findBestFallbackChildren(String moduleName, CourseMindmapVO fallbackMindmap) {
        if (fallbackMindmap == null || fallbackMindmap.getRoot() == null || fallbackMindmap.getRoot().getChildren() == null) {
            return new ArrayList<>();
        }

        CourseMindmapVO.Node best = null;
        int bestScore = -1;
        for (CourseMindmapVO.Node node : fallbackMindmap.getRoot().getChildren()) {
            if (node == null) {
                continue;
            }
            int score = CourseMindmapRuleHelper.scoreLabelSimilarity(moduleName, node.getName());
            if (score > bestScore) {
                bestScore = score;
                best = node;
            }
        }
        return best != null && best.getChildren() != null ? best.getChildren() : new ArrayList<>();
    }

    private void appendFallbackModules(List<CourseMindmapVO.Node> firstLevel,
                                       LinkedHashSet<String> firstLevelKeys,
                                       LinkedHashSet<String> globalSecondLevelKeys,
                                       CourseMindmapVO fallbackMindmap,
                                       int targetCount) {
        if (fallbackMindmap == null || fallbackMindmap.getRoot() == null || fallbackMindmap.getRoot().getChildren() == null) {
            return;
        }

        for (CourseMindmapVO.Node fallbackFirst : fallbackMindmap.getRoot().getChildren()) {
            if (fallbackFirst == null) {
                continue;
            }
            String firstName = CourseMindmapRuleHelper.cleanChapterTitle(fallbackFirst.getName());
            String firstKey = normalizeNodeKey(firstName);
            if (!CourseMindmapRuleHelper.isMeaningfulModuleName(firstName)
                    || StringUtils.isBlank(firstKey)
                    || !firstLevelKeys.add(firstKey)) {
                continue;
            }

            CourseMindmapVO.Node first = new CourseMindmapVO.Node();
            first.setName(firstName);
            first.setChildren(new ArrayList<>());

            LinkedHashSet<String> localSecondKeys = new LinkedHashSet<>();
            if (fallbackFirst.getChildren() != null) {
                for (CourseMindmapVO.Node fallbackChild : fallbackFirst.getChildren()) {
                    if (fallbackChild == null) {
                        continue;
                    }
                    String childName = CourseMindmapRuleHelper.cleanChapterTitle(fallbackChild.getName());
                    String childKey = normalizeNodeKey(childName);
                    if (!CourseMindmapRuleHelper.isMeaningfulKnowledgeName(childName)
                            || StringUtils.isBlank(childKey)
                            || !localSecondKeys.add(childKey)
                            || !globalSecondLevelKeys.add(childKey)) {
                        continue;
                    }
                    CourseMindmapVO.Node child = new CourseMindmapVO.Node();
                    child.setName(childName);
                    child.setChildren(new ArrayList<>());
                    first.getChildren().add(child);

                    if (first.getChildren().size() >= CourseMindmapRuleHelper.MAX_SECOND_LEVEL) {
                        break;
                    }
                }
            }

            if (!first.getChildren().isEmpty()) {
                firstLevel.add(first);
            }
            if (firstLevel.size() >= targetCount || firstLevel.size() >= CourseMindmapRuleHelper.MAX_FIRST_LEVEL) {
                break;
            }
        }
    }

    private int countSecondLevelNodes(List<CourseMindmapVO.Node> firstLevel) {
        int count = 0;
        for (CourseMindmapVO.Node node : firstLevel) {
            if (node != null && node.getChildren() != null) {
                count += node.getChildren().size();
            }
        }
        return count;
    }

    private boolean isValidMindmap(CourseMindmapVO mindmap, int chapterCount) {
        if (mindmap == null || mindmap.getRoot() == null || StringUtils.isBlank(mindmap.getRoot().getName())) {
            return false;
        }
        if (mindmap.getRoot().getChildren() == null || mindmap.getRoot().getChildren().isEmpty()) {
            return false;
        }

        int minFirstLevel = CourseMindmapRuleHelper.resolveMinFirstLevelCount(chapterCount);
        if (mindmap.getRoot().getChildren().size() < minFirstLevel) {
            return false;
        }

        for (CourseMindmapVO.Node first : mindmap.getRoot().getChildren()) {
            if (first == null || StringUtils.isBlank(first.getName())) {
                return false;
            }
        }
        return true;
    }

    private CourseMindmapVO buildEnhancedFallbackMindmap(String courseName, List<String> chapterTitles) {
        return CourseMindmapRuleHelper.buildKeywordFallbackMindmap(courseName, chapterTitles);
    }

    private void saveMindmapCache(Long courseId,
                                  CourseMindmapVO mindmapVO,
                                  String sourceHash,
                                  String status) {
        try {
            CourseMindmap entity = new CourseMindmap();
            entity.setCourseId(courseId);
            entity.setTitle(StringUtils.defaultIfBlank(mindmapVO.getTitle(), "课程思维导图"));
            entity.setMindmapJson(OBJECT_MAPPER.writeValueAsString(stripMeta(mindmapVO)));
            entity.setSourceHash(sourceHash);
            entity.setStatus(status);
            courseMindmapMapper.upsert(entity);
        } catch (Exception ignored) {
        }
    }

    private CourseMindmapVO attachMeta(CourseMindmapVO vo,
                                       String status,
                                       String sourceHash,
                                       Date updatedAt) {
        vo.setStatus(StringUtils.defaultIfBlank(status, "ready"));
        vo.setSourceHash(StringUtils.defaultString(sourceHash));
        vo.setUpdatedAt(formatDate(updatedAt));
        return vo;
    }

    private CourseMindmapVO stripMeta(CourseMindmapVO source) {
        CourseMindmapVO target = new CourseMindmapVO();
        target.setTitle(source.getTitle());
        target.setRoot(source.getRoot());
        return target;
    }

    private CourseMindmapVO cloneMindmap(CourseMindmapVO source) {
        try {
            return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(source), CourseMindmapVO.class);
        } catch (Exception ignored) {
            return source;
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_TIME_FORMATTER);
    }

    private String collectOptionalCourseHints(Course course) {
        List<String> candidates = Arrays.asList(
                readOptionalStringField(course, "intro"),
                readOptionalStringField(course, "summary"),
                readOptionalStringField(course, "outline"),
                readOptionalStringField(course, "structureInfo"),
                readOptionalStringField(course, "courseStructure"),
                readOptionalStringField(course, "structureText"),
                readOptionalStringField(course, "catalogText")
        );
        return joinNonBlank(candidates);
    }

    private String collectOptionalChapterHint(CourseChapter chapter) {
        List<String> candidates = Arrays.asList(
                readOptionalStringField(chapter, "description"),
                readOptionalStringField(chapter, "summary"),
                readOptionalStringField(chapter, "intro"),
                readOptionalStringField(chapter, "outline"),
                readOptionalStringField(chapter, "remark"),
                readOptionalStringField(chapter, "content")
        );
        return joinNonBlank(candidates);
    }

    private String readOptionalStringField(Object target, String fieldName) {
        if (target == null || StringUtils.isBlank(fieldName)) {
            return "";
        }

        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof String) {
                    return normalizeText((String) value);
                }
                return "";
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    private String joinNonBlank(List<String> items) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String item : items) {
            String value = normalizeText(item);
            if (StringUtils.isNotBlank(value)) {
                set.add(value);
            }
        }
        return String.join("；", set);
    }

    private String normalizeText(String text) {
        return CourseMindmapRuleHelper.normalizeText(text);
    }

    private String normalizeNodeKey(String text) {
        return CourseMindmapRuleHelper.normalizeNodeKey(text);
    }
}
