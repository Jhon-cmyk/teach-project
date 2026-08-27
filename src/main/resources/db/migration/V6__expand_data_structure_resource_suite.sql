-- Expand the Data Structure learning suite after the initial V5 seed.
-- All inserts are idempotent so this file can also be verified repeatedly in isolation.

SET @algorithm_category_id := (
    SELECT id FROM course_category WHERE name='算法' AND is_delete=0 ORDER BY id LIMIT 1
);

-- Additional postgraduate video courses ----------------------------------------------

INSERT INTO course
    (name, description, coverImg, videoUrl, type, teacherId, teacherName, sourceType,
     creatorId, creatorRole, publishStatus, categoryId, price, points_cost, pointsCost,
     video_context, face_detection_required, isDelete)
SELECT
    '数据结构考研理论：华中科技大学系统公开课',
    '考研理论方向。大学体系化公开课，覆盖线性表、栈队列、串、数组、树、图、查找和排序，适合建立完整原理框架。',
    'https://i0.hdslb.com/bfs/archive/fd5f9fbe1579ba245e258f32762dd369f5a07b15.jpg',
    'https://www.bilibili.com/video/BV15E411V7S2?p=1',
    'video', 0, '投信箱的邮差', 'platform', NULL, 'admin', 'published',
    @algorithm_category_id, 0, 0, 0,
    '数据结构 考研 理论 基础 原理 大学公开课 线性表 栈 队列 树 二叉树 图 查找 排序', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='数据结构考研理论：华中科技大学系统公开课' AND isDelete=0);
SET @ds_pg_university := (SELECT id FROM course WHERE name='数据结构考研理论：华中科技大学系统公开课' AND isDelete=0 ORDER BY id LIMIT 1);

INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_university,'01 绪论与数据结构基本概念','https://www.bilibili.com/video/BV15E411V7S2?p=1',1,0
WHERE @ds_pg_university IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_university AND sort_order=1 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_university,'02 线性表的逻辑与存储结构','https://www.bilibili.com/video/BV15E411V7S2?p=3',2,0
WHERE @ds_pg_university IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_university AND sort_order=2 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_university,'03 栈的抽象数据类型','https://www.bilibili.com/video/BV15E411V7S2?p=7',3,0
WHERE @ds_pg_university IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_university AND sort_order=3 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_university,'04 队列的基本概念','https://www.bilibili.com/video/BV15E411V7S2?p=13',4,0
WHERE @ds_pg_university IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_university AND sort_order=4 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_university,'05 树与二叉树','https://www.bilibili.com/video/BV15E411V7S2?p=28',5,0
WHERE @ds_pg_university IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_university AND sort_order=5 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_university,'06 图的基本概念与存储','https://www.bilibili.com/video/BV15E411V7S2?p=33',6,0
WHERE @ds_pg_university IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_university AND sort_order=6 AND is_delete=0);

INSERT INTO course
    (name, description, coverImg, videoUrl, type, teacherId, teacherName, sourceType,
     creatorId, creatorRole, publishStatus, categoryId, price, points_cost, pointsCost,
     video_context, face_detection_required, isDelete)
SELECT
    '数据结构考研强化：408 算法大题与排序专题',
    '考研真题强化方向。集中讲解顺序表、链表算法题、数据结构真题大题和排序专题，适合二轮复习与考试训练。',
    'https://i2.hdslb.com/bfs/archive/e33335c4d5e557999b5760b209c43ab8c288b7a1.jpg',
    'https://www.bilibili.com/video/BV1hQy8YdE1Z?p=2',
    'video', 0, '复旦发哥带学408', 'platform', NULL, 'admin', 'published',
    @algorithm_category_id, 0, 0, 0,
    '数据结构 考研 408 真题 考试 理论 算法题 顺序表 链表 排序 强化', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='数据结构考研强化：408 算法大题与排序专题' AND isDelete=0);
SET @ds_pg_algorithm := (SELECT id FROM course WHERE name='数据结构考研强化：408 算法大题与排序专题' AND isDelete=0 ORDER BY id LIMIT 1);

INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_algorithm,'01 顺序表算法大题','https://www.bilibili.com/video/BV1hQy8YdE1Z?p=2',1,0
WHERE @ds_pg_algorithm IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_algorithm AND sort_order=1 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_algorithm,'02 链表算法题基础','https://www.bilibili.com/video/BV1hQy8YdE1Z?p=3',2,0
WHERE @ds_pg_algorithm IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_algorithm AND sort_order=2 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_algorithm,'03 链表算法题强化','https://www.bilibili.com/video/BV1hQy8YdE1Z?p=4',3,0
WHERE @ds_pg_algorithm IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_algorithm AND sort_order=3 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_algorithm,'04 数据结构历年真题大题','https://www.bilibili.com/video/BV1hQy8YdE1Z?p=5',4,0
WHERE @ds_pg_algorithm IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_algorithm AND sort_order=4 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_pg_algorithm,'05 内部排序综合专题','https://www.bilibili.com/video/BV1hQy8YdE1Z?p=6',5,0
WHERE @ds_pg_algorithm IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_pg_algorithm AND sort_order=5 AND is_delete=0);

-- Additional employment-oriented video courses ---------------------------------------

INSERT INTO course
    (name, description, coverImg, videoUrl, type, teacherId, teacherName, sourceType,
     creatorId, creatorRole, publishStatus, categoryId, price, points_cost, pointsCost,
     video_context, face_detection_required, isDelete)
SELECT
    '数据结构就业实战：C 语言完整代码实现',
    '就业实操方向。以完整代码实现顺序表、链表、栈、队列、二叉树和图，适合训练指针、内存与工程调试能力。',
    'https://i1.hdslb.com/bfs/archive/b1c2b6281722598f12dd7f4e6f3371458e7900b5.jpg',
    'https://www.bilibili.com/video/BV1bM411u7Ki?p=2',
    'video', 0, '鲍松山', 'platform', NULL, 'admin', 'published',
    @algorithm_category_id, 0, 0, 0,
    '数据结构 就业 C语言 项目 实战 工程 开发 代码实现 指针 内存 链表 栈 队列 二叉树 图', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='数据结构就业实战：C 语言完整代码实现' AND isDelete=0);
SET @ds_job_c := (SELECT id FROM course WHERE name='数据结构就业实战：C 语言完整代码实现' AND isDelete=0 ORDER BY id LIMIT 1);

INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_c,'01 顺序表完整实现','https://www.bilibili.com/video/BV1bM411u7Ki?p=2',1,0
WHERE @ds_job_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_c AND sort_order=1 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_c,'02 单链表完整实现','https://www.bilibili.com/video/BV1bM411u7Ki?p=6',2,0
WHERE @ds_job_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_c AND sort_order=2 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_c,'03 顺序栈实现','https://www.bilibili.com/video/BV1bM411u7Ki?p=21',3,0
WHERE @ds_job_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_c AND sort_order=3 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_c,'04 链队列实现','https://www.bilibili.com/video/BV1bM411u7Ki?p=24',4,0
WHERE @ds_job_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_c AND sort_order=4 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_c,'05 二叉树创建与遍历','https://www.bilibili.com/video/BV1bM411u7Ki?p=37',5,0
WHERE @ds_job_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_c AND sort_order=5 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_c,'06 图的邻接矩阵实现','https://www.bilibili.com/video/BV1bM411u7Ki?p=47',6,0
WHERE @ds_job_c IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_c AND sort_order=6 AND is_delete=0);

INSERT INTO course
    (name, description, coverImg, videoUrl, type, teacherId, teacherName, sourceType,
     creatorId, creatorRole, publishStatus, categoryId, price, points_cost, pointsCost,
     video_context, face_detection_required, isDelete)
SELECT
    '数据结构就业进阶：黑马 Java 源码与面试题',
    '就业实操与面试方向。通过哨兵链表、环形队列、栈、堆、二叉搜索树和红黑树源码训练工程实现及面试表达。',
    'https://i2.hdslb.com/bfs/archive/f542f29399efb3c072a1cdd754c2c14ecbfbde41.jpg',
    'https://www.bilibili.com/video/BV1Lv4y1e7HL?p=29',
    'video', 0, '黑马程序员', 'platform', NULL, 'admin', 'published',
    @algorithm_category_id, 0, 0, 0,
    '数据结构 就业 Java 项目 实战 工程 源码 面试 链表 队列 栈 堆 二叉树 红黑树', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM course WHERE name='数据结构就业进阶：黑马 Java 源码与面试题' AND isDelete=0);
SET @ds_job_heima := (SELECT id FROM course WHERE name='数据结构就业进阶：黑马 Java 源码与面试题' AND isDelete=0 ORDER BY id LIMIT 1);

INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_heima,'01 单向链表与接口设计','https://www.bilibili.com/video/BV1Lv4y1e7HL?p=29',1,0
WHERE @ds_job_heima IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_heima AND sort_order=1 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_heima,'02 环形数组队列实现','https://www.bilibili.com/video/BV1Lv4y1e7HL?p=94',2,0
WHERE @ds_job_heima IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_heima AND sort_order=2 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_heima,'03 链表栈实现','https://www.bilibili.com/video/BV1Lv4y1e7HL?p=103',3,0
WHERE @ds_job_heima IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_heima AND sort_order=3 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_heima,'04 堆实现优先级队列','https://www.bilibili.com/video/BV1Lv4y1e7HL?p=120',4,0
WHERE @ds_job_heima IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_heima AND sort_order=4 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_heima,'05 二叉搜索树实现','https://www.bilibili.com/video/BV1Lv4y1e7HL?p=162',5,0
WHERE @ds_job_heima IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_heima AND sort_order=5 AND is_delete=0);
INSERT INTO course_chapter (course_id,title,video_url,sort_order,is_delete)
SELECT @ds_job_heima,'06 红黑树原理与源码','https://www.bilibili.com/video/BV1Lv4y1e7HL?p=192',6,0
WHERE @ds_job_heima IS NOT NULL AND NOT EXISTS (SELECT 1 FROM course_chapter WHERE course_id=@ds_job_heima AND sort_order=6 AND is_delete=0);

-- Text course expansion follows below.

-- Enrich the three existing postgraduate text courses to six substantial chapters.

UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 复杂度分析的统一方法\n\n复杂度不是背答案，而是描述输入规模增长时，算法所需时间或空间的增长趋势。分析前先定义输入规模 n，再找出执行次数随 n 变化的基本操作。顺序语句取最大量级，嵌套循环通常相乘，连续循环相加后保留最高阶。\n\n## 三步分析法\n1. 明确输入规模，例如数组长度、顶点数 V 和边数 E。\n2. 找出最频繁的基本操作，写出执行次数 T(n)。\n3. 去掉常数与低阶项，用大 O 表示上界。\n\n## 常见模型\n- 每轮规模减半：O(log n)，例如二分查找。\n- 两层都遍历 n：O(n²)，例如朴素两两比较。\n- 分成两半并在线性时间合并：O(n log n)，例如归并排序。\n- 图的邻接表遍历：O(V+E)。\n\n考试还会区分最好、平均和最坏情况。快速排序平均 O(n log n)，枢轴持续取到极值时最坏 O(n²)。空间复杂度要计算递归栈、辅助数组和动态结点，不能只看局部变量。', tn.title='01 复杂度分析：从循环到递归'
WHERE tc.name='数据结构考研核心：复杂度与线性结构' AND tn.sort_order=1;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 顺序表与链表\n\n顺序表使用连续内存，按下标访问为 O(1)，缓存局部性好；但中间插入和删除需要移动元素，通常为 O(n)。链表通过指针连接结点，访问第 i 个结点为 O(n)，已知前驱后插入或删除可为 O(1)。\n\n## 考试辨析\n- 题目说在第 i 个位置插入时，通常仍需先定位，因此总体 O(n)。\n- 单链表删除给定结点若不知道前驱，不能直接声称 O(1)。\n- 尾插是否 O(1) 取决于是否维护尾指针。\n- 双链表删除结点需要同时修改前驱的 next 和后继的 prev。\n\n## 存储代价\n顺序表容量不足时需要扩容并复制，链表每个结点还要保存指针。选择结构时应综合访问模式、插删频率、内存开销和缓存友好性。', tn.title='02 顺序表与链表：复杂度和存储代价'
WHERE tc.name='数据结构考研核心：复杂度与线性结构' AND tn.sort_order=2;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 栈、队列与循环队列\n\n栈遵循后进先出，核心操作 push、pop、top；队列遵循先进先出，核心操作 offer、poll、front。顺序栈只需维护栈顶指针，链栈通常把表头作为栈顶。\n\n## 循环队列\n设容量为 capacity，队头 front，队尾 rear。队列长度可表示为 `(rear-front+capacity)%capacity`。若牺牲一个单元区分空和满：\n- 队空：front==rear\n- 队满：(rear+1)%capacity==front\n\n也可额外维护 size 或标志位，此时所有空间都能使用。考试必须先看题目采用哪种约定。\n\n## 典型应用\n栈用于括号匹配、递归、表达式求值；队列用于层序遍历、BFS 和任务调度。两个栈可以模拟队列，两个队列也能模拟栈，但操作复杂度不同。', tn.title='03 栈与队列：模型、公式与应用'
WHERE tc.name='数据结构考研核心：复杂度与线性结构' AND tn.sort_order=3;
SET @text_ds_pg_linear := (SELECT id FROM text_course WHERE name='数据结构考研核心：复杂度与线性结构' ORDER BY id LIMIT 1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_linear,'04 串与 KMP 模式匹配','# 串与模式匹配\n\n朴素匹配在失配后把主串起点右移一位，最坏复杂度 O(nm)。KMP 的核心是失配时不回退主串指针，而是利用模式串已经匹配部分的前后缀关系移动模式串。\n\n## next 数组理解\n对模式串每个位置，记录失配后应跳到的最长相等真前缀与真后缀长度。构造 next 仍是线性过程，因此 KMP 总复杂度为 O(n+m)。\n\n答题时要先确认教材对 next 下标和初值的定义。不同版本可能使用 next[0]=-1 或前缀函数写法，数值不同但跳转思想一致。手算时逐位写出已匹配前缀、可用后缀和跳转位置，避免只背数组。',4
WHERE @text_ds_pg_linear IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_linear AND sort_order=4);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_linear,'05 递归、递推与调用栈','# 递归分析\n\n递归算法必须包含终止条件、规模缩小和结果组合。每次调用会保存参数、局部变量和返回地址，因此递归深度决定额外空间。\n\n## 常见递推式\n- T(n)=T(n-1)+O(1)：O(n)\n- T(n)=T(n/2)+O(1)：O(log n)\n- T(n)=2T(n/2)+O(n)：O(n log n)\n\n树的前序遍历虽然代码短，但空间不是 O(1)，最坏递归深度等于树高。斐波那契朴素递归会重复计算，时间呈指数增长；可用记忆化或迭代降为 O(n)。\n\n写递归代码题时先定义函数含义，再证明较小规模调用正确，最后说明当前层如何组合结果。',5
WHERE @text_ds_pg_linear IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_linear AND sort_order=5);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_linear,'06 线性结构综合自测','# 章节自测\n\n1. 为什么顺序表随机访问是 O(1)，链表不是？\n2. 已知单链表某结点地址时，删除它一定是 O(1) 吗？说明限制。\n3. 容量为 8、牺牲一个单元的循环队列最多保存几个元素？\n4. 两个栈模拟队列时，如何保证每个元素的均摊复杂度为 O(1)？\n5. KMP 为什么不需要回退主串指针？\n\n## 建议答案检查点\n回答不能只有结论，要包含存储结构、必要前提和复杂度来源。代码题完成后用空输入、单元素、满容量、首尾位置和重复元素验证边界。能解释这些边界，才算真正掌握线性结构。',6
WHERE @text_ds_pg_linear IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_linear AND sort_order=6);

UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 二叉树性质与遍历\n\n二叉树每个结点最多有两个孩子。前序为根左右，中序为左根右，后序为左右根；层序遍历按层访问，需要队列。前序加中序或后序加中序，在结点值互异时可唯一还原一棵树。\n\n## 高频性质\n- 第 i 层最多 2^(i-1) 个结点。\n- 高度为 h 的二叉树最多 2^h-1 个结点。\n- 非空二叉树中 n0=n2+1。\n- 有 n 个结点的完全二叉树高度为 floor(log2 n)+1。\n\n遍历的时间都是 O(n)，额外空间与树高有关。平衡树为 O(log n)，退化成链时为 O(n)。做题前要确认树高从 0 还是 1 开始定义。', tn.title='01 二叉树性质与四种遍历'
WHERE tc.name='数据结构考研核心：树与图的原理' AND tn.sort_order=1;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 图的存储与遍历\n\n邻接矩阵占 O(V²) 空间，判断两点是否相邻为 O(1)，适合稠密图。邻接表占 O(V+E) 空间，适合稀疏图，但查找特定边需要扫描邻接链。无向图每条边会在邻接表出现两次。\n\nDFS 使用递归或显式栈，适合连通性、路径搜索和拓扑相关问题；BFS 使用队列，可在无权图中求最短边数路径。若图不连通，必须从每个未访问顶点再次启动遍历才能覆盖全部顶点。\n\n遍历邻接矩阵的复杂度为 O(V²)，遍历邻接表为 O(V+E)。复杂度必须和存储结构一起回答。', tn.title='02 图的存储、DFS 与 BFS'
WHERE tc.name='数据结构考研核心：树与图的原理' AND tn.sort_order=2;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 最小生成树与最短路径\n\nPrim 从一个顶点出发，每轮选择连接树内外的最小边，朴素实现更适合稠密图。Kruskal 按边权从小到大选择不成环的边，通常用并查集判环，更适合稀疏图。两者都要求无向连通带权图。\n\nDijkstra 解决非负权单源最短路，已确定最短距离的顶点不会再变；存在负权边时结论失效。Floyd 用动态规划求任意两点最短路，核心状态是只允许前 k 个顶点作为中间点。\n\n最小生成树最小化连接全部顶点的总边权，最短路径最小化特定起终点路径，二者目标不同。', tn.title='03 最小生成树与最短路径辨析'
WHERE tc.name='数据结构考研核心：树与图的原理' AND tn.sort_order=3;
SET @text_ds_pg_graph := (SELECT id FROM text_course WHERE name='数据结构考研核心：树与图的原理' ORDER BY id LIMIT 1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_graph,'04 线索二叉树与树森林转换','# 线索二叉树\n\n普通二叉链表有大量空指针。线索化利用这些空指针保存遍历序列中的前驱或后继，并用标志位区分孩子指针和线索。中序线索树可在不使用栈和递归的情况下按中序访问。\n\n## 树、森林与二叉树\n使用孩子兄弟表示法：左指针指向第一个孩子，右指针指向下一个兄弟。树转换成二叉树时兄弟结点沿右链连接；森林转换时各棵树的根也视为兄弟。\n\n考试常结合遍历关系出题。树的先根遍历对应转换后二叉树的前序，树的后根遍历对应转换后二叉树的中序。',4
WHERE @text_ds_pg_graph IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_graph AND sort_order=4);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_graph,'05 拓扑排序与关键路径','# 有向无环图\n\n拓扑排序每轮选择入度为 0 的顶点输出，并删除它发出的边。若最后输出顶点数少于 V，图中存在有向环。一个 DAG 可能有多个合法拓扑序。\n\n关键路径用于活动在边上的网络。先按拓扑序计算事件最早发生时间，再按逆拓扑序计算最晚发生时间；最早开始等于最晚开始的活动是关键活动。关键路径长度决定项目最短工期。\n\n缩短非关键活动不一定缩短总工期；缩短关键活动后还要重新计算，因为可能产生新的关键路径。',5
WHERE @text_ds_pg_graph IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_graph AND sort_order=5);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_graph,'06 树图综合题解题模板','# 树图综合题模板\n\n## 树题\n先判断是否满足二叉搜索树、完全二叉树或平衡树条件，再选择遍历。涉及祖先和路径时明确递归函数返回值，涉及层次和最短步数时优先考虑队列。\n\n## 图题\n先写清有向或无向、带权或无权、是否连通，再选择邻接矩阵或邻接表。路径存在性用 DFS/BFS，无权最短路用 BFS，非负权最短路用 Dijkstra，依赖顺序用拓扑排序。\n\n## 检查\n所有遍历都要维护 visited，非连通图要有外层循环；复杂度要同时写 V、E 与存储结构。',6
WHERE @text_ds_pg_graph IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_graph AND sort_order=6);

UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 查找结构的选择\n\n顺序查找无需有序，平均比较次数与 n 同阶。二分查找要求随机访问且有序，每次排除一半区间，复杂度 O(log n)，因此不适合链表。二叉搜索树的性能取决于树高，输入有序时可能退化。\n\nAVL 树用平衡因子和旋转维持高度，红黑树放宽平衡换取较少调整。B 树和 B+ 树让一个结点保存多个关键字，降低磁盘访问层数；B+ 树数据集中在叶结点，叶结点有序链接，适合范围查询。\n\n哈希表平均接近 O(1)，但不保持顺序，冲突严重或遭遇恶意输入时会退化。', tn.title='01 查找结构：二分、树与哈希'
WHERE tc.name='数据结构考研核心：查找排序与 408 易错点' AND tn.sort_order=1;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 排序算法性质\n\n稳定排序保持相等关键字原有相对顺序。通常稳定的有冒泡、插入、归并、基数排序；通常不稳定的有选择、快速、希尔和堆排序。\n\n## 复杂度对比\n- 插入排序：最好 O(n)，最坏 O(n²)，适合基本有序和小规模数据。\n- 归并排序：始终 O(n log n)，需要 O(n) 辅助空间。\n- 快速排序：平均 O(n log n)，最坏 O(n²)，递归栈平均 O(log n)。\n- 堆排序：最坏 O(n log n)，额外空间 O(1)。\n\n题目还会考比较次数是否与初始序列有关，以及一趟排序后哪些元素能确定最终位置。', tn.title='02 八类内部排序的性质与复杂度'
WHERE tc.name='数据结构考研核心：查找排序与 408 易错点' AND tn.sort_order=2;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 408 选择题检查清单\n\n做题时先圈出限制条件：平均还是最坏，有序还是无序，有向还是无向，边权能否为负，排序是否要求稳定或原地。\n\n## 高频陷阱\n1. 已知链表结点不等于已知其前驱。\n2. 二分查找只能用于具有随机访问能力的有序结构。\n3. Dijkstra 不能处理负权边。\n4. 拓扑排序只适用于 DAG。\n5. 快排最坏空间可达 O(n)。\n6. 哈希查找长度还与装填因子和冲突策略有关。\n\n计算树高、层数和数组下标前，先确认题目从 0 还是 1 开始编号。', tn.title='03 408 高频易错条件清单'
WHERE tc.name='数据结构考研核心：查找排序与 408 易错点' AND tn.sort_order=3;
SET @text_ds_pg_sort := (SELECT id FROM text_course WHERE name='数据结构考研核心：查找排序与 408 易错点' ORDER BY id LIMIT 1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_sort,'04 哈希表与冲突处理','# 哈希查找\n\n哈希函数把关键字映射到表下标。理想函数计算快、分布均匀，并充分利用关键字信息。装填因子 α=元素数/表长，α 越大通常冲突越多。\n\n## 冲突处理\n- 开放定址：发生冲突后按线性探测、平方探测或双散列寻找空位。删除不能直接清空，否则会截断探测链，需要删除标记。\n- 链地址：同一桶内元素组成链表或其他结构，删除简单，额外需要指针空间。\n\n平均查找长度要分别计算成功与失败情况。开放定址失败查找直到遇到真正空位，链地址失败查找与桶内链长有关。',4
WHERE @text_ds_pg_sort IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_sort AND sort_order=4);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_sort,'05 外部排序与败者树','# 外部排序\n\n当数据无法一次装入内存时，先把数据分块读入内存排序，形成多个初始归并段，再执行多路归并。总时间主要由磁盘读写决定，因此要减少归并趟数。\n\n增加归并路数 k 可以减少趟数，但普通选择最小元素的比较成本会上升。败者树能在 O(log k) 内选出下一最小记录，使多路归并更高效。置换选择可生成长度大于内存工作区的初始归并段。\n\n最佳归并树与哈夫曼树思想相近，通过让较短归并段先合并减少总读写量。',5
WHERE @text_ds_pg_sort IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_sort AND sort_order=5);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_sort,'06 查找排序综合练习','# 综合练习\n\n1. 为什么链表不适合二分查找？\n2. 给出一组关键字，手工建立哈希表并计算成功、失败平均查找长度。\n3. 判断快速、归并、堆排序的稳定性、空间和最坏时间。\n4. 给定一趟排序后的序列，判断可能使用了哪种算法。\n5. B+ 树为什么适合数据库范围查询？\n\n答题时不要只列算法名称。至少写出前提、核心操作、复杂度和一条限制。手算排序过程每一趟都标记已确定最终位置的元素，可以降低漏项。',6
WHERE @text_ds_pg_sort IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_sort AND sort_order=6);

