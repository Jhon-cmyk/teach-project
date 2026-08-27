package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.model.entity.CourseComment;
import com.ruyi.teach.mapper.CourseCommentMapper;
import com.ruyi.teach.model.entity.CourseComment;
import com.ruyi.teach.service.CourseCommentService;
import org.springframework.stereotype.Service;

@Service // 必须加这个注解，把类交给 Spring 容器管理
public class CourseCommentServiceImpl extends ServiceImpl<CourseCommentMapper, CourseComment> implements CourseCommentService {
    // 这里暂时不需要写额外代码，ServiceImpl 已经实现了 IService 里的所有基础方法
}