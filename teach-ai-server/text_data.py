import os
import pymysql
import random

# ================= 1. 数据库配置 =================
# 数据库密码只能通过本地环境变量传入。
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', '3306')),
    'user': os.getenv('DB_USERNAME', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'database': os.getenv('DB_NAME', 'teach_platform'),
    'charset': 'utf8mb4'
}

# ================= 2. 定义丰富的课程数据 =================
# 我们准备了8门课程，涵盖不同领域，确保首页看起来满满当当
COURSES_DATA = [
    {
        "title": "Python 数据分析实战 2026",
        "desc": "基于 Pandas 和 Matplotlib，带你从零处理真实商业数据，掌握数据清洗与可视化的核心技能。",
        "cover": "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/python/python-original.svg",
        "lang": "python",
        "chapters": [
            "1. Python 环境搭建 (Anaconda)", "2. Jupyter Notebook 高效使用指南", "3. NumPy 数组操作基础",
            "4. Pandas Series 与 DataFrame", "5. 真实数据读取：CSV 与 Excel", "6. 数据清洗：缺失值与异常值处理",
            "7. Matplotlib 绘图入门", "8. Seaborn 高级可视化实战", "9. 案例：电商销售数据分析", "10. 案例：股票趋势预测"
        ]
    },
    {
        "title": "Vue 3 + TS 企业级前端开发",
        "desc": "深入理解 Vue 3 组合式 API，配合 TypeScript 构建类型安全的现代化 Web 应用。",
        "cover": "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/vuejs/vuejs-original.svg",
        "lang": "typescript",
        "chapters": [
            "1. Vue 3 新特性总览", "2. Vite 项目快速构建", "3. 组合式 API (Composition API) 基础",
            "4. ref 与 reactive 的区别", "5. Vue Router 4 路由配置", "6. Pinia 状态管理实战",
            "7. TypeScript 在 Vue 中的应用", "8. 组件通信与插槽", "9. 自定义 Hooks 封装", "10. 项目打包与性能优化"
        ]
    },
    {
        "title": "Docker 容器化部署指南",
        "desc": "后端开发必备技能。学习 Docker 镜像构建、容器编排以及 CI/CD 自动化流水线基础。",
        "cover": "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/docker/docker-original.svg",
        "lang": "bash",
        "chapters": [
            "1. 为什么需要 Docker？", "2. Docker 安装与换源", "3. 核心概念：镜像、容器、仓库",
            "4. 常用命令速查手册", "5. Dockerfile 编写实战", "6. 部署 Spring Boot 应用",
            "7. 部署 Vue 前端应用", "8. Docker Compose 多容器编排", "9. 搭建私有镜像仓库", "10. K8s 简单入门概念"
        ]
    },
    {
        "title": "DeepSeek 大模型应用开发",
        "desc": "紧跟 AI 潮流。学习如何调用大模型 API，构建自己的 AI 知识库助手和智能体。",
        "cover": "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/tensorflow/tensorflow-original.svg",
        "lang": "python",
        "chapters": [
            "1. 大语言模型 (LLM) 基础", "2. Prompt Engineering 提示词工程", "3. 调用 DeepSeek API 接口",
            "4. LangChain 框架入门", "5. 向量数据库 (Vector DB) 原理", "6. RAG 检索增强生成实战",
            "7. 开发一个 AI 客服机器人", "8. 语音识别与合成集成", "9. 模型微调 (Fine-tuning) 简介", "10. AI 伦理与安全"
        ]
    },
    {
        "title": "Java 并发编程高阶",
        "desc": "突破 Java 瓶颈。深入 JUC、线程池、锁机制以及 JVM 内存模型，面试加分神器。",
        "cover": "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg",
        "lang": "java",
        "chapters": [
            "1. 进程与线程的区别", "2. Thread 类与 Runnable 接口", "3. synchronized 关键字底层原理",
            "4. volatile 与 内存可见性", "5. JUC 锁：ReentrantLock", "6. 线程池 ThreadPoolExecutor 详解",
            "7. Atomic 原子类", "8. AQS 抽象队列同步器", "9. ThreadLocal 源码分析", "10. 高并发系统设计原则"
        ]
    },
    {
        "title": "MySQL 数据库性能调优",
        "desc": "从 CRUD 到架构师。掌握索引优化、SQL 执行计划分析以及分库分表策略。",
        "cover": "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg",
        "lang": "sql",
        "chapters": [
            "1. MySQL 架构深入解析", "2. InnoDB 存储引擎特性", "3. 索引底层：B+树详解",
            "4. Explain 执行计划分析", "5. 慢查询优化实战", "6. 事务隔离级别与锁",
            "7. MVCC 多版本并发控制", "8. Redo Log 与 Undo Log", "9. 主从复制与读写分离", "10. 分库分表 ShardingSphere"
        ]
    },
    {
        "title": "Redis 缓存核心技术",
        "desc": "高性能系统的基石。学习 Redis 数据结构、持久化机制以及缓存击穿/穿透/雪崩解决方案。",
        "cover": "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/redis/redis-original.svg",
        "lang": "bash",
        "chapters": [
            "1. Redis 安装与基本配置", "2. 五大基础数据结构", "3. 发布订阅与 Stream",
            "4. RDB 与 AOF 持久化对比", "5. 哨兵模式 (Sentinel) 搭建", "6. Redis Cluster 集群原理",
            "7. 缓存穿透、击穿、雪崩解决方案", "8. 分布式锁 Redisson 实战", "9. BitMap 与 HyperLogLog",
            "10. Redis 6.0 多线程模型"
        ]
    },
    {
        "title": "Linux 运维命令行速查",
        "desc": "服务器管理必备。Vim 编辑器、文件权限、进程管理、网络排查命令大全。",
        "cover": "https://cdn.jsdelivr.net/gh/devicons/devicon/icons/linux/linux-original.svg",
        "lang": "bash",
        "chapters": [
            "1. Linux 文件系统目录结构", "2. 文件管理：ls, cd, cp, mv", "3. 权限管理：chmod, chown",
            "4. 文本处理：grep, awk, sed", "5. 进程管理：ps, top, kill", "6. 网络排查：netstat, curl, ping",
            "7. Vim 编辑器从入门到精通", "8. Shell 脚本编写基础", "9. Crontab 定时任务", "10. Systemd 服务管理"
        ]
    }
]


