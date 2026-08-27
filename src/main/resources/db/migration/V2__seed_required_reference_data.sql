-- Required, non-secret reference data that used to be inserted by
-- @PostConstruct schema initializers. Flyway now owns this one-time change.

INSERT INTO ai_model_config
    (interface_key, interface_name, provider, endpoint_url, model_name, enabled, remark, sort_order)
VALUES
    ('chat_stream', '通用 AI 对话', 'DeepSeek', 'https://api.deepseek.com/chat/completions', 'deepseek-chat', 1, '教师端 AI 备课室、智能编写等文本生成接口', 10),
    ('tutor_stream', '学生 AI 助教', 'DeepSeek', 'https://api.deepseek.com/chat/completions', 'deepseek-chat', 1, '学生端课程学习问答与解释接口', 20),
    ('vision_parse', '图片解析模型', 'DashScope', 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', 'qwen-vl-plus', 1, '作业图片、学习截图等视觉理解接口', 30),
    ('lesson_plan', '教案生成模型', 'DeepSeek', 'https://api.deepseek.com/chat/completions', 'deepseek-chat', 1, '教案、教学案例、课堂活动生成接口', 40),
    ('anim_json', '动画脚本生成', 'DeepSeek', 'https://api.deepseek.com/chat/completions', 'deepseek-chat', 1, '互动动画 JSON 与讲解步骤生成接口', 50),
    ('homework_review', '作业智能批改', 'OpenAI Compatible', 'https://api.deepseek.com/chat/completions', 'deepseek-chat', 1, '主观题批改、建议分数与评语生成接口', 60),
    ('oss_storage', 'OSS 对象存储', 'Aliyun OSS', 'https://oss-cn-shanghai.aliyuncs.com', 'ruyi-teach-assets', 1, '课程封面、作业图片、音视频等文件存储服务；AccessKey 仍由后端环境配置管理', 70),
    ('asr_transcribe', 'ASR 语音转写', 'Aliyun NLS', 'https://filetrans.cn-shanghai.aliyuncs.com', '2018-08-17', 1, '课堂录音、学生语音等音频转文字服务；AppKey 与 AccessKey 仍由后端环境配置管理', 80)
ON DUPLICATE KEY UPDATE interface_key = ai_model_config.interface_key;

INSERT INTO course_category
    (name, icon_url, sort_order, is_enabled, is_delete)
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
ON DUPLICATE KEY UPDATE name = course_category.name;

-- Preserve the old one-time category backfill without overwriting an existing choice.
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
  AND (c.name LIKE '%python%' OR c.description LIKE '%python%' OR c.video_context LIKE '%python%');
UPDATE course c
JOIN course_category cc ON cc.name = 'java'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%java%' OR c.description LIKE '%java%' OR c.video_context LIKE '%java%');
UPDATE course c
JOIN course_category cc ON cc.name = 'AI'
SET c.categoryId = cc.id
WHERE c.categoryId IS NULL AND c.isDelete = 0
  AND (c.name LIKE '%AI%' OR c.description LIKE '%AI%' OR c.video_context LIKE '%AI%');
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
