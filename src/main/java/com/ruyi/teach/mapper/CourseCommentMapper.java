package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.CourseComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseCommentMapper extends BaseMapper<CourseComment> {
    // 继承 BaseMapper 即可，里面已经包含了基础的 CRUD 方法，不需要手写任何代码
}