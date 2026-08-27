package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.client.RemoteResourceClient;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.ExternalResourceBookmarkMapper;
import com.ruyi.teach.model.dto.SaveExternalResourceRequest;
import com.ruyi.teach.model.entity.ExternalResourceBookmark;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.model.vo.ExternalResourceSearchItemVO;
import com.ruyi.teach.model.vo.ExternalResourceSearchPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/external/resource")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Tag(name = "外部资源聚合检索")
public class ExternalResourceController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private ExternalResourceBookmarkMapper externalResourceBookmarkMapper;

    @Resource
    private RemoteResourceClient remoteResourceClient;

    @Data
    public static class SaveResult {
        private Long id;
        private Boolean alreadySaved;
    }

    @Operation(summary = "外部资源聚合检索")
    @GetMapping("/search")
    public BaseResponse<ExternalResourceSearchPageVO> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String platform,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "8") long pageSize) {

        long safeCurrent = current <= 0 ? 1 : current;
        long safePageSize = pageSize <= 0 ? 8 : Math.min(pageSize, 20);
        String safePlatform = StringUtils.defaultIfBlank(platform, "all").trim().toLowerCase();
        String kw = StringUtils.trimToEmpty(keyword);

        ExternalResourceSearchPageVO page = new ExternalResourceSearchPageVO();
        page.setCurrent(safeCurrent);
        page.setPageSize(safePageSize);
        page.setSupportNotice("已接入 GitHub、Gitee、论文检索、Bilibili；CSDN 当前提供可保存的站内检索入口，后续可接入 Bing/Google API 增强。");

        if (StringUtils.isBlank(kw)) {
            page.setTotal(0);
            page.setRecords(Collections.emptyList());
            return ResultUtils.success(page);
        }

        List<ExternalResourceSearchItemVO> items = new ArrayList<>();
        if ("all".equals(safePlatform) || "github".equals(safePlatform)) {
            items.addAll(searchGithub(kw, 6));
        }
        if ("all".equals(safePlatform) || "gitee".equals(safePlatform)) {
            items.addAll(searchGitee(kw, 6));
        }
        if ("all".equals(safePlatform) || "paper".equals(safePlatform)) {
            items.addAll(searchPapers(kw, 6));
        }
        if ("all".equals(safePlatform) || "bilibili".equals(safePlatform)) {
            items.addAll(searchBilibili(kw, 6));
        }
        if ("all".equals(safePlatform) || "csdn".equals(safePlatform)) {
            items.add(buildCsdnSearchCard(kw));
        }

        List<ExternalResourceSearchItemVO> distinct = distinctByKey(items);
        int start = (int) ((safeCurrent - 1) * safePageSize);
        int end = Math.min(start + (int) safePageSize, distinct.size());
        page.setTotal(distinct.size());
        page.setRecords(start >= distinct.size() ? Collections.emptyList() : new ArrayList<>(distinct.subList(start, end)));
        return ResultUtils.success(page);
    }

    @Operation(summary = "保存外部资源书签")
    @PostMapping("/save")
    public BaseResponse<SaveResult> save(@RequestBody SaveExternalResourceRequest req, HttpServletRequest request) {
        User loginUser = getLoginTeacher(request);
        if (req == null || StringUtils.isBlank(req.getPlatform()) || StringUtils.isBlank(req.getExternalId())
                || StringUtils.isBlank(req.getTitle()) || StringUtils.isBlank(req.getUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "外部资源参数不完整");
        }

        String platform = req.getPlatform().trim().toLowerCase();
        String externalId = req.getExternalId().trim();

        LambdaQueryWrapper<ExternalResourceBookmark> qw = new LambdaQueryWrapper<>();
        qw.eq(ExternalResourceBookmark::getTeacherId, loginUser.getId())
                .eq(ExternalResourceBookmark::getPlatform, platform)
                .eq(ExternalResourceBookmark::getExternalId, externalId)
                .eq(ExternalResourceBookmark::getIsDelete, 0)
                .last("limit 1");

        ExternalResourceBookmark existing = externalResourceBookmarkMapper.selectOne(qw);
        SaveResult result = new SaveResult();
        if (existing != null) {
            result.setId(existing.getId());
            result.setAlreadySaved(true);
            return ResultUtils.success(result);
        }

        ExternalResourceBookmark entity = new ExternalResourceBookmark();
        entity.setTeacherId(loginUser.getId());
        entity.setPlatform(platform);
        entity.setExternalId(externalId);
        entity.setTitle(truncate(req.getTitle(), 500));
        entity.setSummary(truncate(req.getSummary(), 2000));
        entity.setCover(truncate(req.getCover(), 1024));
        entity.setAuthor(truncate(req.getAuthor(), 255));
        entity.setUrl(truncate(req.getUrl(), 1024));
        entity.setResourceType(truncate(StringUtils.defaultIfBlank(req.getResourceType(), "external"), 64));
        entity.setTagsJson(toJson(req.getTags()));
        entity.setRawJson(truncate(req.getRawJson(), 6000));
        entity.setIsDelete(0);
        externalResourceBookmarkMapper.insert(entity);

        result.setId(entity.getId());
        result.setAlreadySaved(false);
        return ResultUtils.success(result);
    }

    @Operation(summary = "查询当前教师已保存的外部资源key")
    @GetMapping("/saved-keys")
    public BaseResponse<List<String>> savedKeys(HttpServletRequest request) {
        User loginUser = getLoginTeacher(request);
        LambdaQueryWrapper<ExternalResourceBookmark> qw = new LambdaQueryWrapper<>();
        qw.eq(ExternalResourceBookmark::getTeacherId, loginUser.getId())
                .eq(ExternalResourceBookmark::getIsDelete, 0)
                .orderByDesc(ExternalResourceBookmark::getCreateTime);
        List<ExternalResourceBookmark> list = externalResourceBookmarkMapper.selectList(qw);
        List<String> keys = list.stream()
                .map(item -> item.getPlatform() + "-" + item.getExternalId())
                .distinct()
                .collect(Collectors.toList());
        return ResultUtils.success(keys);
    }

    private List<ExternalResourceSearchItemVO> searchGithub(String keyword, int limit) {
        JsonNode root = fetchJson("https://api.github.com/search/repositories?q="
                + enc(keyword) + "&sort=stars&order=desc&per_page=" + limit);
        List<ExternalResourceSearchItemVO> result = new ArrayList<>();
        JsonNode items = root.path("items");
        if (!items.isArray()) {
            return result;
        }
        for (JsonNode node : items) {
            ExternalResourceSearchItemVO item = new ExternalResourceSearchItemVO();
            item.setPlatform("github");
            item.setExternalId(node.path("full_name").asText(node.path("id").asText()));
            item.setId(item.getPlatform() + "-" + item.getExternalId());
            item.setTitle(node.path("full_name").asText("GitHub repository"));
            item.setDesc(StringUtils.defaultIfBlank(node.path("description").asText(""), "GitHub 开源仓库"));
            item.setAuthor(node.path("owner").path("login").asText(""));
            item.setUrl(node.path("html_url").asText(""));
            item.setResourceType("code");
            item.setDate(left(node.path("updated_at").asText(""), 10));
            addTags(item, "GitHub", "开源仓库", node.path("language").asText(""));
            item.setRawJson(node.toString());
            result.add(item);
        }
        return result;
    }

    private List<ExternalResourceSearchItemVO> searchGitee(String keyword, int limit) {
        JsonNode root = fetchJson("https://gitee.com/api/v5/search/repositories?q="
                + enc(keyword) + "&page=1&per_page=" + limit);
        List<ExternalResourceSearchItemVO> result = new ArrayList<>();
        if (!root.isArray()) {
            return result;
        }
        for (JsonNode node : root) {
            ExternalResourceSearchItemVO item = new ExternalResourceSearchItemVO();
            item.setPlatform("gitee");
            item.setExternalId(StringUtils.firstNonBlank(node.path("full_name").asText(""), node.path("id").asText("")));
            item.setId(item.getPlatform() + "-" + item.getExternalId());
            item.setTitle(StringUtils.firstNonBlank(node.path("full_name").asText(""), node.path("name").asText("Gitee repository")));
            item.setDesc(StringUtils.defaultIfBlank(node.path("description").asText(""), "Gitee 开源仓库"));
            item.setAuthor(node.path("owner").path("login").asText(""));
            item.setUrl(node.path("html_url").asText(""));
            item.setResourceType("code");
            item.setDate(left(node.path("updated_at").asText(""), 10));
            addTags(item, "Gitee", "开源仓库", node.path("language").asText(""));
            item.setRawJson(node.toString());
            result.add(item);
        }
        return result;
    }

    private List<ExternalResourceSearchItemVO> searchPapers(String keyword, int limit) {
        List<ExternalResourceSearchItemVO> result = new ArrayList<>();
        result.addAll(searchSemanticScholar(keyword, limit));
        if (result.size() < limit) {
            result.addAll(searchCrossref(keyword, limit - result.size()));
        }
        return result;
    }

    private List<ExternalResourceSearchItemVO> searchSemanticScholar(String keyword, int limit) {
        JsonNode root = fetchJson("https://api.semanticscholar.org/graph/v1/paper/search?query="
                + enc(keyword) + "&limit=" + limit + "&fields=title,abstract,url,year,authors");
        List<ExternalResourceSearchItemVO> result = new ArrayList<>();
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            return result;
        }
        for (JsonNode node : data) {
            ExternalResourceSearchItemVO item = new ExternalResourceSearchItemVO();
            item.setPlatform("paper");
            item.setExternalId(node.path("paperId").asText(node.path("url").asText("paper")));
            item.setId(item.getPlatform() + "-" + item.getExternalId());
            item.setTitle(node.path("title").asText("Research paper"));
            item.setDesc(StringUtils.defaultIfBlank(node.path("abstract").asText(""), "Semantic Scholar 论文结果"));
            item.setAuthor(authors(node.path("authors")));
            item.setUrl(node.path("url").asText(""));
            item.setResourceType("paper");
            item.setDate(node.path("year").asText(""));
            addTags(item, "论文", "Semantic Scholar");
            item.setRawJson(node.toString());
            result.add(item);
        }
        return result;
    }

    private List<ExternalResourceSearchItemVO> searchCrossref(String keyword, int limit) {
        JsonNode root = fetchJson("https://api.crossref.org/works?query="
                + enc(keyword) + "&rows=" + limit);
        List<ExternalResourceSearchItemVO> result = new ArrayList<>();
        JsonNode items = root.path("message").path("items");
        if (!items.isArray()) {
            return result;
        }
        for (JsonNode node : items) {
            ExternalResourceSearchItemVO item = new ExternalResourceSearchItemVO();
            item.setPlatform("paper");
            item.setExternalId(StringUtils.firstNonBlank(node.path("DOI").asText(""), node.path("URL").asText("crossref")));
            item.setId(item.getPlatform() + "-" + item.getExternalId());
            item.setTitle(firstArrayText(node.path("title"), "Research paper"));
            item.setDesc(firstArrayText(node.path("subject"), "Crossref 文献元数据"));
            item.setAuthor(crossrefAuthors(node.path("author")));
            item.setUrl(StringUtils.defaultIfBlank(node.path("URL").asText(""), "https://doi.org/" + item.getExternalId()));
            item.setResourceType("paper");
            item.setDate(crossrefDate(node));
            addTags(item, "论文", "Crossref");
            item.setRawJson(node.toString());
            result.add(item);
        }
        return result;
    }

    private List<ExternalResourceSearchItemVO> searchBilibili(String keyword, int limit) {
        JsonNode root = fetchJson("https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword="
                + enc(keyword) + "&page=1");
        List<ExternalResourceSearchItemVO> result = new ArrayList<>();
        JsonNode items = root.path("data").path("result");
        if (!items.isArray()) {
            return result;
        }
        for (JsonNode node : items) {
            if (result.size() >= limit) {
                break;
            }
            ExternalResourceSearchItemVO item = new ExternalResourceSearchItemVO();
            item.setPlatform("bilibili");
            item.setExternalId(StringUtils.firstNonBlank(node.path("bvid").asText(""), node.path("aid").asText("")));
            item.setId(item.getPlatform() + "-" + item.getExternalId());
            item.setTitle(cleanHtml(node.path("title").asText("Bilibili 视频")));
            item.setDesc(cleanHtml(StringUtils.defaultIfBlank(node.path("description").asText(""), "Bilibili 视频结果")));
            item.setCover(normalizeBiliCover(node.path("pic").asText("")));
            item.setAuthor(node.path("author").asText(""));
            item.setUrl("https://www.bilibili.com/video/" + item.getExternalId());
            item.setResourceType("video");
            item.setDate("");
            addTags(item, "Bilibili", "视频");
            item.setRawJson(node.toString());
            result.add(item);
        }
        return result;
    }

    private ExternalResourceSearchItemVO buildCsdnSearchCard(String keyword) {
        ExternalResourceSearchItemVO item = new ExternalResourceSearchItemVO();
        item.setPlatform("csdn");
        item.setExternalId("search-" + keyword);
        item.setId(item.getPlatform() + "-" + item.getExternalId());
        item.setTitle("CSDN 站内检索：" + keyword);
        item.setDesc("CSDN 暂未接入稳定公开搜索 API，当前保存检索入口。后续配置 Bing/Google 搜索 API 后可返回结构化文章列表。");
        item.setAuthor("CSDN");
        item.setUrl("https://so.csdn.net/so/search?q=" + enc(keyword));
        item.setResourceType("article_search");
        item.setDate("");
        addTags(item, "CSDN", "站内检索");
        item.setRawJson("{\"keyword\":\"" + keyword.replace("\"", "\\\"") + "\"}");
        return item;
    }

    private JsonNode fetchJson(String url) {
        return remoteResourceClient.getJsonOrEmpty(url);
    }

    private User getLoginTeacher(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"teacher".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅教师可保存外部资源");
        }
        return loginUser;
    }

    private List<ExternalResourceSearchItemVO> distinctByKey(List<ExternalResourceSearchItemVO> items) {
        Map<String, ExternalResourceSearchItemVO> map = new LinkedHashMap<>();
        for (ExternalResourceSearchItemVO item : items) {
            if (StringUtils.isBlank(item.getPlatform()) || StringUtils.isBlank(item.getExternalId())) {
                continue;
            }
            map.putIfAbsent(item.getPlatform() + "-" + item.getExternalId(), item);
        }
        return new ArrayList<>(map.values());
    }

    private void addTags(ExternalResourceSearchItemVO item, String... tags) {
        List<String> list = item.getTags() == null ? new ArrayList<>() : item.getTags();
        for (String tag : tags) {
            if (StringUtils.isNotBlank(tag) && !list.contains(tag)) {
                list.add(tag);
            }
        }
        item.setTags(list);
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? Collections.emptyList() : value);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private String enc(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    private String truncate(String text, int maxLength) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String left(String text, int length) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        return text.length() <= length ? text : text.substring(0, length);
    }

    private String cleanHtml(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        return text.replaceAll("<[^>]+>", "").replace("&quot;", "\"").replace("&amp;", "&").trim();
    }

    private String normalizeBiliCover(String cover) {
        if (StringUtils.isBlank(cover)) {
            return "";
        }
        return cover.startsWith("//") ? "https:" + cover : cover;
    }

    private String authors(JsonNode authors) {
        if (!authors.isArray()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (JsonNode author : authors) {
            if (names.size() >= 3) {
                break;
            }
            String name = author.path("name").asText("");
            if (StringUtils.isNotBlank(name)) {
                names.add(name);
            }
        }
        return String.join("、", names);
    }

    private String crossrefAuthors(JsonNode authors) {
        if (!authors.isArray()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (JsonNode author : authors) {
            if (names.size() >= 3) {
                break;
            }
            String name = String.join(" ",
                    StringUtils.defaultString(author.path("given").asText("")),
                    StringUtils.defaultString(author.path("family").asText(""))).trim();
            if (StringUtils.isNotBlank(name)) {
                names.add(name);
            }
        }
        return String.join("、", names);
    }

    private String firstArrayText(JsonNode node, String fallback) {
        if (node.isArray() && !node.isEmpty()) {
            return node.get(0).asText(fallback);
        }
        return fallback;
    }

    private String crossrefDate(JsonNode node) {
        JsonNode parts = node.path("published-print").path("date-parts");
        if (!parts.isArray() || parts.isEmpty()) {
            parts = node.path("published-online").path("date-parts");
        }
        if (!parts.isArray() || parts.isEmpty() || !parts.get(0).isArray() || parts.get(0).isEmpty()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : parts.get(0)) {
            String text = value.asText("");
            if (StringUtils.isNotBlank(text)) {
                values.add(text);
            }
        }
        return String.join("-", values);
    }
}
