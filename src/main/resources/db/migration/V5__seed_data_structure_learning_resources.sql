-- Curated public Data Structure resources for daily recommendations.
-- Video metadata and covers come from the public Bilibili video pages; playback
-- keeps using the official embedded player already supported by the frontend.

SET @algorithm_category_id := (
    SELECT id FROM course_category
    WHERE name = '算法' AND is_delete = 0
    ORDER BY id LIMIT 1
);

INSERT INTO course
    (name, description, coverImg, videoUrl, type, teacherId, teacherName,
     sourceType, creatorId, creatorRole, publishStatus, categoryId, price,
     points_cost, pointsCost, video_context, face_detection_required, isDelete)
SELECT
    '数据结构考研基础：408 与自命题系统课',
    '考研理论方向。系统学习数据结构基本概念、复杂度、线性表、栈队列、树、图、查找与排序，强调基础原理、考试框架和知识体系。',
    'https://i0.hdslb.com/bfs/archive/d55d1dc11c6cba2b805ca4df3e9a596e19e3fc9b.png',
    'https://www.bilibili.com/video/BV1oK4y1i76S?p=3',
    'video', 0, '白话拆解数据结构', 'platform', NULL, 'admin', 'published',
    @algorithm_category_id, 0, 0, 0,
    '数据结构 考研 408 理论 基础 原理 算法复杂度 线性表 链表 栈 队列 二叉树 图 查找 排序',
    0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM course WHERE name = '数据结构考研基础：408 与自命题系统课' AND isDelete = 0
);
SET @ds_pg_foundation := (
    SELECT id FROM course WHERE name = '数据结构考研基础：408 与自命题系统课' AND isDelete = 0 ORDER BY id LIMIT 1
);

INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_foundation, '01 数据结构基本概念', 'https://www.bilibili.com/video/BV1oK4y1i76S?p=3', 1, 0
WHERE @ds_pg_foundation IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_foundation AND sort_order=1 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_foundation, '02 算法效率与复杂度分析', 'https://www.bilibili.com/video/BV1oK4y1i76S?p=4', 2, 0
WHERE @ds_pg_foundation IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_foundation AND sort_order=2 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_foundation, '03 顺序表及其基本操作', 'https://www.bilibili.com/video/BV1oK4y1i76S?p=7', 3, 0
WHERE @ds_pg_foundation IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_foundation AND sort_order=3 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_foundation, '04 栈的表示与实现', 'https://www.bilibili.com/video/BV1oK4y1i76S?p=14', 4, 0
WHERE @ds_pg_foundation IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_foundation AND sort_order=4 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_foundation, '05 二叉树的存储与遍历', 'https://www.bilibili.com/video/BV1oK4y1i76S?p=23', 5, 0
WHERE @ds_pg_foundation IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_foundation AND sort_order=5 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_foundation, '06 图的遍历', 'https://www.bilibili.com/video/BV1oK4y1i76S?p=30', 6, 0
WHERE @ds_pg_foundation IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_foundation AND sort_order=6 AND is_delete=0);

INSERT INTO course
    (name, description, coverImg, videoUrl, type, teacherId, teacherName,
     sourceType, creatorId, creatorRole, publishStatus, categoryId, price,
     points_cost, pointsCost, video_context, face_detection_required, isDelete)
SELECT
    '数据结构考研刷题：王道课后题精讲',
    '考研理论与真题训练方向。覆盖复杂度、链表、栈队列、树、图、查找和排序易错题，用考试题型检验数据结构原理。',
    'https://i0.hdslb.com/bfs/archive/1700c329c1c5813c273250b4f68b279020cd2080.png',
    'https://www.bilibili.com/video/BV12k4y1A79r?p=2',
    'video', 0, '鲍松山', 'platform', NULL, 'admin', 'published',
    @algorithm_category_id, 0, 0, 0,
    '数据结构 考研 王道 408 真题 考试 理论 时间复杂度 链表 栈 队列 二叉树 图 查找 排序',
    0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM course WHERE name = '数据结构考研刷题：王道课后题精讲' AND isDelete = 0
);
SET @ds_pg_exercises := (
    SELECT id FROM course WHERE name = '数据结构考研刷题：王道课后题精讲' AND isDelete = 0 ORDER BY id LIMIT 1
);

INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_exercises, '01 时间复杂度选择题', 'https://www.bilibili.com/video/BV12k4y1A79r?p=2', 1, 0
WHERE @ds_pg_exercises IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_exercises AND sort_order=1 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_exercises, '02 链表选择题', 'https://www.bilibili.com/video/BV12k4y1A79r?p=6', 2, 0
WHERE @ds_pg_exercises IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_exercises AND sort_order=2 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_exercises, '03 栈的典型题', 'https://www.bilibili.com/video/BV12k4y1A79r?p=9', 3, 0
WHERE @ds_pg_exercises IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_exercises AND sort_order=3 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_exercises, '04 二叉树性质题', 'https://www.bilibili.com/video/BV12k4y1A79r?p=19', 4, 0
WHERE @ds_pg_exercises IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_exercises AND sort_order=4 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_exercises, '05 图的基本概念题', 'https://www.bilibili.com/video/BV12k4y1A79r?p=28', 5, 0
WHERE @ds_pg_exercises IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_exercises AND sort_order=5 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_pg_exercises, '06 排序综合题', 'https://www.bilibili.com/video/BV12k4y1A79r?p=46', 6, 0
WHERE @ds_pg_exercises IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_exercises AND sort_order=6 AND is_delete=0);

INSERT INTO course
    (name, description, coverImg, videoUrl, type, teacherId, teacherName,
     sourceType, creatorId, creatorRole, publishStatus, categoryId, price,
     points_cost, pointsCost, video_context, face_detection_required, isDelete)
SELECT
    'Java 数据结构就业实战：从实现到工程应用',
    '就业实操方向。用 Java 动手实现链表、栈、队列、二叉树、哈希表和图，结合工程案例理解数据结构在开发中的应用。',
    'https://i0.hdslb.com/bfs/archive/60921e2665a6c402cf864f77bf7958dddc9674b2.jpg',
    'https://www.bilibili.com/video/BV1tU411U7SF?p=30',
    'video', 0, '动力节点', 'platform', NULL, 'admin', 'published',
    @algorithm_category_id, 0, 0, 0,
    '数据结构 就业 Java 项目 实战 工程 开发 应用 链表 栈 队列 二叉树 哈希表 图',
    0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM course WHERE name = 'Java 数据结构就业实战：从实现到工程应用' AND isDelete = 0
);
SET @ds_job_java := (
    SELECT id FROM course WHERE name = 'Java 数据结构就业实战：从实现到工程应用' AND isDelete = 0 ORDER BY id LIMIT 1
);

INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_java, '01 Java 链表实现案例', 'https://www.bilibili.com/video/BV1tU411U7SF?p=30', 1, 0
WHERE @ds_job_java IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_java AND sort_order=1 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_java, '02 用链表实现栈', 'https://www.bilibili.com/video/BV1tU411U7SF?p=43', 2, 0
WHERE @ds_job_java IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_java AND sort_order=2 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_java, '03 基于数组实现队列', 'https://www.bilibili.com/video/BV1tU411U7SF?p=51', 3, 0
WHERE @ds_job_java IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_java AND sort_order=3 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_java, '04 二叉树遍历实现', 'https://www.bilibili.com/video/BV1tU411U7SF?p=61', 4, 0
WHERE @ds_job_java IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_java AND sort_order=4 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_java, '05 哈希表设计', 'https://www.bilibili.com/video/BV1tU411U7SF?p=86', 5, 0
WHERE @ds_job_java IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_java AND sort_order=5 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_java, '06 图的设计与深度优先遍历', 'https://www.bilibili.com/video/BV1tU411U7SF?p=89', 6, 0
WHERE @ds_job_java IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_java AND sort_order=6 AND is_delete=0);

INSERT INTO course
    (name, description, coverImg, videoUrl, type, teacherId, teacherName,
     sourceType, creatorId, creatorRole, publishStatus, categoryId, price,
     points_cost, pointsCost, video_context, face_detection_required, isDelete)
SELECT
    '数据结构求职强化：LeetCode 与面试题实战',
    '就业实操与面试方向。围绕反转链表、栈实现队列、二叉树、哈希冲突和图搜索完成代码练习，提升项目编码与面试能力。',
    'https://i0.hdslb.com/bfs/archive/d80c4cdf0891740d6145617bd25afdc63b0223b3.png',
    'https://www.bilibili.com/video/BV1ao4y1S7nc?p=40',
    'video', 0, '清风学Java', 'platform', NULL, 'admin', 'published',
    @algorithm_category_id, 0, 0, 0,
    '数据结构 就业 LeetCode 面试 项目 实战 Java 开发 反转链表 栈 队列 二叉树 哈希 图 BFS DFS',
    0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM course WHERE name = '数据结构求职强化：LeetCode 与面试题实战' AND isDelete = 0
);
SET @ds_job_interview := (
    SELECT id FROM course WHERE name = '数据结构求职强化：LeetCode 与面试题实战' AND isDelete = 0 ORDER BY id LIMIT 1
);

INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_interview, '01 反转链表：递归实现', 'https://www.bilibili.com/video/BV1ao4y1S7nc?p=40', 1, 0
WHERE @ds_job_interview IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_interview AND sort_order=1 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_interview, '02 反转链表：迭代实现', 'https://www.bilibili.com/video/BV1ao4y1S7nc?p=41', 2, 0
WHERE @ds_job_interview IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_interview AND sort_order=2 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_interview, '03 用栈实现队列', 'https://www.bilibili.com/video/BV1ao4y1S7nc?p=73', 3, 0
WHERE @ds_job_interview IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_interview AND sort_order=3 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_interview, '04 二叉树高度练习', 'https://www.bilibili.com/video/BV1ao4y1S7nc?p=114', 4, 0
WHERE @ds_job_interview IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_interview AND sort_order=4 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_interview, '05 JDK 8 哈希冲突解决', 'https://www.bilibili.com/video/BV1ao4y1S7nc?p=213', 5, 0
WHERE @ds_job_interview IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_interview AND sort_order=5 AND is_delete=0);
INSERT INTO course_chapter (course_id, title, video_url, sort_order, is_delete)
SELECT @ds_job_interview, '06 图、BFS、DFS 与拓扑排序', 'https://www.bilibili.com/video/BV1ao4y1S7nc?p=323', 6, 0
WHERE @ds_job_interview IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_interview AND sort_order=6 AND is_delete=0);

-- Postgraduate-oriented text courses -------------------------------------------------

INSERT INTO text_course (name, cover_img, description)
SELECT '数据结构考研核心：复杂度与线性结构', '/course-covers/data-structure-theory.svg',
       '考研理论专题。建立复杂度、顺序表、链表、栈与队列的基础原理和考试分析框架。'
WHERE NOT EXISTS (SELECT 1 FROM text_course WHERE name='数据结构考研核心：复杂度与线性结构');
SET @text_ds_pg_linear := (SELECT id FROM text_course WHERE name='数据结构考研核心：复杂度与线性结构' ORDER BY id LIMIT 1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_linear, '01 复杂度分析的统一方法', '# 复杂度分析\n\n先确定基本操作，再统计它随输入规模 n 的执行次数。保留最高阶并忽略常数，得到时间复杂度。\n\n## 考试要点\n- 顺序执行取最大项\n- 嵌套循环通常相乘\n- 递归先写递推式\n- 最好、最坏与平均复杂度必须说明场景\n\n常见数量级：O(1) < O(log n) < O(n) < O(n log n) < O(n²)。', 1
WHERE @text_ds_pg_linear IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_linear AND sort_order=1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_linear, '02 顺序表与链表对比', '# 线性表\n\n顺序表支持 O(1) 随机访问，但中间插入删除需要移动元素。链表访问第 i 个结点需要 O(n)，已知结点位置后的插入删除可达 O(1)。\n\n## 易错点\n- 区分查找结点和修改指针的成本\n- 单链表删除结点通常还需要前驱\n- 循环链表判空条件与普通链表不同\n- 双链表操作必须同时维护前后方向', 2
WHERE @text_ds_pg_linear IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_linear AND sort_order=2);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_linear, '03 栈与队列的典型模型', '# 栈与队列\n\n栈是后进先出，常用于递归、括号匹配和表达式求值；队列是先进先出，常用于层序遍历和广度优先搜索。\n\n## 公式\n循环队列长度可写为 `(rear - front + capacity) % capacity`。若牺牲一个存储单元，队满条件为 `(rear + 1) % capacity == front`。', 3
WHERE @text_ds_pg_linear IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_linear AND sort_order=3);

INSERT INTO text_course (name, cover_img, description)
SELECT '数据结构考研核心：树与图的原理', '/course-covers/data-structure-theory.svg',
       '考研理论专题。系统复习二叉树性质、遍历、最小生成树、最短路径和拓扑排序原理。'
