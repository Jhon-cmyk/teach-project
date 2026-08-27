package com.ruyi.teach.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.teach.mapper.CourseChapterMapper;
import com.ruyi.teach.model.entity.CourseChapter;
import com.ruyi.teach.service.CourseChapterService;
import org.springframework.stereotype.Service;

@Service
public class CourseChapterServiceImpl extends ServiceImpl<CourseChapterMapper, CourseChapter> implements CourseChapterService {
}