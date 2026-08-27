package com.ruyi.teach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruyi.teach.client.RemoteResourceClient;
import com.ruyi.teach.mapper.TeachingCaseMapper;
import com.ruyi.teach.model.entity.TeachingCase;
import com.ruyi.teach.util.CaseDocumentTextExtractor;
import jakarta.annotation.Resource;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class PlatformTeachingCaseService {

    public static final String SCOPE_MINE = "mine";
    public static final String SCOPE_PLATFORM = "platform";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_OFFLINE = "offline";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
    private static final Pattern LINK_PATTERN = Pattern.compile("(?is)<a\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>");
    private static final Pattern XML_LINK_PATTERN = Pattern.compile("(?is)<link>(https?://.*?)</link>");
    private static final Pattern META_CHARSET_PATTERN = Pattern.compile("(?is)<meta[^>]+charset=[\"']?([a-zA-Z0-9_\\-]+)");
    private static final Pattern IMG_PATTERN = Pattern.compile("(?is)<img\\s+[^>]*src=[\"']([^\"']+)[\"'][^>]*>");
    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile("(?is)<(script|style|noscript)[^>]*>.*?</\\1>");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final int TEACHER_CRAWL_MAX_COUNT = 20;
    private static final int TEACHER_RECOMMEND_LIMIT = 5;
    private static final int TEACHER_AUTO_CASE_LIMIT = 3;
    private static final int TEACHER_SEARCH_DEADLINE_MS = 6000;
    private static final int TEACHER_CRAWL_DEADLINE_MS = 20000;
    private static final int TEACHER_CRAWL_CONNECT_TIMEOUT_MS = 3000;
    private static final int TEACHER_CRAWL_READ_TIMEOUT_MS = 6000;
    private static final int CRAWL_FILE_CONNECT_TIMEOUT_MS = 5000;
    private static final int CRAWL_FILE_READ_TIMEOUT_MS = 20000;
    private static final int CRAWL_MAX_FILE_BYTES = 25 * 1024 * 1024;
    private static final int CRAWL_MAX_PAGE_BYTES = 5 * 1024 * 1024;
    private static final int MIN_KEYWORD_SCORE = 8;
    private static final int PREVIEW_TEXT_LIMIT = 8000;
    private static final int CASE_RECOMMEND_CANDIDATE_LIMIT = 60;
    private static final int CASE_RECOMMEND_TOPIC_EVIDENCE_THRESHOLD = 32;
    private static final int CASE_RECOMMEND_WEAK_EVIDENCE_THRESHOLD = 12;
    private static final String PREVIEW_TYPE_DOCUMENT = "document";
    private static final String PREVIEW_TYPE_PAGE = "page";
    private static final Map<String, String> CASE_RECOMMEND_TEXT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> DATA_STRUCTURE_TOPIC_CHILDREN = Map.ofEntries(
            Map.entry("图", List.of("图的广度优先搜索", "广度优先搜索", "图的广度搜索", "广度搜索", "图的深度优先搜索", "深度优先搜索", "图的深度搜索", "深度搜索", "邻接表", "邻接矩阵", "最短路径", "拓扑排序")),
            Map.entry("树", List.of("二叉树", "二叉搜索树", "二叉排序树", "平衡二叉树", "树的遍历", "先序遍历", "中序遍历", "后序遍历")),
            Map.entry("二叉树", List.of("二叉树的遍历", "先序遍历", "中序遍历", "后序遍历", "层序遍历")),
            Map.entry("排序", List.of("冒泡排序", "选择排序", "插入排序", "快速排序", "归并排序", "堆排序", "希尔排序")),
            Map.entry("查找", List.of("折半查找", "二分查找", "顺序查找", "哈希查找", "散列表")),
            Map.entry("队列", List.of("顺序队列", "循环队列", "链式队列", "优先队列", "双端队列")),
            Map.entry("栈", List.of("顺序栈", "链栈", "括号匹配", "表达式求值", "逆波兰表达式")),
            Map.entry("链表", List.of("单链表", "双向链表", "循环链表", "静态链表")),
            Map.entry("线性表", List.of("顺序表", "链表", "单链表", "双向链表", "栈", "队列", "循环队列"))
    );
    private static final Map<String, List<String>> DATA_STRUCTURE_TOPIC_ALIASES = Map.ofEntries(
            Map.entry("广度优先搜索", List.of("bfs", "图的广度优先搜索", "图的广度搜索", "广度搜索")),
            Map.entry("深度优先搜索", List.of("dfs", "图的深度优先搜索", "图的深度搜索", "深度搜索")),
            Map.entry("折半查找", List.of("二分查找", "binarysearch")),
            Map.entry("二分查找", List.of("折半查找", "binarysearch")),
            Map.entry("二叉排序树", List.of("二叉搜索树", "bst")),
            Map.entry("二叉搜索树", List.of("二叉排序树", "bst")),
            Map.entry("队列", List.of("queue")),
            Map.entry("栈", List.of("stack")),
            Map.entry("冒泡排序", List.of("bubblesort")),
            Map.entry("二叉树", List.of("binarytree"))
    );

    private static final Set<String> QUERY_STOP_WORDS = Set.of(
            "\u6559\u5b66", "\u6848\u4f8b", "\u6559\u6848", "\u8bfe\u7a0b", "\u8bbe\u8ba1",
            "\u9879\u76ee", "\u5b9e\u8df5", "\u5b9e\u8bad", "\u9ad8\u6821", "\u8ba1\u7b97\u673a",
            "\u516c\u5f00", "\u8d44\u6e90", "pdf", "word"
    );

    private static final List<String> DEFAULT_SOURCE_URLS = List.of(
            "https://jwc.hrbu.edu.cn/info/1433/7127.htm",
            "https://cse.csu.edu.cn/info/1039/11148.htm",
            "https://jsj.jxue.edu.cn/2023/1215/c347a22834/page.htm",
            "https://ce.njit.edu.cn/__local/B/14/40/384ADEC7E172C7AE85CD81B7D46_903290C7_D746D.pdf",
            "https://www.cs.tsinghua.edu.cn/",
            "https://cs.pku.edu.cn/",
            "https://cs.nankai.edu.cn/",
            "https://cs.xmu.edu.cn/",
            "https://computer.scnu.edu.cn/",
            "https://computer.icourses.cn/"
    );

    private static final List<String> PREFERRED_SEARCH_DOMAINS = List.of(
            "edu.cn",
            "higher.smartedu.cn",
            "icourse163.org",
            "computer.icourses.cn",
            "xuetangx.com",
            "educoder.net"
    );

    @Resource
    private TeachingCaseMapper teachingCaseMapper;

    @Resource
    private AgentIndexService agentIndexService;

    @Resource
    private OssService ossService;

    @Resource
    private TeachingCaseAssetService teachingCaseAssetService;

    @Resource
    private RemoteResourceClient remoteResourceClient;

    @Resource
    private RemoteDocumentTextService remoteDocumentTextService;

    public List<TeachingCase> crawlToPending(String keyword, String sourceUrl, Long adminId) {
        List<String> urls = new ArrayList<>();
        if (StringUtils.isNotBlank(sourceUrl)) {
            urls.add(sourceUrl.trim());
        } else {
            urls.addAll(DEFAULT_SOURCE_URLS);
        }

        List<TeachingCase> result = new ArrayList<>();
        String query = StringUtils.defaultString(keyword).trim();
        boolean strictKeyword = StringUtils.isNotBlank(query);
        for (String url : urls) {
            CrawledCase crawled = crawl(url);
            if (crawled == null) {
                continue;
            }
            if (strictKeyword && keywordScore(crawled, query) < MIN_KEYWORD_SCORE) {
                continue;
            }
            TeachingCase entity = upsertPendingCase(crawled, query, adminId);
            result.add(entity);
        }
        return result;
    }

    public List<TeachingCase> crawlForTeacher(String keyword, Integer count, Long teacherId) {
        String query = StringUtils.trimToEmpty(keyword);
        int safeCount = Math.min(Math.max(count == null ? 5 : count, 1), TEACHER_CRAWL_MAX_COUNT);
        List<String> urls = searchCandidateUrls(query, Math.max(safeCount * 3, 12));
        List<TeachingCase> result = new ArrayList<>();

        List<String> candidates = urls.stream()
                .limit(Math.max(safeCount * 3L, 12L))
                .toList();
        if (candidates.isEmpty()) {
            return result;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(8, candidates.size()));
        CompletionService<CrawledCase> completionService = new ExecutorCompletionService<>(executor);
        try {
            for (String url : candidates) {
                completionService.submit(() -> crawl(url, TEACHER_CRAWL_CONNECT_TIMEOUT_MS, TEACHER_CRAWL_READ_TIMEOUT_MS));
            }

            long deadline = System.currentTimeMillis() + TEACHER_CRAWL_DEADLINE_MS;
            int remaining = candidates.size();
            while (remaining > 0 && result.size() < safeCount && System.currentTimeMillis() < deadline) {
                long waitMs = Math.max(1, deadline - System.currentTimeMillis());
                Future<CrawledCase> future = completionService.poll(waitMs, TimeUnit.MILLISECONDS);
                if (future == null) {
                    break;
                }
                remaining--;

                CrawledCase crawled;
                try {
                    crawled = future.get();
                } catch (Exception ignored) {
                    continue;
                }
                if (crawled == null) {
                    continue;
                }
                if (keywordScore(crawled, query) >= MIN_KEYWORD_SCORE) {
                    TeachingCase entity = upsertTeacherCase(crawled, query, teacherId);
                    agentIndexService.upsertTeachingCase(teachingCaseMapper.selectById(entity.getId()));
                    result.add(entity);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        return result;
    }

    public Page<TeachingCase> pagePlatformCases(long current, long size, String status, String keyword) {
        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        qw.eq(TeachingCase::getScope, SCOPE_PLATFORM)
                .eq(TeachingCase::getIsDelete, 0)
                .eq(StringUtils.isNotBlank(status), TeachingCase::getStatus, status)
                .and(StringUtils.isNotBlank(keyword), wrapper -> wrapper
                        .like(TeachingCase::getTitle, keyword)
                        .or()
                        .like(TeachingCase::getCourseName, keyword)
                        .or()
                        .like(TeachingCase::getKeywords, keyword)
                        .or()
                        .like(TeachingCase::getSummary, keyword))
                .orderByDesc(TeachingCase::getUpdateTime);
        return teachingCaseMapper.selectPage(new Page<>(Math.max(current, 1), Math.min(Math.max(size, 1), 50)), qw);
    }

    public List<RecommendCaseVO> recommend(RecommendRequest req) {
        return recommend(req, null, TEACHER_RECOMMEND_LIMIT);
    }

    public List<RecommendCaseVO> recommend(RecommendRequest req, Long teacherId) {
        return recommend(req, teacherId, TEACHER_RECOMMEND_LIMIT);
    }

    public List<Long> autoRecommendCaseIds(RecommendRequest req, Long teacherId) {
        return recommend(req, teacherId, TEACHER_AUTO_CASE_LIMIT).stream()
                .map(RecommendCaseVO::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<RecommendCaseVO> recommend(RecommendRequest req, Long teacherId, int limit) {
        RecommendRequest safeReq = req == null ? new RecommendRequest() : req;
        String query = StringUtils.joinWith(" ",
                safeReq.getSubject(),
                safeReq.getGrade(),
                safeReq.getTopic(),
                safeReq.getLessonType(),
                safeReq.getCourseName()
        );

        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        qw.eq(TeachingCase::getIsDelete, 0);
        if (teacherId == null) {
            qw.eq(TeachingCase::getScope, SCOPE_PLATFORM)
                    .eq(TeachingCase::getStatus, STATUS_APPROVED);
        } else {
            qw.and(wrapper -> wrapper
                    .and(mine -> mine
                            .eq(TeachingCase::getTeacherId, teacherId)
                            .and(scope -> scope
                                    .eq(TeachingCase::getScope, SCOPE_MINE)
                                    .or()
                                    .isNull(TeachingCase::getScope)
                                    .or()
                                    .eq(TeachingCase::getScope, "")))
                    .or(platform -> platform
                            .eq(TeachingCase::getScope, SCOPE_PLATFORM)
                            .eq(TeachingCase::getStatus, STATUS_APPROVED)));
        }
        qw.orderByDesc(TeachingCase::getUpdateTime);
        List<TeachingCase> cases = teachingCaseMapper.selectList(qw);

        return cases.stream()
                .map(item -> new ScoredCase(item, score(item, query), score(item, query), EvidenceResult.empty()))
                .sorted((a, b) -> Integer.compare(b.metadataScore, a.metadataScore))
                .limit(CASE_RECOMMEND_CANDIDATE_LIMIT)
                .map(item -> scoreRecommendedCase(item.teachingCase, safeReq, query, item.metadataScore))
                .filter(item -> item.score > 0 || cases.size() <= 5)
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(Math.max(limit, 1))
                .map(item -> toRecommendVO(item, safeReq))
                .toList();
    }

    public void approve(Long id, Long reviewerId) {
        TeachingCase update = new TeachingCase();
        update.setId(id);
        update.setStatus(STATUS_APPROVED);
        update.setReviewTime(new Date());
        update.setReviewerId(reviewerId);
        teachingCaseMapper.updateById(update);
        TeachingCase fresh = teachingCaseMapper.selectById(id);
        agentIndexService.upsertTeachingCase(fresh);
    }

    public void reject(Long id, Long reviewerId) {
        TeachingCase update = new TeachingCase();
        update.setId(id);
        update.setStatus(STATUS_REJECTED);
        update.setReviewTime(new Date());
        update.setReviewerId(reviewerId);
        teachingCaseMapper.updateById(update);
        TeachingCase fresh = teachingCaseMapper.selectById(id);
        if (fresh != null) {
            agentIndexService.deleteTeachingCase(fresh.getTeacherId(), id);
        }
    }

    public void offline(Long id, Long reviewerId) {
        TeachingCase update = new TeachingCase();
        update.setId(id);
        update.setStatus(STATUS_OFFLINE);
        update.setReviewTime(new Date());
        update.setReviewerId(reviewerId);
        teachingCaseMapper.updateById(update);
        TeachingCase fresh = teachingCaseMapper.selectById(id);
        if (fresh != null) {
            agentIndexService.deleteTeachingCase(fresh.getTeacherId(), id);
        }
    }

    public void rebuildApprovedPlatformIndex() {
        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        qw.eq(TeachingCase::getScope, SCOPE_PLATFORM)
                .eq(TeachingCase::getStatus, STATUS_APPROVED)
                .eq(TeachingCase::getIsDelete, 0);
        for (TeachingCase teachingCase : teachingCaseMapper.selectList(qw)) {
            agentIndexService.upsertTeachingCase(teachingCase);
        }
    }

    private TeachingCase upsertPendingCase(CrawledCase crawled, String keyword, Long adminId) {
        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        qw.eq(TeachingCase::getScope, SCOPE_PLATFORM)
                .eq(TeachingCase::getSourceUrl, crawled.getSourceUrl())
                .eq(TeachingCase::getIsDelete, 0)
                .last("LIMIT 1");
        TeachingCase existing = teachingCaseMapper.selectOne(qw);

        TeachingCase entity = existing == null ? new TeachingCase() : existing;
        entity.setTeacherId(adminId);
        entity.setTitle(StringUtils.defaultIfBlank(crawled.getTitle(), "公开教学案例").trim());
        entity.setCategory(inferCategory(crawled));
        entity.setDifficulty("medium");
        entity.setCourseName(inferCourseName(crawled, keyword));
        entity.setPdfUrl(StringUtils.defaultIfBlank(crawled.getDocumentUrl(), crawled.getSourceUrl()));
        entity.setScope(SCOPE_PLATFORM);
        entity.setStatus(STATUS_PENDING);
        entity.setSourceUrl(crawled.getSourceUrl());
        entity.setSourceName(crawled.getSourceName());
        entity.setSummary(crawled.getSummary());
        entity.setKeywords(buildKeywords(crawled, keyword));
        entity.setMaterialJson(crawled.getMaterialJson());
        entity.setStructureJson(crawled.getStructureJson());
        entity.setPreviewText(buildPreviewText(crawled));
        entity.setPreviewType(StringUtils.defaultIfBlank(crawled.getPreviewType(), PREVIEW_TYPE_DOCUMENT));
        entity.setRelevanceScore(keywordScore(crawled, keyword));
        entity.setCrawlKeyword(StringUtils.abbreviate(keyword, 255));
        entity.setCrawlTime(new Date());
        entity.setIsDelete(0);

        if (existing == null) {
            teachingCaseMapper.insert(entity);
        } else {
            teachingCaseMapper.updateById(entity);
        }
        teachingCaseAssetService.rebuildCaseImages(entity);
        return entity;
    }

    private TeachingCase upsertTeacherCase(CrawledCase crawled, String keyword, Long teacherId) {
        LambdaQueryWrapper<TeachingCase> qw = new LambdaQueryWrapper<>();
        qw.eq(TeachingCase::getTeacherId, teacherId)
                .eq(TeachingCase::getSourceUrl, crawled.getSourceUrl())
                .eq(TeachingCase::getIsDelete, 0)
                .last("LIMIT 1");
        TeachingCase existing = teachingCaseMapper.selectOne(qw);

        TeachingCase entity = existing == null ? new TeachingCase() : existing;
        entity.setTeacherId(teacherId);
        entity.setTitle(StringUtils.defaultIfBlank(crawled.getTitle(), "公开教学案例").trim());
        entity.setCategory(inferCategory(crawled));
        entity.setDifficulty("medium");
        entity.setCourseName(inferCourseName(crawled, keyword));
        entity.setPdfUrl(StringUtils.defaultIfBlank(crawled.getDocumentUrl(), crawled.getSourceUrl()));
        entity.setScope(SCOPE_MINE);
        entity.setStatus(STATUS_APPROVED);
        entity.setSourceUrl(crawled.getSourceUrl());
        entity.setSourceName(crawled.getSourceName());
        entity.setSummary(crawled.getSummary());
        entity.setKeywords(buildKeywords(crawled, keyword));
        entity.setMaterialJson(crawled.getMaterialJson());
        entity.setStructureJson(crawled.getStructureJson());
        entity.setPreviewText(buildPreviewText(crawled));
        entity.setPreviewType(StringUtils.defaultIfBlank(crawled.getPreviewType(), PREVIEW_TYPE_DOCUMENT));
        entity.setRelevanceScore(keywordScore(crawled, keyword));
        entity.setCrawlKeyword(StringUtils.abbreviate(keyword, 255));
        entity.setCrawlTime(new Date());
        entity.setIsDelete(0);

        if (existing == null) {
            teachingCaseMapper.insert(entity);
        } else {
            teachingCaseMapper.updateById(entity);
        }
        teachingCaseAssetService.rebuildCaseImages(entity);
        return entity;
    }

    private List<String> searchCandidateUrls(String keyword, int limit) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }
        List<String> searchTexts = buildSearchTexts(keyword);
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        long deadline = System.currentTimeMillis() + TEACHER_SEARCH_DEADLINE_MS;
        for (String searchText : searchTexts) {
            if (System.currentTimeMillis() >= deadline || urls.size() >= limit) {
                break;
            }
            collectCandidates("https://www.bing.com/search?format=rss&q=" + enc(searchText), urls, limit);
            if (System.currentTimeMillis() >= deadline || urls.size() >= limit) {
                break;
            }
            collectCandidates("https://duckduckgo.com/html/?q=" + enc(searchText), urls, limit);
            if (System.currentTimeMillis() >= deadline || urls.size() >= limit) {
                break;
            }
            collectCandidates("https://www.bing.com/search?q=" + enc(searchText), urls, limit);
        }
        if (urls.isEmpty()) {
            urls.addAll(DEFAULT_SOURCE_URLS);
        }
        return new ArrayList<>(urls);
    }

    private List<String> buildSearchTexts(String keyword) {
        LinkedHashSet<String> texts = new LinkedHashSet<>();
        for (String alias : queryAliases(keyword)) {
            for (String domain : PREFERRED_SEARCH_DOMAINS) {
                texts.add("site:" + domain + " " + alias + " 教学案例");
                texts.add("site:" + domain + " " + alias + " 课程设计");
                texts.add("site:" + domain + " " + alias + " 实验指导");
            }
            texts.add(alias + " 教学案例");
            texts.add(alias + " 教案 案例");
            texts.add(alias + " 课程设计 案例");
            texts.add(alias + " 实验指导 案例");
            texts.add(alias + " 项目案例 教学");
        }
        return new ArrayList<>(texts);
    }

    private List<String> queryAliases(String keyword) {
        String query = StringUtils.trimToEmpty(keyword);
        String normalized = query.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(query);
        if (normalized.equals("java") || normalized.contains("java程序") || normalized.contains("java语言")) {
            aliases.add("Java程序设计");
            aliases.add("Java语言");
            aliases.add("面向对象程序设计");
            aliases.add("Java课程设计");
        }
        if (normalized.contains("计算机网络") || normalized.contains("computernetwork")) {
            aliases.add("计算机网络");
            aliases.add("网络协议");
            aliases.add("TCP/IP");
            aliases.add("Wireshark");
            aliases.add("网络实验");
        }
        if (normalized.contains("数据结构") || normalized.contains("datastructure")) {
            aliases.add("数据结构");
            aliases.add("链表");
            aliases.add("线性表");
            aliases.add("栈 队列");
            aliases.add("树结构");
            aliases.add("图结构");
            aliases.add("算法 课程设计");
        }
        return aliases.stream().filter(StringUtils::isNotBlank).toList();
    }

    private void collectCandidates(String searchUrl, LinkedHashSet<String> urls, int limit) {
        if (urls.size() >= limit) {
            return;
        }
        String html = fetchHtml(searchUrl);
        if (StringUtils.isBlank(html)) {
            return;
        }
        URI baseUri = URI.create(searchUrl);

        Matcher xmlMatcher = XML_LINK_PATTERN.matcher(html);
        while (xmlMatcher.find() && urls.size() < limit) {
            String href = decodeHtml(xmlMatcher.group(1)).trim();
            if (isSearchResultCandidate(href)) {
                urls.add(href);
            }
        }

        Matcher matcher = LINK_PATTERN.matcher(html);
        while (matcher.find() && urls.size() < limit) {
            String href = normalizeSearchHref(resolve(baseUri, matcher.group(1)));
            if (isSearchResultCandidate(href)) {
                urls.add(href);
            }
        }
    }

    private String fetchHtml(String url) {
        try {
            RemoteResourceClient.DownloadedResource remote = remoteResourceClient.download(
                    "teaching-case-crawler",
                    url,
                    CRAWL_MAX_PAGE_BYTES,
                    Duration.ofMillis(3500)
            );
            return decodePage(remote.bytes(), remote.contentType());
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalizeSearchHref(String href) {
        String safe = StringUtils.defaultString(href);
        try {
            URI uri = URI.create(safe);
            String query = StringUtils.defaultString(uri.getRawQuery());
            for (String part : query.split("&")) {
                if (part.startsWith("uddg=") || part.startsWith("u=") || part.startsWith("url=")) {
                    return URLDecoder.decode(StringUtils.substringAfter(part, "="), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {
        }
        return safe;
    }

    private boolean isSearchResultCandidate(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        return !lower.contains("bing.com")
                && !lower.contains("duckduckgo.com")
                && !lower.contains("microsoft.com")
                && !lower.contains("javascript:")
                && !lower.contains("/search?")
                && !lower.contains("/images/")
                && !lower.contains("/videos/");
    }

    private CrawledCase crawl(String rawUrl) {
        return crawl(rawUrl, 8000, 20000);
    }

    private CrawledCase crawl(String rawUrl, int connectTimeoutMs, int readTimeoutMs) {
        try {
            String lower = rawUrl.toLowerCase(Locale.ROOT);
            if (lower.contains(".pdf") || lower.contains(".doc") || lower.contains(".docx")) {
                return crawlDocument(rawUrl);
            }

            URI uri = URI.create(rawUrl);
            RemoteResourceClient.DownloadedResource remote = remoteResourceClient.download(
                    "teaching-case-crawler",
                    rawUrl,
                    CRAWL_MAX_PAGE_BYTES,
                    Duration.ofMillis(readTimeoutMs)
            );
            String html = decodePage(remote.bytes(), remote.contentType());

            String title = extractTitle(html);
            String pageText = htmlToText(html);
            String documentUrl = firstDocumentLink(html, uri);
            String materialJson = extractMaterials(html, uri);
            CrawledCase crawled = new CrawledCase();
            crawled.setTitle(StringUtils.defaultIfBlank(title, hostName(uri)));
            crawled.setSourceUrl(rawUrl);
            crawled.setSourceName(hostName(uri));
            crawled.setMaterialJson(materialJson);

            CrawledDocument document = StringUtils.isNotBlank(documentUrl) ? downloadAndStoreDocument(documentUrl) : null;
            if (document != null) {
                String documentText = CaseDocumentTextExtractor.extractText(document.bytes(), document.fileName());
                String text = StringUtils.joinWith("\n", pageText, documentText);
                crawled.setDocumentUrl(document.storedUrl());
                crawled.setSummary(summarize(StringUtils.defaultIfBlank(documentText, pageText)));
                crawled.setText(text);
                crawled.setPreviewType(PREVIEW_TYPE_DOCUMENT);
            } else if (looksLikeTeachingCase(crawled) || looksLikeTeachingPage(pageText)) {
                crawled.setDocumentUrl("");
                crawled.setSummary(summarize(pageText));
                crawled.setText(pageText);
                crawled.setPreviewType(PREVIEW_TYPE_PAGE);
            } else {
                return null;
            }
            crawled.setStructureJson(buildStructureJson(crawled.getTitle(), crawled.getText(), materialJson));
            return crawled;
        } catch (Exception ignored) {
            return null;
        }
    }

    private CrawledCase crawlDocument(String rawUrl) {
        URI uri = URI.create(rawUrl);
        CrawledDocument document = downloadAndStoreDocument(rawUrl);
        if (document == null) {
            return null;
        }
        String text = CaseDocumentTextExtractor.extractText(document.bytes(), document.fileName());
        CrawledCase crawled = new CrawledCase();
        crawled.setTitle(titleFromUrl(rawUrl));
        crawled.setSourceUrl(rawUrl);
        crawled.setSourceName(hostName(uri));
        crawled.setDocumentUrl(document.storedUrl());
        crawled.setSummary(summarize(text));
        crawled.setText(text);
        crawled.setPreviewType(PREVIEW_TYPE_DOCUMENT);
        crawled.setMaterialJson("[]");
        crawled.setStructureJson(buildStructureJson(crawled.getTitle(), text, "[]"));
        return crawled;
    }

    private CrawledDocument downloadAndStoreDocument(String url) {
        try {
            RemoteDocument remote = downloadValidDocument(url);
            if (remote == null) {
                return null;
            }
            String storedUrl = ossService.uploadBytes(
                    remote.bytes(),
                    remote.fileName(),
                    "case/crawled",
                    remote.contentType()
            );
            return new CrawledDocument(remote.bytes(), remote.fileName(), storedUrl);
        } catch (Exception ignored) {
            return null;
        }
    }

    private RemoteDocument downloadValidDocument(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            RemoteResourceClient.DownloadedResource remote = remoteResourceClient.download(
                    "teaching-case-document",
                    url,
                    CRAWL_MAX_FILE_BYTES,
                    Duration.ofMillis(CRAWL_FILE_READ_TIMEOUT_MS)
            );
            String contentType = StringUtils.defaultString(remote.contentType()).toLowerCase(Locale.ROOT);
            if (contentType.contains("text/html") || contentType.contains("application/json")) {
                return null;
            }

            byte[] bytes = remote.bytes();

            String fileName = normalizeDocumentFileName(url, bytes);
            if (StringUtils.isBlank(fileName) || !isValidDocumentBytes(bytes, fileName)) {
                return null;
            }
            return new RemoteDocument(bytes, fileName, contentTypeForFile(fileName));
        } catch (Exception ignored) {
            return null;
        }
    }

    private byte[] readLimited(InputStream in, int maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int len;
        while ((len = in.read(buffer)) != -1) {
            total += len;
            if (total > maxBytes) {
                throw new IllegalStateException("Remote document is too large");
            }
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    private String normalizeDocumentFileName(String url, byte[] bytes) {
        String path = "";
        try {
            path = URLDecoder.decode(StringUtils.defaultString(URI.create(url).getPath()), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        String name = StringUtils.substringAfterLast(path, "/");
        String lower = StringUtils.defaultString(name).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".doc")) {
            return name;
        }
        if (looksLikePdf(bytes)) {
            return StringUtils.defaultIfBlank(name, "case") + ".pdf";
        }
        if (looksLikeDocx(bytes)) {
            return StringUtils.defaultIfBlank(name, "case") + ".docx";
        }
        if (looksLikeDoc(bytes)) {
            return StringUtils.defaultIfBlank(name, "case") + ".doc";
        }
        return "";
    }

    boolean isValidDocumentBytes(byte[] bytes, String fileName) {
        String lower = StringUtils.defaultString(fileName).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return looksLikePdf(bytes);
        }
        if (lower.endsWith(".docx")) {
            return looksLikeDocx(bytes);
        }
        if (lower.endsWith(".doc")) {
            return looksLikeDoc(bytes);
        }
        return false;
    }

    private boolean looksLikePdf(byte[] bytes) {
        return bytes != null
                && bytes.length > 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F';
    }

    private boolean looksLikeDoc(byte[] bytes) {
        return bytes != null
                && bytes.length > 8
                && (bytes[0] & 0xFF) == 0xD0
                && (bytes[1] & 0xFF) == 0xCF
                && (bytes[2] & 0xFF) == 0x11
                && (bytes[3] & 0xFF) == 0xE0
                && (bytes[4] & 0xFF) == 0xA1
                && (bytes[5] & 0xFF) == 0xB1
                && (bytes[6] & 0xFF) == 0x1A
                && (bytes[7] & 0xFF) == 0xE1;
    }

    private boolean looksLikeDocx(byte[] bytes) {
        if (bytes == null || bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            return false;
        }
        boolean hasContentTypes = false;
        boolean hasDocument = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if ("[Content_Types].xml".equals(name)) {
                    hasContentTypes = true;
                }
                if ("word/document.xml".equals(name)) {
                    hasDocument = true;
                }
                if (hasContentTypes && hasDocument) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private String contentTypeForFile(String fileName) {
        String lower = StringUtils.defaultString(fileName).toLowerCase(Locale.ROOT);
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        return "application/pdf";
    }

    private int keywordScore(CrawledCase crawled, String keyword) {
        return keywordScore(
                crawled.getTitle(),
                crawled.getSummary(),
                crawled.getText(),
                crawled.getSourceName(),
                keyword
        );
    }

    int keywordScore(String titleValue, String summaryValue, String textValue, String sourceValue, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return MIN_KEYWORD_SCORE;
        }
        int score = 0;
        for (String alias : queryAliases(keyword)) {
            score = Math.max(score, keywordScoreForQuery(titleValue, summaryValue, textValue, sourceValue, alias));
        }
        return applyMismatchPenalty(titleValue, summaryValue, textValue, keyword, score);
    }

    private int keywordScoreForQuery(String titleValue, String summaryValue, String textValue, String sourceValue, String keyword) {
        String query = keyword.toLowerCase(Locale.ROOT).trim();
        String title = StringUtils.defaultString(titleValue).toLowerCase(Locale.ROOT);
        String summary = StringUtils.defaultString(summaryValue).toLowerCase(Locale.ROOT);
        String text = StringUtils.defaultString(textValue).toLowerCase(Locale.ROOT);
        String source = StringUtils.defaultString(sourceValue).toLowerCase(Locale.ROOT);
        String phrase = query.replaceAll("\\s+", "");

        int score = 0;
        if (phrase.length() >= 2) {
            if (title.replaceAll("\\s+", "").contains(phrase)) {
                score += 12;
            }
            if (summary.replaceAll("\\s+", "").contains(phrase)) {
                score += 8;
            }
            if (text.replaceAll("\\s+", "").contains(phrase)) {
                score += 6;
            }
        }

        for (String token : queryTokens(query)) {
            if (title.contains(token)) {
                score += 8;
            }
            if (summary.contains(token)) {
                score += 5;
            }
            if (text.contains(token)) {
                score += 3;
            }
            if (source.contains(token)) {
                score += 1;
            }
        }
        return score;
    }

    private int applyMismatchPenalty(String titleValue, String summaryValue, String textValue, String keyword, int score) {
        String normalizedQuery = StringUtils.defaultString(keyword).toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        String haystack = StringUtils.joinWith(" ", titleValue, summaryValue, textValue).toLowerCase(Locale.ROOT);
        if (normalizedQuery.equals("java")
                && haystack.contains("javascript")
                && !containsAny(haystack, "java程序", "java 语言", "java语言", "java课程", "java 课程", "面向对象", "jdk", "spring")) {
            return Math.min(score, MIN_KEYWORD_SCORE - 1);
        }
        if (normalizedQuery.contains("数据结构")
                && containsAny(haystack, "大数据", "数据分析", "数据挖掘")
                && !containsAny(haystack, "数据结构", "链表", "线性表", "栈", "队列", "树结构", "图结构", "算法")) {
            score -= 10;
        }
        return Math.max(score, 0);
    }

    private boolean containsAny(String text, String... tokens) {
        String safe = StringUtils.defaultString(text);
        for (String token : tokens) {
            if (safe.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<String> queryTokens(String query) {
        List<String> tokens = new ArrayList<>();
        for (String raw : StringUtils.defaultString(query).split("[\\s,;:/\\\\|_+\\-\\[\\](){}<>\"'`~!@#$%^&*=，。；：、？！（）【】《》]+")) {
            String token = raw.trim().toLowerCase(Locale.ROOT);
            if (token.length() < 2 || QUERY_STOP_WORDS.contains(token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private boolean matchesKeyword(CrawledCase crawled, String keyword) {
        String haystack = StringUtils.joinWith(" ",
                crawled.getTitle(),
                crawled.getSummary(),
                crawled.getText(),
                crawled.getSourceName()
        ).toLowerCase(Locale.ROOT);
        for (String token : keyword.toLowerCase(Locale.ROOT).split("[\\s,，、;；:：]+")) {
            if (StringUtils.isNotBlank(token) && haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeTeachingCase(CrawledCase crawled) {
        String haystack = StringUtils.joinWith(" ",
                crawled.getTitle(),
                crawled.getSummary(),
                crawled.getText()
        );
        int score = 0;
        for (String marker : List.of("案例", "教案", "教学", "课程", "项目", "设计", "实践", "实训")) {
            if (StringUtils.defaultString(haystack).contains(marker)) {
                score++;
            }
        }
        return score >= 2;
    }

    private boolean looksLikeTeachingPage(String text) {
        String haystack = StringUtils.defaultString(text);
        int teachingMarkers = 0;
        for (String marker : List.of("教学案例", "教案", "课程设计", "实验指导", "实训", "教学目标", "教学过程", "案例背景")) {
            if (haystack.contains(marker)) {
                teachingMarkers++;
            }
        }
        if (teachingMarkers >= 1 && containsAny(haystack, "课程", "教学", "实验", "项目", "案例")) {
            return true;
        }
        return teachingMarkers >= 2;
    }

    private int score(TeachingCase teachingCase, String query) {
        String haystack = StringUtils.joinWith(" ",
                teachingCase.getTitle(),
                teachingCase.getCourseName(),
                teachingCase.getSummary(),
                teachingCase.getKeywords(),
                teachingCase.getSourceName()
        ).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : StringUtils.defaultString(query).toLowerCase(Locale.ROOT).split("[\\s,，、;；:：]+")) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            if (StringUtils.defaultString(teachingCase.getTitle()).toLowerCase(Locale.ROOT).contains(token)) {
                score += 8;
            }
            if (StringUtils.defaultString(teachingCase.getCourseName()).toLowerCase(Locale.ROOT).contains(token)) {
                score += 6;
            }
            if (haystack.contains(token)) {
                score += 2;
            }
        }
        return score;
    }

    private ScoredCase scoreRecommendedCase(TeachingCase teachingCase,
                                            RecommendRequest req,
                                            String query,
                                            int metadataScore) {
        String topic = StringUtils.trimToEmpty(req.getTopic());
        String subject = StringUtils.defaultIfBlank(req.getSubject(), req.getCourseName());
        EvidenceResult evidence = bestEvidence(teachingCase, topic, subject);
        int domainScore = scoreDomain(teachingCase, subject);

        int finalScore;
        boolean subjectMatched = StringUtils.isBlank(subject) || domainScore > 0 || evidence.subjectScore > 0;
        boolean primaryTopicMatched = isPrimaryTopicMatch(teachingCase, topic);
        if (StringUtils.isBlank(topic)) {
            finalScore = metadataScore + domainScore;
        } else if (subjectMatched && primaryTopicMatched && evidence.directTopicScore >= CASE_RECOMMEND_TOPIC_EVIDENCE_THRESHOLD) {
            finalScore = 200 + evidence.score * 3 + metadataScore + domainScore;
        } else if (subjectMatched && evidence.directTopicScore >= CASE_RECOMMEND_WEAK_EVIDENCE_THRESHOLD) {
            finalScore = 100 + evidence.score * 2 + metadataScore + domainScore;
        } else if (subjectMatched && evidence.relatedTopicScore >= CASE_RECOMMEND_WEAK_EVIDENCE_THRESHOLD) {
            finalScore = 50 + evidence.relatedTopicScore + metadataScore + domainScore;
        } else {
            finalScore = Math.min(metadataScore + domainScore, 24);
        }
        return new ScoredCase(teachingCase, finalScore, metadataScore, evidence);
    }

    private boolean isPrimaryTopicMatch(TeachingCase teachingCase, String topic) {
        if (teachingCase == null || StringUtils.isBlank(topic)) {
            return false;
        }
        String normalizedTopic = compactText(topic);
        if (StringUtils.isBlank(normalizedTopic)) {
            return false;
        }
        if (titleHasPrimaryTopic(teachingCase.getTitle(), normalizedTopic)) {
            return true;
        }
        String documentText = getRecommendationCaseText(teachingCase);
        return hasLabeledCoreTopic(documentText, normalizedTopic);
    }

    private boolean titleHasPrimaryTopic(String title, String normalizedTopic) {
        String safeTitle = StringUtils.defaultString(title);
        for (String part : safeTitle.split("[\\s\\-－—_/:：|｜《》【】()（）]+")) {
            if (topicMatchesPrimaryValue(normalizedTopic, part)) {
                return true;
            }
        }
        String compactTitle = compactText(safeTitle);
        return topicMatchesPrimaryValue(normalizedTopic, compactTitle)
                || topicMatchesPrimaryValue(normalizedTopic, compactTitle.replaceFirst("^数据结构", ""))
                || topicMatchesPrimaryValue(normalizedTopic, compactTitle.replaceFirst("^数据结构教学案例", ""));
    }

    private boolean hasLabeledCoreTopic(String text, String normalizedTopic) {
        String safeText = StringUtils.defaultString(text);
        for (String label : List.of("核心课题", "教学课题", "核心知识点", "主知识点")) {
            int index = safeText.indexOf(label);
            while (index >= 0) {
                String window = safeText.substring(index, Math.min(safeText.length(), index + 80));
                String cleaned = window.replace(label, "")
                        .replaceAll("^[\\s:：,，、;；]+", "");
                String firstValue = cleaned.split("[\\s,，、;；。\\n\\r]+", 2)[0];
                if (topicMatchesPrimaryValue(normalizedTopic, firstValue)) {
                    return true;
                }
                index = safeText.indexOf(label, index + label.length());
            }
        }
        return false;
    }

    private boolean topicMatchesPrimaryValue(String normalizedQueryTopic, String primaryValue) {
        String normalizedPrimary = compactText(primaryValue);
        if (StringUtils.isBlank(normalizedQueryTopic) || StringUtils.isBlank(normalizedPrimary)) {
            return false;
        }
        if (normalizedQueryTopic.equals(normalizedPrimary)) {
            return true;
        }
        Set<String> queryAliases = topicEquivalentTerms(normalizedQueryTopic);
        Set<String> primaryAliases = topicEquivalentTerms(normalizedPrimary);
        for (String queryAlias : queryAliases) {
            if (primaryAliases.contains(queryAlias)) {
                return true;
            }
        }
        for (String queryAlias : queryAliases) {
            for (String primaryAlias : primaryAliases) {
                if (isParentTopicOf(queryAlias, primaryAlias)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> topicEquivalentTerms(String topic) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String normalized = compactText(topic);
        if (StringUtils.isBlank(normalized)) {
            return terms;
        }
        terms.add(normalized);
        for (Map.Entry<String, List<String>> entry : DATA_STRUCTURE_TOPIC_ALIASES.entrySet()) {
            String key = compactText(entry.getKey());
            List<String> values = entry.getValue() == null ? List.of() : entry.getValue();
            boolean matched = normalized.equals(key);
            if (!matched) {
                for (String alias : values) {
                    if (normalized.equals(compactText(alias))) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched) {
                terms.add(key);
                for (String alias : values) {
                    terms.add(compactText(alias));
                }
            }
        }
        return terms;
    }

    private boolean isParentTopicOf(String normalizedParent, String normalizedCandidate) {
        if (normalizedParent.equals(normalizedCandidate)) {
            return true;
        }
        for (Map.Entry<String, List<String>> entry : DATA_STRUCTURE_TOPIC_CHILDREN.entrySet()) {
            String parent = compactText(entry.getKey());
            if (!normalizedParent.equals(parent)) {
                continue;
            }
            for (String child : entry.getValue()) {
                Set<String> childTerms = topicEquivalentTerms(child);
                if (childTerms.contains(normalizedCandidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private EvidenceResult bestEvidence(TeachingCase teachingCase, String topic, String subject) {
        if (StringUtils.isBlank(topic)) {
            return EvidenceResult.empty();
        }
        List<String> directTerms = directTopicTerms(topic);
        List<String> relatedTerms = relatedTopicTerms(topic);
        List<String> subjectTerms = subjectTerms(subject);
        List<String> chunks = new ArrayList<>();
        chunks.add(StringUtils.joinWith(" ",
                teachingCase.getTitle(),
                teachingCase.getCourseName(),
                teachingCase.getSummary(),
                teachingCase.getKeywords()
        ));
        chunks.addAll(splitRecommendationText(getRecommendationCaseText(teachingCase)));

        EvidenceResult best = EvidenceResult.empty();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            EvidenceResult current = scoreEvidenceChunk(chunk, i, directTerms, relatedTerms, subjectTerms);
            if (current.score > best.score) {
                best = current;
            }
        }
        return best;
    }

    private EvidenceResult scoreEvidenceChunk(String chunk,
                                              int chunkIndex,
                                              List<String> directTerms,
                                              List<String> relatedTerms,
                                              List<String> subjectTerms) {
        if (StringUtils.isBlank(chunk)) {
            return EvidenceResult.empty();
        }
        String lower = chunk.toLowerCase(Locale.ROOT);
        String compact = lower.replaceAll("\\s+", "");
        int directTopicScore = 0;
        int relatedTopicScore = 0;
        String matchedTerm = "";
        int matchedStart = -1;
        DirectTopicHit directHit = findDirectTopicHit(chunk, directTerms);
        if (directHit.matched()) {
            directTopicScore = 80;
            matchedTerm = directHit.term();
            matchedStart = directHit.start();
        }
        for (String term : relatedTerms) {
            if (containsCompact(compact, term)) {
                relatedTopicScore += 12;
                if (StringUtils.isBlank(matchedTerm)) {
                    matchedTerm = term;
                    matchedStart = indexOfLoose(lower, term);
                }
            }
        }
        directTopicScore = Math.min(directTopicScore, 120);
        relatedTopicScore = Math.min(relatedTopicScore, 60);
        int topicScore = directTopicScore + relatedTopicScore;

        int subjectScore = 0;
        for (String term : subjectTerms) {
            if (containsCompact(compact, term)) {
                subjectScore += 8;
            }
        }
        subjectScore = Math.min(subjectScore, 32);
        int teachingScore = containsAny(lower, "教学", "课堂", "课程", "案例", "练习", "任务") ? 6 : 0;
        int total = topicScore * 3 + subjectScore + teachingScore;
        if (topicScore <= 0) {
            total = subjectScore + teachingScore;
        }
        String evidenceTitle = chunkIndex == 0 ? "案例元数据" : "正文片段 " + chunkIndex;
        return new EvidenceResult(
                total,
                topicScore,
                directTopicScore,
                relatedTopicScore,
                subjectScore,
                directHit.matched(),
                evidenceSnippet(chunk, matchedTerm, matchedStart),
                evidenceTitle
        );
    }

    private DirectTopicHit findDirectTopicHit(String text, List<String> directTerms) {
        String safe = StringUtils.defaultString(text);
        String lower = safe.toLowerCase(Locale.ROOT);
        for (String term : directTerms) {
            String normalized = compactText(term);
            if (StringUtils.isBlank(normalized)) {
                continue;
            }
            if (normalized.contains("for")) {
                DirectTopicHit hit = findForTopicHit(safe);
                if (hit.matched()) {
                    return hit;
                }
                continue;
            }
            if (normalized.contains("while")) {
                DirectTopicHit hit = findRegexHit(safe, "(?i)\\bwhile\\s*(\\(|循环|语句|loop|statement)");
                if (hit.matched()) {
                    return hit;
                }
                continue;
            }
            int index = indexOfLoose(lower, term);
            if (index >= 0) {
                return new DirectTopicHit(true, term, index);
            }
        }
        return DirectTopicHit.none();
    }

    private DirectTopicHit findForTopicHit(String text) {
        String safe = StringUtils.defaultString(text);
        for (String pattern : List.of(
                "(?i)\\bfor\\s*\\(",
                "(?i)\\bfor\\s*(循环|语句)",
                "(?i)\\bfor[-\\s]*(loop|statement)",
                "for循环",
                "for语句"
        )) {
            DirectTopicHit hit = findRegexHit(safe, pattern);
            if (hit.matched()) {
                return hit;
            }
        }
        return DirectTopicHit.none();
    }

    private DirectTopicHit findRegexHit(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(StringUtils.defaultString(text));
        if (matcher.find()) {
            return new DirectTopicHit(true, matcher.group(), matcher.start());
        }
        return DirectTopicHit.none();
    }

    private int indexOfLoose(String lowerText, String term) {
        String lowerTerm = StringUtils.defaultString(term).toLowerCase(Locale.ROOT);
        int direct = lowerText.indexOf(lowerTerm);
        if (direct >= 0) {
            return direct;
        }
        String compactTerm = compactText(term);
        if (StringUtils.isBlank(compactTerm)) {
            return -1;
        }
        String compactText = compactText(lowerText);
        int compactIndex = compactText.indexOf(compactTerm);
        if (compactIndex < 0) {
            return -1;
        }
        return Math.min(compactIndex, lowerText.length() - 1);
    }

    private List<String> directTopicTerms(String topic) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String normalized = compactText(topic);
        if (StringUtils.isNotBlank(normalized)) {
            terms.add(normalized);
        }
        for (String equivalent : topicEquivalentTerms(topic)) {
            terms.add(equivalent);
        }
        for (String child : DATA_STRUCTURE_TOPIC_CHILDREN.getOrDefault(topicCanonicalName(normalized), List.of())) {
            terms.add(compactText(child));
            terms.addAll(topicEquivalentTerms(child));
        }
        if (normalized.contains("for")) {
            terms.add("for循环");
            terms.add("for语句");
            terms.add("forstatement");
            terms.add("for-loop");
        } else if (normalized.contains("循环")) {
            terms.add("循环结构");
            terms.add("循环语句");
            terms.add("计数循环");
            terms.add("嵌套循环");
        }
        if (normalized.contains("while")) {
            terms.add("while循环");
            terms.add("dowhile");
        }
        return terms.stream().filter(StringUtils::isNotBlank).toList();
    }

    private String topicCanonicalName(String normalizedTopic) {
        if (StringUtils.isBlank(normalizedTopic)) {
            return "";
        }
        for (String key : DATA_STRUCTURE_TOPIC_CHILDREN.keySet()) {
            if (compactText(key).equals(normalizedTopic)) {
                return key;
            }
        }
        for (Map.Entry<String, List<String>> entry : DATA_STRUCTURE_TOPIC_ALIASES.entrySet()) {
            if (compactText(entry.getKey()).equals(normalizedTopic)) {
                return entry.getKey();
            }
            for (String alias : entry.getValue()) {
                if (compactText(alias).equals(normalizedTopic)) {
                    return entry.getKey();
                }
            }
        }
        return normalizedTopic;
    }

    private List<String> relatedTopicTerms(String topic) {
        String normalized = compactText(topic);
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        if (normalized.contains("for") || normalized.contains("循环")) {
            terms.add("循环结构");
            terms.add("循环语句");
            terms.add("嵌套循环");
            terms.add("遍历");
            terms.add("迭代");
            terms.add("重复执行");
            terms.add("流程控制");
            terms.add("控制结构");
            terms.add("数组遍历");
        }
        return terms.stream().filter(StringUtils::isNotBlank).toList();
    }

    private List<String> subjectTerms(String subject) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String normalized = compactText(subject);
        if (StringUtils.isNotBlank(normalized)) {
            terms.add(normalized);
        }
        if (normalized.contains("java")) {
            terms.add("java");
            terms.add("jdk");
            terms.add("java语言");
            terms.add("java程序");
        }
        return terms.stream().filter(StringUtils::isNotBlank).toList();
    }

    private int scoreDomain(TeachingCase teachingCase, String subject) {
        if (StringUtils.isBlank(subject)) {
            return 0;
        }
        String haystack = compactText(StringUtils.joinWith(" ",
                teachingCase.getTitle(),
                teachingCase.getCourseName(),
                teachingCase.getSummary(),
                teachingCase.getKeywords()
        ));
        int score = 0;
        for (String term : subjectTerms(subject)) {
            if (haystack.contains(compactText(term))) {
                score += 8;
            }
        }
        return Math.min(score, 32);
    }

    private String getRecommendationCaseText(TeachingCase teachingCase) {
        String cacheKey = String.join(":",
                String.valueOf(teachingCase.getId()),
                StringUtils.defaultString(teachingCase.getPdfUrl()),
                teachingCase.getUpdateTime() == null ? "" : String.valueOf(teachingCase.getUpdateTime().getTime())
        );
        return CASE_RECOMMEND_TEXT_CACHE.computeIfAbsent(cacheKey, key -> {
        String text = remoteDocumentTextService.extractText(teachingCase.getPdfUrl());
            if (StringUtils.isBlank(text)) {
                text = StringUtils.joinWith("\n",
                        teachingCase.getPreviewText(),
                        teachingCase.getSummary(),
                        teachingCase.getStructureJson()
                );
            }
            return StringUtils.defaultString(text);
        });
    }

    private List<String> splitRecommendationText(String text) {
        String normalized = StringUtils.defaultString(text).replaceAll("\\s+", " ").trim();
        if (StringUtils.isBlank(normalized)) {
            return List.of();
        }
        int chunkSize = 1500;
        if (normalized.length() <= chunkSize) {
            return List.of(normalized);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int overlap = 140;
        while (start < normalized.length() && chunks.size() < 120) {
            int end = Math.min(start + chunkSize, normalized.length());
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

    private boolean containsCompact(String compactText, String term) {
        String normalizedTerm = compactText(term);
        return StringUtils.isNotBlank(normalizedTerm) && compactText.contains(normalizedTerm);
    }

    private String compactText(String text) {
        return StringUtils.defaultString(text)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s,，、;；:：。.!！?？()（）\\[\\]【】{}<>《》\"'`~_]+", "");
    }

    private String evidenceSnippet(String chunk, String matchedTerm, int matchedStart) {
        String normalized = StringUtils.defaultString(chunk).replaceAll("\\s+", " ").trim();
        if (StringUtils.isBlank(normalized)) {
            return "";
        }
        int index = matchedStart;
        if (index < 0) {
            String lower = normalized.toLowerCase(Locale.ROOT);
            String term = StringUtils.defaultString(matchedTerm).toLowerCase(Locale.ROOT);
            index = StringUtils.isBlank(term) ? -1 : lower.indexOf(term);
        }
        if (index < 0) {
            return StringUtils.abbreviate(normalized, 220);
        }
        int start = Math.max(0, Math.min(index, normalized.length()) - 70);
        int end = Math.min(normalized.length(), start + 220);
        return StringUtils.abbreviate(normalized.substring(start, end), 220);
    }

    private RecommendCaseVO toRecommendVO(ScoredCase scoredCase, RecommendRequest req) {
        TeachingCase teachingCase = scoredCase.teachingCase;
        EvidenceResult evidence = scoredCase.evidence;
        RecommendCaseVO vo = new RecommendCaseVO();
        vo.setId(teachingCase.getId());
        vo.setTitle(teachingCase.getTitle());
        vo.setCategory(teachingCase.getCategory());
        vo.setDifficulty(teachingCase.getDifficulty());
        vo.setCourseName(teachingCase.getCourseName());
        vo.setSummary(teachingCase.getSummary());
        vo.setSourceName(teachingCase.getSourceName());
        vo.setSourceUrl(teachingCase.getSourceUrl());
        vo.setMaterialCount(countMaterials(teachingCase.getMaterialJson()));
        vo.setMatchScore(scoredCase.score);
        vo.setEvidenceScore(evidence.score);
        vo.setTopicEvidenceScore(evidence.topicScore);
        vo.setEvidenceSnippet(evidence.snippet);
        vo.setEvidenceTitle(evidence.title);
        boolean subjectMatched = scoredCase.evidence.subjectScore > 0
                || scoreDomain(teachingCase, StringUtils.defaultIfBlank(req.getSubject(), req.getCourseName())) > 0;
        boolean primaryTopicMatched = isPrimaryTopicMatch(teachingCase, req.getTopic());
        if (subjectMatched && primaryTopicMatched && evidence.directEvidenceMatched && evidence.directTopicScore >= CASE_RECOMMEND_TOPIC_EVIDENCE_THRESHOLD) {
            vo.setMatchLevel("precise");
            vo.setMatchReason("主知识点、课程领域和正文证据同时匹配");
        } else if (evidence.directEvidenceMatched && evidence.directTopicScore >= CASE_RECOMMEND_WEAK_EVIDENCE_THRESHOLD) {
            vo.setMatchLevel("evidence");
            vo.setMatchReason("正文片段提到课题，但不是主知识点");
        } else if (evidence.relatedTopicScore >= CASE_RECOMMEND_WEAK_EVIDENCE_THRESHOLD) {
            vo.setMatchLevel("related");
            vo.setMatchReason("仅发现相关知识点，未直接命中课题");
        } else if (scoredCase.metadataScore > 0) {
            vo.setMatchLevel("related");
            vo.setMatchReason("仅课程领域相关，未发现明确课题证据");
        } else {
            vo.setMatchLevel("fallback");
            vo.setMatchReason("案例库兜底推荐");
        }
        return vo;
    }

    private int countMaterials(String materialJson) {
        try {
            return OBJECT_MAPPER.readTree(StringUtils.defaultIfBlank(materialJson, "[]")).size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String extractMaterials(String html, URI baseUri) {
        ArrayNode materials = OBJECT_MAPPER.createArrayNode();
        Set<String> seen = new LinkedHashSet<>();

        Matcher imgMatcher = IMG_PATTERN.matcher(html);
        while (imgMatcher.find() && materials.size() < 20) {
            addMaterial(materials, seen, "image", "页面图片", resolve(baseUri, imgMatcher.group(1)));
        }

        Matcher linkMatcher = LINK_PATTERN.matcher(html);
        while (linkMatcher.find() && materials.size() < 30) {
            String href = resolve(baseUri, linkMatcher.group(1));
            String text = htmlToText(linkMatcher.group(2));
            String lower = href.toLowerCase(Locale.ROOT);
            if (lower.contains(".ppt") || lower.contains(".pptx")) {
                addMaterial(materials, seen, "ppt", StringUtils.defaultIfBlank(text, "课件"), href);
            } else if (lower.contains(".pdf") || lower.contains(".doc") || lower.contains(".docx")) {
                addMaterial(materials, seen, "document", StringUtils.defaultIfBlank(text, "案例附件"), href);
            } else if (lower.contains(".mp4") || lower.contains("video") || lower.contains("bilibili")) {
                addMaterial(materials, seen, "video", StringUtils.defaultIfBlank(text, "视频资源"), href);
            } else if (lower.contains("mooc") || lower.contains("icourse") || lower.contains("course")) {
                addMaterial(materials, seen, "interactive", StringUtils.defaultIfBlank(text, "课程资源"), href);
            }
        }
        return materials.toString();
    }

    private void addMaterial(ArrayNode materials, Set<String> seen, String type, String title, String url) {
        if (StringUtils.isBlank(url) || !seen.add(url)) {
            return;
        }
        ObjectNode item = materials.addObject();
        item.put("type", type);
        item.put("title", StringUtils.abbreviate(StringUtils.defaultIfBlank(title, type), 80));
        item.put("url", url);
    }

    private String buildStructureJson(String title, String text, String materialJson) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("title", StringUtils.defaultString(title));
        node.put("objectives", findSection(text, "目标", 500));
        node.put("leadIn", findSection(text, "导入", 500));
        node.put("activities", findSection(text, "活动", 700));
        node.put("evaluation", findSection(text, "评价", 500));
        try {
            node.set("materials", OBJECT_MAPPER.readTree(StringUtils.defaultIfBlank(materialJson, "[]")));
        } catch (Exception ignored) {
            node.putArray("materials");
        }
        return node.toString();
    }

    private String findSection(String text, String keyword, int max) {
        String safe = StringUtils.defaultString(text);
        int index = safe.indexOf(keyword);
        if (index < 0) {
            return "";
        }
        return StringUtils.abbreviate(safe.substring(index).replaceAll("\\s+", " ").trim(), max);
    }

    private String firstDocumentLink(String html, URI baseUri) {
        Matcher matcher = LINK_PATTERN.matcher(html);
        while (matcher.find()) {
            String href = resolve(baseUri, matcher.group(1));
            String lower = href.toLowerCase(Locale.ROOT);
            if (lower.contains(".pdf") || lower.contains(".doc") || lower.contains(".docx")) {
                return href;
            }
        }
        return "";
    }

    private String extractTitle(String html) {
        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        return StringUtils.abbreviate(htmlToText(matcher.group(1)), 180);
    }

    private String htmlToText(String html) {
        String text = SCRIPT_STYLE_PATTERN.matcher(StringUtils.defaultString(html)).replaceAll(" ");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</li>", "\n");
        text = TAG_PATTERN.matcher(text).replaceAll(" ");
        return decodeHtml(text).replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n")
                .trim();
    }

    private String summarize(String text) {
        String normalized = StringUtils.defaultString(text).replaceAll("\\s+", " ").trim();
        return StringUtils.abbreviate(normalized, 500);
    }

    private String buildPreviewText(CrawledCase crawled) {
        String text = StringUtils.defaultIfBlank(crawled.getText(), crawled.getSummary());
        return StringUtils.abbreviate(StringUtils.defaultString(text).replaceAll("\\s+", " ").trim(), PREVIEW_TEXT_LIMIT);
    }

    private Charset resolveCharset(String contentType) {
        contentType = StringUtils.defaultString(contentType);
        Matcher matcher = Pattern.compile("charset=([^;]+)", Pattern.CASE_INSENSITIVE).matcher(contentType);
        if (matcher.find()) {
            try {
                return Charset.forName(matcher.group(1).trim());
            } catch (Exception ignored) {
            }
        }
        return StandardCharsets.UTF_8;
    }

    private String decodePage(byte[] bytes, String contentType) {
        Charset headerCharset = resolveCharset(contentType);
        String text = new String(bytes, headerCharset);
        Charset metaCharset = findMetaCharset(text);
        if (metaCharset != null && !metaCharset.equals(headerCharset)) {
            text = new String(bytes, metaCharset);
        }
        if (looksMisdecoded(text)) {
            try {
                String gbText = new String(bytes, Charset.forName("GB18030"));
                if (!looksMisdecoded(gbText)) {
                    return gbText;
                }
            } catch (Exception ignored) {
            }
        }
        return text;
    }

    private Charset findMetaCharset(String html) {
        Matcher matcher = META_CHARSET_PATTERN.matcher(StringUtils.defaultString(html));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Charset.forName(matcher.group(1).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean looksMisdecoded(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        long replacementCount = text.chars().filter(ch -> ch == '\uFFFD').count();
        if (replacementCount >= 3) {
            return true;
        }
        String sample = text.substring(0, Math.min(text.length(), 4000));
        return sample.contains("Ã") || sample.contains("Â") || sample.contains("锟斤拷");
    }

    private String resolve(URI baseUri, String href) {
        try {
            return baseUri.resolve(StringUtils.defaultString(href).trim()).toString();
        } catch (Exception ignored) {
            return href;
        }
    }

    private String hostName(URI uri) {
        return StringUtils.defaultIfBlank(uri.getHost(), "公开案例来源");
    }

    private String titleFromUrl(String url) {
        String path = StringUtils.substringAfterLast(URI.create(url).getPath(), "/");
        return StringUtils.defaultIfBlank(path, "公开教学案例");
    }

    private String inferCategory(CrawledCase crawled) {
        String text = StringUtils.joinWith(" ", crawled.getTitle(), crawled.getSummary(), crawled.getText());
        if (text.contains("竞赛") || text.contains("大赛")) {
            return "competition";
        }
        if (text.contains("工程") || text.contains("企业")) {
            return "enterprise";
        }
        return "course_design";
    }

    private String inferCourseName(CrawledCase crawled, String keyword) {
        String text = StringUtils.joinWith(" ", crawled.getTitle(), crawled.getSummary(), keyword);
        for (String course : List.of("程序设计基础", "C语言", "计算机网络", "计算机组成原理", "数据结构", "数据库", "操作系统", "软件工程")) {
            if (StringUtils.defaultString(text).contains(course)) {
                return course;
            }
        }
        return StringUtils.isNotBlank(keyword) ? StringUtils.abbreviate(keyword, 80) : "高校计算机课程";
    }

    private String buildKeywords(CrawledCase crawled, String keyword) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : StringUtils.joinWith(" ", keyword, crawled.getTitle(), crawled.getSummary())
                .split("[\\s,，、;；:：\\-_/]+")) {
            if (StringUtils.isNotBlank(token) && token.length() >= 2 && keywords.size() < 12) {
                keywords.add(token);
            }
        }
        return String.join(",", keywords);
    }

    private String decodeHtml(String text) {
        return StringUtils.defaultString(text)
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private String enc(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    private record ScoredCase(TeachingCase teachingCase, int score, int metadataScore, EvidenceResult evidence) {
    }

    private record EvidenceResult(int score,
                                  int topicScore,
                                  int directTopicScore,
                                  int relatedTopicScore,
                                  int subjectScore,
                                  boolean directEvidenceMatched,
                                  String snippet,
                                  String title) {
        private static EvidenceResult empty() {
            return new EvidenceResult(0, 0, 0, 0, 0, false, "", "");
        }
    }

    private record DirectTopicHit(boolean matched, String term, int start) {
        private static DirectTopicHit none() {
            return new DirectTopicHit(false, "", -1);
        }
    }

    private record RemoteDocument(byte[] bytes, String fileName, String contentType) {
    }

    private record CrawledDocument(byte[] bytes, String fileName, String storedUrl) {
    }

    @Data
    private static class CrawledCase {
        private String title;
        private String sourceUrl;
        private String sourceName;
        private String documentUrl;
        private String summary;
        private String text;
        private String previewType;
        private String materialJson;
        private String structureJson;
    }

    @Data
    public static class RecommendRequest {
        private String subject;
        private String grade;
        private String topic;
        private String lessonType;
        private String courseName;
    }

    @Data
    public static class RecommendCaseVO {
        private Long id;
        private String title;
        private String category;
        private String difficulty;
        private String courseName;
        private String summary;
        private String sourceName;
        private String sourceUrl;
        private Integer materialCount;
        private Integer matchScore;
        private String matchReason;
        private String matchLevel;
        private Integer evidenceScore;
        private Integer topicEvidenceScore;
        private String evidenceSnippet;
        private String evidenceTitle;
    }
}
