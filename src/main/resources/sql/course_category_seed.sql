CREATE TABLE IF NOT EXISTS course_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL COMMENT 'category display name',
    icon_url VARCHAR(500) NOT NULL COMMENT 'category icon url',
    sort_order INT NOT NULL DEFAULT 0,
    is_enabled TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_category_name (name),
    KEY idx_course_category_enabled_order (is_enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='student dashboard course category icons';

SET @course_category_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'course'
      AND COLUMN_NAME = 'categoryId'
);
SET @course_category_column_sql := IF(
    @course_category_column_exists = 0,
    'ALTER TABLE course ADD COLUMN categoryId BIGINT NULL COMMENT ''course category id'' AFTER publishStatus',
    'SELECT 1'
);
PREPARE course_category_column_stmt FROM @course_category_column_sql;
EXECUTE course_category_column_stmt;
DEALLOCATE PREPARE course_category_column_stmt;

SET @course_category_course_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'course'
      AND INDEX_NAME = 'idx_course_category_id'
);
SET @course_category_course_index_sql := IF(
    @course_category_course_index_exists = 0,
    'ALTER TABLE course ADD KEY idx_course_category_id (categoryId)',
    'SELECT 1'
);
PREPARE course_category_course_index_stmt FROM @course_category_course_index_sql;
EXECUTE course_category_course_index_stmt;
DEALLOCATE PREPARE course_category_course_index_stmt;

DELETE newer FROM course_category newer
INNER JOIN course_category older
    ON newer.name = older.name
    AND newer.id > older.id;

SET @course_category_name_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'course_category'
      AND INDEX_NAME = 'uk_course_category_name'
);
SET @course_category_name_index_sql := IF(
    @course_category_name_index_exists = 0,
    'ALTER TABLE course_category ADD UNIQUE KEY uk_course_category_name (name)',
    'SELECT 1'
);
PREPARE course_category_name_index_stmt FROM @course_category_name_index_sql;
EXECUTE course_category_name_index_stmt;
DEALLOCATE PREPARE course_category_name_index_stmt;

INSERT INTO course_category (name, icon_url, sort_order, is_enabled, is_delete)
VALUES
    ('编程', '/icons/types/编程.png', 10, 1, 0),
    ('算法', '/icons/types/算法.png', 20, 1, 0),
    ('前端', '/icons/types/前端.png', 30, 1, 0),
    ('后端', '/icons/types/后端.png', 40, 1, 0),
    ('数据', '/icons/types/数据.png', 50, 1, 0),
    ('运维', '/icons/types/运维.png', 60, 1, 0),
    ('python', '/icons/types/python.png', 70, 1, 0),
    ('java', '/icons/types/java.png', 80, 1, 0),
    ('C', '/icons/types/C.png', 90, 1, 0),
    ('AI', '/icons/types/AI.png', 100, 1, 0),
    ('设计', '/icons/types/设计.png', 110, 1, 0),
    ('职场', '/icons/types/职场.png', 120, 1, 0),
    ('心理', '/icons/types/心理.png', 130, 1, 0),
    ('多模态', '/icons/types/多模态.png', 140, 1, 0),
    ('阅读', '/icons/types/阅读.png', 150, 1, 0),
    ('人工智能', '/icons/types/人工智能.png', 160, 1, 0),
    ('深度学习', '/icons/types/深度学习.png', 170, 1, 0),
    ('机器学习', '/icons/types/机器学习.png', 180, 1, 0)
ON DUPLICATE KEY UPDATE
    icon_url = VALUES(icon_url),
    sort_order = VALUES(sort_order),
    is_enabled = VALUES(is_enabled),
    is_delete = 0,
    update_time = CURRENT_TIMESTAMP;

UPDATE course c
JOIN course_category cc ON cc.name = '编程'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%编程%' OR c.description LIKE '%编程%' OR c.video_context LIKE '%编程%');

UPDATE course c
JOIN course_category cc ON cc.name = '算法'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%算法%' OR c.description LIKE '%算法%' OR c.video_context LIKE '%算法%');

UPDATE course c
JOIN course_category cc ON cc.name = '前端'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%前端%' OR c.description LIKE '%前端%' OR c.video_context LIKE '%前端%');

UPDATE course c
JOIN course_category cc ON cc.name = '后端'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%后端%' OR c.description LIKE '%后端%' OR c.video_context LIKE '%后端%');

UPDATE course c
JOIN course_category cc ON cc.name = '数据'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%数据%' OR c.description LIKE '%数据%' OR c.video_context LIKE '%数据%');

UPDATE course c
JOIN course_category cc ON cc.name = '运维'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%运维%' OR c.description LIKE '%运维%' OR c.video_context LIKE '%运维%');

UPDATE course c
JOIN course_category cc ON cc.name = 'python'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (LOWER(c.name) LIKE '%python%' OR LOWER(c.description) LIKE '%python%' OR LOWER(c.video_context) LIKE '%python%');

UPDATE course c
JOIN course_category cc ON cc.name = 'java'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (LOWER(c.name) LIKE '%java%' OR LOWER(c.description) LIKE '%java%' OR LOWER(c.video_context) LIKE '%java%');

UPDATE course c
JOIN course_category cc ON cc.name = 'AI'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (LOWER(c.name) LIKE '%ai%' OR LOWER(c.description) LIKE '%ai%' OR LOWER(c.video_context) LIKE '%ai%');

UPDATE course c
JOIN course_category cc ON cc.name = '设计'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%设计%' OR c.description LIKE '%设计%' OR c.video_context LIKE '%设计%');

UPDATE course c
JOIN course_category cc ON cc.name = '职场'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%职场%' OR c.description LIKE '%职场%' OR c.video_context LIKE '%职场%');

UPDATE course c
JOIN course_category cc ON cc.name = '心理'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%心理%' OR c.description LIKE '%心理%' OR c.video_context LIKE '%心理%');

UPDATE course c
JOIN course_category cc ON cc.name = '多模态'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%多模态%' OR c.description LIKE '%多模态%' OR c.video_context LIKE '%多模态%');

UPDATE course c
JOIN course_category cc ON cc.name = '阅读'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%阅读%' OR c.description LIKE '%阅读%' OR c.video_context LIKE '%阅读%');

UPDATE course c
JOIN course_category cc ON cc.name = '人工智能'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%人工智能%' OR c.description LIKE '%人工智能%' OR c.video_context LIKE '%人工智能%');

UPDATE course c
JOIN course_category cc ON cc.name = '深度学习'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%深度学习%' OR c.description LIKE '%深度学习%' OR c.video_context LIKE '%深度学习%');

UPDATE course c
JOIN course_category cc ON cc.name = '机器学习'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%机器学习%' OR c.description LIKE '%机器学习%' OR c.video_context LIKE '%机器学习%');
