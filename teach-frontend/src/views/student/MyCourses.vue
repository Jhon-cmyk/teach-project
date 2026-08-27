<template>
  <div class="edu-app">

    <main class="edu-main">
      <div class="page-shell">
        <section class="page-intro glass-panel">
          <div class="intro-copy">
            <div class="overline"><book-outlined /> 我的课程</div>
            <h1 class="page-title">班级课程与学习任务集中查看</h1>
            <p class="subtitle">查看老师分配的课程，按需进入学习页面继续完成任务。</p>
          </div>
          <div class="hero-stats">
            <div class="mini-stat">
              <span class="mini-label">课程总数</span>
              <div class="mini-value">{{ myCourseList.length }}</div>
            </div>
          </div>
        </section>

        <section class="course-section glass-panel">
          <div v-if="loading" class="loading-state glass-panel">课程加载中...</div>

          <div v-else-if="myCourseList.length === 0" class="empty-state glass-panel">
            <div class="empty-icon"><coffee-outlined /></div>
            <h3>暂时没有课程</h3>
            <p>当前班级还没有分配课程，稍后再来看看。</p>
            <button class="go-back-btn" @click="router.push('/student/dashboard')">返回工作台</button>
          </div>

          <div v-else class="course-grid">
            <div v-for="course in pagedCourseList" :key="course.id" class="course-card glass-panel" @click="goToLearn(course.id)">
              <div class="card-image-box">
                <img :src="getCover(course)" />
                <div class="tag-badge">必修</div>
                <div class="overlay-play"><div class="play-btn"><caret-right-outlined /></div></div>
              </div>
              <div class="card-content">
                <h4 class="course-title" :title="course.name">{{ course.name }}</h4>
                <div class="course-meta">
                  <div class="author-info">
                    <a-avatar :size="24" :src="course.teacherAvatar" style="background: linear-gradient(135deg, #5b6cff, #39c6ff); font-size: 10px;">T</a-avatar>
                    <span>{{ course.teacherName || '任课教师' }}</span>
                  </div>
                  <span class="enter-text">进入学习</span>
                </div>
              </div>
            </div>
          </div>

          <div class="pagination-area" v-if="myCourseList.length > 0">
            <a-pagination
              v-model:current="currentPage"
              v-model:pageSize="pageSize"  :total="myCourseList.length"
              :showSizeChanger="false"
              @change="handlePageChange"
              show-less-items
            />
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { getLoginUserRaw } from '@/utils/authStorage';
import { message } from 'ant-design-vue';

// 补全用到的 Ant Design 矢量图标
import {
  BookOutlined,
  CoffeeOutlined,
  CaretRightOutlined
} from '@ant-design/icons-vue';

const router = useRouter();
const myCourseList = ref<any[]>([]);
const userAvatar = ref('');
const userName = ref('同学你好');
const loading = ref(true);


// 🌟 修改点 3：插入以下前端分页逻辑
const currentPage = ref(1);
const pageSize = ref(8); // 每页 8 条，正好排满两行

const pagedCourseList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return myCourseList.value.slice(start, end);
});

const handlePageChange = (_page: number) => {
  // 单屏布局，无需滚动
};
// 🌟 插入结束，下方保留原有的 getCover 方法不变

const getCover = (course: any) => {
  if (course.coverImg && course.coverImg.startsWith('http')) return course.coverImg;
  return `https://picsum.photos/seed/${course.id}/320/200`;
};

const goToLearn = (id: number) => {
  if(id) router.push(`/learn/${id}`);
};

