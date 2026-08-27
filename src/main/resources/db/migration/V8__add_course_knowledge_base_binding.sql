ALTER TABLE `course`
    ADD COLUMN `knowledgeRepoId` VARCHAR(64) NULL COMMENT '星火知识库ID' AFTER `face_detection_required`,
    ADD COLUMN `knowledgeRepoName` VARCHAR(120) NULL COMMENT '星火知识库名称' AFTER `knowledgeRepoId`,
    ADD COLUMN `knowledgeKeywords` VARCHAR(500) NULL COMMENT '通用问答自动路由关键词' AFTER `knowledgeRepoName`,
    ADD COLUMN `knowledgeSyncStatus` VARCHAR(30) NOT NULL DEFAULT 'empty' COMMENT 'empty/processing/ready/failed' AFTER `knowledgeKeywords`,
    ADD COLUMN `knowledgeUpdatedAt` DATETIME NULL COMMENT '知识库最近同步时间' AFTER `knowledgeSyncStatus`,
    ADD INDEX `idx_course_knowledge_repo` (`knowledgeRepoId`);
