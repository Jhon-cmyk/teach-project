-- Platform/private teaching case ownership migration.
-- Run after teaching_case_platform_upgrade.sql and teaching_case_preview_upgrade.sql.

ALTER TABLE teaching_case
    ADD COLUMN source_case_id BIGINT NULL COMMENT 'Platform case id copied into teacher private case' AFTER source_url;

CREATE INDEX idx_teaching_case_teacher_source_case ON teaching_case(teacher_id, source_case_id, is_delete);

UPDATE teaching_case
SET scope = 'platform',
    status = 'approved'
WHERE is_delete = 0
  AND (scope IS NULL OR scope = '' OR scope = 'mine');
