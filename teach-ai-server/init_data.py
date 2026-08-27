import os
import requests
import pymysql
import time
import random

# ================= 配置区域 =================

# 1. 数据库配置
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USERNAME', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'db': os.getenv('DB_NAME', 'teach_platform'),
    'charset': 'utf8mb4'
}

# 2. 演示视频地址 (数据库里的 videoUrl 将全部指向这个)
DEFAULT_VIDEO_URL = "http://localhost:8820/api/profile/6828565b3d8d4d7d81164c3377a9dc24.mp4"

# 3. B 站 Cookie 只能通过本地环境变量传入，不得写入源码或提交到 Git。
MY_COOKIE = os.getenv('BILIBILI_COOKIE', '')


# ===========================================

def get_bilibili_data(keyword):
    """从 B站 API 获取数据 (带 Cookie 伪装)"""
    print(f"🕷️ 正在爬取关键词: {keyword} ...")

    # B站搜索接口
    url = "https://api.bilibili.com/x/web-interface/search/type"

    params = {
        "search_type": "video",
        "keyword": keyword,
        "page": 1,
        "page_size": 10
    }

    # 伪装头：加上 Referer 和 Cookie，假装是从浏览器发出的
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer": "https://search.bilibili.com/",
        "Cookie": MY_COOKIE  # <--- 注入灵魂
    }

    try:
        resp = requests.get(url, params=params, headers=headers)

        # 调试：如果报错，打印出来看看 B站 回复了啥
        if resp.status_code != 200:
            print(f"❌ 请求被拦截，状态码: {resp.status_code}")
            return []

        data = resp.json()  # 尝试解析 JSON

        if data['code'] == 0:
            return data['data']['result']
        else:
            print(f"⚠️ API 返回错误: {data.get('message')}")
            return []

    except Exception as e:
        print(f"❌ 爬取异常: {e}")
        # 如果解析失败，可能是 Cookie 过期或者没填对
        print("💡 提示：请检查 Cookie 是否填写正确且完整。")
        return []


def save_to_db(video_list):
    """写入数据库 (适配驼峰命名 coverImg, videoUrl, 并补充 teacherId)"""
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    success_count = 0

    # ⚠️ 修正点1：SQL 语句里加上了 teacherId
    sql = """
        INSERT INTO course 
        (name, description, coverImg, videoUrl, type, price, pointsCost, teacherName, createTime, teacherId)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW(), 1)
    """
    # ↑ 注意最后那个 1，就是强行指定 teacherId 为 1 (管理员ID)

    for v in video_list:
        # 数据清洗
        title = v['title'].replace('<em class="keyword">', '').replace('</em>', '')
        desc = v['description'][:200]

        # 处理封面图
        cover = v['pic']
        if cover.startswith("//"):
            cover = "https:" + cover

        author = v['author']

        # 随机生成价格和积分
        price = random.choice([0, 0, 99, 199])
        points = 0 if price == 0 else random.randint(50, 500)
        course_type = "video"

        try:
            # ⚠️ 修正点2：cursor.execute 不需要改参数数量，因为 1 已经硬编码在 SQL 里了
            cursor.execute(sql, (
                title,
                desc,
                cover,
                DEFAULT_VIDEO_URL,
                course_type,
                price,
                points,
                author
            ))
            success_count += 1
            print(f"   [OK] {title[:15]}...")
        except Exception as e:
            print(f"   [Error] 插入失败: {e}")

    conn.commit()
    cursor.close()
    conn.close()
    return success_count


if __name__ == "__main__":
    if not MY_COOKIE:
        print("❌ 错误：请先在本地设置 BILIBILI_COOKIE 环境变量。")
        exit()

    KEYWORDS = ["Java教程", "Python入门", "Vue3实战", "人工智能基础"]

    total_added = 0
    for kw in KEYWORDS:
        videos = get_bilibili_data(kw)
        if videos:
            count = save_to_db(videos)
            total_added += count
        time.sleep(2)  # 礼貌延迟

    print(f"\n🎉 全部完成！共入库 {total_added} 条真实课程数据。")
