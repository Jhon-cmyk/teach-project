-- =====================================================
-- 教师端课程图谱：新增学习内容正文字段
-- 注意：项目已配置 map-underscore-to-camel-case: false
--      现有表列名已使用 camelCase (symbolSize / isDelete 等)
--      本迁移保持同样的命名约定
-- =====================================================

ALTER TABLE `course_graph_node`
  ADD COLUMN `learningContent` LONGTEXT NULL DEFAULT NULL COMMENT '学习内容正文（HTML）' AFTER `learnUrl`;
