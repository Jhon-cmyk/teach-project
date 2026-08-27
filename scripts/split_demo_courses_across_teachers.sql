-- Split the 8 class demo courses across 4 teachers, 2 courses per teacher.
-- Safe to run repeatedly. Missing teacher accounts are created with password 123456.

SET @semester_label := '2025-2026-2';
SET @default_password := MD5(CONCAT('ruyi_teach', '123456'));

INSERT INTO `user` (userAccount, userPassword, userName, userRole, teacher_title, teacher_register_code, points, isDelete)
SELECT 'teacher1', @default_password, '王老师', 'teacher', '教授', 'SEED-TEACHER-001', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE userAccount = 'teacher1' AND userRole = 'teacher' AND isDelete = 0
);

INSERT INTO `user` (userAccount, userPassword, userName, userRole, teacher_title, teacher_register_code, points, isDelete)
SELECT 'teacherli', @default_password, '李老师', 'teacher', '高级实验师', 'SEED-TEACHER-002', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE userAccount = 'teacherli' AND userRole = 'teacher' AND isDelete = 0
);

INSERT INTO `user` (userAccount, userPassword, userName, userRole, teacher_title, teacher_register_code, points, isDelete)
SELECT '20230001', @default_password, '张明远', 'teacher', '副教授', 'SEED-TEACHER-003', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE userAccount = '20230001' AND userRole = 'teacher' AND isDelete = 0
);

INSERT INTO `user` (userAccount, userPassword, userName, userRole, teacher_title, teacher_register_code, points, isDelete)
SELECT '20230002', @default_password, '李雅婷', 'teacher', '讲师', 'SEED-TEACHER-004', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE userAccount = '20230002' AND userRole = 'teacher' AND isDelete = 0
);

SELECT id INTO @wang_teacher_id
FROM `user`
WHERE userRole = 'teacher' AND userAccount = 'teacher1' AND isDelete = 0
LIMIT 1;

SELECT id INTO @li_teacher_id
FROM `user`
WHERE userRole = 'teacher' AND userAccount = 'teacherli' AND isDelete = 0
LIMIT 1;

SELECT id INTO @zhang_teacher_id
FROM `user`
WHERE userRole = 'teacher' AND userAccount = '20230001' AND isDelete = 0
LIMIT 1;

SELECT id INTO @liya_teacher_id
FROM `user`
WHERE userRole = 'teacher' AND userAccount = '20230002' AND isDelete = 0
LIMIT 1;

UPDATE course
SET teacherId = @wang_teacher_id,
    teacherName = '王老师'
WHERE id IN (63, 64)
  AND isDelete = 0;

UPDATE course
SET teacherId = @li_teacher_id,
    teacherName = '李老师'
WHERE id IN (65, 66)
  AND isDelete = 0;

UPDATE course
SET teacherId = @zhang_teacher_id,
    teacherName = '张明远'
WHERE id IN (67, 68)
  AND isDelete = 0;

UPDATE course
SET teacherId = @liya_teacher_id,
    teacherName = '李雅婷'
WHERE id IN (69, 70)
  AND isDelete = 0;

UPDATE teacher_schedule
SET teacher_id = CASE
        WHEN linked_course_id IN (63, 64) THEN @wang_teacher_id
        WHEN linked_course_id IN (65, 66) THEN @li_teacher_id
        WHEN linked_course_id IN (67, 68) THEN @zhang_teacher_id
        WHEN linked_course_id IN (69, 70) THEN @liya_teacher_id
        ELSE teacher_id
    END
WHERE linked_course_id IN (63, 64, 65, 66, 67, 68, 69, 70)
  AND is_delete = 0;

DELETE FROM teacher_course_assignment
WHERE course_id IN (63, 64, 65, 66, 67, 68, 69, 70)
  AND semester = @semester_label;

INSERT INTO teacher_course_assignment (teacher_id, course_id, semester, assigned_by, is_delete)
VALUES
    (@wang_teacher_id, 63, @semester_label, NULL, 0),
    (@wang_teacher_id, 64, @semester_label, NULL, 0),
    (@li_teacher_id, 65, @semester_label, NULL, 0),
    (@li_teacher_id, 66, @semester_label, NULL, 0),
    (@zhang_teacher_id, 67, @semester_label, NULL, 0),
    (@zhang_teacher_id, 68, @semester_label, NULL, 0),
    (@liya_teacher_id, 69, @semester_label, NULL, 0),
    (@liya_teacher_id, 70, @semester_label, NULL, 0);
