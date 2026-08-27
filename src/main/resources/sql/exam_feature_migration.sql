-- 在线考试功能 数据库迁移
-- 执行日期: 2026-04-24

ALTER TABLE homework_assignment ADD COLUMN durationMinutes INT DEFAULT NULL COMMENT '考试时长(分钟)，仅assignmentType=exam使用';
ALTER TABLE homework_submission ADD COLUMN teacherRemark VARCHAR(1000) DEFAULT NULL COMMENT '教师批阅总评语(考试用)';
