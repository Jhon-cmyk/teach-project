-- Platform shared teaching case library upgrade.
-- Run once on MySQL before using admin case crawling/review features.

ALTER TABLE teaching_case
    ADD COLUMN scope VARCHAR(30) NOT NULL DEFAULT 'mine' COMMENT 'mine-teacher private, platform-shared platform case' AFTER pdf_url,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'approved' COMMENT 'pending/approved/rejected/offline' AFTER scope,
    ADD COLUMN source_url VARCHAR(1000) NULL COMMENT 'Original public page or file URL' AFTER status,
    ADD COLUMN source_name VARCHAR(100) NULL COMMENT 'Source website name' AFTER source_url,
    ADD COLUMN summary TEXT NULL COMMENT 'Case summary for recommendation' AFTER source_name,
    ADD COLUMN keywords VARCHAR(500) NULL COMMENT 'Case keywords' AFTER summary,
    ADD COLUMN material_json TEXT NULL COMMENT 'Extracted material links JSON' AFTER keywords,
    ADD COLUMN structure_json MEDIUMTEXT NULL COMMENT 'Extracted case structure JSON' AFTER material_json,
    ADD COLUMN crawl_time DATETIME NULL COMMENT 'Crawl time' AFTER structure_json,
    ADD COLUMN review_time DATETIME NULL COMMENT 'Review time' AFTER crawl_time,
    ADD COLUMN reviewer_id BIGINT NULL COMMENT 'Admin reviewer user id' AFTER review_time;

CREATE INDEX idx_teaching_case_scope_status ON teaching_case(scope, status, is_delete);
CREATE INDEX idx_teaching_case_source_url ON teaching_case(source_url(255));
