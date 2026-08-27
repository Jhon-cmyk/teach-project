-- ============================================================
-- 社区讨论种子数据：为教师端"待加入精选的讨论"提供演示数据
-- 使用方式：在数据库中执行此脚本
-- 效果：插入 4 条有教师回复的讨论帖，会出现在社区处理台的"待加入精选"中
-- ============================================================

-- 1. 计算机网络 - 静态路由问题
INSERT INTO community_post (title, content, post_type, course_id, course_name, user_id, author_name, status, is_hot, is_teacher_answered, view_count, reply_count, last_active_time, create_time, update_time, is_delete)
VALUES (
  '配置了静态路由后为什么还是ping不通对端？',
  '按照实验手册在两台路由器上分别配置了静态路由，show ip route 也能看到路由条目，但从 PC1 ping PC2 就是超时。已经确认接口都是 up 状态，请问还需要检查什么？',
  'discussion', 102, '计算机网络', 1, '张同学',
  'open', 0, 0, 187, 0, NOW(), DATE_SUB(NOW(), INTERVAL 10 HOUR), NOW(), 0
);
SET @post1_id = LAST_INSERT_ID();
INSERT INTO community_reply (post_id, user_id, author_name, content, is_teacher, create_time, update_time, is_delete)
VALUES (@post1_id, 2, '王老师',
  '静态路由常见的踩坑点：1）路由回指 — 静态路由是单向的，对端路由器也必须有一条指向你这边网段的路由。2）检查两边接口的 IP 是否在同一广播域，特别是串行链路要确认封装一致。3）用 extended ping 指定源地址测试，能更准确判断问题在哪里。你先 show run 把两端配置贴出来看看。',
  1, DATE_SUB(NOW(), INTERVAL 8 HOUR), NOW(), 0);
SET @reply1_id = LAST_INSERT_ID();
-- 更新帖子统计数据
UPDATE community_post SET reply_count = reply_count + 1, is_teacher_answered = 1, last_active_time = DATE_SUB(NOW(), INTERVAL 8 HOUR), update_time = NOW() WHERE id = @post1_id;

-- 2. Python - GIL 问题
INSERT INTO community_post (title, content, post_type, course_id, course_name, user_id, author_name, status, is_hot, is_teacher_answered, view_count, reply_count, last_active_time, create_time, update_time, is_delete)
VALUES (
  'Python多线程处理CPU密集型任务为什么反而更慢？',
  '写了一个图片批处理脚本，用 threading.Thread 开了 8 个线程并行处理图片缩放，结果比单线程还慢。换成 multiprocessing 后速度明显提升。想知道背后的原理，以及以后怎么判断该用哪个。',
  'discussion', 103, 'Python核心进阶实战', 3, '李同学',
  'open', 1, 0, 892, 0, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 26 HOUR), NOW(), 0
);
SET @post2_id = LAST_INSERT_ID();
INSERT INTO community_reply (post_id, user_id, author_name, content, is_teacher, create_time, update_time, is_delete)
VALUES (@post2_id, 2, '李老师',
  '核心原因是 GIL（全局解释器锁）。CPython 的 GIL 确保同一时刻只有一个线程能执行 Python 字节码，所以 CPU 密集型任务的多线程其实是在"并发"而非"并行"，还要加上线程切换的开销，自然更慢。多进程每个进程有独立 GIL，能真正利用多核。\n\n判断原则很简单：\n- CPU 密集型（计算、图片处理、视频编码）→ 用 multiprocessing\n- IO 密集型（网络请求、文件读写、数据库查询）→ 用 threading 或 asyncio\n\n你的场景是图片处理，属于 CPU 密集型，用多进程是正确的选择。',
  1, DATE_SUB(NOW(), INTERVAL 24 HOUR), NOW(), 0);
SET @reply2_id = LAST_INSERT_ID();
UPDATE community_post SET reply_count = reply_count + 1, is_teacher_answered = 1, last_active_time = DATE_SUB(NOW(), INTERVAL 24 HOUR), update_time = NOW() WHERE id = @post2_id;

