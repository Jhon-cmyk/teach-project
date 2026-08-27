-- =====================================================
-- 教师端课程图谱 Phase 1: 增加父子关系与学习链接字段
-- 注意：项目已配置 map-underscore-to-camel-case: false
--      现有表列名已使用 camelCase (symbolSize / isDelete 等)
--      本迁移保持同样的命名约定
-- =====================================================

ALTER TABLE `course_graph_node`
  ADD COLUMN `parentId` VARCHAR(64) NULL DEFAULT NULL COMMENT '父节点 id，null 表示根节点' AFTER `id`,
  ADD COLUMN `learnUrl` VARCHAR(512) NULL DEFAULT NULL COMMENT '叶子节点学习链接 URL' AFTER `resourceTypes`,
  ADD INDEX `idx_parent_id` (`parentId`);