WHERE NOT EXISTS (SELECT 1 FROM text_course WHERE name='数据结构考研核心：树与图的原理');
SET @text_ds_pg_graph := (SELECT id FROM text_course WHERE name='数据结构考研核心：树与图的原理' ORDER BY id LIMIT 1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_graph, '01 二叉树性质与遍历', '# 二叉树\n\n前序是根左右，中序是左根右，后序是左右根。前序与中序或后序与中序可唯一确定一棵二叉树。\n\n## 高频结论\n- 第 i 层最多有 2^(i-1) 个结点\n- 高度为 h 的二叉树最多有 2^h-1 个结点\n- 非空二叉树中叶子数 n0 与度为 2 的结点数 n2 满足 n0=n2+1', 1
WHERE @text_ds_pg_graph IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_graph AND sort_order=1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_graph, '02 图的存储与遍历', '# 图\n\n邻接矩阵适合稠密图，空间复杂度 O(V²)；邻接表适合稀疏图，空间复杂度 O(V+E)。DFS 借助递归或栈，BFS 借助队列。\n\n无向图遍历所有邻接表时，每条边会被访问两次；有向图的入度与出度需要分开统计。', 2
WHERE @text_ds_pg_graph IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_graph AND sort_order=2);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_graph, '03 图算法辨析', '# 图算法\n\n- Prim：从顶点扩张最小生成树，适合稠密图\n- Kruskal：按边权排序并用并查集判环，适合稀疏图\n- Dijkstra：解决非负权单源最短路\n- Floyd：动态规划求任意两点最短路\n- 拓扑排序：仅适用于有向无环图\n\n考试时先判断问题类型和边权约束，再选择算法。', 3
WHERE @text_ds_pg_graph IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_graph AND sort_order=3);

INSERT INTO text_course (name, cover_img, description)
SELECT '数据结构考研核心：查找排序与 408 易错点', '/course-covers/data-structure-theory.svg',
       '考研真题与考试专题。梳理查找结构、排序算法性质、复杂度以及 408 常见陷阱。'
WHERE NOT EXISTS (SELECT 1 FROM text_course WHERE name='数据结构考研核心：查找排序与 408 易错点');
SET @text_ds_pg_sort := (SELECT id FROM text_course WHERE name='数据结构考研核心：查找排序与 408 易错点' ORDER BY id LIMIT 1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_sort, '01 查找结构的选择', '# 查找\n\n有序顺序表可二分查找，平均复杂度 O(log n)；二叉搜索树的性能取决于树高；平衡树通过旋转约束高度；哈希表以空间换时间，但必须处理冲突。\n\nB 树和 B+ 树通过多路平衡降低外存访问次数，是数据库索引题的重点。', 1
WHERE @text_ds_pg_sort IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_sort AND sort_order=1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_sort, '02 排序算法性质表', '# 排序\n\n稳定算法：冒泡、插入、归并、基数。通常不稳定：选择、快速、希尔、堆排序。\n\n归并排序时间稳定为 O(n log n)，但需要 O(n) 辅助空间；快速排序平均 O(n log n)，最坏 O(n²)；堆排序最坏仍为 O(n log n)。', 2
WHERE @text_ds_pg_sort IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_sort AND sort_order=2);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_pg_sort, '03 408 选择题检查清单', '# 答题检查清单\n\n1. 题目问的是平均、最坏还是最好情况？\n2. 数据是否有序，图是否带权，边权能否为负？\n3. 链表操作是否已知前驱？\n4. 排序是否要求稳定或原地？\n5. 树的高度从 0 还是 1 开始定义？\n\n先圈定条件再套结论，可以显著减少易错题失分。', 3
WHERE @text_ds_pg_sort IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_sort AND sort_order=3);

-- Employment-oriented text courses ---------------------------------------------------

INSERT INTO text_course (name, cover_img, description)
SELECT '数据结构就业实战：Java 集合与链表实现', '/course-covers/data-structure-practice.svg',
       '就业项目实战专题。用 Java 接口设计、单元测试和复杂度分析实现动态数组、链表与集合。'
WHERE NOT EXISTS (SELECT 1 FROM text_course WHERE name='数据结构就业实战：Java 集合与链表实现');
SET @text_ds_job_list := (SELECT id FROM text_course WHERE name='数据结构就业实战：Java 集合与链表实现' ORDER BY id LIMIT 1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_list, '01 从 List 接口开始设计', '# 工程化 List\n\n先定义 `size`、`get`、`set`、`add`、`remove` 接口，再分别实现动态数组和链表。调用方只依赖接口，便于替换实现。\n\n## 实战要求\n- 所有下标先做边界检查\n- 扩容策略写成独立方法\n- 修改结构后维护 size\n- 为首尾、中间、空集合编写测试', 1
WHERE @text_ds_job_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_list AND sort_order=1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_list, '02 链表实现的指针安全', '# 链表实现\n\n插入时先保存后继，再连接新结点；删除时先接通前后结点，再释放目标引用。双向链表要同时维护 `prev` 和 `next`。\n\n建议用哨兵结点统一空链表和首结点逻辑，减少分支，也降低空指针错误概率。', 2
WHERE @text_ds_job_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_list AND sort_order=2);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_list, '03 反转链表与环检测', '# 高频编码题\n\n反转链表的迭代版本维护 `prev`、`current`、`next` 三个引用。环检测使用快慢指针：慢指针每次一步，快指针每次两步，相遇说明存在环。\n\n完成实现后，用空链表、单结点、两结点、有环和无环输入做测试。', 3
WHERE @text_ds_job_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_list AND sort_order=3);

