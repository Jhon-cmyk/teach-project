CREATE TABLE IF NOT EXISTS `video_timeline_analysis_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `chapter_id` bigint NOT NULL,
  `teacher_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `source_video_url` varchar(1000) NOT NULL,
  `transcript_json` mediumtext DEFAULT NULL,
  `result_json` mediumtext DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `is_delete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_vtat_chapter_status` (`chapter_id`, `status`, `is_delete`),
  KEY `idx_vtat_teacher_time` (`teacher_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
