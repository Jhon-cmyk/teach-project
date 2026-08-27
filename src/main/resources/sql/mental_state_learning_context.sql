CREATE TABLE IF NOT EXISTS `mental_state_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `assessment_mode` varchar(30) DEFAULT 'subjective',
  `answers_json` text,
  `stress_level` int DEFAULT NULL,
  `energy_level` int DEFAULT NULL,
  `focus_level` int DEFAULT NULL,
  `cognitive_load` int DEFAULT NULL,
  `flow_score` int DEFAULT NULL,
  `emotion_score` int DEFAULT NULL,
  `verdict` varchar(1000) DEFAULT '',
  `theories_json` text,
  `risk_flags_json` text,
  `suggestions_json` text,
  `fatigue_snapshot` mediumtext,
  `monitor_seconds` int DEFAULT 0,
  `yawn_count` int DEFAULT 0,
  `fatigue_count` int DEFAULT 0,
  `focus_rate` int DEFAULT 0,
  `learning_profile_days` int DEFAULT 7,
  `learning_context_summary` varchar(2000) DEFAULT '',
  `learning_profile_snapshot` mediumtext,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mental_state_user_time` (`user_id`, `create_time`),
  KEY `idx_mental_state_mode` (`assessment_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mental_state_record' AND COLUMN_NAME = 'learning_profile_days') = 0,
  'ALTER TABLE `mental_state_record` ADD COLUMN `learning_profile_days` int DEFAULT 7 COMMENT ''learning profile window in days''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mental_state_record' AND COLUMN_NAME = 'learning_context_summary') = 0,
  'ALTER TABLE `mental_state_record` ADD COLUMN `learning_context_summary` varchar(2000) DEFAULT '''' COMMENT ''learning context summary used in assessment''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mental_state_record' AND COLUMN_NAME = 'learning_profile_snapshot') = 0,
  'ALTER TABLE `mental_state_record` ADD COLUMN `learning_profile_snapshot` mediumtext COMMENT ''learning profile snapshot json used in assessment''',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
