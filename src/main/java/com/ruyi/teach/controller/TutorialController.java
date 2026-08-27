package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.mapper.TextCourseMapper;
import com.ruyi.teach.mapper.TextNodeMapper;
import com.ruyi.teach.model.entity.TextCourse;
import com.ruyi.teach.model.entity.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired; // ✅ 改用这个
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文字教程接口
 */
@RestController
@RequestMapping("/tutorial")
@CrossOrigin
public class TutorialController {

    // 🔥 把 @Resource 改成 @Autowired
    @Autowired
    private TextCourseMapper textCourseMapper;

    @Autowired
    private TextNodeMapper textNodeMapper;

    // 1. 获取所有教程列表
    @GetMapping("/list")
    public BaseResponse<List<TextCourse>> getCourseList() {
        List<TextCourse> list = textCourseMapper.selectList(null);
        return ResultUtils.success(list);
    }

    @GetMapping("/page")
    public BaseResponse<Page<TextCourse>> getCoursePage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "latest") String sort) {

        long safeCurrent = Math.max(current, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);

        QueryWrapper<TextCourse> query = new QueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            String keyword = name.trim();
            query.and(wrapper -> wrapper
                    .like("name", keyword)
                    .or()
                    .like("description", keyword));
        }

        applyCategoryFilter(query, category);

        if ("hot".equalsIgnoreCase(sort)) {
            query.orderByDesc("id");
        } else {
            query.orderByDesc("create_time").orderByDesc("id");
        }

        Page<TextCourse> page = textCourseMapper.selectPage(new Page<>(safeCurrent, safeSize), query);
        return ResultUtils.success(page);
    }

    // 2. 获取某教程的目录树
    @GetMapping("/directory/{courseId}")
    public BaseResponse<List<TextNode>> getDirectory(@PathVariable Long courseId) {
        QueryWrapper<TextNode> query = new QueryWrapper<>();
        query.eq("course_id", courseId)
                .select("id", "title", "sort_order")
                .orderByAsc("sort_order");

        List<TextNode> nodes = textNodeMapper.selectList(query);
        return ResultUtils.success(nodes);
    }

    // 3. 获取某一节的详细内容
    @GetMapping("/node/{nodeId}")
    public BaseResponse<TextNode> getNodeDetail(@PathVariable Long nodeId) {
        TextNode node = textNodeMapper.selectById(nodeId);
        return ResultUtils.success(node);
    }

    private void applyCategoryFilter(QueryWrapper<TextCourse> query, String category) {
        if (StringUtils.isBlank(category) || "all".equalsIgnoreCase(category)) {
            return;
        }

        switch (category) {
            case "backend":
                query.and(wrapper -> wrapper
                        .like("name", "Java")
                        .or().like("name", "Spring")
                        .or().like("name", "Docker")
                        .or().like("name", "Nginx")
                        .or().like("name", "Shell"));
                break;
            case "frontend":
                query.and(wrapper -> wrapper
                        .like("name", "Vue")
                        .or().like("name", "React")
                        .or().like("name", "Web")
                        .or().like("name", "HTML")
                        .or().like("name", "CSS")
                        .or().like("name", "JavaScript")
                        .or().like("name", "TypeScript"));
                break;
            case "ai":
                query.and(wrapper -> wrapper
                        .like("name", "AI")
                        .or().like("name", "Python")
                        .or().like("name", "DeepSeek")
                        .or().like("name", "机器学习")
                        .or().like("name", "人工智能"));
                break;
            case "cs":
                query.and(wrapper -> wrapper
                        .like("name", "数据结构")
                        .or().like("name", "算法")
                        .or().like("name", "计算机")
                        .or().like("name", "网络")
                        .or().like("name", "HTTP")
                        .or().like("name", "Git")
                        .or().like("name", "Linux")
                        .or().like("name", "C 语言"));
                break;
            default:
                break;
        }
    }
}
