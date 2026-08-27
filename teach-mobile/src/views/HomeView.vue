<script setup lang="ts">
import { onMounted } from 'vue'
import { useSessionStore } from '@/stores/session'
import { useStudentHomeStore } from '@/stores/studentHome'

const session = useSessionStore()
const home = useStudentHomeStore()

onMounted(() => {
  home.load()
})
</script>

<template>
  <main class="page home-page">
    <header class="hero">
      <div>
        <p>今天继续前进</p>
        <h1>{{ session.displayName }}，早上好</h1>
      </div>
      <button class="secondary-button" :disabled="home.checkInLoading" @click="home.checkIn">
        {{ home.checkInLoading ? '打卡中' : '打卡' }}
      </button>
    </header>

    <section class="today-panel panel">
      <div>
        <span>今日学习</span>
        <strong>{{ home.todayMinutes }}</strong>
        <small>分钟</small>
      </div>
      <div>
        <span>连续活跃</span>
        <strong>{{ home.activeDays }}</strong>
        <small>天</small>
      </div>
      <div>
        <span>画像分</span>
        <strong>{{ home.overallScore }}</strong>
        <small>/100</small>
      </div>
    </section>

    <p v-if="home.lastError" class="soft-warning">{{ home.lastError }}</p>

    <section class="section-title">
      <h2>今日待办</h2>
      <RouterLink class="ghost-link" to="/homework">{{ home.pendingHomework.length }} 项作业</RouterLink>
    </section>
    <section class="focus-list">
      <template v-for="task in home.focusTasks" :key="task.id">
        <RouterLink v-if="task.to" class="focus-card panel" :to="task.to">
          <span>{{ task.tag }}</span>
          <div>
            <h3>{{ task.title }}</h3>
            <p>{{ task.meta }}</p>
          </div>
          <strong>处理</strong>
        </RouterLink>
        <article v-else class="focus-card panel">
          <span>{{ task.tag }}</span>
          <div>
            <h3>{{ task.title }}</h3>
            <p>{{ task.meta }}</p>
          </div>
          <strong>安排</strong>
        </article>
      </template>
      <div v-if="!home.loading && !home.focusTasks.length" class="empty-state panel">
        今天没有新的待办，适合复盘最近一次作业。
      </div>
    </section>

    <section class="section-title">
      <h2>今日推荐</h2>
      <span>来自学习画像</span>
    </section>
    <section class="recommend-list">
      <article
        v-for="item in home.profile?.recommendations?.slice(0, 3)"
        :key="item.id"
        class="recommend-card panel"
      >
        <span>{{ item.knowledgeName || item.resourceType }}</span>
        <h3>{{ item.resourceTitle }}</h3>
        <p>{{ item.recommendationReason }}</p>
      </article>
      <div v-if="!home.profile?.recommendations?.length" class="empty-state panel">
        登录后会根据你的课程、练习和错题生成推荐。
      </div>
    </section>

    <section class="section-title">
      <h2>我的课程</h2>
      <RouterLink class="ghost-link" to="/courses">全部</RouterLink>
    </section>
    <section class="course-strip">
      <article v-for="course in home.courses.slice(0, 4)" :key="course.id" class="course-card panel">
        <div class="course-cover">
          <img v-if="course.coverImg" :src="course.coverImg" alt="" />
          <span v-else>{{ course.name.slice(0, 1) }}</span>
        </div>
        <h3>{{ course.name }}</h3>
        <p>{{ course.teacherName || '平台课程' }}</p>
      </article>
      <div v-if="!home.courses.length" class="empty-state panel">暂无课程数据。</div>
    </section>
  </main>
</template>

<style scoped>
.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-top: 8px;
}

.hero p {
  margin: 0 0 8px;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 900;
}

.hero h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.16;
  letter-spacing: 0;
}

.today-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 22px;
  padding: 16px;
}

.today-panel div {
  min-width: 0;
}

.today-panel span,
.today-panel small {
  display: block;
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.today-panel strong {
  display: inline-block;
  margin: 6px 2px 2px 0;
  color: var(--green-deep);
  font-size: 30px;
  line-height: 1;
}

.soft-warning {
  margin: 12px 0 0;
  color: #9b5d21;
  font-size: 12px;
}

.focus-list {
  display: grid;
  gap: 10px;
}

.focus-card {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.focus-card span {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 8px;
  color: #fffdf8;
  background: var(--blue);
  font-size: 12px;
  font-weight: 900;
}

.focus-card:nth-child(2n) span {
  background: var(--gold);
}

.focus-card h3 {
  margin: 0 0 5px;
  font-size: 15px;
  line-height: 1.35;
}

.focus-card p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.55;
}

.focus-card strong {
  color: var(--green-deep);
  font-size: 13px;
}

.recommend-list {
  display: grid;
  gap: 12px;
}

.recommend-card {
  padding: 16px;
}

.recommend-card span {
  color: var(--gold);
  font-size: 12px;
  font-weight: 900;
}

.recommend-card h3 {
  margin: 8px 0;
  font-size: 17px;
}

.recommend-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
  font-size: 13px;
}

.course-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.course-card {
  overflow: hidden;
  padding-bottom: 12px;
}

.course-cover {
  display: grid;
  height: 92px;
  place-items: center;
  color: #fffdf8;
  background:
    linear-gradient(135deg, rgba(31, 122, 91, 0.9), rgba(45, 95, 139, 0.74)),
    #1f7a5b;
  font-size: 34px;
  font-weight: 900;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-card h3 {
  margin: 12px 12px 4px;
  font-size: 15px;
  line-height: 1.35;
}

.course-card p {
  margin: 0 12px;
  color: var(--muted);
  font-size: 12px;
}
</style>
