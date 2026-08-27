CREATE TABLE IF NOT EXISTS `video_knowledge_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chapter_id` bigint NOT NULL,
  `start_second` int NOT NULL DEFAULT 0,
  `end_second` int NOT NULL DEFAULT 0,
  `knowledge_name` varchar(120) NOT NULL,
  `description` varchar(500) DEFAULT '',
  `difficulty` varchar(20) DEFAULT '中',
  `sort_order` int NOT NULL DEFAULT 1,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_vks_chapter_time` (`chapter_id`, `start_second`, `end_second`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `video_learning_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `chapter_id` bigint NOT NULL,
  `started_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `last_event_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(20) DEFAULT 'active',
  `intervention_count` int NOT NULL DEFAULT 0,
  `muted_until_end` tinyint NOT NULL DEFAULT 0,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_vls_student_chapter` (`student_id`, `chapter_id`, `started_at`),
  KEY `idx_vls_course_chapter` (`course_id`, `chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `video_learning_event` (
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
  KEY `idx_vle_session_segment` (`session_id`, `segment_id`, `event_type`, `event_time`),
  KEY `idx_vle_student_time` (`student_id`, `event_time`),
  KEY `idx_vle_segment` (`segment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
