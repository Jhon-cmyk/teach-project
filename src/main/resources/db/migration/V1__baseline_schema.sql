-- Flyway baseline schema generated from the verified local MySQL structure on 2026-07-24.
-- Schema only: no users, courses, submissions, demo content, or credentials are included.
-- Existing non-empty databases use Flyway baseline-on-migrate and do not execute V1.

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL,
  `admin_account` varchar(100) NOT NULL,
  `admin_name` varchar(100) DEFAULT NULL,
  `module` varchar(80) NOT NULL,
  `action` varchar(80) NOT NULL,
  `target_type` varchar(80) DEFAULT NULL,
  `target_id` varchar(100) DEFAULT NULL,
  `summary` varchar(1000) DEFAULT NULL,
  `request_ip` varchar(80) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_audit_time` (`create_time`),
  KEY `idx_admin_audit_admin` (`admin_id`),
  KEY `idx_admin_audit_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='admin operation audit log';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_import_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `import_type` varchar(40) NOT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `created_count` int NOT NULL DEFAULT '0',
  `skipped_count` int NOT NULL DEFAULT '0',
  `error_count` int NOT NULL DEFAULT '0',
  `error_json` mediumtext,
  `status` varchar(30) NOT NULL DEFAULT 'success',
  `admin_id` bigint DEFAULT NULL,
  `admin_account` varchar(100) DEFAULT NULL,
  `admin_name` varchar(100) DEFAULT NULL,
  `request_ip` varchar(80) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_import_time` (`create_time`),
  KEY `idx_admin_import_type` (`import_type`),
  KEY `idx_admin_import_admin` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='admin data import batch records';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `interface_key` varchar(64) NOT NULL COMMENT 'stable interface key',
  `interface_name` varchar(80) NOT NULL COMMENT 'display name',
  `provider` varchar(60) DEFAULT NULL COMMENT 'model provider',
  `endpoint_url` varchar(500) NOT NULL COMMENT 'model endpoint url',
  `model_name` varchar(120) NOT NULL COMMENT 'model name',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1 enabled, 0 disabled',
  `remark` varchar(255) DEFAULT NULL COMMENT 'admin remark',
  `sort_order` int NOT NULL DEFAULT '0',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_config_key` (`interface_key`),
  KEY `idx_ai_model_config_enabled` (`enabled`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI model endpoint configuration';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师用户ID',
  `type` varchar(20) NOT NULL COMMENT '资源类型: plan / quiz / anim',
  `title` varchar(255) NOT NULL COMMENT '资源标题',
  `content` longtext NOT NULL COMMENT '资源内容(Markdown文本 或 HTML源码)',
  `params_json` text COMMENT '生成时的参数快照(JSON), 方便回溯',
  `is_published` tinyint DEFAULT '0' COMMENT '是否发布到资源库(0否1是)',
  `is_delete` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `source_id` bigint DEFAULT NULL COMMENT '来源资源ID',
  `source_type` varchar(32) DEFAULT NULL COMMENT '来源资源类型',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_type` (`teacher_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI备课室生成资源表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_analysis_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `audio_url` varchar(1024) NOT NULL COMMENT '音频文件云端链接',
  `plan_text` longtext COMMENT '教案文本(供AI参考)',
  `plan_resource_id` bigint DEFAULT NULL COMMENT '关联教案资源ID',
  `plan_title_snapshot` varchar(255) DEFAULT NULL COMMENT '教案标题快照',
  `transcript_json` longtext COMMENT '完整的语音转写JSON(含说话人、时间戳)',
  `ai_report` longtext COMMENT 'AI最终生成的Markdown评课报告',
  `status` varchar(50) DEFAULT 'pending' COMMENT '状态: pending/transcribing/analyzing/completed',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级评课记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coding_problem` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '题目标题',
  `description` mediumtext NOT NULL COMMENT '题目描述(Markdown)',
  `difficulty` varchar(20) NOT NULL DEFAULT 'medium' COMMENT 'easy/medium/hard',
  `languages` json NOT NULL COMMENT '支持的语言列表 ["java","python","cpp","javascript"]',
  `time_limit_ms` int NOT NULL DEFAULT '5000' COMMENT '单次运行超时(ms)',
  `memory_limit_kb` int NOT NULL DEFAULT '262144' COMMENT '单次运行内存上限(KB)',
  `graph_node_id` bigint unsigned DEFAULT NULL COMMENT '关联图谱节点(预留)',
  `course_id` bigint unsigned DEFAULT NULL COMMENT '所属课程',
  `semester_label` varchar(50) DEFAULT NULL COMMENT '所属学年学期，如 2025-2026-1',
  `creator_id` bigint unsigned NOT NULL COMMENT '创建教师ID',
  `is_public` tinyint NOT NULL DEFAULT '0' COMMENT '0=私有 1=公开',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_semester_label` (`semester_label`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编程题';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coding_problem_publish` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `problem_id` bigint unsigned NOT NULL COMMENT '题目ID',
  `class_id` bigint unsigned NOT NULL COMMENT '发布班级ID',
  `chapter_id` bigint unsigned DEFAULT NULL COMMENT '关联章节ID',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `created_by` bigint unsigned NOT NULL COMMENT '发布教师ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_problem_id` (`problem_id`),
  KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编程题发布记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coding_problem_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `problem_id` bigint unsigned NOT NULL COMMENT '题目ID',
  `language` varchar(30) NOT NULL COMMENT '语言标识: java/python/cpp/javascript',
  `starter_code` mediumtext COMMENT '学生初始代码模板',
  `reference_solution` mediumtext COMMENT '参考解答',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problem_lang` (`problem_id`,`language`),
  KEY `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编程题多语言模板';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coding_submission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `problem_id` bigint unsigned NOT NULL COMMENT '题目ID',
  `publish_id` bigint unsigned DEFAULT NULL COMMENT '发布记录ID',
  `student_id` bigint unsigned NOT NULL COMMENT '学生ID',
  `language` varchar(30) NOT NULL COMMENT '提交语言',
  `code` mediumtext NOT NULL COMMENT '学生提交代码',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/judged/error',
  `passed_count` int NOT NULL DEFAULT '0' COMMENT '通过用例数',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '总用例数',
  `test_score` int NOT NULL DEFAULT '0' COMMENT '测试用例得分(0-100)',
  `ai_score` int NOT NULL DEFAULT '0' COMMENT 'AI评审得分(0-100)',
  `final_score` int NOT NULL DEFAULT '0' COMMENT '最终加权得分(0-100)',
  `ai_review_md` mediumtext COMMENT 'AI评审报告(Markdown)',
  `runtime_ms` int DEFAULT NULL COMMENT '运行耗时(ms)',
  `memory_kb` int DEFAULT NULL COMMENT '内存占用(KB)',
  `judge_detail` json DEFAULT NULL COMMENT '逐用例判定详情',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_problem_id` (`problem_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_publish_id` (`publish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编程题提交记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coding_test_case` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `problem_id` bigint unsigned NOT NULL COMMENT '题目ID',
  `input` mediumtext COMMENT '标准输入',
  `expected_output` mediumtext NOT NULL COMMENT '期望输出',
  `is_sample` tinyint NOT NULL DEFAULT '0' COMMENT '1=样例用例(学生可见) 0=隐藏用例',
  `score` int NOT NULL DEFAULT '0' COMMENT '该用例分值权重',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编程题测试用例';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_featured_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '精选ID',
  `post_id` bigint NOT NULL COMMENT '关联帖子ID',
  `reply_id` bigint DEFAULT NULL COMMENT '关联回复ID（精选的那条回复）',
  `teacher_id` bigint DEFAULT NULL COMMENT '答疑教师ID',
  `teacher_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教师名称（冗余）',
  `excerpt` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '精选回答摘要',
  `is_recommended` tinyint NOT NULL DEFAULT '0' COMMENT '是否推荐: 0-否 1-是',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重（越大越靠前）',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_sort_order` (`sort_order` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答疑精选表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '接收通知的用户ID',
  `type` varchar(64) NOT NULL COMMENT '通知类型: post_replied / post_resolved / post_featured / followed_discussion_updated',
  `post_id` bigint NOT NULL COMMENT '关联帖子ID',
  `reply_id` bigint DEFAULT NULL COMMENT '关联回复ID，可选',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` varchar(500) NOT NULL COMMENT '通知内容',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读: 0未读 1已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_read_time` (`user_id`,`is_read`,`create_time`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区动态提醒';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `title` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '正文内容',
  `post_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'discussion' COMMENT '帖子类型: discussion-讨论 / homework-作业互助',
  `course_id` bigint DEFAULT NULL COMMENT '关联课程ID',
  `course_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程名称（冗余）',
  `user_id` bigint DEFAULT NULL COMMENT '发帖人ID',
  `author_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发帖人名称（冗余）',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'open' COMMENT '状态: open-待解决 / resolved-已解决（仅 homework 类型有意义）',
  `is_hot` tinyint NOT NULL DEFAULT '0' COMMENT '是否热门: 0-否 1-是',
  `is_teacher_answered` tinyint NOT NULL DEFAULT '0' COMMENT '是否有老师回答: 0-否 1-是',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览数',
  `reply_count` int NOT NULL DEFAULT '0' COMMENT '回复数',
  `last_active_time` datetime DEFAULT NULL COMMENT '最后活跃时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_type` (`post_type`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_last_active` (`last_active_time` DESC),
  KEY `idx_create_time` (`create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区帖子表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `community_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回复ID',
  `post_id` bigint NOT NULL COMMENT '关联帖子ID',
  `user_id` bigint DEFAULT NULL COMMENT '回复人ID',
  `author_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回复人名称（冗余）',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回复内容',
  `is_teacher` tinyint NOT NULL DEFAULT '0' COMMENT '是否教师回复: 0-否 1-是',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区回复表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(256) NOT NULL COMMENT '课程名称',
  `description` varchar(1024) DEFAULT NULL COMMENT '课程简介',
  `coverImg` varchar(1024) DEFAULT NULL COMMENT '课程封面URL',
  `videoUrl` varchar(1024) DEFAULT NULL COMMENT '课程视频URL',
  `type` varchar(256) DEFAULT 'video' COMMENT '课程类型：video/text',
  `teacherId` bigint NOT NULL COMMENT '发布教师的ID',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `points_cost` int DEFAULT '0' COMMENT '解锁所需积分',
  `teacherName` varchar(255) DEFAULT '金牌讲师' COMMENT '讲师名称',
  `sourceType` varchar(32) NOT NULL DEFAULT 'teacher' COMMENT '课程来源: platform / teacher',
  `creatorId` bigint DEFAULT NULL COMMENT '创建人ID',
  `creatorRole` varchar(32) NOT NULL DEFAULT 'teacher' COMMENT '创建人角色: admin / teacher',
  `publishStatus` varchar(32) NOT NULL DEFAULT 'published' COMMENT '发布状态: draft / published / offline',
  `categoryId` bigint DEFAULT NULL COMMENT 'course category id',
  `price` int DEFAULT '0' COMMENT '价格',
  `pointsCost` int DEFAULT '0' COMMENT '积分造价',
  `video_context` text COMMENT '视频核心知识库(用于AI上下文)',
  `face_detection_required` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Whether students must enable face detection while learning',
  PRIMARY KEY (`id`),
  KEY `idx_teacherId` (`teacherId`),
  KEY `idx_course_sourceType` (`sourceType`),
  KEY `idx_course_publishStatus` (`publishStatus`),
  KEY `idx_course_creatorId` (`creatorId`),
  KEY `idx_course_category_id` (`categoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '分类名称',
  `icon_url` varchar(1024) NOT NULL COMMENT '分类图标地址',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值，越小越靠前',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_category_name` (`name`),
  KEY `idx_course_category_enabled_sort` (`is_enabled`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程分类图标表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_chapter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '章节选集ID',
  `course_id` bigint NOT NULL COMMENT '关联的课程ID (course表主键)',
  `title` varchar(255) NOT NULL COMMENT '章节标题 (例如: 01. Java环境搭建)',
  `video_url` varchar(1024) NOT NULL COMMENT '本集视频链接',
  `anim_html` longtext COMMENT '交互课件HTML源码',
  `sort_order` int DEFAULT '0' COMMENT '播放顺序 (越小越靠前)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0-未删, 1-已删)',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`) COMMENT '加快按课程查询选集的速度'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程章节(多集)视频表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_class_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '关联 course 表的 id',
  `class_id` bigint NOT NULL COMMENT '关联 sys_class 表的 id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '排课时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_class` (`course_id`,`class_id`) COMMENT '防止同一门课给同一个班重复排'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程-班级排课映射表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '所属课程ID',
  `user_id` bigint NOT NULL COMMENT '发表用户的ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '用户昵称(冗余字段方便查询)',
  `user_avatar` varchar(255) DEFAULT NULL COMMENT '用户头像(冗余)',
  `content` text NOT NULL COMMENT '评论内容',
  `likes` int DEFAULT '0' COMMENT '点赞总数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发表时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程观点评论表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_favour` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_graph_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `source` varchar(64) NOT NULL COMMENT '源节点ID',
  `target` varchar(64) NOT NULL COMMENT '目标节点ID',
  `relationType` varchar(32) NOT NULL DEFAULT 'normal' COMMENT 'normal / prerequisite / contains / related / easyToConfuseWith / appliesTo',
  `description` varchar(255) DEFAULT NULL COMMENT '关系说明',
  `sortOrder` int NOT NULL DEFAULT '0' COMMENT '排序值',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_graph_link` (`source`,`target`),
  KEY `idx_course_graph_link_source` (`source`),
  KEY `idx_course_graph_link_target` (`target`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程图谱关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_graph_node` (
  `id` varchar(64) NOT NULL COMMENT '图谱节点ID',
  `parentId` varchar(64) DEFAULT NULL COMMENT '父节点 id，null 表示根节点',
  `name` varchar(128) NOT NULL COMMENT '节点名称',
  `category` varchar(64) NOT NULL COMMENT '所属分类名称',
  `symbolSize` int NOT NULL DEFAULT '42' COMMENT '节点尺寸',
  `description` text COMMENT '节点教学简介',
  `difficulty` varchar(16) NOT NULL DEFAULT 'medium' COMMENT 'low / medium / high',
  `importance` varchar(16) NOT NULL DEFAULT 'medium' COMMENT 'low / medium / high',
  `estimatedHours` int NOT NULL DEFAULT '4' COMMENT '预计学时',
  `teachingWeek` int NOT NULL DEFAULT '1' COMMENT '建议周次',
  `commonMistakes` text COMMENT '常见误区JSON数组',
  `teachingTips` text COMMENT '教学建议JSON数组',
  `resourceCount` int NOT NULL DEFAULT '0' COMMENT '节点资源数量摘要',
  `exerciseCount` int NOT NULL DEFAULT '0' COMMENT '节点练习数量摘要',
  `isCore` tinyint NOT NULL DEFAULT '0' COMMENT '是否核心节点',
  `isKeyPoint` tinyint NOT NULL DEFAULT '0' COMMENT '是否重点节点',
  `resourceSummary` text COMMENT '节点资源摘要',
  `resourceTypes` text COMMENT '资源类型JSON数组',
  `learnUrl` varchar(512) DEFAULT NULL COMMENT '叶子节点学习链接 URL',
  `learningContent` longtext COMMENT '学习内容正文（HTML）',
  `sortOrder` int NOT NULL DEFAULT '0' COMMENT '排序值',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程图谱节点表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_graph_node_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nodeId` varchar(64) NOT NULL COMMENT '鐭ヨ瘑鍥捐氨鑺傜偣ID',
  `teacherId` bigint NOT NULL COMMENT '缁戝畾鏁欏笀ID',
  `activityType` varchar(32) NOT NULL COMMENT '娲诲姩绫诲瀷: homework / practice / coding',
  `activityId` bigint NOT NULL COMMENT '娲诲姩ID',
  `activityTitle` varchar(255) DEFAULT NULL COMMENT '娲诲姩鏍囬?蹇?収',
  `weight` int NOT NULL DEFAULT '1' COMMENT '鏉冮噸',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_activity` (`nodeId`,`activityType`,`activityId`),
  KEY `idx_nodeId` (`nodeId`),
  KEY `idx_teacherId` (`teacherId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='鐭ヨ瘑鍥捐氨鑺傜偣-瀛︿範娲诲姩缁戝畾';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_graph_node_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `studentId` bigint NOT NULL COMMENT '瀛︾敓ID',
  `nodeId` varchar(64) NOT NULL COMMENT '鐭ヨ瘑鍥捐氨鑺傜偣ID',
  `completionRate` int NOT NULL DEFAULT '0' COMMENT '瀹屾垚鐜?0-100',
  `masteryRate` int NOT NULL DEFAULT '0' COMMENT '鎺屾彙鐜?0-100',
  `studyMinutes` int NOT NULL DEFAULT '0' COMMENT '瀛︿範鏃堕暱(鍒嗛挓)',
  `lastStudyTime` datetime DEFAULT NULL COMMENT '鏈?悗瀛︿範鏃堕棿',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_node` (`studentId`,`nodeId`),
  KEY `idx_studentId` (`studentId`),
  KEY `idx_nodeId` (`nodeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='瀛︾敓鐭ヨ瘑鐐瑰?涔犺繘搴';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_graph_preference` (
  `teacherId` bigint NOT NULL COMMENT '教师用户ID',
  `focusedNodeIds` text COMMENT '教学关注节点ID列表(JSON数组)',
  `recentVisitedNodeIds` text COMMENT '最近访问节点ID列表(JSON数组)',
  `recentEditedNodeIds` text COMMENT '最近编辑节点ID列表(JSON数组)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`teacherId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师课程图谱偏好表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_graph_resource_link` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `node_id` varchar(128) NOT NULL COMMENT '图谱节点ID',
  `resource_id` bigint NOT NULL COMMENT 'AI资源ID',
  `resource_type` varchar(32) NOT NULL COMMENT '资源类型: plan / quiz / anim',
  `relevance_score` int NOT NULL DEFAULT '100' COMMENT '推荐权重，越大越靠前',
  `source` varchar(64) NOT NULL DEFAULT 'graph-workflow' COMMENT '绑定来源',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_node_resource` (`teacher_id`,`node_id`,`resource_id`),
  KEY `idx_teacher_node` (`teacher_id`,`node_id`),
  KEY `idx_teacher_resource` (`teacher_id`,`resource_id`),
  KEY `idx_teacher_type` (`teacher_id`,`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程图谱节点-资源映射表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_mindmap` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `title` varchar(255) NOT NULL COMMENT '导图标题',
  `mindmap_json` longtext NOT NULL COMMENT '导图JSON',
  `source_hash` varchar(64) NOT NULL COMMENT '课程+章节摘要hash',
  `status` varchar(32) NOT NULL DEFAULT 'ready' COMMENT 'ready/fallback',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_id` (`course_id`),
  KEY `idx_source_hash` (`source_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程总思维导图缓存表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `external_resource_bookmark` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `teacher_id` bigint NOT NULL COMMENT '淇濆瓨鏁欏笀ID',
  `platform` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '澶栭儴骞冲彴: github/gitee/paper/bilibili/csdn',
  `external_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '澶栭儴璧勬簮鍞?竴ID',
  `title` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '璧勬簮鏍囬?',
  `summary` text COLLATE utf8mb4_unicode_ci COMMENT '璧勬簮鎽樿?',
  `cover` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '灏侀潰URL',
  `author` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浣滆?/鍙戝竷鑰',
  `url` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '澶栭儴璁块棶鍦板潃',
  `resource_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '璧勬簮绫诲瀷: code/paper/video/article_search',
  `tags_json` text COLLATE utf8mb4_unicode_ci COMMENT '鏍囩?JSON',
  `raw_json` text COLLATE utf8mb4_unicode_ci COMMENT '鍘熷?澶栭儴鍏冩暟鎹甁SON',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '閫昏緫鍒犻櫎',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_platform_external` (`teacher_id`,`platform`,`external_id`,`is_delete`),
  KEY `idx_teacher_platform` (`teacher_id`,`platform`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏁欏笀澶栭儴璧勬簮鏀惰棌';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fatigue_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '记录日期（按天聚合的关键字段）',
  `yawn_count` int NOT NULL DEFAULT '0' COMMENT '打哈欠次数',
  `fatigue_count` int NOT NULL DEFAULT '0' COMMENT '闭眼疲劳次数',
  `no_face_count` int NOT NULL DEFAULT '0' COMMENT '离开屏幕次数',
  `normal_count` int NOT NULL DEFAULT '0' COMMENT '正常检测帧数',
  `total_detections` int NOT NULL DEFAULT '0' COMMENT '总检测帧数',
  `monitor_seconds` int NOT NULL DEFAULT '0' COMMENT '摄像头开启累计秒数',
  `events` text COMMENT '疲劳事件时间线JSON（用于时序图渲染）',
  `ear_samples` text COMMENT 'EAR采样序列JSON（用于趋势分析）',
  `mar_samples` text COMMENT 'MAR采样序列JSON',
  `last_status` varchar(20) DEFAULT 'normal' COMMENT '最后一次检测状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `course_id` bigint DEFAULT NULL COMMENT 'course link for fatigue monitoring',
  `chapter_id` bigint DEFAULT NULL COMMENT 'chapter link for fatigue monitoring',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`record_date`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_record_date` (`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生疲劳检测按天记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacherId` bigint NOT NULL COMMENT '发布教师ID',
  `classId` bigint DEFAULT NULL COMMENT '目标班级ID',
  `courseId` bigint DEFAULT NULL COMMENT '关联课程ID',
  `chapterId` bigint DEFAULT NULL COMMENT '关联章节ID',
  `chapterTitleSnapshot` varchar(255) DEFAULT NULL COMMENT '章节标题快照',
  `quizResourceId` bigint NOT NULL COMMENT '原始试卷资源ID(ai_resource)',
  `title` varchar(255) NOT NULL COMMENT '作业标题',
  `quizTitleSnapshot` varchar(255) DEFAULT NULL COMMENT '试卷原标题快照',
  `contentSnapshot` mediumtext NOT NULL COMMENT '试卷内容快照(Markdown)',
  `paramsSnapshot` text COMMENT '试卷参数快照(JSON)',
  `teacherNote` varchar(500) DEFAULT NULL COMMENT '教师寄语',
  `assignmentType` varchar(32) NOT NULL DEFAULT 'homework' COMMENT '作业类型 homework/chapter_practice',
  `status` varchar(20) NOT NULL DEFAULT 'published' COMMENT 'draft/published/closed',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `allowRedo` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许重做 0否1是',
  `maxAttemptCount` int NOT NULL DEFAULT '1' COMMENT '最大作答次数',
  `questionCount` int DEFAULT NULL COMMENT '题目数量',
  `totalScore` int DEFAULT NULL COMMENT '试卷总分',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  `durationMinutes` int DEFAULT NULL COMMENT '考试时长(分钟)，仅exam使用',
  `answerMode` varchar(20) NOT NULL DEFAULT 'online' COMMENT 'online/image/mixed',
  `imageGranularity` varchar(20) NOT NULL DEFAULT 'both' COMMENT 'whole/per_question/both',
  `gradingMode` varchar(20) NOT NULL DEFAULT 'auto' COMMENT 'auto/ai_review',
  `targetStudentId` bigint DEFAULT NULL COMMENT 'target student for personal practice',
  `sourceType` varchar(32) DEFAULT NULL COMMENT 'teacher_bank/platform_bank/ai_generated',
  PRIMARY KEY (`id`),
  KEY `idx_teacher` (`teacherId`),
  KEY `idx_class` (`classId`),
  KEY `idx_status` (`status`),
  KEY `idx_homework_chapter` (`chapterId`),
  KEY `idx_homework_course_chapter_class_status` (`courseId`,`chapterId`,`classId`,`status`,`createTime`),
  KEY `idx_homework_type` (`assignmentType`),
  KEY `idx_homework_course_chapter_class_type` (`courseId`,`chapterId`,`classId`,`assignmentType`,`status`),
  KEY `idx_homework_target_student` (`targetStudentId`,`assignmentType`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作业任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework_monitor_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacherId` bigint NOT NULL COMMENT '教师ID',
  `classId` bigint DEFAULT NULL COMMENT '班级ID，为空表示全部班级',
  `publishDate` varchar(20) DEFAULT NULL COMMENT '筛选的发布日期 yyyy-MM-dd，为空表示全部日期',
  `quizResourceId` bigint DEFAULT NULL COMMENT '...',
  `quizTitleSnapshot` varchar(255) DEFAULT NULL COMMENT '...',
  `reportTitle` varchar(255) NOT NULL COMMENT '报告标题',
  `reportMarkdown` longtext COMMENT 'Markdown 报告正文',
  `assignmentIdsJson` longtext COMMENT '本次报告覆盖的作业ID数组JSON',
  `summaryJson` longtext COMMENT '关键统计摘要JSON',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_create_time` (`teacherId`,`createTime`),
  KEY `idx_class_id` (`classId`),
  KEY `idx_teacher_quiz_create` (`teacherId`,`quizResourceId`,`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师作业学情诊断报告表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework_submission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignmentId` bigint NOT NULL COMMENT '作业任务ID',
  `teacherId` bigint DEFAULT NULL COMMENT '出题教师ID(冗余)',
  `studentId` bigint NOT NULL COMMENT '学生ID',
  `classId` bigint DEFAULT NULL COMMENT '班级ID(冗余)',
  `courseId` bigint DEFAULT NULL COMMENT '课程ID(冗余)',
  `attemptNo` int NOT NULL DEFAULT '1' COMMENT '第几次作答',
  `submitStatus` varchar(20) NOT NULL DEFAULT 'draft' COMMENT 'draft/submitted/judging/completed/failed',
  `studentAnswerJson` mediumtext COMMENT '学生答案JSON',
  `objectiveScore` int DEFAULT NULL COMMENT '客观题得分',
  `subjectiveScore` int DEFAULT NULL COMMENT '主观题得分',
  `totalScore` int DEFAULT NULL COMMENT '总分',
  `correctCount` int DEFAULT NULL COMMENT '答对题数',
  `wrongCount` int DEFAULT NULL COMMENT '答错题数',
  `aiReportMarkdown` mediumtext COMMENT 'AI批改报告(Markdown)',
  `aiReportJson` mediumtext COMMENT 'AI报告结构化JSON',
  `aiRawResponse` mediumtext COMMENT 'AI原始返回',
  `submitTime` datetime DEFAULT NULL COMMENT '提交时间',
  `judgeTime` datetime DEFAULT NULL COMMENT '判题完成时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  `teacherRemark` varchar(1000) DEFAULT NULL COMMENT '教师批阅总评语(考试用)',
  `submissionType` varchar(20) NOT NULL DEFAULT 'online' COMMENT 'online/image/mixed',
  `gradingModeSnapshot` varchar(20) NOT NULL DEFAULT 'auto' COMMENT 'auto/ai_review',
  `reviewStatus` varchar(20) NOT NULL DEFAULT 'none' COMMENT 'none/pending/approved',
  `aiSuggestedTotalScore` int DEFAULT NULL COMMENT 'AI suggested total score before teacher review',
  `visionStatus` varchar(20) DEFAULT NULL COMMENT 'pending/completed/failed',
  `visionResultJson` text COMMENT 'Vision recognition result JSON',
  PRIMARY KEY (`id`),
  KEY `idx_assignment` (`assignmentId`),
  KEY `idx_student` (`studentId`),
  KEY `idx_status` (`submitStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生作答记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework_submission_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submissionId` bigint NOT NULL COMMENT '提交记录ID',
  `questionNo` varchar(10) DEFAULT NULL COMMENT '题号',
  `questionType` varchar(20) DEFAULT NULL COMMENT '题型 radio/checkbox/judge/fill/text',
  `stemSnapshot` text COMMENT '题干快照',
  `standardAnswer` text COMMENT '标准答案',
  `studentAnswer` text COMMENT '学生答案',
  `fullScore` int DEFAULT NULL COMMENT '该题满分',
  `score` int DEFAULT NULL COMMENT '该题得分',
  `isCorrect` tinyint DEFAULT NULL COMMENT '是否正确 0否1是',
  `aiComment` text COMMENT 'AI点评',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `imageUrlsJson` text COMMENT 'Original answer image URLs JSON',
  `recognizedText` text COMMENT 'Vision recognized text',
  `visionConfidence` double DEFAULT NULL COMMENT 'Vision confidence',
  `aiSuggestedScore` int DEFAULT NULL COMMENT 'AI suggested score before teacher review',
  PRIMARY KEY (`id`),
  KEY `idx_submission` (`submissionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作答明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homework_submission_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submissionId` bigint NOT NULL COMMENT 'Submission ID',
  `questionNo` varchar(64) DEFAULT NULL COMMENT 'Question number; null for whole-paper upload',
  `imageUrl` varchar(1000) NOT NULL COMMENT 'Image URL',
  `imageOrder` int NOT NULL DEFAULT '0' COMMENT 'Image order in group',
  `recognizedText` text COMMENT 'Vision recognized text',
  `visionJson` text COMMENT 'Raw vision result JSON',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/completed/failed',
  `errorMessage` varchar(1000) DEFAULT NULL COMMENT 'Vision error message',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_submission` (`submissionId`),
  KEY `idx_submission_question` (`submissionId`,`questionNo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Homework submission image table';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hw_reminder` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '发送提醒的教师ID',
  `class_id` bigint DEFAULT NULL COMMENT '目标班级ID（NULL 表示对该教师所有班级广播）',
  `message` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提醒内容',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_create_time` (`create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提醒通知表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `learning_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `studentId` bigint NOT NULL,
  `classId` bigint DEFAULT NULL,
  `courseId` bigint DEFAULT NULL,
  `chapterId` bigint DEFAULT NULL,
  `resourceId` bigint DEFAULT NULL,
  `resourceType` varchar(40) DEFAULT '',
  `knowledgeName` varchar(120) DEFAULT '',
  `eventType` varchar(40) NOT NULL,
  `durationSecond` int DEFAULT NULL,
  `score` decimal(8,2) DEFAULT NULL,
  `correct` tinyint DEFAULT NULL,
  `extraJson` text,
  `eventTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_learning_event_student_time` (`studentId`,`eventTime`),
  KEY `idx_learning_event_course_chapter` (`courseId`,`chapterId`),
  KEY `idx_learning_event_type` (`eventType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `major_curriculum_course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `major` varchar(120) NOT NULL,
  `semester_no` int NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `course_name` varchar(200) NOT NULL,
  `course_type` varchar(40) DEFAULT 'required',
  `credits` decimal(4,1) DEFAULT NULL,
  `hours` int DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_mcc_major_semester` (`major`,`semester_no`),
  KEY `idx_mcc_course_name` (`course_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='major four-year curriculum courses';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mental_state_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '评估用户ID',
  `assessment_mode` varchar(20) NOT NULL COMMENT '评估模式: dual(双通道融合) / subjective(纯主观)',
  `answers_json` text NOT NULL COMMENT '五轮主观问卷答案(JSON数组)',
  `stress_level` tinyint NOT NULL COMMENT '压力负荷',
  `energy_level` tinyint NOT NULL COMMENT '能量水平',
  `focus_level` tinyint NOT NULL COMMENT '专注状态',
  `cognitive_load` tinyint NOT NULL COMMENT '认知负荷',
  `flow_score` tinyint NOT NULL COMMENT '心流指数',
  `emotion_score` tinyint NOT NULL COMMENT '情绪效价',
  `verdict` varchar(1024) DEFAULT NULL COMMENT '综合评估结论',
  `theories_json` text COMMENT '理论依据(JSON数组)',
  `risk_flags_json` text COMMENT '风险标识(JSON数组)',
  `suggestions_json` text COMMENT '三级建议(JSON: immediate/shortTerm/habit)',
  `fatigue_snapshot` longtext COMMENT '疲劳监测数据快照(events/earSamples/marSamples JSON)',
  `monitor_seconds` int DEFAULT '0' COMMENT '监测时长(秒)',
  `yawn_count` int DEFAULT '0' COMMENT '哈欠次数',
  `fatigue_count` int DEFAULT '0' COMMENT '闭眼疲劳次数',
  `focus_rate` int DEFAULT '0' COMMENT '专注率(%)',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评估时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `learning_profile_days` int DEFAULT '7' COMMENT 'learning profile window in days',
  `learning_context_summary` varchar(2000) DEFAULT '' COMMENT 'learning context summary used in assessment',
  `learning_profile_snapshot` mediumtext COMMENT 'learning profile snapshot json used in assessment',
  PRIMARY KEY (`id`),
  KEY `idx_user_create_time` (`user_id`,`create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认知状态评估记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `micro_course_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `status` varchar(32) NOT NULL DEFAULT 'queued' COMMENT 'queued/running/succeeded/failed',
  `progress` int NOT NULL DEFAULT '0' COMMENT '渲染进度 0-100',
  `title` varchar(255) DEFAULT NULL COMMENT '微课标题',
  `script_json` longtext COMMENT '脚本与分镜JSON',
  `video_url` varchar(1024) DEFAULT NULL COMMENT '生成视频URL',
  `cover_url` varchar(1024) DEFAULT NULL COMMENT '封面URL',
  `subtitle_url` varchar(1024) DEFAULT NULL COMMENT '字幕URL',
  `audio_url` varchar(1024) DEFAULT NULL COMMENT '配音URL',
  `duration_seconds` int DEFAULT NULL COMMENT '实际视频时长秒',
  `warnings_json` text COMMENT '渲染警告JSON',
  `render_stats_json` text COMMENT '渲染统计JSON',
  `params_json` longtext COMMENT '生成参数快照',
  `error_message` text COMMENT '失败原因',
  `course_id` bigint DEFAULT NULL COMMENT '发布课程ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '发布章节ID',
  `resource_id` bigint DEFAULT NULL COMMENT 'AI资源ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_status` (`teacher_id`,`status`),
  KEY `idx_course_chapter` (`course_id`,`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='微课生成任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_banner` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) NOT NULL COMMENT '广告图标题',
  `image_url` varchar(1024) NOT NULL COMMENT '图片地址',
  `target_url` varchar(1024) DEFAULT NULL COMMENT '跳转地址',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值，越小越靠前',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_platform_banner_enabled_sort` (`is_enabled`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台首页广告图表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `points_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(50) NOT NULL COMMENT '积分获取途径: checkin(签到), plan(完成计划), admin(后台奖励)',
  `points` int NOT NULL COMMENT '获取的分数 (纯荣誉体系，无负数)',
  `description` varchar(255) DEFAULT NULL COMMENT '明细描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_time_user` (`create_time`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户积分获取流水表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_daily_recommendation_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `studentId` bigint NOT NULL,
  `recommendDate` date NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'pending',
  `courseId` bigint DEFAULT NULL,
  `goal` varchar(80) DEFAULT '',
  `difficultyText` varchar(500) DEFAULT '',
  `availableMinutes` int DEFAULT NULL,
  `preferredResourceType` varchar(30) DEFAULT 'balanced',
  `answersJson` text,
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily_recommendation_student_date` (`studentId`,`recommendDate`,`isDelete`),
  KEY `idx_daily_recommendation_status` (`studentId`,`status`,`recommendDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_knowledge_mastery` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `studentId` bigint NOT NULL,
  `courseId` bigint DEFAULT NULL,
  `chapterId` bigint DEFAULT NULL,
  `knowledgeName` varchar(120) NOT NULL,
  `masteryScore` int NOT NULL DEFAULT '0',
  `status` varchar(30) DEFAULT 'partial',
  `evidenceSummary` varchar(800) DEFAULT '',
  `lastEvidenceTime` datetime DEFAULT NULL,
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_mastery_student_score` (`studentId`,`masteryScore`),
  KEY `idx_mastery_course_chapter` (`courseId`,`chapterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_learning_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `studentId` bigint NOT NULL,
  `courseId` bigint DEFAULT NULL,
  `dominantType` varchar(40) DEFAULT 'balanced',
  `videoScore` int NOT NULL DEFAULT '0',
  `textScore` int NOT NULL DEFAULT '0',
  `practiceScore` int NOT NULL DEFAULT '0',
  `discussionScore` int NOT NULL DEFAULT '0',
  `aiScore` int NOT NULL DEFAULT '0',
  `resourceScore` int NOT NULL DEFAULT '0',
  `summary` varchar(500) DEFAULT '',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `profileCompleted` tinyint NOT NULL DEFAULT '0' COMMENT '1 means onboarding assessment or learning history exists',
  `personalityType` varchar(40) DEFAULT '' COMMENT 'student learning personality/profile type',
  `assessmentJson` text COMMENT 'onboarding assessment answers',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_preference_student_course` (`studentId`,`courseId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_resource_recommendation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `studentId` bigint NOT NULL,
  `courseId` bigint DEFAULT NULL,
  `chapterId` bigint DEFAULT NULL,
  `resourceId` bigint DEFAULT NULL,
  `resourceType` varchar(40) DEFAULT '',
  `resourceTitle` varchar(200) DEFAULT '',
  `knowledgeName` varchar(120) DEFAULT '',
  `recommendationReason` varchar(800) DEFAULT '',
  `practiceSuggestion` varchar(800) DEFAULT '',
  `status` varchar(30) DEFAULT 'pending',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0',
  `recommendationSource` varchar(40) DEFAULT 'profile' COMMENT 'profile/daily_survey',
  PRIMARY KEY (`id`),
  KEY `idx_recommendation_student_status` (`studentId`,`status`),
  KEY `idx_recommendation_resource` (`resourceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_schedule_analysis_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `source_file_name` varchar(255) DEFAULT NULL COMMENT '原始课表文件名',
  `source_file_url` varchar(1024) DEFAULT NULL COMMENT '原始课表文件地址，可为空',
  `extracted_json` longtext COMMENT 'AI提取出的原始课程JSON',
  `matched_json` longtext NOT NULL COMMENT '匹配后的课程JSON',
  `insights_json` longtext COMMENT 'AI建议JSON',
  `semester_label` varchar(64) NOT NULL COMMENT '学期标识，例如 2025-2026-2',
  `status` varchar(32) NOT NULL DEFAULT 'completed' COMMENT '状态 pending/completed/failed',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_semester` (`user_id`,`semester_label`),
  KEY `idx_user_update_time` (`user_id`,`update_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生课表分析记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `last_study_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后学习时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`,`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习历史表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(255) NOT NULL COMMENT '计划内容',
  `is_completed` tinyint DEFAULT '0' COMMENT '是否完成(0未完, 1完成)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `name` varchar(100) NOT NULL COMMENT '班级名称 (例如: 2025级计算机科学1班)',
  `major` varchar(100) DEFAULT NULL COMMENT '所属专业',
  `college` varchar(100) DEFAULT NULL COMMENT '所属学院',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_course_assignment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `semester` varchar(40) NOT NULL,
  `assigned_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tca_teacher_semester` (`teacher_id`,`semester`),
  KEY `idx_tca_course_semester` (`course_id`,`semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='teacher semester course assignments';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_registration_code` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `register_code` varchar(80) NOT NULL,
  `teacher_name` varchar(100) DEFAULT NULL,
  `teacher_title` varchar(80) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'unused',
  `used_by` bigint DEFAULT NULL,
  `used_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_register_code` (`register_code`),
  KEY `idx_teacher_register_status` (`status`),
  KEY `idx_teacher_register_used_by` (`used_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='teacher registration code';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teacher_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `course_name` varchar(255) NOT NULL COMMENT '课程名称',
  `linked_course_id` bigint DEFAULT NULL COMMENT '关联的平台/网络课程ID',
  `class_name` varchar(255) DEFAULT NULL COMMENT '班级名称',
  `teaching_plan_id` bigint DEFAULT NULL COMMENT '关联教案ID（ai_resource）',
  `week_start` int NOT NULL COMMENT '开始周次',
  `week_end` int NOT NULL COMMENT '结束周次',
  `day_of_week` int NOT NULL COMMENT '星期几：1-周一, 2-周二 ... 7-周日',
  `start_period` int NOT NULL COMMENT '开始节次',
  `end_period` int NOT NULL COMMENT '结束节次',
  `semester_label` varchar(50) NOT NULL COMMENT '学年学期标签，如 2025-2026-2',
  `is_delete` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除, 1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师排课表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_case` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `title` varchar(255) NOT NULL COMMENT '案例标题',
  `category` varchar(50) NOT NULL COMMENT '案例分类：course_design-课程设计, enterprise-企业实际工程, competition-大赛资源, small_project-小项目',
  `difficulty` varchar(20) NOT NULL COMMENT '难度等级：easy-初级, medium-中等, hard-困难',
  `course_name` varchar(255) DEFAULT NULL COMMENT '适用课程（用户自定义输入）',
  `pdf_url` varchar(500) NOT NULL COMMENT 'PDF文件OSS地址',
  `scope` varchar(30) NOT NULL DEFAULT 'mine' COMMENT 'mine-teacher private, platform-shared platform case',
  `status` varchar(30) NOT NULL DEFAULT 'approved' COMMENT 'pending/approved/rejected/offline',
  `source_url` varchar(1000) DEFAULT NULL COMMENT 'Original public page or file URL',
  `source_case_id` bigint DEFAULT NULL COMMENT 'Platform case id copied into teacher private case',
  `source_name` varchar(100) DEFAULT NULL COMMENT 'Source website name',
  `summary` text COMMENT 'Case summary for recommendation',
  `keywords` varchar(500) DEFAULT NULL COMMENT 'Case keywords',
  `material_json` text COMMENT 'Extracted material links JSON',
  `structure_json` mediumtext COMMENT 'Extracted case structure JSON',
  `preview_text` mediumtext COMMENT 'Stored preview text extracted during crawl',
  `preview_type` varchar(30) NOT NULL DEFAULT 'document' COMMENT 'document/page',
  `relevance_score` int DEFAULT NULL COMMENT 'Keyword relevance score from crawler',
  `crawl_keyword` varchar(255) DEFAULT NULL COMMENT 'Original crawl keyword',
  `crawl_time` datetime DEFAULT NULL COMMENT 'Crawl time',
  `review_time` datetime DEFAULT NULL COMMENT 'Review time',
  `reviewer_id` bigint DEFAULT NULL COMMENT 'Admin reviewer user id',
  `is_delete` tinyint DEFAULT '0' COMMENT '是否删除：0-未删除, 1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_teaching_case_scope_status` (`scope`,`status`,`is_delete`),
  KEY `idx_teaching_case_source_url` (`source_url`(255)),
  KEY `idx_teaching_case_crawl_keyword` (`crawl_keyword`),
  KEY `idx_teaching_case_teacher_source_case` (`teacher_id`,`source_case_id`,`is_delete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学案例表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teaching_case_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `case_id` bigint NOT NULL COMMENT 'Teaching case ID',
  `type` varchar(30) NOT NULL DEFAULT 'image' COMMENT 'Asset type',
  `url` varchar(1000) NOT NULL COMMENT 'Asset URL',
  `title` varchar(255) DEFAULT NULL COMMENT 'Display title',
  `caption` varchar(500) DEFAULT NULL COMMENT 'Image caption',
  `context` text COMMENT 'Nearby or inferred context',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT 'Original order in case',
  `hash` varchar(64) DEFAULT NULL COMMENT 'Content hash',
  `width` int DEFAULT NULL COMMENT 'Image width',
  `height` int DEFAULT NULL COMMENT 'Image height',
  `source` varchar(30) NOT NULL DEFAULT 'docx' COMMENT 'docx/pdf/page/manual',
  `is_delete` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_case_type` (`case_id`,`type`,`is_delete`),
  KEY `idx_case_order` (`case_id`,`sort_order`),
  KEY `idx_hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Teaching case reusable assets';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `text_course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(255) NOT NULL COMMENT '教程名称',
  `cover_img` varchar(500) DEFAULT NULL COMMENT '封面图片URL',
  `description` varchar(1000) DEFAULT NULL COMMENT '教程简介',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文字教程课程表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `text_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '归属的教程ID',
  `title` varchar(255) NOT NULL COMMENT '章节标题',
  `content` longtext COMMENT '正文内容(Markdown格式)',
  `sort_order` int DEFAULT '0' COMMENT '排序字段(越小越靠前)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教程章节内容表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `userAccount` varchar(256) NOT NULL COMMENT '账号',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `userPassword` varchar(512) NOT NULL COMMENT '密码',
  `userName` varchar(256) DEFAULT NULL COMMENT '姓名',
  `userAvatar` varchar(1024) DEFAULT NULL COMMENT '用户头像',
  `userRole` varchar(50) NOT NULL DEFAULT 'student' COMMENT '角色：student(学生), teacher(教师), admin(管理员)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `points` int DEFAULT '0' COMMENT '用户积分',
  `user_avatar` varchar(1024) DEFAULT NULL COMMENT '用户头像',
  `user_profile` varchar(500) DEFAULT NULL COMMENT '个性签名',
  `class_id` bigint DEFAULT NULL COMMENT '所属班级ID(仅当userRole为student时有意义)',
  `teacher_title` varchar(80) DEFAULT NULL COMMENT 'teacher professional title',
  `teacher_register_code` varchar(80) DEFAULT NULL COMMENT 'teacher registration code',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_login_log_user_time` (`user_id`,`login_time`),
  KEY `idx_user_login_log_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_knowledge_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chapter_id` bigint NOT NULL,
  `start_second` int NOT NULL DEFAULT '0',
  `end_second` int NOT NULL DEFAULT '0',
  `knowledge_name` varchar(120) NOT NULL,
  `description` varchar(500) DEFAULT '',
  `difficulty` varchar(20) DEFAULT '中',
  `sort_order` int NOT NULL DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_vks_chapter_time` (`chapter_id`,`start_second`,`end_second`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_learning_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `chapter_id` bigint NOT NULL,
  `segment_id` bigint DEFAULT NULL,
  `event_type` varchar(40) NOT NULL,
  `from_second` int DEFAULT NULL,
  `to_second` int DEFAULT NULL,
  `duration_second` int DEFAULT NULL,
  `playback_rate` decimal(4,2) DEFAULT NULL,
  `event_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `extra_json` text,
  PRIMARY KEY (`id`),
  KEY `idx_vle_session_segment` (`session_id`,`segment_id`,`event_type`,`event_time`),
  KEY `idx_vle_student_time` (`student_id`,`event_time`),
  KEY `idx_vle_segment` (`segment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_learning_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `chapter_id` bigint NOT NULL,
  `started_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `last_event_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(20) DEFAULT 'active',
  `intervention_count` int NOT NULL DEFAULT '0',
  `muted_until_end` tinyint NOT NULL DEFAULT '0',
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_vls_student_chapter` (`student_id`,`chapter_id`,`started_at`),
  KEY `idx_vls_course_chapter` (`course_id`,`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_timeline_analysis_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `chapter_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `source_video_url` varchar(1000) NOT NULL,
  `transcript_json` mediumtext,
  `result_json` mediumtext,
  `error_message` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `is_delete` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_vtat_chapter_status` (`chapter_id`,`status`,`is_delete`),
  KEY `idx_vtat_teacher_time` (`teacher_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