// 调用联查接口
const fetchMyClassesCourses = async () => {
  loading.value = true;
  try {
    const data = await request.get<any[], any[]>('/course/list/my-class', {
      skipErrorToast: true
    });

    myCourseList.value = data || [];
  } catch (error: any) {
    console.error(error);
    message.error(error?.message || '获取班级课程失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  const uStr = getLoginUserRaw();
  if(uStr) {
    const user = JSON.parse(uStr);
    userAvatar.value = user.userAvatar;
    userName.value = user.userName || user.userAccount;
  }
  fetchMyClassesCourses();
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

/* 1. 统一晨雾留白背景，锁定页面总高度 */
.edu-app {
  height: calc(100vh - 82px); /* 锁死高度，干掉全局滚动条 */
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%);
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #0f172a;
}

/* 删除了 bg-decoration 和 ball 相关的代码 */

/* 2. 纯白无毛玻璃的扁平质感卡片 */
.glass-panel {
  background: #FFFFFF;
  border: 1px solid rgba(0,0,0,0.03);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}

/* 3. 页面主体自适应 */
.edu-main {
  flex: 1;
  min-height: 0;
  width: 75%;
  max-width: 1600px;
  min-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
}

/* 4. 单屏布局：内部不滚动，所有内容自适应填满 */
.page-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}
.header-right { display: flex; align-items: center; gap: 12px; }
.header-chip {
  height: 44px; min-width: 44px; padding: 0 14px; display: flex; align-items: center; justify-content: center;
  border-radius: 14px; background: rgba(255,255,255,0.76); border: 1px solid rgba(226,232,240,.9); color: #64748b;
}
.user-profile {
  display: flex; align-items: center; gap: 10px; cursor: pointer; padding: 6px 8px 6px 6px;
  border-radius: 18px; background: rgba(255,255,255,.78); border: 1px solid rgba(226,232,240,.85);
}
.user-info { display: flex; flex-direction: column; line-height: 1.18; }
.username { font-size: 14px; font-weight: 800; color: #0f172a; }
.user-role { font-size: 11px; color: #94a3b8; }


.page-intro {
  padding: 16px 20px; /* 大幅减小内边距 */
  border-radius: 5px; /* 统一 5px 圆角 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-shrink: 0;
}
.intro-copy { flex: 1; }
.overline { display: inline-flex; align-items: center; gap: 6px; margin-bottom: 6px; padding: 4px 10px; border-radius: 5px; background: rgba(91,108,255,.10); color: #5b6cff; font-size: 12px; font-weight: 800; }
.page-title { margin: 0; font-size: 22px; line-height: 1.2; font-weight: 800; color: #0f172a; }
.subtitle { margin-top: 4px; font-size: 14px; line-height: 1.5; color: #64748b; max-width: 800px; }

.hero-stats { display: flex; gap: 10px; flex-shrink: 0; }
.mini-stat { min-width: 90px; padding: 10px 14px; border-radius: 5px; background: rgba(248,250,252,.82); border: 1px solid rgba(226,232,240,.86); }
.mini-label { display: block; font-size: 12px; font-weight: 700; color: #94a3b8; margin-bottom: 2px; }
.mini-value { font-size: 22px; font-weight: 800; color: #0f172a; }
@media (max-width: 1180px) {
  .header-inner { flex-wrap: wrap; height: auto; padding: 16px 20px; }
  .nav-links { order: 3; width: 100%; overflow-x: auto; }
  .edu-main { padding: 20px; }
  .page-intro { flex-direction: column; align-items: flex-start; }
}
@media (max-width: 768px) {
  .page-title { font-size: 28px; }
  .subtitle { font-size: 14px; }
  .hero-stats, .summary-chips { width: 100%; }
  .mini-stat { flex: 1 1 140px; }
}

.course-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 20px 24px;
  border-radius: 5px;
  overflow: hidden;
}
.loading-state, .empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-radius: 5px;
  padding: 40px 28px;
  text-align: center;
  gap: 8px;
}
.empty-icon { font-size: 36px; color: #94a3b8; }

/* 课程网格：4 列 × 2 行 = 8 个，自适应填满区域 */
.course-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 16px;
}

.course-card {
  border-radius: 5px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  min-height: 0;
  transition: transform .25s ease, box-shadow .25s ease;
}
.course-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(15,23,42,.08); }

/* 封面图比例 16:9 */
.card-image-box { position: relative; aspect-ratio: 16 / 9; overflow: hidden; background: #f3f4f6; flex-shrink: 0; }
.card-image-box img { width: 100%; height: 100%; object-fit: cover; transition: transform .35s ease; }
.course-card:hover .card-image-box img { transform: scale(1.05); }

.tag-badge { position: absolute; left: 10px; top: 10px; padding: 4px 8px; border-radius: 4px; background: rgba(15,23,42,.7); color: #fff; font-size: 11px; font-weight: 700; }
.overlay-play { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: linear-gradient(180deg, transparent, rgba(15,23,42,.28)); opacity: 0; transition: .25s ease; }
.course-card:hover .overlay-play { opacity: 1; }
.play-btn { width: 44px; height: 44px; border-radius: 50%; background: rgba(255,255,255,.92); color: #0f172a; display: flex; align-items: center; justify-content: center; font-size: 18px; }

/* 卡片内容区 */
.card-content { padding: 12px 14px; display: flex; flex-direction: column; flex: 1; min-height: 0; }
.course-title {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.4;
  color: #1f2937;
  font-weight: 700;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.course-meta { margin-top: auto; display: flex; align-items: center; justify-content: space-between; gap: 8px; padding-top: 10px; border-top: 1px dashed #e5e7eb; }
.author-info { display: flex; align-items: center; gap: 6px; color: #64748b; font-size: 12px; font-weight: 600; }
.enter-text { color: #5b6cff; font-size: 12px; font-weight: 700; }

/* 🌟 修改点 4：将分页样式粘贴在 CSS 的最末尾 */
/* ================= 翻页组件美化 ================= */
/* 修改原有的 .pagination-area 样式 */
.pagination-area {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px dashed #e5e7eb;
  width: 100%;
}

.pagination-area :deep(.ant-pagination-item),
.pagination-area :deep(.ant-pagination-prev),
.pagination-area :deep(.ant-pagination-next),
.pagination-area :deep(.ant-pagination-jump-prev),
.pagination-area :deep(.ant-pagination-jump-next) {
  min-width: 36px;
  height: 36px;
  line-height: 34px;
  border-radius: 6px;
  background: #FFFFFF;
  border: 1px solid #e5e7eb;
  margin: 0 4px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.pagination-area :deep(.ant-pagination-item a) {
  color: #4b5563;
  font-weight: 500;
  transition: color 0.3s;
}

/* 1. 普通页码悬浮状态：白底加蓝边，蓝字 */
.pagination-area :deep(.ant-pagination-item:hover),
.pagination-area :deep(.ant-pagination-prev:hover:not(.ant-pagination-disabled)),
.pagination-area :deep(.ant-pagination-next:hover:not(.ant-pagination-disabled)) {
  border-color: #5b6cff !important;
  background: #f8faff !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(91, 108, 255, 0.1);
}

.pagination-area :deep(.ant-pagination-item:hover a) {
  color: #5b6cff !important;
}

/* 2. 当前选中页码状态：强制蓝底白字，并干掉默认的点击外发光 */
.pagination-area :deep(.ant-pagination-item-active),
.pagination-area :deep(.ant-pagination-item-active:focus),
.pagination-area :deep(.ant-pagination-item-active:hover) {
  background-color: #5b6cff !important;
  border-color: #5b6cff !important;
  /* 👇 核心：加上 !important 强制使用我们的底部投影，覆盖掉 Ant Design 自带的淡蓝色发光光环 */
  box-shadow: 0 4px 12px rgba(91, 108, 255, 0.25) !important;
  outline: none !important; /* 拔掉轮廓线 */
  transform: none;
}

.pagination-area :deep(.ant-pagination-item-active a),
.pagination-area :deep(.ant-pagination-item-active:hover a),
.pagination-area :deep(.ant-pagination-item-active:focus a) {
  color: #ffffff !important;
}

.pagination-area :deep(.ant-pagination-prev .ant-pagination-item-link),
.pagination-area :deep(.ant-pagination-next .ant-pagination-item-link) {
  color: #6b7280;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pagination-area :deep(.ant-pagination-disabled),
.pagination-area :deep(.ant-pagination-disabled:hover) {
  background: #f9fafb;
  border-color: #e5e7eb;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.pagination-area :deep(.ant-pagination-disabled .ant-pagination-item-link) {
  color: #d1d5db;
}
</style>
