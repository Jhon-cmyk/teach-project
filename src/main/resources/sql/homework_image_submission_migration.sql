ALTER TABLE homework_assignment
    ADD COLUMN answerMode VARCHAR(20) NOT NULL DEFAULT 'online' COMMENT 'online/image/mixed',
    ADD COLUMN imageGranularity VARCHAR(20) NOT NULL DEFAULT 'per_question' COMMENT 'per_question',
    ADD COLUMN gradingMode VARCHAR(20) NOT NULL DEFAULT 'auto' COMMENT 'auto/ai_review';

ALTER TABLE homework_submission
    ADD COLUMN submissionType VARCHAR(20) NOT NULL DEFAULT 'online' COMMENT 'online/image/mixed',
    ADD COLUMN gradingModeSnapshot VARCHAR(20) NOT NULL DEFAULT 'auto' COMMENT 'auto/ai_review',
    ADD COLUMN reviewStatus VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT 'none/pending/approved',
    ADD COLUMN aiSuggestedTotalScore INT DEFAULT NULL COMMENT 'AI suggested total score before teacher review',
    ADD COLUMN visionStatus VARCHAR(20) DEFAULT NULL COMMENT 'pending/completed/failed',
    ADD COLUMN visionResultJson TEXT DEFAULT NULL COMMENT 'Vision recognition result JSON';

ALTER TABLE homework_submission_detail
    ADD COLUMN imageUrlsJson TEXT DEFAULT NULL COMMENT 'Original answer image URLs JSON',
    ADD COLUMN recognizedText TEXT DEFAULT NULL COMMENT 'Vision recognized text',
    ADD COLUMN visionConfidence DOUBLE DEFAULT NULL COMMENT 'Vision confidence',
    ADD COLUMN aiSuggestedScore INT DEFAULT NULL COMMENT 'AI suggested score before teacher review';

CREATE TABLE IF NOT EXISTS homework_submission_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submissionId BIGINT NOT NULL COMMENT 'Submission ID',
    questionNo VARCHAR(64) NOT NULL COMMENT 'Question number',
    imageUrl VARCHAR(1000) NOT NULL COMMENT 'Image URL',
    imageOrder INT NOT NULL DEFAULT 0 COMMENT 'Image order in group',
    recognizedText TEXT DEFAULT NULL COMMENT 'Vision recognized text',
    visionJson TEXT DEFAULT NULL COMMENT 'Raw vision result JSON',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/completed/failed',
    errorMessage VARCHAR(1000) DEFAULT NULL COMMENT 'Vision error message',
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    INDEX idx_submission (submissionId),
    INDEX idx_submission_question (submissionId, questionNo)
) COMMENT='Homework submission image table';