-- Enrich the three existing employment text courses to six substantial chapters.

UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 从 List 接口开始设计\n\n工程实现先定义契约，再选择存储结构。一个最小 List 接口应包含 size、isEmpty、get、set、add、remove 和 indexOf，并明确空值、越界和重复元素的行为。调用方只依赖接口，动态数组和链表才能自由替换。\n\n## 动态数组关键点\n容量不足时创建更大数组并复制。常见扩容倍数为 1.5 或 2，单次复制 O(n)，但连续追加的均摊复杂度仍为 O(1)。删除后是否缩容要权衡空间和频繁复制。\n\n## 测试清单\n覆盖空集合、单元素、首尾和中间插入、非法下标、扩容边界、删除后再次插入。测试不仅验证返回值，还要验证 size 与内部顺序。', tn.title='01 List 接口与动态数组工程实现'
WHERE tc.name='数据结构就业实战：Java 集合与链表实现' AND tn.sort_order=1;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 链表的指针安全\n\n链表错误通常不是算法不会，而是修改顺序不安全。插入时先保存后继，再让新结点连接前后；删除时先接通目标结点的前驱和后继，再清理目标引用。双向链表要同时维护 prev 与 next。\n\n## 哨兵结点\n头尾哨兵不保存业务数据，可以统一空链表、首结点和尾结点操作，减少 null 分支。`head.next` 指向首元素，`tail.prev` 指向尾元素，空表时二者直接相连。\n\n## 调试方法\n每次修改后检查正向和反向遍历、size、首尾引用。用图画出操作前后的结点关系，再写赋值语句，避免旧引用被提前覆盖。', tn.title='02 哨兵链表与指针安全'
WHERE tc.name='数据结构就业实战：Java 集合与链表实现' AND tn.sort_order=2;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 反转链表与环检测\n\n迭代反转维护 prev、current、next 三个引用。每轮先保存 next，再让 current.next 指向 prev，最后整体前移。循环结束后 prev 是新头。递归版本在回溯阶段反转指针，但会占用调用栈。\n\nFloyd 快慢指针可检测环：slow 每次一步，fast 每次两步，相遇说明有环。相遇后让一个指针回到头部，两者同步前进，再次相遇的位置就是环入口。\n\n## 必测输入\n空链表、单结点、两结点、奇偶长度、首结点成环、中间结点成环和无环。面试时同时说明时间 O(n)、空间 O(1)。', tn.title='03 反转链表、环检测与边界测试'
WHERE tc.name='数据结构就业实战：Java 集合与链表实现' AND tn.sort_order=3;
SET @text_ds_job_list := (SELECT id FROM text_course WHERE name='数据结构就业实战：Java 集合与链表实现' ORDER BY id LIMIT 1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_list,'04 Java ArrayList 源码观察','# ArrayList 源码观察\n\nArrayList 底层是 Object 数组。add 前调用容量检查，扩容后使用数组复制；get 先检查下标再直接访问。remove(index) 删除后把后续元素向前移动，并把末尾引用置空，帮助垃圾回收。\n\n## 工程启示\n- 初始化时能估计规模就传入容量，减少扩容。\n- 频繁在头部插删不适合 ArrayList。\n- 遍历中结构性修改会触发 fail-fast，除非使用迭代器自己的 remove。\n- subList 是原列表视图，不是独立复制。\n\n阅读源码时重点看不变量：size 不超过数组长度、有效元素位于 0 到 size-1。',4
WHERE @text_ds_job_list IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_list AND sort_order=4);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_list,'05 LRU 链表节点操作练习','# LRU 中的链表\n\nLRU 需要在 O(1) 内把任意结点移动到头部并淘汰尾部结点，因此使用哈希表加双向链表。链表维护访问顺序，哈希表从 key 定位结点。\n\n建议把链表操作拆成三个私有方法：removeNode、addAfterHead、moveToHead。get 命中后 moveToHead；put 已存在时更新并移动；新增后超容量则删除 tail.prev，并同步删除 map 映射。\n\n任何分支都必须保证 map 大小和链表业务结点数一致。用容量 1、重复 put、访问后淘汰和不存在 key 验证。',5
WHERE @text_ds_job_list IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_list AND sort_order=5);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_list,'06 集合实现项目验收清单','# 项目验收\n\n完成动态数组和链表后，不要只看示例输出。\n\n## 正确性\n- 所有越界都抛出一致异常。\n- 首尾与空结构操作不出现空指针。\n- size 在成功修改后恰好变化一次。\n- 删除对象后不残留无用引用。\n\n## 质量\n- 公共接口不暴露 Node。\n- 重复逻辑提取为私有方法。\n- 使用参数化测试覆盖多组输入。\n- 在 README 记录复杂度表和设计取舍。\n\n最后用一万次随机 add、remove、get 与 JDK List 对拍，能发现许多手工用例遗漏的指针问题。',6
WHERE @text_ds_job_list IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_list AND sort_order=6);

UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 栈在解析器中的应用\n\n中缀表达式转后缀表达式时，操作数直接输出；运算符根据优先级和结合性与栈顶比较，必要时弹出；左括号入栈，遇到右括号弹出直到左括号。计算后缀表达式只需一个操作数栈。\n\n## 工程边界\n输入可能包含空格、多位数、小数、负号和非法字符。词法切分应和求值分离，错误信息要指出位置。除法还需处理除零。\n\n同一栈结构还可用于撤销、浏览历史、括号校验和深度优先搜索。接口应限制外部只能操作栈顶，避免业务代码破坏后进先出不变量。', tn.title='01 栈实现表达式解析器'
WHERE tc.name='数据结构就业实战：栈队列、缓存与任务调度' AND tn.sort_order=1;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# LRU 缓存组合设计\n\n哈希表负责 O(1) 定位结点，双向链表负责 O(1) 调整访问顺序。头部表示最近使用，尾部表示最久未使用。get 命中后移动到头部；put 更新已有值或插入新结点；超过容量时淘汰尾部。\n\n## 常见错误\n- 只从链表删除，没有从 map 删除。\n- 更新已有 key 后重复增加 size。\n- 容量为 0 或 1 时哨兵连接出错。\n- 在并发环境中把复合操作误认为线程安全。\n\n生产实现还要考虑过期时间、容量度量、统计命中率和并发策略。面试手写版本先保证单线程不变量，再讨论扩展。', tn.title='02 LRU 缓存：哈希表加双向链表'
WHERE tc.name='数据结构就业实战：栈队列、缓存与任务调度' AND tn.sort_order=2;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 优先队列驱动任务调度\n\n优先队列通常由二叉堆实现。数组下标从 0 开始时，父结点为 `(i-1)/2`，左右孩子为 `2i+1` 和 `2i+2`。插入后上浮，删除堆顶后把末尾元素移到根并下潜。\n\n任务对象应包含优先级、创建时间和唯一序号。比较器先比较优先级，再比较时间或序号，避免同优先级顺序不确定。修改已入堆元素的优先级会破坏堆，应删除重插或提供专门调整操作。\n\n调度系统还需处理饥饿，可随等待时间提升优先级。', tn.title='03 二叉堆与优先任务调度'
WHERE tc.name='数据结构就业实战：栈队列、缓存与任务调度' AND tn.sort_order=3;
SET @text_ds_job_queue := (SELECT id FROM text_course WHERE name='数据结构就业实战：栈队列、缓存与任务调度' ORDER BY id LIMIT 1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_queue,'04 环形队列与消息缓冲区','# 环形队列\n\n固定容量缓冲区用数组配合 head、tail 实现，移动时取模，避免普通数组队列出队后整体搬移。区分空和满可牺牲一个单元、维护 size，或使用单调递增序号。\n\n在生产者消费者场景中，队列还要定义满时策略：阻塞、拒绝、覆盖最旧数据或扩容。策略属于业务契约，不能隐藏在实现细节里。\n\n并发版本需要锁或原子变量保证可见性和复合操作一致性。先完成单线程不变量测试，再增加并发测试，避免把数据结构问题和线程问题混在一起调试。',4
WHERE @text_ds_job_queue IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_queue AND sort_order=4);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_queue,'05 单调栈与单调队列','# 单调结构\n\n单调栈在入栈时弹出破坏单调性的元素，可在线性时间解决下一个更大元素、柱状图最大矩形和温度等待天数。每个元素最多入栈出栈一次，所以总复杂度 O(n)。\n\n单调队列在队尾维护候选值的单调性，同时从队头删除滑出窗口的元素，可求滑动窗口最大值。队列中通常保存下标，既能判断过期，也能访问原值。\n\n关键不是背模板，而是解释被弹出的元素为何永远不可能成为后续答案。',5
WHERE @text_ds_job_queue IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_queue AND sort_order=5);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_queue,'06 调度与缓存项目测试方案','# 测试方案\n\n## 队列和堆\n测试空结构、容量边界、重复优先级、升降序输入和大量随机数据。每次操作后验证堆顶，并把全部元素弹出检查顺序。\n\n## LRU\n测试重复访问改变淘汰顺序、更新已有键、容量 1、未命中和连续淘汰。维护一个简单但慢的参考实现进行随机对拍。\n\n## 可观测性\n记录命中率、淘汰次数、队列长度和任务等待时间。数据结构正确只是第一步，这些指标才能判断配置是否适合实际流量。',6
WHERE @text_ds_job_queue IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_queue AND sort_order=6);

UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# Trie 搜索建议服务\n\nTrie 每个结点保存子字符映射和单词结束标记。插入、精确查询和前缀定位的复杂度与字符串长度 L 有关，不直接依赖词库数量。定位前缀结点后用 DFS 收集有限数量候选。\n\n## 工程设计\n字符集较小时可用定长数组，字符集大时使用 Map 节省空间。热门前缀可以在结点缓存 Top K，查询更快但更新更复杂。还要限制最大词长、结点数和返回数量。\n\n删除单词时只有当结点不再属于其他单词前缀，才能向上清理。大小写、Unicode 归一化和敏感词过滤应在统一预处理层完成。', tn.title='01 Trie 搜索建议服务'
WHERE tc.name='数据结构就业实战：树、图、哈希与面试项目' AND tn.sort_order=1;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 图实现依赖分析\n\n把任务或模块作为顶点，依赖关系作为有向边。Kahn 拓扑排序先统计入度，把所有入度为 0 的顶点入队；每输出一个顶点，就降低其后继入度。最终处理数少于顶点总数说明存在环。\n\n## 工程输出\n仅返回失败不够，应进一步用 DFS 颜色标记找出一条环路径，帮助用户定位循环依赖。顶点和边要去重，缺失依赖需要明确报错。\n\n该模型可用于课程先修、构建系统、工作流和任务编排。若希望并行执行，可把每轮所有入度为 0 的任务作为同一批次。', tn.title='02 依赖图、拓扑排序与环检测'
WHERE tc.name='数据结构就业实战：树、图、哈希与面试项目' AND tn.sort_order=2;
UPDATE text_node tn JOIN text_course tc ON tc.id=tn.course_id
SET tn.content='# 哈希索引的工程边界\n\n哈希函数应计算快并让键均匀分布。链地址法删除简单，开放定址更依赖装填因子和探测策略。扩容时通常必须按新容量重新散列，而不是简单复制桶。\n\n## 容易忽略的问题\n- 可变对象作为 key 后字段改变，会导致无法再次定位。\n- equals 相等的对象必须具有相同 hash。\n- 恶意碰撞会把平均 O(1) 退化。\n- 并发修改需要同步或使用并发容器。\n\n面试回答不能只说查找 O(1)，应说明这是平均情况，并解释冲突、装填因子、扩容和最坏复杂度。', tn.title='03 哈希索引、扩容与冲突边界'
WHERE tc.name='数据结构就业实战：树、图、哈希与面试项目' AND tn.sort_order=3;
SET @text_ds_job_graph := (SELECT id FROM text_course WHERE name='数据结构就业实战：树、图、哈希与面试项目' ORDER BY id LIMIT 1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_graph,'04 二叉搜索树与范围查询','# 二叉搜索树项目\n\n对任一结点，左子树键更小，右子树键更大。get、put、remove 的复杂度与树高相关。删除有两个孩子的结点时，可用右子树最小结点或左子树最大结点替换。\n\n范围查询利用中序遍历有序性：若当前键大于下界才访问左子树，位于区间时收集，若小于上界才访问右子树，从而跳过无关分支。\n\n普通 BST 会因有序输入退化。项目中要么随机化输入，要么选择 AVL、红黑树或标准库有序映射。',4
WHERE @text_ds_job_graph IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_graph AND sort_order=4);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_graph,'05 并查集实现网络连通性','# 并查集\n\n并查集维护若干不相交集合，支持 find 查询代表元和 union 合并集合。按秩合并避免小树挂大树，路径压缩让查找过程中结点直接连接根，二者结合后的均摊复杂度接近常数。\n\n## 应用\n- 判断无向图加边是否成环。\n- Kruskal 最小生成树。\n- 账户合并和好友关系。\n- 动态连通性。\n\n实现时 parent 数组初始指向自己，可另维护 size 或 rank。测试重复合并、自连接和多层路径压缩。',5
WHERE @text_ds_job_graph IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_graph AND sort_order=5);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_graph,'06 小型项目交付要求','# 项目交付要求\n\n从 Trie、依赖图、哈希索引或并查集中选择一个完成可运行项目。\n\n## 必交内容\n1. 清晰的接口与输入输出示例。\n2. 至少覆盖正常、边界和错误路径的单元测试。\n3. README 中的数据结构选择理由与复杂度表。\n4. 一组规模测试，说明内存和耗时趋势。\n5. 对非法输入、资源上限和失败情况的处理。\n\n代码评审时重点检查不变量是否集中维护、公共接口是否泄露内部结点，以及测试能否捕捉一次错误指针更新。',6
WHERE @text_ds_job_graph IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_graph AND sort_order=6);

