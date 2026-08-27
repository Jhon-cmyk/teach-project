ALTER TABLE `student_learning_preference`
    ADD COLUMN `universityName` varchar(120) NOT NULL DEFAULT '' COMMENT 'current university name' AFTER `personalityType`,
    ADD COLUMN `developmentGoal` varchar(30) NOT NULL DEFAULT '' COMMENT 'postgraduate/employment/undecided' AFTER `universityName`;
