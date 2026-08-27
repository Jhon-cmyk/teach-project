-- =============================================================
-- AI 智能学伴 + RAG 答疑 相关数据表
-- 适用数据库: MySQL 8.x (utf8mb4)
-- 使用方式: 手动执行一次 (项目未接入 Flyway/Liquibase)
-- =============================================================

-- -------------------------------------------------------------
-- 1) 知识块表: RAG 检索的基本单位
--    来源可以是 AiResource (教案/试题/动画脚本) / Course 章节 / CourseGraph 节点
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_knowledge_chunk` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `source_type`     VARCHAR(32)  NOT NULL COMMENT 'ai_resource / course_chapter / graph_node',
    `source_id`       BIGINT       NOT NULL COMMENT '来源实体主键',
    `course_id`       BIGINT       NULL     COMMENT '所属课程 (便于过滤)',
    `chapter_id`      BIGINT       NULL     COMMENT '所属章节 (可空)',
    `graph_node_id`   VARCHAR(64)  NULL     COMMENT '关联知识图谱节点 ID (course_graph_node.id, 可空)',
    `chunk_index`     INT          NOT NULL DEFAULT 0 COMMENT '同一来源内的分块序号',
    `title`           VARCHAR(255) NULL     COMMENT '冗余标题, 用于引用展示',
    `content`         MEDIUMTEXT   NOT NULL COMMENT '分块原文',
    `embedding_json`  MEDIUMTEXT   NULL     COMMENT 'JSON 数组: [f1,f2,...] (1024 维 float)',
    `token_count`     INT          NULL     COMMENT '估算 token 数',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_source` (`source_type`, `source_id`),
    KEY `idx_course` (`course_id`),
    KEY `idx_graph_node` (`graph_node_id`),
    FULLTEXT KEY `ft_content` (`content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 学伴 RAG 知识块 (含向量)';

-- -------------------------------------------------------------
-- 2) 学伴会话表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL,
    `title`          VARCHAR(255) NULL     COMMENT '会话标题 (取首条消息前 20 字)',
    `course_id`      BIGINT       NULL     COMMENT '会话绑定课程 (可选, 用于上下文过滤)',
    `last_active_at` DATETIME     NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`, `last_active_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 学伴会话';

-- -------------------------------------------------------------
-- 3) 会话消息表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id`     BIGINT       NOT NULL,
    `role`           VARCHAR(16)  NOT NULL COMMENT 'user / assistant / system',
    `content`        MEDIUMTEXT   NOT NULL,
    `citations_json` TEXT         NULL     COMMENT '引用来源 [{chunkId,sourceType,sourceId,title}]',
    `mental_snapshot_json` TEXT   NULL     COMMENT '本次回答时学情快照 (压力/能量/认知负荷等)',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_delete`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_session` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 学伴会话消息';

-- -------------------------------------------------------------
-- 4) 问题统计表 (教师端答疑热点)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_question_stat` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL,
    `course_id`      BIGINT       NULL,
    `graph_node_id`  VARCHAR(64)  NULL     COMMENT '命中的知识图谱节点 ID (可为空)',
    `question_hash`  CHAR(32)     NOT NULL COMMENT '问题归一化后的 MD5, 用于去重累计',
    `question_text`  VARCHAR(512) NOT NULL COMMENT '原始问题文本',
    `hit_count`      INT          NOT NULL DEFAULT 1,
    `last_asked_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_course_hash` (`user_id`, `course_id`, `question_hash`),
    KEY `idx_course_time` (`course_id`, `last_asked_at`),
    KEY `idx_graph_node` (`graph_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 学伴问题统计 (教师端热点分析)';

-- -------------------------------------------------------------
-- 后置修正: 如果已按旧版 DDL 执行过 (graph_node_id 为 BIGINT),
-- 执行下面两条 ALTER 把类型改为 VARCHAR(64) 以匹配 course_graph_node.id
-- -------------------------------------------------------------
-- ALTER TABLE `ai_knowledge_chunk` MODIFY COLUMN `graph_node_id` VARCHAR(64) NULL COMMENT '关联知识图谱节点 ID';
-- ALTER TABLE `ai_question_stat`   MODIFY COLUMN `graph_node_id` VARCHAR(64) NULL COMMENT '命中的知识图谱节点 ID';
-- ALTER TABLE `ai_question_stat`   DROP INDEX `idx_graph_node`, ADD INDEX `idx_graph_node` (`graph_node_id`);
-- ALTER TABLE `ai_knowledge_chunk` DROP INDEX `idx_graph_node`, ADD INDEX `idx_graph_node` (`graph_node_id`);
