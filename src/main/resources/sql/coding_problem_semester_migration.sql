-- 为编程题库补充学期维度，便于与教师课表按学年学期联动
ALTER TABLE `coding_problem`
    ADD COLUMN `semester_label` VARCHAR(50) DEFAULT NULL COMMENT '所属学年学期，如 2025-2026-1' AFTER `course_id`;

CREATE INDEX `idx_semester_label` ON `coding_problem` (`semester_label`);
