-- =====================================================
-- 知识点-学习活动绑定表
-- 用于将作业/练习/编程题绑定到知识图谱节点
-- 列名使用驼峰命名，与项目其他表保持一致
-- =====================================================

CREATE TABLE IF NOT EXISTS `course_graph_node_activity` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `nodeId` VARCHAR(64) NOT NULL COMMENT '知识图谱节点ID',
    `teacherId` BIGINT NOT NULL COMMENT '绑定教师ID',
    `activityType` VARCHAR(32) NOT NULL COMMENT '活动类型: homework / practice / coding',
    `activityId` BIGINT NOT NULL COMMENT '活动ID',
    `activityTitle` VARCHAR(255) COMMENT '活动标题快照',
    `weight` INT NOT NULL DEFAULT 1 COMMENT '权重',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete` TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_node_activity` (`nodeId`, `activityType`, `activityId`),
    KEY `idx_nodeId` (`nodeId`),
    KEY `idx_teacherId` (`teacherId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱节点-学习活动绑定';

-- =====================================================
-- 学生-知识点进度表
-- 记录每个学生在每个知识点上的完成率和掌握率
-- =====================================================

CREATE TABLE IF NOT EXISTS `course_graph_node_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `studentId` BIGINT NOT NULL COMMENT '学生ID',
    `nodeId` VARCHAR(64) NOT NULL COMMENT '知识图谱节点ID',
    `completionRate` INT NOT NULL DEFAULT 0 COMMENT '完成率 0-100',
    `masteryRate` INT NOT NULL DEFAULT 0 COMMENT '掌握率 0-100',
    `studyMinutes` INT NOT NULL DEFAULT 0 COMMENT '学习时长(分钟)',
    `lastStudyTime` DATETIME COMMENT '最后学习时间',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete` TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_student_node` (`studentId`, `nodeId`),
    KEY `idx_studentId` (`studentId`),
    KEY `idx_nodeId` (`nodeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生知识点学习进度';