-- Two new long-form text courses complete both direction pools.

INSERT INTO text_course(name,cover_img,description)
SELECT '数据结构考研专题：408 代码题与证明方法','/course-covers/data-structure-theory.svg',
       '考研理论与真题专题。用统一模板训练线性表、树、图和排序代码题，并掌握正确性与复杂度证明。'
WHERE NOT EXISTS(SELECT 1 FROM text_course WHERE name='数据结构考研专题：408 代码题与证明方法');
SET @text_ds_pg_proof := (SELECT id FROM text_course WHERE name='数据结构考研专题：408 代码题与证明方法' ORDER BY id LIMIT 1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_proof,'01 代码题的五步书写框架','# 408 代码题框架\n\n第一步写清数据结构定义和函数含义；第二步列出输入、输出和失败条件；第三步给出核心算法；第四步说明边界；第五步分析时间与空间复杂度。\n\n## 伪代码要求\n变量命名表达含义，指针修改按安全顺序，循环前说明不变量。题目未要求完整可编译代码时，不必花篇幅写输入输出，但关键结点类型和返回值不能省略。\n\n## 得分点\n即使代码没有完全写完，也应写出思路、主要循环、终止条件和复杂度。空表、单结点、首尾结点与重复关键字是最常见边界。',1
WHERE @text_ds_pg_proof IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_proof AND sort_order=1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_proof,'02 线性表算法题模式','# 线性表代码题\n\n顺序表常见模式是双指针覆盖、原地划分、归并和删除重复元素。链表常见模式是哨兵结点、快慢指针、反转、分割和合并。\n\n## 例：删除有序顺序表重复项\n用 read 扫描，write 指向下一个保留位置。当当前值与最后保留值不同，就写入并前移 write。每个元素只访问一次，时间 O(n)，空间 O(1)。\n\n## 链表检查\n修改 next 前先保存后继；删除区间要保留区间前驱；合并链表可用 dummy 统一头结点。写完后画三结点小例子逐语句验证。',2
WHERE @text_ds_pg_proof IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_proof AND sort_order=2);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_proof,'03 树与递归代码题模式','# 树题递归定义\n\n树题先定义函数返回什么。求高度返回子树高度；判断平衡可返回高度并用特殊值表示失衡；最近公共祖先返回当前子树中找到的目标或祖先。清晰的返回含义比背模板重要。\n\n## 正确性说明\n基本情况为空树或叶子结点。假设左右子树递归结果正确，当前结点按题意组合，即可用结构归纳说明整个算法正确。\n\n## 复杂度\n若每个结点访问一次，时间 O(n)；递归空间等于树高 h。平衡树为 O(log n)，最坏退化树为 O(n)。',3
WHERE @text_ds_pg_proof IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_proof AND sort_order=3);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_proof,'04 图算法题的状态设计','# 图算法代码题\n\n图题必须明确存储方式和 visited。BFS 的队列元素可附带距离、父结点或状态；DFS 的递归参数可附带路径信息。非连通图需要外层遍历所有顶点。\n\n## 常见状态\n- 无权最短路：dist 与 parent。\n- 环检测：未访问、访问中、已完成三色状态。\n- 拓扑排序：入度数组和零入度队列。\n- 最小生成树：边集合和并查集。\n\n复杂度按邻接表写 O(V+E)，按邻接矩阵写 O(V²)。若状态包含额外维度，要把状态总数计入复杂度。',4
WHERE @text_ds_pg_proof IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_proof AND sort_order=4);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_proof,'05 正确性证明与循环不变量','# 正确性证明\n\n循环不变量是在每轮开始或结束都成立的命题。证明分三步：初始化成立；若本轮前成立，执行循环体后仍成立；循环终止时，不变量与终止条件共同推出结果正确。\n\n## 例：插入排序\n不变量为前 i 个元素已经有序且包含原前 i 个元素。初始一个元素显然有序；插入第 i 个元素后仍有序且元素不丢失；当 i=n 时整个数组有序。\n\n递归算法可用数学归纳或结构归纳。除了结果正确，还应说明算法终止，因为每次调用都让问题规模严格缩小。',5
WHERE @text_ds_pg_proof IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_proof AND sort_order=5);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_pg_proof,'06 代码题四周训练安排','# 四周训练\n\n## 第一周\n顺序表、链表、栈队列，每天两题，重点写边界和复杂度。\n\n## 第二周\n树遍历、树高、路径与二叉搜索树，每题先写递归函数含义。\n\n## 第三周\n图遍历、拓扑、最短路和并查集，明确状态与存储结构。\n\n## 第四周\n排序、查找和综合真题，限时完成并按评分点复盘。\n\n错题不要只抄答案，记录错误属于状态设计、边界、指针顺序、复杂度还是证明。隔两天不看答案重写，才能确认真正掌握。',6
WHERE @text_ds_pg_proof IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_pg_proof AND sort_order=6);

INSERT INTO text_course(name,cover_img,description)
SELECT '数据结构就业项目课：从容器到搜索服务','/course-covers/data-structure-practice.svg',
       '就业项目实战专题。完成动态容器、LRU、调度器、依赖图和搜索建议五个可测试模块，形成作品集项目。'
