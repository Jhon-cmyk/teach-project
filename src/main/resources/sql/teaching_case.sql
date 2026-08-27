-- 教学案例表
CREATE TABLE IF NOT EXISTS teaching_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    title VARCHAR(255) NOT NULL COMMENT '案例标题',
    category VARCHAR(50) NOT NULL COMMENT '案例分类：course_design-课程设计, enterprise-企业实际工程, competition-大赛资源, small_project-小项目',
    difficulty VARCHAR(20) NOT NULL COMMENT '难度等级：easy-初级, medium-中等, hard-困难',
    course_name VARCHAR(255) COMMENT '适用课程（用户自定义输入）',
    pdf_url VARCHAR(500) NOT NULL COMMENT 'PDF文件OSS地址',
    is_delete TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除, 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学案例表';
