<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchMyCourseLearningProfile, getCourseChapters } from '@/api/student'
import type { CourseChapter, StudentLearningProfile } from '@/types/student'

const route = useRoute()
const courseId = computed(() => String(route.params.id || ''))
const courseName = computed(() => String(route.query.name || '课程学习'))

const chapters = ref<CourseChapter[]>([])
const profile = ref<StudentLearningProfile | null>(null)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  const results = await Promise.allSettled([
    getCourseChapters(courseId.value),
    fetchMyCourseLearningProfile({ courseId: courseId.value, days: 14 })
  ])

  if (results[0].status === 'fulfilled') chapters.value = results[0].value
  if (results[1].status === 'fulfilled') profile.value = results[1].value

  const failed = results.find((item) => item.status === 'rejected') as PromiseRejectedResult | undefined
  if (failed) error.value = failed.reason?.message || '课程数据暂时不可用'
  loading.value = false
}

onMounted(load)
</script>

<template>
  <main class="page">
    <header class="course-head">
      <RouterLink class="ghost-link" to="/courses">‹ 返回课程</RouterLink>
      <h1>{{ courseName }}</h1>
      <p>按章节推进学习，诊断会跟随你的练习和观看记录更新。</p>
    </header>

    <section class="course-snapshot panel">
      <div>
        <span>章节</span>
        <strong>{{ chapters.length }}</strong>
      </div>
      <div>
        <span>推荐</span>
        <strong>{{ profile?.recommendations?.length || 0 }}</strong>
      </div>
      <div>
        <span>画像分</span>
        <strong>{{ profile?.insight?.overallScore || 0 }}</strong>
      </div>
    </section>

    <p v-if="error" class="error-text">{{ error }}</p>

    <section class="section-title">
      <h2>章节目录</h2>
      <span>{{ loading ? '加载中' : '继续学习' }}</span>
    </section>
    <section class="chapter-list">
      <article v-for="chapter in chapters" :key="chapter.id" class="chapter-card panel">
        <div class="chapter-index">{{ chapter.sortOrder ?? chapter.id }}</div>
        <div>
          <h2>{{ chapter.title }}</h2>
          <p>{{ chapter.description || (chapter.videoUrl ? '视频资源已准备，可在 Web 端继续完整学习。' : '暂无章节简介。') }}</p>
        </div>
      </article>
      <div v-if="!loading && !chapters.length" class="empty-state panel">暂无章节内容。</div>
    </section>

    <section class="section-title">
      <h2>本课建议</h2>
      <span>近 14 天</span>
    </section>
    <section class="recommend-list">
      <article v-for="item in profile?.recommendations?.slice(0, 3)" :key="item.id" class="recommend-card panel">
        <strong>{{ item.knowledgeName || '学习建议' }}</strong>
        <h3>{{ item.resourceTitle }}</h3>
        <p>{{ item.recommendationReason }}</p>
      </article>
      <div v-if="!profile?.recommendations?.length" class="empty-state panel">暂无本课个性化建议。</div>
    </section>
  </main>
</template>

<style scoped>
.course-head h1 {
  margin: 14px 0 8px;
  font-size: 30px;
  line-height: 1.16;
}

.course-head p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
  font-size: 14px;
}

.course-snapshot {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 18px;
  padding: 16px;
}

.course-snapshot span {
  display: block;
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.course-snapshot strong {
  display: block;
  margin-top: 6px;
  color: var(--green-deep);
  font-size: 28px;
}

.error-text {
  color: #af4a31;
  font-size: 13px;
}

.chapter-list,
.recommend-list {
  display: grid;
  gap: 12px;
}

.chapter-card {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  padding: 14px;
}

.chapter-index {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  color: #fffdf8;
  background: var(--green);
  font-weight: 900;
}

.chapter-card h2,
.recommend-card h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.chapter-card p,
.recommend-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.65;
  font-size: 13px;
}

.recommend-card {
  padding: 16px;
}

.recommend-card strong {
  color: var(--gold);
  font-size: 12px;
}
</style>