INSERT INTO text_course (name, cover_img, description)
SELECT '数据结构就业实战：栈队列、缓存与任务调度', '/course-covers/data-structure-practice.svg',
       '就业工程应用专题。通过表达式解析、消息队列、LRU 缓存和优先任务调度练习数据结构。'
WHERE NOT EXISTS (SELECT 1 FROM text_course WHERE name='数据结构就业实战：栈队列、缓存与任务调度');
SET @text_ds_job_queue := (SELECT id FROM text_course WHERE name='数据结构就业实战：栈队列、缓存与任务调度' ORDER BY id LIMIT 1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_queue, '01 栈在解析器中的应用', '# 表达式解析\n\n扫描中缀表达式时，操作数进入输出序列，运算符根据优先级入栈或出栈，最终得到后缀表达式。计算后缀表达式只需一个操作数栈。\n\n项目中还可用栈实现撤销、浏览历史和括号校验。', 1
WHERE @text_ds_job_queue IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_queue AND sort_order=1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_queue, '02 LRU 缓存组合设计', '# LRU 缓存\n\n哈希表负责 O(1) 定位结点，双向链表负责 O(1) 调整访问顺序。读取命中后把结点移到头部；容量满时删除尾结点，并同步删除哈希映射。\n\n这是组合数据结构的典型项目，也是后端面试高频题。', 2
WHERE @text_ds_job_queue IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_queue AND sort_order=2);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_queue, '03 优先队列驱动任务调度', '# 任务调度\n\n优先队列通常由二叉堆实现，插入和删除最高优先级元素为 O(log n)，查看堆顶为 O(1)。\n\n设计任务对象时要明确比较规则，并为同优先级任务增加时间或序号作为稳定的第二排序键。', 3
WHERE @text_ds_job_queue IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_queue AND sort_order=3);

INSERT INTO text_course (name, cover_img, description)
SELECT '数据结构就业实战：树、图、哈希与面试项目', '/course-covers/data-structure-practice.svg',
       '就业开发与面试专题。通过目录树、搜索建议、依赖分析和哈希索引完成可运行的小项目。'
WHERE NOT EXISTS (SELECT 1 FROM text_course WHERE name='数据结构就业实战：树、图、哈希与面试项目');
SET @text_ds_job_graph := (SELECT id FROM text_course WHERE name='数据结构就业实战：树、图、哈希与面试项目' ORDER BY id LIMIT 1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_graph, '01 Trie 搜索建议服务', '# Trie 项目\n\n每个结点保存子字符映射和单词结束标记。插入和查询复杂度与单词长度相关。搜索建议可先定位前缀结点，再 DFS 收集有限数量结果。\n\n工程上还要限制字符集、结果数量和结点规模，避免内存无限增长。', 1
WHERE @text_ds_job_graph IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_graph AND sort_order=1);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_graph, '02 图实现依赖分析', '# 依赖图\n\n把任务或模块作为顶点，依赖关系作为有向边。使用入度表和队列执行拓扑排序；若最终处理的顶点数少于总顶点数，说明存在循环依赖。\n\n这个模型可用于课程先修、构建系统、工作流和任务编排。', 2
WHERE @text_ds_job_graph IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_graph AND sort_order=2);
INSERT INTO text_node (course_id, title, content, sort_order)
SELECT @text_ds_job_graph, '03 哈希索引的工程边界', '# 哈希索引\n\n良好的哈希函数应让键均匀分布。链地址法实现简单，开放寻址更依赖装载因子和探测策略。\n\n项目中要关注扩容成本、线程安全、可变键和恶意碰撞。面试回答不能只说平均 O(1)，还应解释最坏情况和扩容过程。', 3
WHERE @text_ds_job_graph IS NOT NULL AND NOT EXISTS (SELECT 1 FROM text_node WHERE course_id=@text_ds_job_graph AND sort_order=3);
