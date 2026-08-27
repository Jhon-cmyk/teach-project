ALTER TABLE `student_learning_preference`
    ADD COLUMN `aiQuestionCount` INT NOT NULL DEFAULT 0 COMMENT '累计有效 AI 助教提问数' AFTER `developmentGoal`,
    ADD COLUMN `aiProfileSummary` VARCHAR(1000) NOT NULL DEFAULT '' COMMENT 'AI 提问形成的学习画像摘要' AFTER `aiQuestionCount`,
    ADD COLUMN `aiProfileJson` TEXT NULL COMMENT 'AI 提问主题、次数及近期问题聚合 JSON' AFTER `aiProfileSummary`,
    ADD COLUMN `lastAiQuestionTime` DATETIME NULL COMMENT '最近一次 AI 助教提问时间' AFTER `aiProfileJson`;

ALTER TABLE `learning_event`
    ADD INDEX `idx_learning_event_ai_profile` (`studentId`, `eventType`, `eventTime`);
