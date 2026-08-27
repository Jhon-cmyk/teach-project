-- =====================================================
-- 课程图谱链接修复脚本
-- 作用：把链状结构改成正确的 3 层树状结构
-- 执行前建议先备份：SELECT * FROM course_graph_link WHERE isDelete = 0;
-- =====================================================

-- 1. 彻底删除所有现有链接（唯一索引不区分软删除，必须硬删除才能重新插入）
DELETE FROM course_graph_link WHERE id IS NOT NULL;

-- 2. 批量插入正确的父子链接（根据节点名称自动匹配 id）
INSERT INTO course_graph_link (source, target, relationType, description, sortOrder, isDelete, createTime, updateTime)
SELECT p.id, c.id, 'related', '', 0, 0, NOW(), NOW()
FROM (
    -- 第1层 → 第2层
    SELECT '计算机科学' AS pname, '后端开发' AS cname
    UNION ALL SELECT '计算机科学', '前端开发'
    UNION ALL SELECT '计算机科学', '人工智能'
    -- 第2层 → 第3层（后端开发）
    UNION ALL SELECT '后端开发', 'Java'
    UNION ALL SELECT '后端开发', 'MySQL'
    UNION ALL SELECT '后端开发', 'Spring Boot'
    UNION ALL SELECT '后端开发', 'JavaWeb 项目实战'
    -- 第2层 → 第3层（前端开发）
    UNION ALL SELECT '前端开发', 'HTML/CSS'
    UNION ALL SELECT '前端开发', 'Vue3'
    UNION ALL SELECT '前端开发', '前端工程化'
    UNION ALL SELECT '前端开发', '组件化实战'
    -- 第2层 → 第3层（人工智能）
    UNION ALL SELECT '人工智能', 'Python'
    UNION ALL SELECT '人工智能', '机器学习基础'
    UNION ALL SELECT '人工智能', '深度学习入门'
    UNION ALL SELECT '人工智能', '计算机视觉案例'
) AS rel
JOIN course_graph_node p ON p.name = rel.pname AND p.isDelete = 0
JOIN course_graph_node c ON c.name = rel.cname AND c.isDelete = 0;

-- 3. 同时修复 parentId，让数据更规范
UPDATE course_graph_node n
JOIN (
    SELECT '后端开发' AS cname, '计算机科学' AS pname
    UNION ALL SELECT '前端开发', '计算机科学'
    UNION ALL SELECT '人工智能', '计算机科学'
    UNION ALL SELECT 'Java', '后端开发'
    UNION ALL SELECT 'MySQL', '后端开发'
    UNION ALL SELECT 'Spring Boot', '后端开发'
    UNION ALL SELECT 'JavaWeb 项目实战', '后端开发'
    UNION ALL SELECT 'HTML/CSS', '前端开发'
    UNION ALL SELECT 'Vue3', '前端开发'
    UNION ALL SELECT '前端工程化', '前端开发'
    UNION ALL SELECT '组件化实战', '前端开发'
    UNION ALL SELECT 'Python', '人工智能'
    UNION ALL SELECT '机器学习基础', '人工智能'
    UNION ALL SELECT '深度学习入门', '人工智能'
    UNION ALL SELECT '计算机视觉案例', '人工智能'
) AS rel ON n.name = rel.cname
JOIN course_graph_node p ON p.name = rel.pname AND p.isDelete = 0
SET n.parentId = p.id;

-- 4. 确保根节点的 parentId 为 null
UPDATE course_graph_node SET parentId = NULL WHERE name = '计算机科学';
