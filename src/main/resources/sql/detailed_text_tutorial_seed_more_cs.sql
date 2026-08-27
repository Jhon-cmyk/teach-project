-- Additional computer-science focused text tutorial seed data.
-- Safe to run repeatedly: courses are matched by name, nodes are matched by course + title.

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

DROP TEMPORARY TABLE IF EXISTS tmp_more_cs_course;
CREATE TEMPORARY TABLE tmp_more_cs_course (
    slug VARCHAR(80) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    cover_img VARCHAR(500) DEFAULT '',
    description VARCHAR(800) DEFAULT ''
) ENGINE=Memory DEFAULT CHARSET=utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_more_cs_node;
CREATE TEMPORARY TABLE tmp_more_cs_node (
    slug VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content LONGTEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_more_cs_course (slug, name, cover_img, description) VALUES
('c-memory', 'C 语言指针与内存图解', '/icons/types/C.png', '用图文方式拆解地址、指针、数组、字符串、结构体和内存生命周期，适合 C 语言进阶复盘。'),
('computer-network', '计算机网络协议图解', '/icons/types/编程.png', '从分层模型、HTTP、TCP、DNS 到抓包分析，建立网络请求从浏览器到服务器的完整路径感。'),
('operating-system', '操作系统进程线程与内存管理', '/icons/types/编程.png', '围绕进程、线程、调度、虚拟内存、文件系统等核心概念，帮助学生读懂操作系统基础。'),
('computer-organization', '计算机组成原理通俗笔记', '/icons/types/编程.png', '把 CPU、内存、总线、指令、缓存和二进制表示串起来，理解程序在硬件上如何运行。'),
('software-engineering', '软件工程与需求分析入门', '/icons/types/设计.png', '从需求澄清、用例、原型、迭代计划到验收标准，训练把想法变成可开发任务的能力。'),
('design-patterns', '常用设计模式图文入门', '/icons/types/设计.png', '用实际业务例子解释单例、工厂、策略、观察者、适配器等常用模式，避免只背概念。'),
('web-security', 'Web 安全基础与防护', '/icons/types/运维.png', '理解 XSS、CSRF、SQL 注入、权限绕过和接口安全，建立日常开发中的安全意识。'),
('testing-quality', '软件测试与质量保障基础', '/icons/types/阅读.png', '覆盖单元测试、集成测试、测试用例设计、缺陷管理和回归验证，适合课程项目质量提升。'),
('http-api', 'HTTP API 设计与调试实战', '/icons/types/后端.png', '围绕 REST 风格、状态码、请求参数、接口文档和调试工具，提升前后端联调效率。'),
('message-queue', '消息队列与异步系统入门', '/icons/types/后端.png', '从同步调用痛点讲到队列模型、削峰填谷、重试、幂等和死信队列，理解异步架构。'),
('distributed-system', '分布式系统基础概念', '/icons/types/后端.png', '用服务拆分、负载均衡、一致性、熔断、限流和链路追踪解释分布式系统的基本问题。'),
('nginx-gateway', 'Nginx 反向代理与网关配置', '/icons/types/运维.png', '学习静态资源托管、反向代理、负载均衡、HTTPS 和常见网关配置，适合部署实践。'),
('shell-automation', 'Shell 脚本自动化入门', '/icons/types/运维.png', '从变量、参数、条件、循环到日志清理和批量部署脚本，培养命令行自动化思维。'),
('compiler-basic', '编译原理入门图解', '/icons/types/编程.png', '用词法分析、语法分析、语义检查、中间代码和优化解释一段代码如何变成可执行程序。'),
('oop-modeling', '面向对象建模与 UML 入门', '/icons/types/设计.png', '通过类图、时序图、状态图和职责划分，把业务需求转成清晰的软件模型。');

INSERT INTO tmp_more_cs_node (slug, sort_order, title, content) VALUES
('c-memory', 1, '地址、指针与变量',
'<h2>指针先从地址理解</h2><p>变量保存数据，内存地址说明这个数据放在哪里。指针变量保存的不是普通数字含义，而是另一个变量的地址。理解这一点后，取地址、解引用、指针传参都会清楚很多。</p><pre><code class="language-c">int score = 90;
int *p = &score;
printf("%d", *p);</code></pre><ul><li><code>&score</code> 表示取变量地址。</li><li><code>p</code> 保存地址。</li><li><code>*p</code> 沿着地址找到对应数据。</li></ul><blockquote>学习指针时建议画两列：左边写变量名，右边写地址和值。每执行一行代码，就更新一次图。</blockquote>'),
('c-memory', 2, '数组、字符串与指针关系',
'<h2>数组名和指针很像，但不是完全一样</h2><p>数组在内存中连续存放，数组名在很多表达式里会退化为首元素地址。字符串本质上是以 <code>\0</code> 结尾的字符数组。</p><pre><code class="language-c">char name[] = "Code";
printf("%c", name[0]);
printf("%c", *(name + 1));</code></pre><p><code>name[1]</code> 和 <code>*(name + 1)</code> 都能访问第二个字符。区别在于数组拥有固定大小的内存空间，而普通指针只是保存地址。</p><table><thead><tr><th>写法</th><th>含义</th></tr></thead><tbody><tr><td>arr[i]</td><td>访问第 i 个元素</td></tr><tr><td>arr + i</td><td>第 i 个元素的地址</td></tr><tr><td>*(arr + i)</td><td>第 i 个元素的值</td></tr></tbody></table>'),
('c-memory', 3, '结构体与动态内存',
'<h2>结构体把相关字段打包</h2><p>当一个对象有多个属性时，用结构体比多个散落变量更清晰。动态内存则适合运行时才知道需要多少空间的场景。</p><pre><code class="language-c">typedef struct {
    int id;
    char name[32];
} Student;

Student *stu = malloc(sizeof(Student));
stu->id = 1;
free(stu);</code></pre><ul><li><code>.</code> 用于结构体变量访问字段。</li><li><code>-></code> 用于结构体指针访问字段。</li><li><code>malloc</code> 后必须思考何时 <code>free</code>。</li></ul><blockquote>动态内存的核心问题不是申请，而是所有权：谁申请，谁释放，什么时候释放。</blockquote>'),
('c-memory', 4, '常见内存错误排查',
'<h2>内存错误通常很隐蔽</h2><p>C 语言不会像高级语言一样替你检查所有边界，因此数组越界、空指针、重复释放、释放后继续使用都很常见。</p><table><thead><tr><th>错误</th><th>表现</th><th>预防</th></tr></thead><tbody><tr><td>数组越界</td><td>结果异常或崩溃</td><td>严格检查下标范围</td></tr><tr><td>空指针</td><td>运行时崩溃</td><td>使用前判断是否为 NULL</td></tr><tr><td>内存泄漏</td><td>长时间运行变慢</td><td>malloc 和 free 成对出现</td></tr></tbody></table><p>调试时先缩小范围：确认是哪一次写入后数据开始异常，再检查对应指针是否有效。</p>'),

('computer-network', 1, '网络分层模型',
'<h2>分层是为了降低复杂度</h2><p>计算机网络把复杂通信拆成多个层次，每层只关注自己的职责。应用层关心业务数据，传输层关心进程之间的可靠传输，网络层关心跨网络寻址，链路层关心相邻设备传输。</p><table><thead><tr><th>层次</th><th>关键词</th><th>例子</th></tr></thead><tbody><tr><td>应用层</td><td>具体协议</td><td>HTTP、DNS</td></tr><tr><td>传输层</td><td>端口、可靠性</td><td>TCP、UDP</td></tr><tr><td>网络层</td><td>IP、路由</td><td>IPv4、IPv6</td></tr><tr><td>链路层</td><td>局域网传输</td><td>以太网、Wi-Fi</td></tr></tbody></table><blockquote>排查网络问题时按层定位：域名是否解析、IP 是否可达、端口是否开放、应用是否正常。</blockquote>'),
('computer-network', 2, 'HTTP 请求的一生',
'<h2>从输入网址到页面出现</h2><p>浏览器访问网站时，会经历 DNS 解析、建立连接、发送 HTTP 请求、服务器处理、返回响应、浏览器渲染等步骤。</p><ol><li>浏览器解析 URL，确认协议、域名、路径。</li><li>通过 DNS 找到服务器 IP。</li><li>建立 TCP 连接，HTTPS 还要进行 TLS 握手。</li><li>发送请求头和请求体。</li><li>服务器返回状态码、响应头和响应体。</li></ol><pre><code class="language-http">GET /student/tutorial HTTP/1.1
Host: example.com
Accept: text/html</code></pre><p>理解这条链路后，404、500、跨域、证书错误、接口超时都会更容易定位。</p>'),
('computer-network', 3, 'TCP 可靠传输',
'<h2>TCP 为什么可靠</h2><p>TCP 通过序号、确认、重传、流量控制和拥塞控制，让字节流尽量可靠、有序地到达对方。它的可靠性不是魔法，而是一套持续校验和补救机制。</p><ul><li>三次握手：确认双方收发能力。</li><li>序号与确认号：知道哪些数据已经收到。</li><li>超时重传：丢包后重新发送。</li><li>滑动窗口：控制发送速度。</li></ul><blockquote>如果接口偶发慢，不一定是代码慢，也可能是网络重传、拥塞、DNS 或连接复用问题。</blockquote>'),
('computer-network', 4, 'DNS、抓包与排障',
'<h2>DNS 是网络世界的通讯录</h2><p>DNS 把域名解析成 IP。很多访问失败不是服务器挂了，而是域名解析异常、缓存过期或本地代理配置错误。</p><pre><code class="language-bash">nslookup example.com
ping example.com
curl -I https://example.com</code></pre><p>抓包工具可以观察真实请求。学习阶段重点看请求方法、URL、状态码、耗时、请求头和响应头。先用浏览器 Network 面板就能完成大多数排查。</p><table><thead><tr><th>现象</th><th>优先检查</th></tr></thead><tbody><tr><td>域名打不开</td><td>DNS 解析</td></tr><tr><td>连接超时</td><td>端口、防火墙、服务状态</td></tr><tr><td>状态码 500</td><td>后端日志</td></tr></tbody></table>'),

('operating-system', 1, '进程与线程',
'<h2>进程是资源单位，线程是执行单位</h2><p>一个运行中的程序通常对应一个进程。进程拥有独立地址空间、文件句柄等资源。线程运行在进程内部，共享进程资源，但有自己的调用栈。</p><table><thead><tr><th>概念</th><th>重点</th></tr></thead><tbody><tr><td>进程</td><td>资源隔离更强，切换成本较高</td></tr><tr><td>线程</td><td>共享数据方便，但更容易出现并发问题</td></tr></tbody></table><blockquote>多线程不是越多越快。线程太多会增加切换成本和锁竞争。</blockquote>'),
('operating-system', 2, 'CPU 调度与上下文切换',
'<h2>操作系统决定谁先运行</h2><p>当多个任务都想使用 CPU 时，调度器会按照策略分配时间片。上下文切换会保存当前任务状态，再恢复另一个任务状态。</p><ul><li>时间片轮转让多个任务看起来同时运行。</li><li>优先级可以让关键任务更早得到 CPU。</li><li>频繁切换会消耗额外时间。</li></ul><p>理解调度后，就能解释为什么程序在 CPU 很忙时响应变慢，以及为什么 IO 密集任务和 CPU 密集任务的优化方式不同。</p>'),
('operating-system', 3, '虚拟内存与分页',
'<h2>每个进程都以为自己有连续内存</h2><p>虚拟内存让进程看到独立、连续的地址空间，操作系统和硬件负责把虚拟地址映射到物理内存。分页机制把内存切成固定大小的页，便于管理和隔离。</p><ul><li>虚拟地址：程序中看到的地址。</li><li>物理地址：真实内存位置。</li><li>页表：记录映射关系。</li><li>缺页：访问的页暂时不在内存，需要加载。</li></ul><blockquote>内存不足时系统变慢，常常和频繁换页有关。</blockquote>'),
('operating-system', 4, '文件系统与权限',
'<h2>文件系统负责组织持久化数据</h2><p>文件、目录、路径、权限都是文件系统提供的抽象。程序读写文件时，操作系统会处理路径解析、权限检查、缓存和磁盘 IO。</p><pre><code class="language-bash">ls -l
chmod 644 app.log
tail -f app.log</code></pre><p>权限排查时关注三类对象：文件所有者、所属组、其他用户。服务启动失败、日志写不进去、上传文件失败，经常和目录权限有关。</p>'),

('computer-organization', 1, '二进制与数据表示',
'<h2>计算机内部只处理二进制</h2><p>整数、字符、图片、音频最终都要编码成二进制。不同编码规则决定同一串比特如何被解释。理解编码可以帮助你解释乱码、溢出和精度误差。</p><ul><li>整数常用补码表示，方便做加减法。</li><li>字符需要字符集和编码，例如 Unicode、UTF-8。</li><li>浮点数用近似方式表示小数，因此会有精度问题。</li></ul><pre><code class="language-text">十进制 13 = 二进制 1101</code></pre>'),
('computer-organization', 2, 'CPU、寄存器与指令',
'<h2>CPU 执行的是一条条指令</h2><p>高级语言代码会被编译或解释成更底层的指令。CPU 通过取指、译码、执行不断推进程序。寄存器是 CPU 内部非常快的小容量存储。</p><table><thead><tr><th>部件</th><th>作用</th></tr></thead><tbody><tr><td>控制器</td><td>协调指令执行</td></tr><tr><td>运算器</td><td>进行算术和逻辑运算</td></tr><tr><td>寄存器</td><td>保存当前运算所需数据</td></tr></tbody></table><p>性能优化时，CPU 不只看主频，还要考虑缓存命中、分支预测、指令流水线等因素。</p>'),
('computer-organization', 3, '缓存层次结构',
'<h2>越靠近 CPU 越快也越贵</h2><p>现代计算机有多级存储：寄存器、L1/L2/L3 缓存、内存、磁盘。程序性能很大程度上受数据是否能快速拿到影响。</p><pre><code class="language-text">寄存器 -> CPU 缓存 -> 内存 -> SSD -> 网络存储</code></pre><blockquote>局部性原理：刚访问过的数据和附近的数据，很可能很快再次被访问。数组连续存储通常比链表更容易利用缓存。</blockquote>'),
('computer-organization', 4, '程序从源码到运行',
'<h2>一段程序要经历多个阶段</h2><p>源码需要经过预处理、编译、汇编、链接，最终形成可执行文件。运行时，操作系统把程序加载到内存，准备栈、堆、全局区等运行环境。</p><ol><li>编译器检查语法并生成目标代码。</li><li>链接器把多个目标文件和库组合起来。</li><li>加载器把程序放入内存。</li><li>CPU 从入口地址开始执行。</li></ol><p>理解这个流程后，编译错误、链接错误、运行时错误就不再混成一团。</p>'),

('software-engineering', 1, '需求不是一句想法',
'<h2>需求要能被开发和验收</h2><p>一句“做一个学习推荐功能”还不是可开发需求。需要继续澄清用户是谁、在什么场景下、要解决什么问题、成功标准是什么。</p><table><thead><tr><th>问题</th><th>示例</th></tr></thead><tbody><tr><td>用户是谁</td><td>学生、教师、管理员</td></tr><tr><td>触发场景</td><td>学生做完练习后</td></tr><tr><td>输出结果</td><td>推荐 3 个复习资源</td></tr><tr><td>验收标准</td><td>能按薄弱知识点排序</td></tr></tbody></table><blockquote>需求分析的目标是减少误解，不是写厚文档。</blockquote>'),
('software-engineering', 2, '用例与用户故事',
'<h2>从用户目标组织功能</h2><p>用户故事常用格式是：作为某类用户，我希望完成某件事，以便获得某种价值。它能提醒团队不要只从按钮和页面出发。</p><pre><code class="language-text">作为学生，
我希望看到按薄弱知识点推荐的图文教程，
以便在课后快速补齐基础概念。</code></pre><p>用例则更关注交互流程，包括前置条件、主流程、异常流程和后置结果。二者可以结合使用。</p>'),
('software-engineering', 3, '迭代计划与任务拆分',
'<h2>把大功能拆成可交付的小块</h2><p>一个大需求可以拆成数据表、接口、页面、权限、测试和上线配置。每个任务要有明确输入和完成标准。</p><ul><li>先做最小闭环：能创建、能查看、能编辑。</li><li>再补体验优化：搜索、筛选、分页。</li><li>最后补运营能力：统计、审计、批量导入。</li></ul><p>拆任务时避免“开发推荐系统”这种过大描述，改成“新增推荐记录表并提供查询接口”。</p>'),
('software-engineering', 4, '验收标准与变更管理',
'<h2>验收标准让完成变得可判断</h2><p>如果没有验收标准，功能是否完成就容易变成口头感觉。好的验收标准应该具体、可观察、可测试。</p><ul><li>管理员可以新增图文教程，并至少添加一个章节。</li><li>学生端列表能展示新增教程卡片。</li><li>阅读页能按排序展示章节目录。</li><li>重复导入不会产生同名课程。</li></ul><blockquote>需求变更不可怕，怕的是没有记录。每次变更都要说明原因、影响范围和是否调整排期。</blockquote>'),

('design-patterns', 1, '为什么需要设计模式',
'<h2>设计模式是常见问题的经验解法</h2><p>设计模式不是固定模板，更不是炫技。它的价值在于给常见变化点提供稳定结构，让代码在需求变化时更容易调整。</p><ul><li>创建型模式关注对象如何创建。</li><li>结构型模式关注对象如何组合。</li><li>行为型模式关注对象如何协作。</li></ul><blockquote>判断是否需要模式：当前代码是否已经因为变化而难维护？如果没有，不要为了模式而模式。</blockquote>'),
('design-patterns', 2, '工厂与策略模式',
'<h2>工厂负责创建，策略负责变化</h2><p>当对象创建逻辑复杂，或者需要根据类型创建不同对象时，可以考虑工厂。当同一业务流程中某一步算法经常变化时，可以考虑策略模式。</p><pre><code class="language-java">interface ScoreStrategy {
    int calculate(int rawScore);
}

class NormalScoreStrategy implements ScoreStrategy {
    public int calculate(int rawScore) {
        return rawScore;
    }
}</code></pre><p>策略模式让调用方依赖抽象，不直接写一长串 if else。以后新增评分规则，只需要新增策略实现。</p>'),
('design-patterns', 3, '观察者与发布订阅',
'<h2>一个事件触发多个后续动作</h2><p>当“课程发布”后需要通知学生、记录日志、刷新推荐、发送消息时，如果都写在一个方法里会越来越臃肿。观察者模式可以把事件源和后续处理解耦。</p><pre><code class="language-text">课程发布事件
  -> 通知学生
  -> 写入审计日志
  -> 更新学习推荐</code></pre><blockquote>观察者模式适合扩展后续动作，但要注意失败处理和执行顺序，否则排查问题会变难。</blockquote>'),
('design-patterns', 4, '适配器与装饰器',
'<h2>结构型模式解决组合问题</h2><p>适配器用于接口不兼容但又想复用已有能力的场景。装饰器用于不改变原对象的前提下添加额外功能。</p><table><thead><tr><th>模式</th><th>典型场景</th></tr></thead><tbody><tr><td>适配器</td><td>接入第三方接口，字段或方法不一致</td></tr><tr><td>装饰器</td><td>给请求处理增加日志、鉴权、缓存</td></tr></tbody></table><p>学习模式时最好结合项目代码思考：哪里变化频繁，哪里依赖混乱，哪里新增功能会牵一发动全身。</p>'),

('web-security', 1, 'XSS 跨站脚本攻击',
'<h2>XSS 是把恶意脚本带进页面</h2><p>如果用户提交的内容未经处理就显示在页面中，攻击者可能插入脚本，盗取信息或执行恶意操作。</p><pre><code class="language-html">&lt;script&gt;alert("attack")&lt;/script&gt;</code></pre><h3>防护思路</h3><ul><li>输出到页面前进行转义或净化。</li><li>富文本内容使用白名单过滤。</li><li>重要 Cookie 设置 HttpOnly。</li><li>配置内容安全策略 CSP。</li></ul><blockquote>允许用户输入 HTML 时，必须明确允许哪些标签和属性，不能简单信任前端校验。</blockquote>'),
('web-security', 2, 'SQL 注入与参数化查询',
'<h2>SQL 注入来自把输入拼进 SQL</h2><p>如果把用户输入直接拼接到 SQL 字符串中，攻击者可能改变查询含义。参数化查询能让数据库把输入当作数据，而不是 SQL 语法。</p><pre><code class="language-sql">-- 风险写法
SELECT * FROM user WHERE name = &apos;&apos; OR &apos;1&apos;=&apos;1&apos;;

-- 推荐思路：使用参数绑定
SELECT * FROM user WHERE name = ?;</code></pre><ul><li>不要拼接用户输入。</li><li>限制查询字段和排序字段白名单。</li><li>数据库账号遵循最小权限原则。</li></ul>'),
('web-security', 3, 'CSRF 与身份凭证',
'<h2>CSRF 利用的是用户已登录状态</h2><p>当用户已经登录某网站，攻击页面可能诱导浏览器带着已有 Cookie 请求目标网站。服务器如果只看 Cookie，就可能误以为是用户本人操作。</p><h3>防护方式</h3><ul><li>重要操作使用 CSRF Token。</li><li>Cookie 设置 SameSite。</li><li>敏感操作增加二次确认。</li><li>接口校验 Origin 或 Referer。</li></ul><blockquote>登录态越自动，越要关注请求是不是用户真实意图。</blockquote>'),
('web-security', 4, '权限设计与接口安全',
'<h2>认证回答你是谁，授权回答你能做什么</h2><p>很多安全问题不是没有登录，而是登录后越权访问。比如学生访问管理员接口，或普通教师修改其他教师课程。</p><ul><li>接口层必须校验角色和资源归属。</li><li>不要只在前端隐藏按钮。</li><li>敏感字段不要直接返回给前端。</li><li>操作日志能帮助事后追踪。</li></ul><pre><code class="language-text">用户身份 -> 角色权限 -> 资源归属 -> 操作审计</code></pre>'),

('testing-quality', 1, '测试金字塔',
'<h2>不同层次的测试解决不同问题</h2><p>单元测试验证小函数或小类，集成测试验证模块之间能否协作，端到端测试模拟真实用户流程。它们互相补充，不应只依赖一种。</p><table><thead><tr><th>测试</th><th>特点</th></tr></thead><tbody><tr><td>单元测试</td><td>快，定位精准</td></tr><tr><td>集成测试</td><td>能发现接口和数据层问题</td></tr><tr><td>E2E 测试</td><td>贴近真实使用，但成本高</td></tr></tbody></table><blockquote>越底层的测试越应该多，越上层的测试越应该精选关键路径。</blockquote>'),
('testing-quality', 2, '单元测试怎么写',
'<h2>单元测试关注输入和输出</h2><p>一个好的单元测试应该说明给定什么条件、执行什么操作、期望什么结果。测试名要能描述场景。</p><pre><code class="language-java">@Test
void shouldReturnPassedWhenScoreIsAbove60() {
    boolean passed = scoreService.isPassed(75);
    assertTrue(passed);
}</code></pre><ul><li>正常场景要测。</li><li>边界值要测，例如 0、60、100。</li><li>异常输入要测，例如空值、非法格式。</li></ul>'),
('testing-quality', 3, '测试用例设计',
'<h2>测试不是随便点点页面</h2><p>设计测试用例时，可以从等价类、边界值、状态变化和异常流程入手。比如登录功能不仅测正确账号，还要测密码错误、账号为空、账号被禁用。</p><table><thead><tr><th>方法</th><th>例子</th></tr></thead><tbody><tr><td>等价类</td><td>合法手机号和非法手机号</td></tr><tr><td>边界值</td><td>密码长度 5、6、20、21</td></tr><tr><td>状态转换</td><td>草稿 -> 发布 -> 下线</td></tr></tbody></table><p>好的测试用例能让缺陷更早暴露，而不是等用户遇到。</p>'),
('testing-quality', 4, '缺陷管理与回归验证',
'<h2>修复缺陷后还要防止它回来</h2><p>缺陷记录应包含复现步骤、实际结果、期望结果、环境信息和截图或日志。修复后要做回归验证，确认原问题解决且没有影响相关功能。</p><ul><li>先复现，再修复。</li><li>保留最小复现步骤。</li><li>补充自动化测试覆盖关键逻辑。</li><li>上线前检查相邻功能。</li></ul><blockquote>无法复现的缺陷不要急着关闭，先补日志和监控，争取下次能抓到证据。</blockquote>'),

('http-api', 1, 'REST 资源建模',
'<h2>接口路径应该表达资源</h2><p>REST 风格强调用路径表达资源，用 HTTP 方法表达动作。例如教程资源可以设计为 <code>/tutorials</code> 和 <code>/tutorials/{id}</code>。</p><table><thead><tr><th>方法</th><th>含义</th></tr></thead><tbody><tr><td>GET</td><td>读取资源</td></tr><tr><td>POST</td><td>创建资源或提交动作</td></tr><tr><td>PUT</td><td>整体更新</td></tr><tr><td>DELETE</td><td>删除资源</td></tr></tbody></table><p>实际项目不必机械追求纯 REST，但路径和方法要稳定、清楚、一致。</p>'),
('http-api', 2, '状态码与错误响应',
'<h2>状态码是接口的第一层语义</h2><p>前端看到状态码就应该大致知道发生了什么。业务系统还会在响应体里提供业务错误码和提示信息。</p><ul><li>200：请求成功。</li><li>400：参数错误。</li><li>401：未登录。</li><li>403：无权限。</li><li>404：资源不存在。</li><li>500：服务内部错误。</li></ul><pre><code class="language-json">{
  "code": 40001,
  "message": "课程名称不能为空",
  "data": null
}</code></pre>'),
('http-api', 3, '分页、筛选与排序',
'<h2>列表接口要控制数据规模</h2><p>如果列表接口一次返回所有数据，数据量变大后页面会慢，数据库也会吃力。分页、筛选和排序是后台系统最常见的接口能力。</p><pre><code class="language-http">GET /tutorials?page=1&pageSize=20&keyword=network&sort=createTime_desc</code></pre><ul><li>分页参数要有默认值和最大限制。</li><li>筛选字段要做白名单校验。</li><li>排序字段不能直接相信前端输入。</li></ul>'),
('http-api', 4, '接口调试与文档',
'<h2>联调问题要有证据</h2><p>接口联调时不要只说“请求失败”，而要提供请求地址、方法、参数、响应状态码、响应体和后端日志。工具可以使用浏览器 Network、Postman、curl 或 Apifox。</p><pre><code class="language-bash">curl -X GET "http://localhost:8080/tutorial/list"</code></pre><p>接口文档至少包含：功能说明、请求方法、路径、参数、响应示例、错误码。文档越清晰，沟通成本越低。</p>'),

('message-queue', 1, '为什么需要消息队列',
'<h2>异步系统解决同步链路过长的问题</h2><p>如果用户提交作业后，系统要评分、生成报告、发通知、更新统计，全部同步执行会让用户等待很久。消息队列可以先接收任务，再由后台慢慢处理。</p><pre><code class="language-text">用户请求 -> 写入任务消息 -> 立即返回
后台消费者 -> 评分 -> 报告 -> 通知</code></pre><ul><li>削峰填谷：高峰请求先排队。</li><li>系统解耦：生产者不直接依赖消费者。</li><li>失败重试：临时失败后可再次处理。</li></ul>'),
('message-queue', 2, '生产者、消费者与主题',
'<h2>消息队列的基本角色</h2><p>生产者负责发送消息，队列或主题负责保存消息，消费者负责处理消息。主题用于按业务分类，例如作业提交、课程发布、通知发送。</p><table><thead><tr><th>角色</th><th>职责</th></tr></thead><tbody><tr><td>Producer</td><td>生成并发送消息</td></tr><tr><td>Broker</td><td>存储和投递消息</td></tr><tr><td>Consumer</td><td>订阅并处理消息</td></tr></tbody></table><blockquote>消息内容要尽量清楚，通常包含事件类型、业务 id、发生时间和必要上下文。</blockquote>'),
('message-queue', 3, '重试、幂等与死信队列',
'<h2>异步系统一定要面对失败</h2><p>消费者处理消息时可能数据库超时、第三方接口失败或代码异常。简单重试能解决临时问题，但重复消费又可能造成重复扣费、重复通知等问题，所以需要幂等设计。</p><ul><li>重试：临时失败后再次处理。</li><li>幂等：同一消息处理多次结果仍一致。</li><li>死信队列：多次失败后转移，等待人工排查。</li></ul><p>常见幂等方式包括业务唯一键、处理记录表、状态机校验。</p>'),
('message-queue', 4, '消息积压与监控',
'<h2>队列积压说明处理速度跟不上生产速度</h2><p>消息积压可能来自消费者数量不足、处理逻辑变慢、数据库瓶颈或某类消息持续失败。监控要关注队列长度、消费延迟、失败次数和重试次数。</p><table><thead><tr><th>现象</th><th>可能原因</th></tr></thead><tbody><tr><td>队列长度持续上升</td><td>消费者处理太慢</td></tr><tr><td>失败消息集中</td><td>某个业务逻辑异常</td></tr><tr><td>延迟突然升高</td><td>下游服务变慢</td></tr></tbody></table><blockquote>队列不是垃圾桶。消息堆进去后，仍然要设计可观测、可恢复的处理流程。</blockquote>'),

('distributed-system', 1, '从单体到分布式',
'<h2>分布式不是为了显得高级</h2><p>当单体系统在团队协作、性能、部署频率或模块边界上遇到明显瓶颈时，才需要考虑拆分服务。拆分会带来网络调用、数据一致性、监控和运维复杂度。</p><ul><li>单体优点：简单、事务容易、部署直接。</li><li>分布式优点：独立扩展、独立部署、团队边界清晰。</li><li>分布式代价：调用失败、链路变长、数据一致性更难。</li></ul><blockquote>能用清晰模块解决的问题，不一定要拆成微服务。</blockquote>'),
('distributed-system', 2, '服务注册、发现与负载均衡',
'<h2>服务之间要知道彼此在哪里</h2><p>在分布式系统中，服务实例可能动态上下线。服务注册中心记录可用实例，服务发现让调用方找到目标服务，负载均衡决定请求发给哪个实例。</p><pre><code class="language-text">服务启动 -> 注册实例
调用方 -> 查询可用实例
负载均衡 -> 选择一个实例调用</code></pre><p>负载均衡策略包括轮询、随机、最少连接和按权重分配。实际选择要看业务特点和服务能力。</p>'),
('distributed-system', 3, '一致性与事务边界',
'<h2>跨服务事务更难</h2><p>单个数据库事务可以保证原子性，但服务拆分后，订单、库存、支付可能属于不同服务。强一致性成本高，很多业务会采用最终一致性。</p><ul><li>强一致：所有节点立即看到相同结果，成本较高。</li><li>最终一致：短时间内可能不一致，但最终会收敛。</li><li>补偿机制：失败后执行反向操作或修正状态。</li></ul><blockquote>设计分布式流程时，先定义哪些状态可以短暂不一致，哪些必须强一致。</blockquote>'),
('distributed-system', 4, '熔断、限流与链路追踪',
'<h2>系统要能优雅地失败</h2><p>当下游服务变慢或失败时，上游不能无限等待。熔断可以暂时停止调用故障服务，限流可以保护系统不被流量压垮，链路追踪帮助定位慢在哪一段。</p><table><thead><tr><th>机制</th><th>作用</th></tr></thead><tbody><tr><td>熔断</td><td>避免故障扩散</td></tr><tr><td>限流</td><td>控制入口流量</td></tr><tr><td>降级</td><td>保留核心功能</td></tr><tr><td>追踪</td><td>定位跨服务问题</td></tr></tbody></table>'),

('nginx-gateway', 1, 'Nginx 能做什么',
'<h2>Nginx 常见角色</h2><p>Nginx 可以作为静态资源服务器、反向代理、负载均衡入口和 HTTPS 终止点。它经常位于用户请求和后端服务之间。</p><pre><code class="language-text">用户 -> Nginx -> 前端静态文件
用户 -> Nginx -> 后端 API 服务</code></pre><ul><li>托管前端构建后的 dist 文件。</li><li>把 /api 请求转发到后端。</li><li>给多个后端实例做负载均衡。</li><li>配置 HTTPS 证书。</li></ul>'),
('nginx-gateway', 2, '反向代理配置',
'<h2>反向代理隐藏真实后端地址</h2><p>用户只访问 Nginx，Nginx 再把请求转发到后端服务。这样前端域名、跨域、证书和后端端口都能集中管理。</p><pre><code class="language-nginx">location /api/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}</code></pre><p>排查代理问题时，重点看路径是否被正确保留、后端是否可达、请求头是否需要转发。</p>'),
('nginx-gateway', 3, '负载均衡与健康检查',
'<h2>多个实例共同承担流量</h2><p>负载均衡可以把请求分配给多个后端实例，提高吞吐和可用性。基础配置可以用 upstream 定义服务列表。</p><pre><code class="language-nginx">upstream teach_api {
    server 127.0.0.1:8080;
    server 127.0.0.1:8081;
}

location /api/ {
    proxy_pass http://teach_api/;
}</code></pre><blockquote>负载均衡不是万能。如果数据库是瓶颈，只增加应用实例可能效果有限。</blockquote>'),
('nginx-gateway', 4, 'HTTPS 与常见故障',
'<h2>HTTPS 保护传输过程</h2><p>HTTPS 通过 TLS 加密浏览器和服务器之间的数据。配置时要关注证书文件、私钥文件、域名是否匹配以及证书是否过期。</p><table><thead><tr><th>故障</th><th>检查点</th></tr></thead><tbody><tr><td>证书不可信</td><td>证书链和颁发机构</td></tr><tr><td>域名不匹配</td><td>证书绑定域名</td></tr><tr><td>502 Bad Gateway</td><td>后端服务是否正常</td></tr><tr><td>静态资源 404</td><td>root 和 try_files 配置</td></tr></tbody></table>'),

('shell-automation', 1, '变量、参数与退出码',
'<h2>Shell 脚本把命令串成流程</h2><p>脚本可以接收参数、设置变量、执行命令并根据结果决定下一步。退出码为 0 通常表示成功，非 0 表示失败。</p><pre><code class="language-bash">#!/usr/bin/env bash
name="$1"
echo "hello $name"
echo "last status: $?"</code></pre><ul><li><code>$1</code> 表示第一个参数。</li><li><code>$?</code> 表示上一条命令退出码。</li><li>变量引用建议加双引号，避免空格导致问题。</li></ul>'),
('shell-automation', 2, '条件与循环',
'<h2>脚本也需要控制流程</h2><p>条件判断适合检查文件是否存在、参数是否为空、命令是否成功。循环适合批量处理文件或重复执行任务。</p><pre><code class="language-bash">if [ -f "app.log" ]; then
  echo "log exists"
fi

for file in *.log; do
  echo "$file"
done</code></pre><blockquote>写脚本时先在少量测试文件上运行，确认无误后再处理大量文件。</blockquote>'),
('shell-automation', 3, '日志清理与备份脚本',
'<h2>自动化从小任务开始</h2><p>日志清理、数据库备份、构建打包、服务重启都适合用脚本规范起来。脚本要输出关键日志，方便失败时排查。</p><pre><code class="language-bash">backup_dir="./backup/$(date +%Y%m%d)"
mkdir -p "$backup_dir"
cp app.log "$backup_dir/app.log"</code></pre><ul><li>关键路径使用变量保存。</li><li>每个危险操作前打印目标。</li><li>失败时及时退出，避免继续造成连锁问题。</li></ul>'),
('shell-automation', 4, '脚本安全习惯',
'<h2>自动化越强，越要谨慎</h2><p>脚本可能批量修改或删除文件，因此要养成安全习惯。尤其是删除、移动、覆盖文件前，要确认变量不为空、路径在预期目录内。</p><ul><li>使用 <code>set -euo pipefail</code> 提前暴露错误。</li><li>给重要变量设置默认值或显式检查。</li><li>危险操作先 dry run。</li><li>记录执行时间、操作者和目标路径。</li></ul><blockquote>脚本不是临时命令的简单堆叠，它也是需要维护的代码。</blockquote>'),

('compiler-basic', 1, '编译器整体流程',
'<h2>编译器把源代码翻译成目标代码</h2><p>编译过程通常包括词法分析、语法分析、语义分析、中间代码生成、优化和目标代码生成。每一步都把程序从一种形式转换成另一种形式。</p><pre><code class="language-text">源码 -> Token -> 语法树 -> 中间代码 -> 目标代码</code></pre><p>理解编译流程后，错误信息会更有层次：有些是词法错误，有些是语法错误，有些是类型或作用域错误。</p>'),
('compiler-basic', 2, '词法分析与 Token',
'<h2>词法分析把字符流切成记号</h2><p>源代码本质上是一串字符。词法分析器会识别关键字、标识符、数字、字符串、运算符和分隔符，形成 Token 序列。</p><pre><code class="language-text">int age = 18;

Token:
KEYWORD(int), IDENT(age), OP(=), NUMBER(18), SEMI(;)</code></pre><p>词法错误通常发生在非法字符、字符串没有闭合、数字格式不正确等场景。</p>'),
('compiler-basic', 3, '语法树与语义检查',
'<h2>语法分析关心结构是否正确</h2><p>语法分析会根据语言文法构建抽象语法树 AST。语义检查进一步确认变量是否声明、类型是否匹配、函数调用参数是否正确。</p><pre><code class="language-text">赋值语句
├─ 左侧: age
└─ 右侧: 18</code></pre><ul><li>语法错误：括号不匹配、缺少分号。</li><li>语义错误：变量未定义、类型不兼容。</li></ul><blockquote>代码编辑器中的很多红线提示，其实就是在做轻量级语法和语义分析。</blockquote>'),
('compiler-basic', 4, '优化与运行时',
'<h2>优化不是改变含义，而是改善执行</h2><p>编译器优化会在保持程序语义不变的前提下减少重复计算、删除无用代码、改善寄存器使用。运行时还要处理函数调用、栈帧、堆内存和异常。</p><ul><li>常量折叠：提前计算固定表达式。</li><li>死代码删除：移除永远不会执行的代码。</li><li>内联：减少函数调用开销。</li></ul><p>学习编译原理能帮助你更深地理解语言特性，也能提升阅读报错和性能分析的能力。</p>'),

('oop-modeling', 1, '对象建模从职责开始',
'<h2>先找职责，再找类</h2><p>面向对象建模不是把数据库表照搬成类，而是识别系统里有哪些对象、它们承担什么职责、彼此如何协作。</p><ul><li>学生：查看课程、提交作业、阅读教程。</li><li>教师：发布任务、批改作业、查看分析。</li><li>教程：保存封面、简介、章节内容。</li></ul><blockquote>一个类如果同时负责太多事情，后期会很难修改。职责清楚比类数量少更重要。</blockquote>'),
('oop-modeling', 2, '类图怎么读',
'<h2>类图表达静态结构</h2><p>类图展示类、字段、方法以及类之间的关系。常见关系包括关联、聚合、组合、继承和实现。</p><pre><code class="language-text">TutorialCourse
  - id
  - name
  + getChapters()

TutorialChapter
  - title
  - content</code></pre><table><thead><tr><th>关系</th><th>含义</th></tr></thead><tbody><tr><td>继承</td><td>是一种</td></tr><tr><td>组合</td><td>整体拥有部分，生命周期强相关</td></tr><tr><td>关联</td><td>知道或使用另一个对象</td></tr></tbody></table>'),
('oop-modeling', 3, '时序图表达交互',
'<h2>时序图关注谁先调用谁</h2><p>当一个功能涉及前端、控制器、服务、数据库等多个对象时，时序图能帮助团队看清请求流动顺序。</p><pre><code class="language-text">用户 -> 前端页面 -> Controller -> Service -> Mapper -> 数据库
数据库 -> Mapper -> Service -> Controller -> 前端页面 -> 用户</code></pre><p>时序图特别适合讲清楚登录、支付、提交作业、生成报告等有明确时间顺序的流程。</p>'),
('oop-modeling', 4, '从模型到代码',
'<h2>模型最终要服务实现</h2><p>建模不是为了画漂亮图，而是为了让代码结构更清楚。模型完成后，可以映射到包结构、类职责、接口定义和数据库关系。</p><ul><li>类图帮助确定核心类和关系。</li><li>时序图帮助确定接口调用顺序。</li><li>状态图帮助处理发布、审核、下线等状态变化。</li><li>用例图帮助确认功能边界。</li></ul><blockquote>模型不需要一次画到完美。随着需求变清楚，模型也应该迭代。</blockquote>');

INSERT INTO text_course (name, cover_img, description, create_time)
SELECT c.name, c.cover_img, c.description, NOW()
FROM tmp_more_cs_course c
WHERE NOT EXISTS (
    SELECT 1
    FROM text_course tc
    WHERE tc.name = c.name
);

INSERT INTO text_node (course_id, title, content, sort_order, create_time)
SELECT tc.id, n.title, n.content, n.sort_order, NOW()
FROM tmp_more_cs_node n
JOIN tmp_more_cs_course c ON c.slug = n.slug
JOIN text_course tc ON tc.name = c.name
WHERE NOT EXISTS (
    SELECT 1
    FROM text_node tn
    WHERE tn.course_id = tc.id
      AND tn.title = n.title
);

DROP TEMPORARY TABLE IF EXISTS tmp_more_cs_node;
DROP TEMPORARY TABLE IF EXISTS tmp_more_cs_course;

COMMIT;
