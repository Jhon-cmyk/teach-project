-- Detailed text tutorial seed data for the student tutorial library.
-- Safe to run more than once: courses are matched by name, nodes are matched by course + title.

START TRANSACTION;

CREATE TABLE IF NOT EXISTS text_course (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    cover_img VARCHAR(500) DEFAULT '',
    description VARCHAR(800) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_text_course_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='text and image tutorial courses';

CREATE TABLE IF NOT EXISTS text_node (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content LONGTEXT,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_text_node_course_sort (course_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='text tutorial chapter nodes';

DROP TEMPORARY TABLE IF EXISTS tmp_text_tutorial_course;
CREATE TEMPORARY TABLE tmp_text_tutorial_course (
    slug VARCHAR(80) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    cover_img VARCHAR(500) DEFAULT '',
    description VARCHAR(800) DEFAULT ''
) ENGINE=Memory DEFAULT CHARSET=utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_text_tutorial_node;
CREATE TEMPORARY TABLE tmp_text_tutorial_node (
    slug VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content LONGTEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_text_tutorial_course (slug, name, cover_img, description) VALUES
('python-foundation', 'Python 基础语法图文教程', '/icons/types/python.png', '从变量、分支、循环到函数，把 Python 初学阶段最容易混淆的语法点串成一条清晰路线。'),
('java-oop', 'Java 面向对象核心笔记', '/icons/types/java.png', '用类、对象、封装、继承、多态和接口搭建 Java 面向对象知识框架，适合作为课后复盘材料。'),
('data-structure', '数据结构知识卡片', '/icons/types/数据.png', '围绕数组、链表、栈、队列、树和哈希表，解释它们适合解决什么问题以及如何选型。'),
('algorithm-complexity', '算法复杂度与常见策略', '/icons/types/算法.png', '把时间复杂度、空间复杂度、双指针、递归、动态规划等内容拆成可读的图文笔记。'),
('sql-practical', '数据库 SQL 查询入门', '/icons/types/数据.png', '从 SELECT、WHERE、JOIN 到 GROUP BY，帮助学生建立写 SQL 的基本思路。'),
('frontend-basic', '前端 HTML CSS JavaScript 入门', '/icons/types/前端.png', '用页面结构、样式布局和交互逻辑三个视角理解前端基础。'),
('ai-basic', '人工智能基础概念手册', '/icons/types/人工智能.png', '用通俗语言解释数据集、模型、训练、推理、过拟合和评估指标，适合 AI 入门阅读。'),
('springboot-rest', 'Spring Boot REST 接口开发笔记', '/icons/types/后端.png', '围绕 Controller、Service、Mapper、DTO 和统一响应，整理后端接口开发的常见流程。'),
('git-workflow', 'Git 版本控制实用教程', '/icons/types/编程.png', '从提交、分支、合并到冲突处理，帮助学生形成稳定的协作开发习惯。'),
('study-method', '高效学习与复盘方法', '/icons/types/阅读.png', '面向编程学习场景，提供预习、练习、错题整理、阶段复盘和项目输出的方法。');

INSERT INTO tmp_text_tutorial_node (slug, sort_order, title, content) VALUES
('python-foundation', 1, '变量、类型与输入输出',
'<h2>学习目标</h2><p>这一章先解决 Python 程序最基本的三个动作：保存数据、识别数据类型、把结果展示出来。初学者不需要一开始记住所有语法，先能读懂一段小程序，知道每一行在做什么。</p><blockquote>核心记忆：变量像贴在数据上的标签，类型决定这个数据能参与哪些运算。</blockquote><h3>变量不是盒子，而是名字</h3><p>在 Python 中，<code>name = "小明"</code> 表示让变量 <code>name</code> 指向字符串。之后你写 <code>print(name)</code>，程序会顺着这个名字找到对应的数据。</p><ul><li><code>int</code>：整数，例如年龄、次数、排名。</li><li><code>float</code>：小数，例如成绩、价格、平均值。</li><li><code>str</code>：文本，例如姓名、提示语、文件路径。</li><li><code>bool</code>：真假值，例如是否登录、是否通过。</li></ul><pre><code class="language-python">name = "小明"
score = 86
passed = score >= 60
print(name, score, passed)</code></pre><h3>输入得到的永远先是字符串</h3><p><code>input()</code> 读取到的内容默认是字符串。如果要参与数学计算，需要转换为 <code>int</code> 或 <code>float</code>。很多运行错误都来自忘记转换类型。</p><pre><code class="language-python">age_text = input("请输入年龄：")
age = int(age_text)
print(age + 1)</code></pre><p>练习时可以把每个变量旁边写上它的含义和类型，等程序变长后，这个习惯能显著减少混乱。</p>'),
('python-foundation', 2, '条件判断与循环',
'<h2>为什么需要控制流程</h2><p>真实程序不会从头到尾只执行一条直线。用户输入不同、成绩不同、状态不同，程序就要走不同路径。条件判断负责选择路径，循环负责重复完成一组动作。</p><h3>条件判断：先写清楚问题</h3><p>写 <code>if</code> 前先问自己：程序根据什么条件做决定？条件结果只能是真或假。</p><pre><code class="language-python">score = 75
if score >= 90:
    level = "优秀"
elif score >= 60:
    level = "及格"
else:
    level = "需要补练"
print(level)</code></pre><h3>循环：重复，但不是盲目重复</h3><p><code>for</code> 更适合遍历已知集合，<code>while</code> 更适合不知道会重复多少次的场景。写循环时要同时关注三件事：初始状态、继续条件、每轮变化。</p><table><thead><tr><th>结构</th><th>适用场景</th><th>常见风险</th></tr></thead><tbody><tr><td>for</td><td>遍历列表、字符串、range</td><td>索引越界或循环体太复杂</td></tr><tr><td>while</td><td>等待条件达成</td><td>忘记更新条件导致死循环</td></tr></tbody></table><blockquote>调试技巧：在循环体里临时打印关键变量，观察它每一轮是否按预期变化。</blockquote>'),
('python-foundation', 3, '列表、字典与常见遍历',
'<h2>列表和字典分别解决什么问题</h2><p>列表适合保存一组有顺序的数据，例如成绩列表、任务列表、章节列表。字典适合保存一组有名称的数据，例如用户信息、课程配置、统计结果。</p><h3>列表：关注顺序</h3><pre><code class="language-python">scores = [88, 92, 76]
scores.append(95)
average = sum(scores) / len(scores)</code></pre><p>列表常见操作包括追加、删除、排序、切片。不要把所有数据都塞进列表，如果每个元素都有明确名称，字典通常更清楚。</p><h3>字典：关注键值关系</h3><pre><code class="language-python">student = {
    "name": "小明",
    "score": 88,
    "level": "B"
}
print(student["name"])</code></pre><h3>遍历时读懂变量含义</h3><p>遍历列表时，循环变量代表每一个元素；遍历字典时，要明确自己拿的是键、值，还是键值对。</p><pre><code class="language-python">for key, value in student.items():
    print(key, value)</code></pre><p>建议练习：把一个学生成绩列表整理成字典统计结果，包含最高分、最低分、平均分和及格人数。</p>'),
('python-foundation', 4, '函数拆分与小项目组织',
'<h2>函数让程序从一团代码变成多个步骤</h2><p>当一段逻辑需要重复使用，或者一段代码已经长到难以一眼看懂时，就应该考虑写成函数。函数名应该说明它做什么，而不是说明它怎么做。</p><pre><code class="language-python">def calc_average(scores):
    if not scores:
        return 0
    return sum(scores) / len(scores)

def print_report(name, scores):
    average = calc_average(scores)
    print(f"{name} 的平均分是 {average:.1f}")</code></pre><h3>拆函数的三个信号</h3><ul><li>同样的几行代码出现了两次以上。</li><li>一段代码需要注释很多句才能解释清楚。</li><li>主流程读起来不像步骤，而像细节堆叠。</li></ul><blockquote>小项目建议结构：先写主流程，再把输入、计算、输出分别拆成函数。</blockquote><p>完成本章后，可以尝试写一个“成绩分析器”：输入多名学生成绩，输出平均分、最高分、低于平均分的学生名单。这个小项目能同时练习列表、字典、循环和函数。</p>'),

('java-oop', 1, '类、对象与封装',
'<h2>从现实对象理解类</h2><p>Java 的面向对象不是为了增加概念，而是为了把复杂业务拆成更容易维护的对象。类是模板，对象是根据模板创建出来的具体实例。</p><pre><code class="language-java">public class Student {
    private String name;
    private int score;

    public Student(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public boolean isPassed() {
        return score >= 60;
    }
}</code></pre><h3>封装的价值</h3><p>字段设为 <code>private</code>，不是为了麻烦，而是为了避免外部随意修改对象内部状态。对外暴露的方法应该表达业务意图，例如 <code>isPassed()</code> 比直接判断分数更清楚。</p><ul><li>属性描述对象拥有什么。</li><li>方法描述对象能做什么。</li><li>构造方法负责创建对象时的必要初始化。</li></ul>'),
('java-oop', 2, '继承、多态与接口',
'<h2>继承不是复制代码的唯一手段</h2><p>继承适合表达“是一种”的关系，例如 <code>Teacher</code> 是一种 <code>User</code>。如果只是想复用几行代码，优先考虑组合或工具方法，避免继承层级过深。</p><h3>多态：同一个入口，不同实现</h3><pre><code class="language-java">interface Notifier {
    void send(String message);
}

class EmailNotifier implements Notifier {
    public void send(String message) {
        System.out.println("email: " + message);
    }
}

class SmsNotifier implements Notifier {
    public void send(String message) {
        System.out.println("sms: " + message);
    }
}</code></pre><p>当变量类型写成接口 <code>Notifier</code>，调用方就不需要关心具体是邮件还是短信。以后新增站内信通知，只需要新增实现类，调用方不必大改。</p><blockquote>判断接口是否合理：调用方是否真的需要面对多个不同实现？如果只有一个实现，接口可能暂时没有必要。</blockquote>'),
('java-oop', 3, '集合框架与泛型',
'<h2>集合解决批量数据问题</h2><p>Java 集合框架提供了多种容器。选择容器时，不要先背类名，先想你需要什么操作：按顺序遍历、快速查找、去重，还是保存键值关系。</p><table><thead><tr><th>集合</th><th>特点</th><th>典型用途</th></tr></thead><tbody><tr><td>ArrayList</td><td>按顺序存储，查询方便</td><td>课程列表、成绩列表</td></tr><tr><td>HashSet</td><td>元素不重复</td><td>标签去重、用户去重</td></tr><tr><td>HashMap</td><td>键值对</td><td>按 id 查用户、统计计数</td></tr></tbody></table><pre><code class="language-java">Map&lt;String, Integer&gt; counter = new HashMap&lt;&gt;();
counter.put("Python", 3);
counter.put("Java", 5);
Integer count = counter.get("Java");</code></pre><p>泛型让集合知道自己保存什么类型，减少强制类型转换，也能让编译器提前发现错误。</p>'),
('java-oop', 4, '异常处理与代码可维护性',
'<h2>异常不是程序失败，而是需要处理的异常情况</h2><p>文件不存在、网络超时、参数为空，这些都不是正常流程，但真实系统一定会遇到。异常处理的目标是让问题可定位、可恢复、可提示。</p><pre><code class="language-java">try {
    int value = Integer.parseInt(input);
    System.out.println(value);
} catch (NumberFormatException e) {
    System.out.println("请输入合法数字");
}</code></pre><h3>好代码的几个判断</h3><ul><li>方法长度适中，一个方法只表达一层逻辑。</li><li>变量名能说明含义，不靠猜。</li><li>异常信息能帮助定位问题。</li><li>公共逻辑抽到合适位置，但不过度抽象。</li></ul><blockquote>复盘问题时，不只问“怎么让它不报错”，还要问“为什么这里可能出现这种输入”。</blockquote>'),

('data-structure', 1, '数组与链表的选择',
'<h2>数组和链表的核心差异</h2><p>数组把元素放在连续空间里，适合通过下标快速访问。链表通过节点互相指向，适合频繁插入和删除。选择结构时，要看主要操作是什么，而不是看哪个名字更熟悉。</p><table><thead><tr><th>结构</th><th>查询</th><th>插入删除</th><th>适用场景</th></tr></thead><tbody><tr><td>数组</td><td>快</td><td>中间位置较慢</td><td>排行榜、固定列表</td></tr><tr><td>链表</td><td>慢</td><td>拿到节点后较快</td><td>任务队列、撤销链</td></tr></tbody></table><p>如果题目经常出现“第几个元素”“按下标访问”，数组更自然；如果题目强调“不断在头部插入”“删除当前节点”，链表更值得考虑。</p>'),
('data-structure', 2, '栈与队列',
'<h2>两个非常有画面感的结构</h2><p>栈是后进先出，像浏览器返回历史；队列是先进先出，像排队处理任务。它们的价值在于限制操作方式，让程序状态更可控。</p><h3>栈的典型场景</h3><ul><li>括号匹配：遇到左括号入栈，遇到右括号出栈检查。</li><li>函数调用：后调用的函数先返回。</li><li>撤销操作：最近一次操作最先撤销。</li></ul><h3>队列的典型场景</h3><ul><li>广度优先搜索。</li><li>消息任务排队。</li><li>打印任务、审核任务等顺序处理。</li></ul><blockquote>解题提示：看到“最近的先处理”想栈，看到“先来的先处理”想队列。</blockquote>'),
('data-structure', 3, '树、二叉树与遍历',
'<h2>树用来表达层级关系</h2><p>目录结构、组织架构、课程知识图谱都可以看成树。二叉树是每个节点最多两个孩子的树，很多算法题会从它开始训练递归思维。</p><h3>三种深度优先遍历</h3><ul><li>前序：先访问根，再左子树，再右子树。</li><li>中序：先左子树，再根，再右子树。</li><li>后序：先左子树，再右子树，再根。</li></ul><pre><code class="language-text">        A
      /   \
     B     C
    / \
   D   E</code></pre><p>前序结果是 A B D E C。练习遍历时可以先在纸上画箭头，理解访问顺序后再写代码。</p>'),
('data-structure', 4, '哈希表与快速查找',
'<h2>哈希表为什么快</h2><p>哈希表通过键计算位置，理想情况下可以接近 O(1) 查找。它适合解决“是否出现过”“某个值出现几次”“通过 id 找对象”这类问题。</p><pre><code class="language-python">nums = [2, 7, 11, 15]
target = 9
seen = {}
for i, num in enumerate(nums):
    need = target - num
    if need in seen:
        print(seen[need], i)
    seen[num] = i</code></pre><p>这段两数之和的思路是：一边遍历，一边记录已经见过的数字。每次只问“我需要的另一个数是否已经出现”。</p><blockquote>哈希表常见代价：它用额外空间换查询速度，所以复盘时要同时写出时间复杂度和空间复杂度。</blockquote>'),

('algorithm-complexity', 1, '时间复杂度怎么估算',
'<h2>复杂度看的是增长趋势</h2><p>时间复杂度不是精确秒数，而是输入规模变大时，运行次数如何增长。它帮助我们判断程序能不能在大数据量下跑得动。</p><table><thead><tr><th>复杂度</th><th>直观理解</th><th>常见代码</th></tr></thead><tbody><tr><td>O(1)</td><td>固定次数</td><td>取数组第一个元素</td></tr><tr><td>O(n)</td><td>遍历一遍</td><td>求最大值</td></tr><tr><td>O(n²)</td><td>双层遍历</td><td>两两比较</td></tr><tr><td>O(log n)</td><td>每次砍半</td><td>二分查找</td></tr></tbody></table><p>估算时先找循环，再看循环之间是并列还是嵌套。并列通常相加取最高阶，嵌套通常相乘。</p>'),
('algorithm-complexity', 2, '双指针与滑动窗口',
'<h2>双指针减少重复扫描</h2><p>双指针常用于有序数组、字符串区间和连续子数组。它的核心是用两个位置共同描述当前状态，避免每次从头重新计算。</p><h3>滑动窗口的四步</h3><ol><li>右指针扩展窗口，把新元素纳入统计。</li><li>判断窗口是否满足或违反条件。</li><li>必要时移动左指针缩小窗口。</li><li>在合适时机更新答案。</li></ol><pre><code class="language-text">left ---- current window ---- right</code></pre><blockquote>滑动窗口题不要急着写代码，先明确窗口中需要维护哪些信息，例如和、字符计数、最大值或最小值。</blockquote>'),
('algorithm-complexity', 3, '递归与分治',
'<h2>递归是在解决同一种更小的问题</h2><p>递归函数要有两个部分：终止条件和递推关系。终止条件负责停下来，递推关系负责把大问题交给更小的问题。</p><pre><code class="language-python">def factorial(n):
    if n <= 1:
        return 1
    return n * factorial(n - 1)</code></pre><h3>分治思想</h3><p>分治是把问题拆成相互独立的小问题，分别解决后合并结果。归并排序就是典型例子：先拆成两半，分别排序，再合并成有序数组。</p><ul><li>能否拆成同类小问题？</li><li>小问题之间是否相对独立？</li><li>结果如何合并？</li></ul>'),
('algorithm-complexity', 4, '动态规划入门',
'<h2>动态规划解决有重叠子问题的最优解</h2><p>如果一个问题可以拆成子问题，而且很多子问题会重复出现，就可以考虑动态规划。不要一开始就背公式，先定义状态。</p><h3>四个步骤</h3><ol><li>定义状态：<code>dp[i]</code> 表示什么。</li><li>确定转移：<code>dp[i]</code> 从哪些状态来。</li><li>设置初始值：最小规模下答案是什么。</li><li>确定遍历顺序：先算谁，后算谁。</li></ol><pre><code class="language-python">dp = [0] * (n + 1)
dp[1] = 1
for i in range(2, n + 1):
    dp[i] = dp[i - 1] + dp[i - 2]</code></pre><blockquote>写动态规划时，状态定义写得越清楚，代码越不容易乱。</blockquote>'),

('sql-practical', 1, 'SELECT 与 WHERE',
'<h2>SQL 是在描述你想要什么数据</h2><p>写 SQL 时先明确三件事：从哪张表查、要哪些列、筛选什么条件。不要一上来就写很长语句，先把最小查询跑通。</p><pre><code class="language-sql">SELECT id, name, score
FROM student
WHERE score >= 60
ORDER BY score DESC;</code></pre><h3>WHERE 常见条件</h3><ul><li><code>=</code>、<code>&gt;</code>、<code>&lt;</code> 用于精确或范围判断。</li><li><code>LIKE</code> 用于模糊匹配。</li><li><code>IN</code> 用于多个候选值。</li><li><code>BETWEEN</code> 用于区间。</li></ul><blockquote>不要用 SELECT * 写长期代码。明确列名更安全，也方便后来维护。</blockquote>'),
('sql-practical', 2, 'JOIN 连接查询',
'<h2>连接查询把多张表的信息拼起来</h2><p>一个学生表只保存学生信息，课程表只保存课程信息，选课表保存学生和课程之间的关系。JOIN 的作用就是按照关联字段把它们组合起来。</p><pre><code class="language-sql">SELECT s.name, c.name AS course_name
FROM student s
JOIN student_course sc ON sc.student_id = s.id
JOIN course c ON c.id = sc.course_id;</code></pre><table><thead><tr><th>JOIN 类型</th><th>含义</th></tr></thead><tbody><tr><td>INNER JOIN</td><td>两边都匹配才返回</td></tr><tr><td>LEFT JOIN</td><td>保留左表全部记录</td></tr></tbody></table><p>排查 JOIN 结果异常时，先检查关联字段是否写对，再检查数据是否真的存在对应关系。</p>'),
('sql-practical', 3, 'GROUP BY 与聚合统计',
'<h2>聚合是在回答统计问题</h2><p>当问题变成“每个班多少人”“每门课平均分是多少”“每天新增多少条记录”时，就需要 GROUP BY。</p><pre><code class="language-sql">SELECT class_id, COUNT(*) AS student_count, AVG(score) AS avg_score
FROM student
GROUP BY class_id
HAVING AVG(score) >= 80;</code></pre><h3>WHERE 和 HAVING 的区别</h3><p><code>WHERE</code> 在分组前筛选原始行，<code>HAVING</code> 在分组后筛选统计结果。记住这个顺序，很多聚合错误就能避免。</p><blockquote>统计类 SQL 建议给聚合列起别名，例如 student_count、avg_score，前端或报表读取时更清晰。</blockquote>'),
('sql-practical', 4, '索引与慢查询意识',
'<h2>索引像书的目录</h2><p>没有索引时，数据库可能需要从头到尾扫描整张表。有了合适索引，数据库可以更快定位数据。但索引不是越多越好，因为写入时也要维护索引。</p><ul><li>经常出现在 WHERE 条件中的列适合考虑索引。</li><li>经常用于 JOIN 的关联字段适合考虑索引。</li><li>低区分度字段单独建索引价值有限，例如性别、是否删除。</li></ul><pre><code class="language-sql">CREATE INDEX idx_student_class_score
ON student(class_id, score);</code></pre><p>学习阶段先形成意识：当数据量变大后，正确结果只是第一步，查询速度也会成为重要问题。</p>'),

('frontend-basic', 1, 'HTML 负责结构',
'<h2>HTML 是页面的骨架</h2><p>HTML 不只是把文字放到页面上，更重要的是表达内容结构。标题、段落、列表、表格、表单都有自己的语义标签。</p><pre><code class="language-html">&lt;article&gt;
  &lt;h1&gt;课程标题&lt;/h1&gt;
  &lt;p&gt;课程简介内容&lt;/p&gt;
  &lt;ul&gt;
    &lt;li&gt;第一章&lt;/li&gt;
    &lt;li&gt;第二章&lt;/li&gt;
  &lt;/ul&gt;
&lt;/article&gt;</code></pre><p>语义清楚的 HTML 对样式、搜索、无障碍和后期维护都有帮助。不要所有内容都写成 <code>div</code>。</p>'),
('frontend-basic', 2, 'CSS 负责视觉与布局',
'<h2>CSS 决定页面如何呈现</h2><p>CSS 可以控制颜色、字体、间距、布局和响应式效果。初学时最容易混淆的是盒模型：内容、内边距、边框、外边距共同决定元素占用空间。</p><pre><code class="language-css">.card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}</code></pre><h3>布局优先掌握 Flex 和 Grid</h3><ul><li>Flex 适合一维排列，例如导航栏、按钮组。</li><li>Grid 适合二维网格，例如课程卡片列表。</li></ul><blockquote>写样式时先处理结构和间距，再处理颜色和装饰，页面会更稳定。</blockquote>'),
('frontend-basic', 3, 'JavaScript 负责交互',
'<h2>JavaScript 让页面响应用户动作</h2><p>按钮点击、表单校验、接口请求、列表筛选都离不开 JavaScript。学习时不要只记语法，要理解“事件触发后，状态发生变化，界面重新呈现”的过程。</p><pre><code class="language-javascript">const keyword = "python";
const list = ["python 基础", "java 入门", "sql 查询"];
const result = list.filter(item =&gt; item.includes(keyword));
console.log(result);</code></pre><h3>常见交互流程</h3><ol><li>监听用户动作，例如点击或输入。</li><li>读取当前状态。</li><li>执行计算或请求接口。</li><li>把结果渲染到页面。</li></ol>'),
('frontend-basic', 4, '从静态页到组件化',
'<h2>组件化解决复用和复杂度</h2><p>当页面变复杂后，所有 HTML、CSS、JS 写在一起会越来越难维护。组件化把页面拆成独立的小块，例如课程卡片、搜索框、章节目录。</p><pre><code class="language-text">页面
├─ 搜索区域
├─ 分类筛选
└─ 教程卡片列表
   ├─ 教程卡片
   └─ 教程卡片</code></pre><p>一个好组件应该有清楚的输入和输出。输入可能是课程数据，输出可能是点击事件。拆组件不是为了数量多，而是为了让每一块职责清楚。</p><blockquote>练习建议：把一个教程列表页拆成 SearchBar、FilterTabs、TutorialCard 三个组件。</blockquote>'),

('ai-basic', 1, '数据、模型与任务',
'<h2>AI 系统的三个关键词</h2><p>人工智能应用通常围绕数据、模型和任务展开。数据提供经验，模型从数据中学习规律，任务定义模型要解决的问题。</p><table><thead><tr><th>概念</th><th>说明</th><th>例子</th></tr></thead><tbody><tr><td>数据</td><td>训练或评估使用的样本</td><td>图片、文本、成绩记录</td></tr><tr><td>模型</td><td>从数据中学习到的函数</td><td>分类器、推荐模型</td></tr><tr><td>任务</td><td>希望模型完成的目标</td><td>识别图片、预测成绩</td></tr></tbody></table><p>学习 AI 时，不要只问用了什么模型，还要问数据从哪里来、标签是否可靠、任务定义是否清楚。</p>'),
('ai-basic', 2, '训练、验证与测试',
'<h2>为什么要拆分数据集</h2><p>如果模型只在训练数据上表现好，不能说明它真的学会了规律。验证集用于调参，测试集用于最终评估。三者分开，是为了减少自欺欺人的结果。</p><ul><li>训练集：模型直接学习的数据。</li><li>验证集：开发过程中用来比较方案。</li><li>测试集：尽量最后才使用，用来估计真实效果。</li></ul><blockquote>如果同一批数据既用来训练又用来宣称效果，评估结果往往会过于乐观。</blockquote><p>在课程项目里，即使数据量不大，也应该保留一部分样本做测试，这能帮助你发现模型是否只记住了例题。</p>'),
('ai-basic', 3, '过拟合与泛化',
'<h2>过拟合是把训练题背得太熟</h2><p>过拟合指模型在训练集上表现很好，但遇到新数据就明显变差。它像学生只背答案，没有理解解题方法。</p><h3>常见原因</h3><ul><li>训练数据太少或太单一。</li><li>模型太复杂，记住了噪声。</li><li>训练时间过长，细节也被过度学习。</li></ul><h3>缓解方法</h3><ul><li>增加数据或做数据增强。</li><li>简化模型。</li><li>使用正则化、早停等策略。</li></ul><p>泛化能力才是模型真正有价值的地方，也就是它面对未见过样本时仍然能稳定工作。</p>'),
('ai-basic', 4, '评估指标怎么看',
'<h2>准确率不是唯一指标</h2><p>不同任务需要不同指标。比如疾病筛查更关注漏诊，垃圾邮件识别更关注误判，推荐系统还要看点击率、停留时长和满意度。</p><table><thead><tr><th>指标</th><th>关注点</th></tr></thead><tbody><tr><td>Accuracy</td><td>整体预测正确比例</td></tr><tr><td>Precision</td><td>预测为正的样本里有多少是真的</td></tr><tr><td>Recall</td><td>真实为正的样本里找回了多少</td></tr><tr><td>F1</td><td>Precision 和 Recall 的综合</td></tr></tbody></table><blockquote>看指标时一定要结合业务场景。没有场景，指标只是数字。</blockquote>'),

('springboot-rest', 1, '一次接口请求如何流动',
'<h2>后端接口的基本链路</h2><p>一个 REST 接口通常从 Controller 进入，经过 Service 处理业务逻辑，再通过 Mapper 或 Repository 访问数据库，最后返回统一响应。</p><pre><code class="language-text">浏览器或前端
  -> Controller 接收请求
  -> Service 处理规则
  -> Mapper 查询数据库
  -> Response 返回结果</code></pre><p>分层的目的不是增加文件数量，而是让每层只关心自己的职责。Controller 不应该堆太多业务规则，Mapper 不应该处理复杂业务判断。</p>'),
('springboot-rest', 2, 'Controller 与请求参数',
'<h2>Controller 负责把 HTTP 请求转成 Java 调用</h2><p>常见参数来源包括路径参数、查询参数和请求体。写接口时要选择合适的方式，接口才容易理解。</p><pre><code class="language-java">@GetMapping("/tutorial/{id}")
public BaseResponse&lt;TutorialVO&gt; detail(@PathVariable Long id) {
    return ResultUtils.success(tutorialService.detail(id));
}

@PostMapping("/tutorial/save")
public BaseResponse&lt;Long&gt; save(@RequestBody TutorialSaveRequest request) {
    return ResultUtils.success(tutorialService.save(request));
}</code></pre><ul><li><code>@PathVariable</code>：资源路径的一部分，例如 id。</li><li><code>@RequestParam</code>：筛选、分页、排序等查询条件。</li><li><code>@RequestBody</code>：复杂对象，通常用于新增或编辑。</li></ul>'),
('springboot-rest', 3, 'DTO、VO 与实体类',
'<h2>不要把数据库实体直接当成所有对象</h2><p>实体类对应数据库表，DTO 用于接收请求，VO 用于返回给前端。它们分开后，接口会更稳定，也能避免敏感字段被误返回。</p><table><thead><tr><th>对象</th><th>用途</th></tr></thead><tbody><tr><td>Entity</td><td>数据库表结构映射</td></tr><tr><td>DTO / Request</td><td>接收前端提交的数据</td></tr><tr><td>VO</td><td>整理后返回给前端展示</td></tr></tbody></table><blockquote>当字段名、字段数量、校验规则开始变多时，DTO 和 VO 的价值会非常明显。</blockquote>'),
('springboot-rest', 4, '统一响应与异常处理',
'<h2>统一响应让前后端协作更稳定</h2><p>前端调用接口时，希望每个接口都有一致结构，例如 code、message、data。这样请求成功、失败、未登录、无权限都能用统一方式处理。</p><pre><code class="language-json">{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "name": "Python 基础"
  }
}</code></pre><p>异常处理也应该集中管理。业务错误抛出明确异常，由全局异常处理器转换成统一响应，避免每个 Controller 都写重复的 try catch。</p>'),

('git-workflow', 1, '提交记录是项目时间线',
'<h2>Git 记录的是一次次有意义的变化</h2><p>一次好的提交应该包含一个清楚的目的，例如“新增图文教程导入脚本”或“修复登录状态丢失”。不要把许多无关修改塞进同一个提交。</p><pre><code class="language-bash">git status
git add src/main/resources/sql/detailed_text_tutorial_seed.sql
git commit -m "Add detailed text tutorial seed data"</code></pre><h3>提交前检查</h3><ul><li>确认只包含本次任务相关文件。</li><li>运行必要测试或构建。</li><li>提交信息能让队友看懂目的。</li></ul>'),
('git-workflow', 2, '分支让多人协作更清楚',
'<h2>分支是独立工作的空间</h2><p>在新功能分支上开发，可以避免直接影响主分支。功能完成后，再通过合并请求或代码评审进入主线。</p><pre><code class="language-bash">git switch -c feature/tutorial-seed
git push origin feature/tutorial-seed</code></pre><p>分支命名建议包含类型和主题，例如 <code>feature/tutorial-import</code>、<code>fix/login-session</code>。名称不用太长，但要能表达方向。</p>'),
('git-workflow', 3, '合并与冲突处理',
'<h2>冲突说明同一位置被不同分支修改</h2><p>冲突不是错误，而是 Git 无法替你判断应该保留哪一份。处理冲突时先理解两边改动的意图，再决定保留、合并或重写。</p><pre><code class="language-text">&lt;&lt;&lt;&lt;&lt;&lt;&lt; HEAD
当前分支内容
&#61;&#61;&#61;&#61;&#61;&#61;&#61;
要合并进来的内容
&gt;&gt;&gt;&gt;&gt;&gt;&gt; feature</code></pre><ol><li>打开冲突文件。</li><li>阅读两边内容。</li><li>删除冲突标记，整理成最终版本。</li><li>运行测试，再提交解决结果。</li></ol>'),
('git-workflow', 4, '撤销操作的安全顺序',
'<h2>先看状态，再撤销</h2><p>Git 有很多撤销命令，危险程度不同。养成先看 <code>git status</code> 和 <code>git diff</code> 的习惯，可以避免误删同伴或自己的工作。</p><table><thead><tr><th>场景</th><th>建议命令</th></tr></thead><tbody><tr><td>取消暂存</td><td>git restore --staged file</td></tr><tr><td>丢弃工作区单个文件修改</td><td>git restore file</td></tr><tr><td>查看某次提交</td><td>git show commitId</td></tr></tbody></table><blockquote>不要随手使用 reset hard。它很强，但也很容易把未保存工作清掉。</blockquote>'),

('study-method', 1, '预习：先建立地图',
'<h2>预习不是提前学完，而是知道要学什么</h2><p>正式上课前花十分钟浏览标题、目录、示例和作业要求，可以让大脑先建立知识地图。这样听课时更容易把细节挂到对应位置。</p><ul><li>扫一遍目录，标出陌生词。</li><li>看一段示例代码，猜它在做什么。</li><li>写下两个问题，带着问题听课。</li></ul><blockquote>预习的目标不是正确率，而是降低陌生感。</blockquote>'),
('study-method', 2, '练习：从模仿到变形',
'<h2>编程学习一定要动手</h2><p>只看教程会产生“我懂了”的错觉。有效练习可以分三步：先照着写，再遮住重写，最后改变需求做一个变形版本。</p><ol><li>照写：熟悉语法和运行流程。</li><li>重写：检查自己是否能独立还原。</li><li>变形：把输入、输出或规则改掉。</li></ol><p>例如学完列表统计后，不只求平均分，还可以改成统计及格人数、最高分学生、低于平均分名单。</p>'),
('study-method', 3, '错题与报错整理',
'<h2>错误是最有价值的学习材料</h2><p>每次报错都记录三件事：错误现象、真正原因、以后如何避免。不要只复制最终答案，否则下次换个形式还会卡住。</p><table><thead><tr><th>记录项</th><th>示例</th></tr></thead><tbody><tr><td>现象</td><td>运行时报 IndexError</td></tr><tr><td>原因</td><td>循环访问了不存在的下标</td></tr><tr><td>预防</td><td>遍历列表优先用 for item in list</td></tr></tbody></table><blockquote>把错误整理成自己的语言，比收藏十篇文章更有用。</blockquote>'),
('study-method', 4, '阶段复盘与项目输出',
'<h2>用小项目把知识连起来</h2><p>每学完一个阶段，最好做一个能展示的小作品。作品不一定大，但要能把多个知识点串起来，例如成绩分析器、课程卡片页、SQL 统计报表。</p><h3>复盘模板</h3><ul><li>我学会了哪些概念？</li><li>我能独立写出哪些代码？</li><li>我最容易错在哪里？</li><li>下一步要补哪三个点？</li></ul><p>项目输出会迫使你处理真实细节：命名、边界情况、文件组织、调试过程。这些能力只靠看书很难形成。</p>');

INSERT INTO text_course (name, cover_img, description, create_time)
SELECT c.name, c.cover_img, c.description, NOW()
FROM tmp_text_tutorial_course c
WHERE NOT EXISTS (
    SELECT 1
    FROM text_course tc
    WHERE tc.name = c.name
);

INSERT INTO text_node (course_id, title, content, sort_order, create_time)
SELECT tc.id, n.title, n.content, n.sort_order, NOW()
FROM tmp_text_tutorial_node n
JOIN tmp_text_tutorial_course c ON c.slug = n.slug
JOIN text_course tc ON tc.name = c.name
WHERE NOT EXISTS (
    SELECT 1
    FROM text_node tn
    WHERE tn.course_id = tc.id
      AND tn.title = n.title
);

DROP TEMPORARY TABLE IF EXISTS tmp_text_tutorial_node;
DROP TEMPORARY TABLE IF EXISTS tmp_text_tutorial_course;

COMMIT;
