-- Repair legacy community posts that used demo course IDs (such as 101-105).
-- Only courses assigned to a class are candidates, and only an unambiguous name
-- match is migrated. Posts such as "高等数学" remain untouched when no taught
-- course exists, so they cannot leak into a teacher's processing desk.

START TRANSACTION;

CREATE TEMPORARY TABLE community_course_id_repair AS
SELECT
    p.id AS post_id,
    MIN(c.id) AS course_id
FROM community_post p
INNER JOIN course c
    ON c.isDelete = 0
INNER JOIN course_class_relation relation
    ON relation.course_id = c.id
WHERE p.is_delete = 0
  AND NOT EXISTS (
      SELECT 1
      FROM course exact_course
      WHERE exact_course.id = p.course_id
        AND exact_course.isDelete = 0
  )
  AND CHAR_LENGTH(REPLACE(LOWER(c.name), ' ', '')) >= 2
  AND (
      REPLACE(LOWER(p.course_name), ' ', '') COLLATE utf8mb4_0900_ai_ci
          LIKE CONCAT('%', REPLACE(LOWER(c.name), ' ', ''), '%') COLLATE utf8mb4_0900_ai_ci
      OR REPLACE(LOWER(c.name), ' ', '') COLLATE utf8mb4_0900_ai_ci
          LIKE CONCAT('%', REPLACE(LOWER(p.course_name), ' ', ''), '%') COLLATE utf8mb4_0900_ai_ci
  )
GROUP BY p.id
HAVING COUNT(DISTINCT c.id) = 1;

UPDATE community_post post
INNER JOIN community_course_id_repair repair
    ON repair.post_id = post.id
SET post.course_id = repair.course_id,
    post.update_time = NOW();

SELECT ROW_COUNT() AS repaired_post_count;

DROP TEMPORARY TABLE community_course_id_repair;

COMMIT;