WHERE NOT EXISTS(SELECT 1 FROM text_course WHERE name='数据结构就业项目课：从容器到搜索服务');
SET @text_ds_job_lab := (SELECT id FROM text_course WHERE name='数据结构就业项目课：从容器到搜索服务' ORDER BY id LIMIT 1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_lab,'01 项目骨架与统一测试基线','# 项目准备\n\n建立 core、test 和 benchmark 三个模块。core 只放数据结构与接口，test 放单元和随机对拍，benchmark 记录不同规模耗时。每个模块都提供 README，说明需求、接口、复杂度和已知限制。\n\n## 统一约定\n- 非法参数抛出明确异常。\n- 不向调用方暴露内部 Node。\n- 公共操作写复杂度注释。\n- 所有结构提供 size、isEmpty 和 clear。\n- 使用固定随机种子让失败可复现。\n\n先写测试再补实现，能够迫使接口边界在编码前确定。',1
WHERE @text_ds_job_lab IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_lab AND sort_order=1);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_lab,'02 项目一：动态数组与双向链表','# 容器项目\n\n实现 MyArrayList 与 MyLinkedList，共同遵循自定义 List 接口。动态数组完成扩缩容、按下标增删和迭代器；双向链表使用头尾哨兵，完成双向遍历。\n\n## 验收\n用 JDK ArrayList 作为参考，对随机操作序列进行对拍。每轮随机选择 add、remove、set、get，并比较返回值、size 和完整内容。至少执行一万次操作。\n\n最后写一份对比报告：头部插入、尾部追加、随机访问分别在哪种结构更快，实际结果是否符合复杂度分析。',2
WHERE @text_ds_job_lab IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_lab AND sort_order=2);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_lab,'03 项目二：可观测 LRU 缓存','# LRU 项目\n\n实现 get、put、remove 和 capacity，内部使用 HashMap 加双向链表。增加 hitCount、missCount、evictionCount，提供命中率统计。可选扩展为每条记录增加过期时间。\n\n## 验收\n测试访问改变顺序、更新不增加容量、淘汰同步删除映射、容量 1 和连续未命中。随机测试可用 LinkedHashMap 的 accessOrder 模式作为参考。\n\nREADME 解释为什么单独使用数组、链表或哈希表无法同时满足 O(1) 查询和 O(1) 淘汰。',3
WHERE @text_ds_job_lab IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_lab AND sort_order=3);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_lab,'04 项目三：优先任务调度器','# 调度器项目\n\n使用二叉堆实现 PriorityScheduler。任务包含 id、priority、createdAt 和 payload。支持 submit、peek、poll、cancel，并保证同优先级按提交顺序处理。\n\n## 实现选择\ncancel 若要求 O(log n)，需要额外 Map 记录任务在堆数组中的下标；交换堆元素时同步更新下标。若不加 Map，取消只能线性查找。把这个取舍写入设计说明。\n\n测试重复优先级、取消堆顶和中间任务、大量随机提交与弹出，并持续验证堆序。',4
WHERE @text_ds_job_lab IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_lab AND sort_order=4);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_lab,'05 项目四：依赖图执行计划','# 依赖图项目\n\n输入任务及依赖边，输出合法执行顺序和可并行批次。用邻接表保存后继，用入度数组执行 Kahn 算法。存在环时返回一条具体环路径，而不是只返回失败。\n\n## 边界\n拒绝未知任务、自依赖和重复边；空图返回空计划；多个零入度任务按稳定规则排序，保证结果可复现。\n\n扩展接口可计算受某任务影响的全部后继，或在新增依赖后增量检查是否成环。',5
WHERE @text_ds_job_lab IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_lab AND sort_order=5);
INSERT INTO text_node(course_id,title,content,sort_order)
SELECT @text_ds_job_lab,'06 项目五：Trie 搜索建议与作品集交付','# 搜索建议项目\n\n实现 addWord、removeWord、contains 和 suggest(prefix,k)。Trie 结点使用 Map 保存孩子，并在前缀结点下按稳定顺序收集最多 k 个候选。扩展版可记录词频并缓存 Top K。\n\n## 作品集交付\n把五个模块放在同一仓库，提供架构图、运行命令、复杂度表、测试报告和基准结果。为每个模块准备一个实际使用示例，不只展示数据结构本身。\n\n面试讲解顺序：需求约束、结构选择、不变量、关键边界、复杂度、测试方式和可以继续优化的地方。',6
WHERE @text_ds_job_lab IS NOT NULL AND NOT EXISTS(SELECT 1 FROM text_node WHERE course_id=@text_ds_job_lab AND sort_order=6);

-- Every chapter includes an actionable learning/checkpoint section instead of ending as a short note.
UPDATE text_node tn
JOIN text_course tc ON tc.id=tn.course_id
SET tn.content=CONCAT(tn.content,
    '\n\n## 本节学习任务\n\n1. 合上正文，用自己的话解释“', tn.title, '”解决什么问题，以及使用它的前提。\n2. 为核心过程手算一个至少包含 5 个元素的例子，记录每一步状态变化。\n3. 分别写出最好、平均或最坏情况下的时间复杂度，并说明额外空间来自哪里。\n4. 找出一个容易写错的边界条件，给出能触发错误的最小输入。\n\n## 达标标准\n\n能够在不看答案时复述关键不变量，独立完成一道同类题，并解释为什么算法会终止且结果正确。')
WHERE tc.name LIKE '数据结构%考研%' AND tn.content NOT LIKE '%## 本节学习任务%';

UPDATE text_node tn
JOIN text_course tc ON tc.id=tn.course_id
SET tn.content=CONCAT(tn.content,
    '\n\n## 动手练习与验收\n\n1. 把本节结构实现成独立类或模块，公共接口不暴露内部结点。\n2. 至少编写空输入、单元素、容量边界、重复数据和非法参数五类测试。\n3. 使用一组随机操作与标准库或简单参考实现对拍，失败时输出可复现的随机种子。\n4. 在 README 中记录接口、不变量、复杂度、设计取舍和一项可继续优化的方向。\n\n## 完成标准\n\n代码可运行、测试可重复、边界有明确行为，并能在三分钟内说明为什么选择这种数据结构。')
WHERE (tc.name LIKE '数据结构%就业%' OR tc.name LIKE '数据结构%项目课%')
  AND tn.content NOT LIKE '%## 动手练习与验收%';
