-- ============================================================
-- 编程练习沙箱模块建表脚本
-- ============================================================

CREATE TABLE IF NOT EXISTS `coding_problem` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title`           VARCHAR(200)   NOT NULL COMMENT '题目标题',
    `description`     MEDIUMTEXT     NOT NULL COMMENT '题目描述(Markdown)',
    `difficulty`      VARCHAR(20)    NOT NULL DEFAULT 'medium' COMMENT 'easy/medium/hard',
    `languages`       JSON           NOT NULL COMMENT '支持的语言列表 ["java","python","cpp","javascript"]',
    `time_limit_ms`   INT            NOT NULL DEFAULT 5000 COMMENT '单次运行超时(ms)',
    `memory_limit_kb` INT            NOT NULL DEFAULT 262144 COMMENT '单次运行内存上限(KB)',
    `graph_node_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '关联图谱节点(预留)',
    `course_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '所属课程',
    `semester_label`  VARCHAR(50)    DEFAULT NULL COMMENT '所属学年学期，如 2025-2026-1',
    `creator_id`      BIGINT UNSIGNED NOT NULL COMMENT '创建教师ID',
    `is_public`       TINYINT        NOT NULL DEFAULT 0 COMMENT '0=私有 1=公开',
    `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`       TINYINT        NOT NULL DEFAULT 0,
    INDEX `idx_course_id` (`course_id`),
    INDEX `idx_semester_label` (`semester_label`),
    INDEX `idx_creator_id` (`creator_id`),
    INDEX `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编程题';

CREATE TABLE IF NOT EXISTS `coding_problem_template` (
    `id`                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `problem_id`        BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    `language`          VARCHAR(30)     NOT NULL COMMENT '语言标识: java/python/cpp/javascript',
    `starter_code`      MEDIUMTEXT      DEFAULT NULL COMMENT '学生初始代码模板',
    `reference_solution` MEDIUMTEXT     DEFAULT NULL COMMENT '参考解答',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`         TINYINT         NOT NULL DEFAULT 0,
    UNIQUE INDEX `uk_problem_lang` (`problem_id`, `language`),
    INDEX `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编程题多语言模板';

CREATE TABLE IF NOT EXISTS `coding_test_case` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `problem_id`      BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    `input`           MEDIUMTEXT      DEFAULT NULL COMMENT '标准输入',
    `expected_output` MEDIUMTEXT      NOT NULL COMMENT '期望输出',
    `is_sample`       TINYINT         NOT NULL DEFAULT 0 COMMENT '1=样例用例(学生可见) 0=隐藏用例',
    `score`           INT             NOT NULL DEFAULT 0 COMMENT '该用例分值权重',
    `sort_order`      INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`       TINYINT         NOT NULL DEFAULT 0,
    INDEX `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编程题测试用例';

CREATE TABLE IF NOT EXISTS `coding_problem_publish` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `problem_id`      BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    `class_id`        BIGINT UNSIGNED NOT NULL COMMENT '发布班级ID',
    `chapter_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '关联章节ID',
    `deadline`        DATETIME        DEFAULT NULL COMMENT '截止时间',
    `created_by`      BIGINT UNSIGNED NOT NULL COMMENT '发布教师ID',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`       TINYINT         NOT NULL DEFAULT 0,
    INDEX `idx_problem_id` (`problem_id`),
    INDEX `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编程题发布记录';

CREATE TABLE IF NOT EXISTS `coding_submission` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `problem_id`      BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    `publish_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '发布记录ID',
    `student_id`      BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `language`        VARCHAR(30)     NOT NULL COMMENT '提交语言',
    `code`            MEDIUMTEXT      NOT NULL COMMENT '学生提交代码',
    `status`          VARCHAR(20)     NOT NULL DEFAULT 'pending' COMMENT 'pending/running/judged/error',
    `passed_count`    INT             NOT NULL DEFAULT 0 COMMENT '通过用例数',
    `total_count`     INT             NOT NULL DEFAULT 0 COMMENT '总用例数',
    `test_score`      INT             NOT NULL DEFAULT 0 COMMENT '测试用例得分(0-100)',
    `ai_score`        INT             NOT NULL DEFAULT 0 COMMENT 'AI评审得分(0-100)',
    `final_score`     INT             NOT NULL DEFAULT 0 COMMENT '最终加权得分(0-100)',
    `ai_review_md`    MEDIUMTEXT      DEFAULT NULL COMMENT 'AI评审报告(Markdown)',
    `runtime_ms`      INT             DEFAULT NULL COMMENT '运行耗时(ms)',
    `memory_kb`       INT             DEFAULT NULL COMMENT '内存占用(KB)',
    `judge_detail`    JSON            DEFAULT NULL COMMENT '逐用例判定详情',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`       TINYINT         NOT NULL DEFAULT 0,
    INDEX `idx_problem_id` (`problem_id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_publish_id` (`publish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编程题提交记录';
