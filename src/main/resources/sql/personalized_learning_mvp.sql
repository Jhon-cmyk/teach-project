CREATE TABLE IF NOT EXISTS `learning_event` (
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
  `isDelete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_learning_event_student_time` (`studentId`, `eventTime`),
  KEY `idx_learning_event_course_chapter` (`courseId`, `chapterId`),
  KEY `idx_learning_event_type` (`eventType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `student_learning_preference` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `studentId` bigint NOT NULL,
  `courseId` bigint DEFAULT NULL,
  `dominantType` varchar(40) DEFAULT 'balanced',
  `videoScore` int NOT NULL DEFAULT 0,
  `textScore` int NOT NULL DEFAULT 0,
  `practiceScore` int NOT NULL DEFAULT 0,
  `discussionScore` int NOT NULL DEFAULT 0,
  `aiScore` int NOT NULL DEFAULT 0,
  `resourceScore` int NOT NULL DEFAULT 0,
  `summary` varchar(500) DEFAULT '',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_preference_student_course` (`studentId`, `courseId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `student_knowledge_mastery` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `studentId` bigint NOT NULL,
  `courseId` bigint DEFAULT NULL,
  `chapterId` bigint DEFAULT NULL,
  `knowledgeName` varchar(120) NOT NULL,
  `masteryScore` int NOT NULL DEFAULT 0,
  `status` varchar(30) DEFAULT 'partial',
  `evidenceSummary` varchar(800) DEFAULT '',
  `lastEvidenceTime` datetime DEFAULT NULL,
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_mastery_student_score` (`studentId`, `masteryScore`),
  KEY `idx_mastery_course_chapter` (`courseId`, `chapterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `student_resource_recommendation` (
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
  `recommendationSource` varchar(40) DEFAULT 'profile',
  `status` varchar(30) DEFAULT 'pending',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_recommendation_student_status` (`studentId`, `status`),
  KEY `idx_recommendation_resource` (`resourceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `student_daily_recommendation_session` (
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
  `isDelete` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily_recommendation_student_date` (`studentId`, `recommendDate`, `isDelete`),
  KEY `idx_daily_recommendation_status` (`studentId`, `status`, `recommendDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
