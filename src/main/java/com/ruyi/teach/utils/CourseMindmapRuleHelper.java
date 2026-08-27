package com.ruyi.teach.utils;

import com.ruyi.teach.model.vo.CourseMindmapVO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public final class CourseMindmapRuleHelper {

    public static final int MAX_FIRST_LEVEL = 6;
    public static final int MIN_FIRST_LEVEL = 3;
    public static final int MAX_SECOND_LEVEL = 4;
    public static final int MIN_SECOND_LEVEL = 2;

    private static final List<String> GENERIC_NAMES = Arrays.asList(
            "模块", "知识点", "内容", "主要内容", "核心内容", "课程内容", "课程模块", "学习内容",
            "第一部分", "第二部分", "第三部分", "第四部分", "第五部分", "第六部分",
            "模块一", "模块二", "模块三", "模块四", "模块五", "模块六",
            "部分一", "部分二", "部分三", "部分四", "部分五", "部分六"
    );

    private static final List<Template> TECH_TEMPLATES = Arrays.asList(
            new Template("基础与环境准备", Arrays.asList(
                    "导论", "概述", "基础", "入门", "认识", "简介", "环境", "安装", "配置", "开发工具",
                    "web基础", "html", "css", "javascript", "语法", "快速开始"
            )),
            new Template("核心概念与运行机制", Arrays.asList(
                    "核心", "原理", "机制", "生命周期", "对象", "类", "接口", "组件", "结构",
                    "容器", "流程", "架构", "规范", "mvc", "监听器", "过滤器"
            )),
            new Template("请求处理与会话管理", Arrays.asList(
                    "请求", "响应", "会话", "cookie", "session", "servlet", "jsp", "ajax", "表单",
                    "参数", "路由", "controller", "通信", "接口"
            )),
            new Template("数据访问与持久化", Arrays.asList(
                    "数据库", "sql", "jdbc", "mybatis", "持久化", "事务", "dao", "mapper", "redis",
                    "缓存", "数据源", "orm"
            )),
            new Template("框架整合与工程实践", Arrays.asList(
                    "spring", "springboot", "框架", "整合", "拦截器", "权限", "安全", "工程化",
                    "日志", "测试", "前后端分离", "工程", "构建"
            )),
            new Template("项目实战与部署优化", Arrays.asList(
                    "项目", "案例", "实战", "部署", "发布", "上线", "打包", "性能", "优化",
                    "监控", "docker", "nginx", "linux"
            ))
    );

    private static final List<Template> GENERAL_TEMPLATES = Arrays.asList(
            new Template("课程导入与基础认知", Arrays.asList(
                    "导论", "概述", "认识", "基础", "入门", "背景", "简介", "总览"
            )),
            new Template("核心概念与基本原理", Arrays.asList(
                    "概念", "原理", "理论", "基础知识", "结构", "核心", "框架"
            )),
            new Template("方法流程与关键步骤", Arrays.asList(
                    "方法", "流程", "步骤", "策略", "分析", "设计", "实现"
            )),
            new Template("重点专题与能力训练", Arrays.asList(
                    "专题", "训练", "技巧", "进阶", "拓展", "应用", "能力"
            )),
            new Template("综合应用与案例实践", Arrays.asList(
                    "案例", "实践", "综合", "任务", "实训", "项目", "应用"
            )),
            new Template("复盘总结与拓展提升", Arrays.asList(
                    "总结", "复习", "提升", "拓展", "评估", "优化", "复盘"
            ))
    );

    private static final List<String> TECH_HINT_WORDS = Arrays.asList(
            "java", "web", "spring", "springboot", "数据库", "sql", "jdbc", "mybatis", "redis",
            "html", "css", "javascript", "vue", "react", "前端", "后端", "框架", "接口",
            "网络", "算法", "编程", "开发", "部署", "docker", "nginx", "linux", "servlet", "jsp"
    );

    private CourseMindmapRuleHelper() {
    }

    public static String normalizeText(String text) {
        return StringUtils.trimToEmpty(text).replaceAll("\\s+", " ");
    }

    public static String cleanChapterTitle(String title) {
        String text = normalizeText(title);
        if (StringUtils.isBlank(text)) {
            return "";
        }

        String cleaned = text
                .replaceAll("^(第?[0-9一二三四五六七八九十]+[章节单元课讲部分篇])[:：、.．\\-\\s]*", "")
                .replaceAll("^(chapter|part|section)\\s*[0-9]+[:：、.．\\-\\s]*", "")
                .replaceAll("^[0-9]+(\\.[0-9]+)*[:：、.．\\-\\s]+", "")
                .replaceAll("^(专题|实验|案例|项目|任务)[0-9一二三四五六七八九十]+[:：、.．\\-\\s]*", "")
                .replaceAll("^[【\\[](.*?)[】\\]]", "$1")
                .replaceAll("^[：:、.．\\-\\s]+", "")
                .replaceAll("[：:、.．\\-\\s]+$", "")
                .trim();

        return StringUtils.defaultIfBlank(cleaned, text);
    }

    public static List<String> normalizeChapterTitles(List<String> rawTitles) {
        List<String> result = new ArrayList<>();
        if (rawTitles == null || rawTitles.isEmpty()) {
            return result;
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String rawTitle : rawTitles) {
            String title = cleanChapterTitle(rawTitle);
            if (StringUtils.isBlank(title)) {
                continue;
            }
            String key = normalizeNodeKey(title);
            if (StringUtils.isBlank(key) || !keys.add(key)) {
                continue;
            }
            result.add(title);
        }
        return result;
    }

    public static String normalizeNodeKey(String text) {
        return normalizeText(text)
                .toLowerCase()
                .replaceAll("[\\s\\p{Punct}，。！？、；：·（）()\\[\\]【】《》“”‘’]", "");
    }

    public static boolean isMeaningfulModuleName(String name) {
        String text = cleanChapterTitle(name);
        if (StringUtils.isBlank(text)) {
            return false;
        }

        String compact = text.replaceAll("\\s+", "");
        if (compact.length() < 2 || compact.length() > 18) {
            return false;
        }
        if (GENERIC_NAMES.contains(compact)) {
            return false;
        }
        return !compact.matches("^(模块|部分|单元|专题)[一二三四五六七八九十0-9]+$")
                && !compact.matches("^第[一二三四五六七八九十0-9]+(部分|模块|单元)$")
                && !compact.matches("^part[0-9]+$");
    }

    public static boolean isLowQualityModuleName(String name) {
        String text = cleanChapterTitle(name).replaceAll("\\s+", "");
        if (StringUtils.isBlank(text)) {
            return true;
        }
        if (!isMeaningfulModuleName(text)) {
            return true;
        }
        return text.equals("课程重点")
                || text.equals("课程总览")
                || text.equals("学习重点")
                || text.equals("综合内容")
                || text.endsWith("模块")
                || text.endsWith("部分")
                || text.endsWith("内容");
    }

    public static boolean isMeaningfulKnowledgeName(String name) {
        String text = cleanChapterTitle(name);
        if (StringUtils.isBlank(text)) {
            return false;
        }

        String compact = text.replaceAll("\\s+", "");
        if (compact.length() < 2 || compact.length() > 22) {
            return false;
        }
        if (compact.matches("^[0-9.]+$")) {
            return false;
        }
        return !GENERIC_NAMES.contains(compact)
                && !compact.equals("知识点")
                && !compact.equals("重点")
                && !compact.equals("核心知识")
                && !compact.equals("主要知识");
    }

    public static int resolveDesiredFirstLevelCount(int chapterCount) {
        if (chapterCount >= 12) {
            return 6;
        }
        if (chapterCount >= 8) {
            return 5;
        }
        if (chapterCount >= 5) {
            return 4;
        }
        return Math.max(1, Math.min(3, chapterCount));
    }

    public static int resolveMinFirstLevelCount(int chapterCount) {
        if (chapterCount >= 8) {
            return 4;
        }
        if (chapterCount >= 4) {
            return 3;
        }
        return 1;
    }

    public static int resolveMinSecondLevelCount(int chapterCount) {
        return chapterCount >= 4 ? MIN_SECOND_LEVEL : 1;
    }

    public static boolean isTechCourse(String courseName, List<String> chapterTitles) {
        StringBuilder sb = new StringBuilder(normalizeText(courseName).toLowerCase());
        if (chapterTitles != null) {
            for (String chapterTitle : chapterTitles) {
                sb.append(' ').append(normalizeText(chapterTitle).toLowerCase());
            }
        }

        String text = sb.toString();
        int score = 0;
        for (String word : TECH_HINT_WORDS) {
            if (text.contains(word)) {
                score++;
            }
        }
        return score >= 2;
    }

    public static List<String> suggestModuleLabels(String courseName, List<String> chapterTitles) {
        int desired = resolveDesiredFirstLevelCount(chapterTitles == null ? 0 : chapterTitles.size());
        List<Template> templates = pickTemplates(courseName, chapterTitles, desired);
        List<String> labels = new ArrayList<>();
        for (Template template : templates) {
            labels.add(template.name);
        }
        return labels;
    }

    public static CourseMindmapVO buildKeywordFallbackMindmap(String courseName, List<String> chapterTitles) {
        List<String> normalizedTitles = normalizeChapterTitles(chapterTitles);

        CourseMindmapVO vo = new CourseMindmapVO();
        vo.setTitle(StringUtils.defaultIfBlank(courseName, "课程") + "课程思维导图");

        CourseMindmapVO.Node root = new CourseMindmapVO.Node();
        root.setName(StringUtils.defaultIfBlank(courseName, "未命名课程"));

        if (normalizedTitles.isEmpty()) {
            CourseMindmapVO.Node child = new CourseMindmapVO.Node();
            child.setName("课程总览");
            child.setChildren(new ArrayList<>());
            root.setChildren(new ArrayList<>(Arrays.asList(child)));
            vo.setRoot(root);
            return vo;
        }

        int desiredBucketCount = resolveDesiredFirstLevelCount(normalizedTitles.size());
        List<Template> templates = pickTemplates(courseName, normalizedTitles, desiredBucketCount);
        List<List<String>> buckets = assignTitlesToBuckets(normalizedTitles, templates);
        rebalanceBuckets(buckets, resolveMinSecondLevelCount(normalizedTitles.size()));

        List<CourseMindmapVO.Node> modules = new ArrayList<>();
        LinkedHashSet<String> globalChildKeys = new LinkedHashSet<>();

        for (int i = 0; i < templates.size(); i++) {
            List<String> bucket = buckets.get(i);
            if (bucket.isEmpty()) {
                continue;
            }

            CourseMindmapVO.Node module = new CourseMindmapVO.Node();
            module.setName(templates.get(i).name);
            module.setChildren(buildChildren(bucket, globalChildKeys));
            if (!module.getChildren().isEmpty()) {
                modules.add(module);
            }
        }

        if (modules.isEmpty()) {
            CourseMindmapVO.Node module = new CourseMindmapVO.Node();
            module.setName(isTechCourse(courseName, normalizedTitles) ? "课程结构总览" : "课程内容总览");
            module.setChildren(buildChildren(normalizedTitles, globalChildKeys));
            if (module.getChildren().isEmpty()) {
                module.setChildren(new ArrayList<>());
            }
            modules.add(module);
        }

        root.setChildren(modules);
        vo.setRoot(root);
        return vo;
    }

    public static int scoreLabelSimilarity(String left, String right) {
        String leftKey = normalizeNodeKey(left);
        String rightKey = normalizeNodeKey(right);
        if (StringUtils.isBlank(leftKey) || StringUtils.isBlank(rightKey)) {
            return 0;
        }
        if (leftKey.equals(rightKey)) {
            return 100;
        }
        if (leftKey.contains(rightKey) || rightKey.contains(leftKey)) {
            return 70;
        }

        int shared = 0;
        for (int i = 0; i < leftKey.length(); i++) {
            char c = leftKey.charAt(i);
            if (rightKey.indexOf(c) >= 0) {
                shared++;
            }
        }

        int score = shared * 6;
        for (String hint : Arrays.asList("基础", "核心", "请求", "会话", "数据", "框架", "项目", "部署", "实践")) {
            if (left.contains(hint) && right.contains(hint)) {
                score += 12;
            }
        }
        return score;
    }

    private static List<Template> pickTemplates(String courseName, List<String> chapterTitles, int desiredCount) {
        List<Template> source = isTechCourse(courseName, chapterTitles) ? TECH_TEMPLATES : GENERAL_TEMPLATES;
        int count = Math.max(1, Math.min(desiredCount, source.size()));
        return new ArrayList<>(source.subList(0, count));
    }

    private static List<List<String>> assignTitlesToBuckets(List<String> titles, List<Template> templates) {
        List<List<String>> buckets = new ArrayList<>();
        for (int i = 0; i < templates.size(); i++) {
            buckets.add(new ArrayList<>());
        }

        int bucketCount = templates.size();
        int total = titles.size();
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);
            int bestIndex = -1;
            int bestScore = -1;

            for (int j = 0; j < templates.size(); j++) {
                int score = scoreTitleForTemplate(title, templates.get(j));
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = j;
                }
            }

            if (bestScore <= 0) {
                if (total <= 1) {
                    bestIndex = 0;
                } else {
                    bestIndex = Math.min(bucketCount - 1, (int) Math.floor((double) i * bucketCount / total));
                }
            }

            buckets.get(bestIndex).add(title);
        }
        return buckets;
    }

    private static void rebalanceBuckets(List<List<String>> buckets, int minPerBucket) {
        if (buckets == null || buckets.isEmpty()) {
            return;
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            int sparseIndex = findSparseBucket(buckets, minPerBucket);
            int donorIndex = findDonorBucket(buckets, minPerBucket);
            if (sparseIndex >= 0 && donorIndex >= 0 && donorIndex != sparseIndex) {
                List<String> donor = buckets.get(donorIndex);
                String moved = donor.remove(donor.size() - 1);
                buckets.get(sparseIndex).add(moved);
                changed = true;
            }
        }
    }

    private static int findSparseBucket(List<List<String>> buckets, int minPerBucket) {
        for (int i = 0; i < buckets.size(); i++) {
            List<String> bucket = buckets.get(i);
            if (!bucket.isEmpty() && bucket.size() < minPerBucket) {
                return i;
            }
        }
        for (int i = 0; i < buckets.size(); i++) {
            if (buckets.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static int findDonorBucket(List<List<String>> buckets, int minPerBucket) {
        int bestIndex = -1;
        int maxSize = minPerBucket;
        for (int i = 0; i < buckets.size(); i++) {
            int size = buckets.get(i).size();
            if (size > maxSize) {
                maxSize = size;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static int scoreTitleForTemplate(String title, Template template) {
        String text = normalizeText(title).toLowerCase();
        int score = 0;
        for (String keyword : template.keywords) {
            if (text.contains(keyword.toLowerCase())) {
                score += 10;
            }
        }
        for (String moduleWord : splitLabelWords(template.name)) {
            if (text.contains(moduleWord)) {
                score += 4;
            }
        }
        return score;
    }

    private static List<String> splitLabelWords(String label) {
        List<String> words = new ArrayList<>();
        for (String part : label.split("与|和|及|、")) {
            String text = normalizeText(part).replace("实践", "").replace("应用", "");
            if (StringUtils.isNotBlank(text)) {
                words.add(text);
            }
        }
        return words;
    }

    private static List<CourseMindmapVO.Node> buildChildren(List<String> bucket, LinkedHashSet<String> globalChildKeys) {
        List<CourseMindmapVO.Node> children = new ArrayList<>();
        LinkedHashSet<String> localKeys = new LinkedHashSet<>();

        for (String raw : bucket) {
            String name = buildKnowledgePointName(raw);
            String key = normalizeNodeKey(name);
            if (!isMeaningfulKnowledgeName(name)
                    || StringUtils.isBlank(key)
                    || !localKeys.add(key)
                    || !globalChildKeys.add(key)) {
                continue;
            }

            CourseMindmapVO.Node child = new CourseMindmapVO.Node();
            child.setName(name);
            child.setChildren(new ArrayList<>());
            children.add(child);

            if (children.size() >= MAX_SECOND_LEVEL) {
                break;
            }
        }

        return children;
    }

    private static String buildKnowledgePointName(String raw) {
        String title = cleanChapterTitle(raw);
        title = title.replaceAll("^(认识|了解|掌握|学习|理解)", "").trim();
        return StringUtils.defaultIfBlank(title, raw);
    }

    private static final class Template {
        private final String name;
        private final List<String> keywords;

        private Template(String name, List<String> keywords) {
            this.name = name;
            this.keywords = keywords;
        }
    }
}