def get_connection():
    return pymysql.connect(**DB_CONFIG)


def generate_markdown_content(course_title, chapter_title, lang):
    """
    生成带对应语言高亮的 Markdown 内容
    """
    code_snippet = ""
    if lang == 'python':
        code_snippet = f"""
```python
import pandas as pd

def analyze_data():
    print("正在处理: {chapter_title}")
    df = pd.read_csv('data.csv')
    return df.describe()
```"""
    elif lang == 'java':
        code_snippet = f"""
```java
public class Demo {{
    public static void main(String[] args) {{
        System.out.println("学习章节: {chapter_title}");
    }}
}}
```"""
    elif lang == 'typescript':
        code_snippet = f"""
```typescript
interface User {{
    id: number;
    name: string;
}}

const init = (title: string) => {{
    console.log(`Loading ${{title}}...`);
}}
```"""
    else:
        code_snippet = f"""
```bash
# 执行以下命令
docker run -d --name demo-app -p 8080:80 nginx
echo "正在学习: {chapter_title}"
```"""

    return f"""
# {chapter_title}

> 📚 教程归属：《{course_title}》

## 1. 核心概念
欢迎来到 **{chapter_title}** 的学习界面。本章节我们将深入探讨该技术的核心原理。

![Banner](https://picsum.photos/800/300?random={random.randint(1, 1000)})

## 2. 关键知识点
- **原理分析**：理解底层运行机制。
- **实战应用**：如何在生产环境中使用。
- **常见误区**：新手容易踩的坑。

## 3. 代码演示 🔥
请阅读以下代码片段，并尝试在本地环境中运行：

{code_snippet}

## 4. 总结
通过本节学习，你应该已经掌握了基本的操作流程。
建议完成课后练习题，巩固所学知识。
"""


def init_data():
    conn = None
    cursor = None
    try:
        print("🚀 [Python脚本] 开始连接数据库...")
        conn = get_connection()
        cursor = conn.cursor()

        # --- 0. (可选) 清空旧数据，防止重复 ---
        # 如果你想保留旧数据，请注释掉这几行
        print("🧹 正在清理旧数据...")
        cursor.execute("TRUNCATE TABLE text_node")  # 先删子表
        cursor.execute("DELETE FROM text_course")  # 为了重置自增ID，可以用 TRUNCATE text_course，但因为有外键约束，DELETE 更稳妥
        cursor.execute("ALTER TABLE text_course AUTO_INCREMENT = 1")  # 重置 ID 从 1 开始
        print("🧹 旧数据清理完毕。")
        # ------------------------------------

        for course in COURSES_DATA:
            print(f"📚 正在创建课程: {course['title']}...")

            # 1. 插入课程
            sql_course = "INSERT INTO text_course (name, cover_img, description) VALUES (%s, %s, %s)"
            cursor.execute(sql_course, (course['title'], course['cover'], course['desc']))
            course_id = cursor.lastrowid

            # 2. 插入章节
            print(f"   📝 正在生成 {len(course['chapters'])} 个章节...")
            sql_node = "INSERT INTO text_node (course_id, title, content, sort_order) VALUES (%s, %s, %s, %s)"

            for i, chap_title in enumerate(course['chapters']):
                content = generate_markdown_content(course['title'], chap_title, course['lang'])
                cursor.execute(sql_node, (course_id, chap_title, content, i + 1))

            print(f"   ✅ {course['title']} 生成完毕！")

        conn.commit()
        print("\n🎉🎉🎉 所有数据填充完成！快去刷新前端页面看看效果吧！")

    except Exception as e:
        if conn:
            conn.rollback()
        print(f"\n❌ 发生错误: {e}")

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()


if __name__ == "__main__":
    init_data()
