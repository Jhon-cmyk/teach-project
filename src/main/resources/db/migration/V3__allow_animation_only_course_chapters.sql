ALTER TABLE `course_chapter`
    MODIFY COLUMN `video_url` varchar(1024) NULL COMMENT '本集视频链接；纯交互课件章节可为空';
