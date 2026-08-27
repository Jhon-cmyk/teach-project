START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_computer_0809_major;
CREATE TEMPORARY TABLE tmp_computer_0809_major AS
SELECT
    id AS root_id,
    REPLACE(id, '_root', '') AS prefix,
    name AS major_name,
    sortOrder AS base_sort
FROM course_graph_node
WHERE isDelete = 0
  AND category = '计算机类（0809）'
  AND (parentId IS NULL OR parentId = '');

DROP TEMPORARY TABLE IF EXISTS tmp_graph_module;
CREATE TEMPORARY TABLE tmp_graph_module (
    module_key VARCHAR(32) NOT NULL,
    module_name VARCHAR(64) NOT NULL,
    module_desc VARCHAR(255) NOT NULL,
    module_hours INT NOT NULL,
    module_sort INT NOT NULL,
    PRIMARY KEY (module_key)
) DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_graph_module (module_key, module_name, module_desc, module_hours, module_sort)
VALUES
    ('basic', '专业基础课', '的数学基础、程序设计基础、计算机系统基础与专业导论内容。', 48, 1),
    ('core', '专业核心课', '的关键理论、核心技术体系、工程方法和主干课程内容。', 64, 2),
    ('advanced', '前沿方向', '的行业趋势、智能化方向、平台技术和新兴应用内容。', 40, 3),
    ('practice', '实践环节', '的课程设计、综合实训、项目交付和工程实践内容。', 56, 4);

INSERT INTO course_graph_node
    (id, parentId, name, category, symbolSize, description, difficulty, importance, estimatedHours, teachingWeek,
     commonMistakes, teachingTips, resourceCount, exerciseCount, isCore, isKeyPoint, resourceSummary, resourceTypes,
     learnUrl, learningContent, sortOrder, createTime, updateTime, isDelete)
SELECT
    CONCAT(m.prefix, '_', g.module_key),
    m.root_id,
    g.module_name,
    g.module_name,
    40,
    CONCAT(m.major_name, g.module_desc),
    CASE WHEN g.module_key IN ('core', 'advanced') THEN 'high' ELSE 'medium' END,
    CASE WHEN g.module_key IN ('basic', 'core') THEN 'high' ELSE 'medium' END,
    g.module_hours,
    g.module_sort,
    NULL,
    NULL,
    0,
    0,
    CASE WHEN g.module_key IN ('basic', 'core') THEN 1 ELSE 0 END,
    CASE WHEN g.module_key = 'core' THEN 1 ELSE 0 END,
    NULL,
    NULL,
    NULL,
    CONCAT(
        '<h3>', m.major_name, ' - ', g.module_name, '</h3>',
        '<p>本模块覆盖', m.major_name, g.module_desc, '</p>',
        '<h3>教学组织建议</h3>',
        '<p>建议按“概念理解、案例分析、工具实践、阶段评价”的路径组织学习，并与课程资源、练习和项目任务绑定。</p>'
    ),
    m.base_sort * 100 + g.module_sort,
    NOW(),
    NOW(),
    0
FROM tmp_computer_0809_major m
CROSS JOIN tmp_graph_module g
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    parentId = VALUES(parentId),
    name = VALUES(name),
    category = VALUES(category),
    symbolSize = VALUES(symbolSize),
    description = VALUES(description),
    difficulty = VALUES(difficulty),
    importance = VALUES(importance),
    estimatedHours = VALUES(estimatedHours),
    teachingWeek = VALUES(teachingWeek),
    isCore = VALUES(isCore),
    isKeyPoint = VALUES(isKeyPoint),
    learningContent = VALUES(learningContent),
    sortOrder = VALUES(sortOrder),
    updateTime = NOW(),
    isDelete = 0;

INSERT INTO course_graph_link
    (source, target, relationType, description, sortOrder, createTime, updateTime, isDelete)
SELECT
    m.root_id,
    CONCAT(m.prefix, '_', g.module_key),
    '包含',
    g.module_name,
    m.base_sort * 100 + g.module_sort,
    NOW(),
    NOW(),
    0
FROM tmp_computer_0809_major m
CROSS JOIN tmp_graph_module g
WHERE NOT EXISTS (
    SELECT 1
    FROM course_graph_link l
    WHERE l.isDelete = 0
      AND l.source = m.root_id
      AND l.target = CONCAT(m.prefix, '_', g.module_key)
);

DROP TEMPORARY TABLE IF EXISTS tmp_course_template;
CREATE TEMPORARY TABLE tmp_course_template (
    module_key VARCHAR(32) NOT NULL,
    course_key VARCHAR(32) NOT NULL,
    course_suffix VARCHAR(128) NOT NULL,
    course_desc VARCHAR(255) NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    importance VARCHAR(16) NOT NULL,
    hours INT NOT NULL,
    course_sort INT NOT NULL,
    PRIMARY KEY (module_key, course_key)
) DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_course_template
    (module_key, course_key, course_suffix, course_desc, difficulty, importance, hours, course_sort)
