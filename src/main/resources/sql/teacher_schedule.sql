-- 教师排课表
CREATE TABLE IF NOT EXISTS teacher_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    course_name VARCHAR(255) NOT NULL COMMENT '课程名称',
    linked_course_id BIGINT COMMENT '关联的平台/网络课程ID',
    class_name VARCHAR(255) COMMENT '班级名称',
    teaching_plan_id BIGINT COMMENT '关联教案ID（ai_resource）',
    week_start INT NOT NULL COMMENT '开始周次',
    week_end INT NOT NULL COMMENT '结束周次',
    day_of_week INT NOT NULL COMMENT '星期几：1-周一, 2-周二 ... 7-周日',
    start_period INT NOT NULL COMMENT '开始节次',
    end_period INT NOT NULL COMMENT '结束节次',
    semester_label VARCHAR(50) NOT NULL COMMENT '学年学期标签，如 2025-2026-2',
    is_delete TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除, 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师排课表';
