-- Teaching case crawl preview upgrade.
-- Run once after teaching_case_platform_upgrade.sql.

ALTER TABLE teaching_case
    ADD COLUMN preview_text MEDIUMTEXT NULL COMMENT 'Stored preview text extracted during crawl' AFTER structure_json,
    ADD COLUMN preview_type VARCHAR(30) NOT NULL DEFAULT 'document' COMMENT 'document/page' AFTER preview_text,
    ADD COLUMN relevance_score INT NULL COMMENT 'Keyword relevance score from crawler' AFTER preview_type,
    ADD COLUMN crawl_keyword VARCHAR(255) NULL COMMENT 'Original crawl keyword' AFTER relevance_score;

CREATE INDEX idx_teaching_case_crawl_keyword ON teaching_case(crawl_keyword);