VALUES
    ('basic', 'intro', '专业导论与技术基础', '建立专业认知，理解技术体系、典型场景和人才能力结构。', 'low', 'high', 24, 1),
    ('basic', 'programming', '程序设计与数据处理基础', '训练程序设计、数据表示、问题抽象和基础工具使用能力。', 'medium', 'high', 48, 2),
    ('core', 'system', '核心系统与平台技术', '学习本专业主干系统、平台架构、关键算法和工程实现方法。', 'high', 'high', 56, 1),
    ('core', 'engineering', '工程设计与实现方法', '围绕需求分析、方案设计、开发实现、测试验证形成工程闭环。', 'high', 'high', 48, 2),
    ('advanced', 'frontier', '前沿技术专题', '关注智能化、可信化、沉浸式、平台化等新兴技术趋势。', 'high', 'medium', 32, 1),
    ('advanced', 'application', '行业应用与创新实践', '结合教育、医疗、工业、政务、传媒等场景开展应用创新。', 'medium', 'medium', 32, 2),
    ('practice', 'design', '综合课程设计', '以阶段性项目串联知识点，完成从方案到原型的综合训练。', 'medium', 'high', 40, 1),
    ('practice', 'project', '工程实训与项目交付', '模拟真实项目过程，完成团队协作、文档沉淀、测试部署和答辩交付。', 'high', 'high', 56, 2);

INSERT INTO course_graph_node
    (id, parentId, name, category, symbolSize, description, difficulty, importance, estimatedHours, teachingWeek,
     commonMistakes, teachingTips, resourceCount, exerciseCount, isCore, isKeyPoint, resourceSummary, resourceTypes,
     learnUrl, learningContent, sortOrder, createTime, updateTime, isDelete)
SELECT
    CONCAT(m.prefix, '_', g.module_key, '_', t.course_key),
    CONCAT(m.prefix, '_', g.module_key),
    CONCAT(m.major_name, t.course_suffix),
    g.module_name,
    30,
    t.course_desc,
    t.difficulty,
    t.importance,
    t.hours,
    g.module_sort,
    NULL,
    NULL,
    0,
    0,
    CASE WHEN t.importance = 'high' THEN 1 ELSE 0 END,
    CASE WHEN g.module_key = 'core' THEN 1 ELSE 0 END,
    NULL,
    NULL,
    NULL,
    CONCAT(
        '<h3>课程定位</h3>',
        '<p>', CONCAT(m.major_name, t.course_suffix), '用于支撑', m.major_name, '“', g.module_name, '”模块，', t.course_desc, '</p>',
        '<h3>核心内容</h3>',
        '<p>围绕基础概念、关键方法、典型案例和实践任务展开，建议配套课堂讲解、随堂练习、项目任务和阶段评价。</p>',
        '<h3>学习产出</h3>',
        '<p>学生应能说明关键概念，完成基础实验或项目任务，并将方法迁移到真实问题场景中。</p>'
    ),
    m.base_sort * 1000 + g.module_sort * 100 + t.course_sort,
    NOW(),
    NOW(),
    0
FROM tmp_computer_0809_major m
JOIN tmp_graph_module g
JOIN tmp_course_template t ON t.module_key = g.module_key
WHERE NOT EXISTS (
    SELECT 1
    FROM course_graph_node child
    WHERE child.isDelete = 0
      AND child.parentId = CONCAT(m.prefix, '_', g.module_key)
)
ON DUPLICATE KEY UPDATE
    parentId = VALUES(parentId),
    name = VALUES(name),
    category = VALUES(category),
    symbolSize = VALUES(symbolSize),
    description = VALUES(description),
    difficulty = VALUES(difficulty),
    importance = VALUES(importance),
    estimatedHours = VALUES(estimatedHours),
    teachingWeek = VALUES(teachingWeek),
    isCore = VALUES(isCore),
    isKeyPoint = VALUES(isKeyPoint),
    learningContent = VALUES(learningContent),
    sortOrder = VALUES(sortOrder),
    updateTime = NOW(),
    isDelete = 0;

INSERT INTO course_graph_link
    (source, target, relationType, description, sortOrder, createTime, updateTime, isDelete)
SELECT
    CONCAT(m.prefix, '_', g.module_key),
    CONCAT(m.prefix, '_', g.module_key, '_', t.course_key),
    '包含',
    t.course_suffix,
    m.base_sort * 1000 + g.module_sort * 100 + t.course_sort,
    NOW(),
    NOW(),
    0
FROM tmp_computer_0809_major m
JOIN tmp_graph_module g
JOIN tmp_course_template t ON t.module_key = g.module_key
JOIN course_graph_node target_node
  ON target_node.id = CONCAT(m.prefix, '_', g.module_key, '_', t.course_key)
 AND target_node.isDelete = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM course_graph_link l
    WHERE l.isDelete = 0
      AND l.source = CONCAT(m.prefix, '_', g.module_key)
      AND l.target = CONCAT(m.prefix, '_', g.module_key, '_', t.course_key)
);

COMMIT;
