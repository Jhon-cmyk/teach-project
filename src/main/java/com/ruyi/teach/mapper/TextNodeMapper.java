package com.ruyi.teach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruyi.teach.model.entity.TextNode; // ✅ 引入正确的实体类路径
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TextNodeMapper extends BaseMapper<TextNode> {
}