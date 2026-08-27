package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruyi.teach.model.entity.CourseComment;
import com.ruyi.teach.service.CourseCommentService;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CourseCommentController {

    @Autowired
    private CourseCommentService commentService;

    // 1. 获取某门课程的所有观点（按时间倒序）
    @GetMapping("/list")
    public BaseResponse<List<CourseComment>> getList(@RequestParam("courseId") Long courseId) {
        QueryWrapper<CourseComment> query = new QueryWrapper<>();
        query.eq("course_id", courseId).orderByDesc("create_time");
        List<CourseComment> list = commentService.list(query);
        return ResultUtils.success(list);
    }

    // 2. 发表新观点
    @PostMapping("/add")
    public BaseResponse<Boolean> addComment(@RequestBody CourseComment comment) {
        boolean saved = commentService.save(comment);
        return ResultUtils.success(saved);
    }

    // 3. 删除观点
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteComment(@PathVariable("id") Long id) {
        boolean removed = commentService.removeById(id);
        return ResultUtils.success(removed);
    }

    // 4. 点赞功能
    @PostMapping("/like/{id}")
    public BaseResponse<Boolean> toggleLike(@PathVariable("id") Long id, @RequestParam("isLike") boolean isLike) {
        CourseComment comment = commentService.getById(id);
        if (comment != null) {
            int currentLikes = comment.getLikes() == null ? 0 : comment.getLikes();
            comment.setLikes(isLike ? currentLikes + 1 : Math.max(0, currentLikes - 1));
            commentService.updateById(comment);
        }
        return ResultUtils.success(true);
    }
}