-- 3. 数据结构 - 哈希表冲突
INSERT INTO community_post (title, content, post_type, course_id, course_name, user_id, author_name, status, is_hot, is_teacher_answered, view_count, reply_count, last_active_time, create_time, update_time, is_delete)
VALUES (
  'Java HashMap 在大量哈希冲突时的性能退化问题',
  '在做大作业时发现 HashMap 在存储大量自定义对象时 get 操作变得很慢，debug 发现很多 key 都映射到了同一个桶。我已经重写了 hashCode() 方法，但似乎没改善。想问下正确的做法是什么？链地址法在链表过长时是不是会退化成 O(n)？',
  'discussion', 101, '数据结构', 4, '赵同学',
  'open', 0, 0, 423, 0, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 16 HOUR), NOW(), 0
);
SET @post3_id = LAST_INSERT_ID();
INSERT INTO community_reply (post_id, user_id, author_name, content, is_teacher, create_time, update_time, is_delete)
VALUES (@post3_id, 2, '陈老师',
  '好问题。先说结论：Java 8 以后当链表长度超过 8 时会自动转为红黑树，最坏复杂度从 O(n) 降到 O(log n)，所以一般不用担心。\n\n但你的 hashCode() 写得不好确实会影响性能。正确的做法是：\n1. 用所有重要字段参与计算，用 31 作为乘数：`return Objects.hash(field1, field2, field3)`\n2. 确保不相等的对象尽量产生不同的 hashCode\n3. 如果自定义类作为 key，务必同时正确重写 equals()\n\n你把自定义类的 hashCode 和 equals 贴出来，我帮你看看问题在哪。',
  1, DATE_SUB(NOW(), INTERVAL 14 HOUR), NOW(), 0);
SET @reply3_id = LAST_INSERT_ID();
UPDATE community_post SET reply_count = reply_count + 1, is_teacher_answered = 1, last_active_time = DATE_SUB(NOW(), INTERVAL 14 HOUR), update_time = NOW() WHERE id = @post3_id;

-- 4. 数据库原理 - 事务隔离级别
INSERT INTO community_post (title, content, post_type, course_id, course_name, user_id, author_name, status, is_hot, is_teacher_answered, view_count, reply_count, last_active_time, create_time, update_time, is_delete)
VALUES (
  'MySQL RR 隔离级别下真的不会出现幻读吗？',
  '课上讲 InnoDB 默认隔离级别是 REPEATABLE READ，通过 MVCC + next-key lock 解决了幻读。但我做了一个实验：在 RR 下开启事务 A 执行 SELECT，事务 B INSERT 一条新数据并提交，事务 A 再次 SELECT 确实读不到新数据。但如果事务 A 执行 UPDATE 所有行，再 SELECT 就能看到那条新数据了。这是不是说明 RR 并没有完全解决幻读？',
  'discussion', 105, '数据库原理', 5, '钱同学',
  'open', 1, 0, 678, 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 20 HOUR), NOW(), 0
);
SET @post4_id = LAST_INSERT_ID();
INSERT INTO community_reply (post_id, user_id, author_name, content, is_teacher, create_time, update_time, is_delete)
VALUES (@post4_id, 2, '张老师',
  '非常敏锐的观察！你遇到的正是 RR 下"幻读"的边界情况，MySQL 官方文档称之为"phantom reads in UPDATE"。\n\n解释一下：RR 下的 consistent read（快照读）通过 MVCC 确实看不见新插入的行。但 **UPDATE 属于当前读（current read）**，它读到的是最新的已提交数据，然后在这个最新基础上加锁更新。所以 UPDATE 之后，这行就"进入"了你的事务视野。\n\n严格来说这不是幻读的经典定义（幻读指同一查询在不同时间返回不同行集），但这种行为确实超出了很多人的预期。如果业务上需要完全防止这种情况，可以用 SERIALIZABLE 隔离级别，或者配合显式锁（SELECT ... FOR UPDATE）来控制。\n\n给这个思考题打个满分！',
  1, DATE_SUB(NOW(), INTERVAL 18 HOUR), NOW(), 0);
SET @reply4_id = LAST_INSERT_ID();
UPDATE community_post SET reply_count = reply_count + 1, is_teacher_answered = 1, last_active_time = DATE_SUB(NOW(), INTERVAL 18 HOUR), update_time = NOW() WHERE id = @post4_id;
