ALTER TABLE `micro_course_task`
  ADD COLUMN IF NOT EXISTS `audio_url` varchar(1024) DEFAULT NULL COMMENT '配音URL' AFTER `subtitle_url`,
  ADD COLUMN IF NOT EXISTS `duration_seconds` int DEFAULT NULL COMMENT '实际视频时长秒' AFTER `audio_url`,
  ADD COLUMN IF NOT EXISTS `warnings_json` text COMMENT '渲染警告JSON' AFTER `duration_seconds`,
  ADD COLUMN IF NOT EXISTS `render_stats_json` text COMMENT '渲染统计JSON' AFTER `warnings_json`;
