<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMyClassCourses, getRecommendedCourses } from '@/api/student'
import type { Course } from '@/types/student'

const courses = ref<Course[]>([])
const loading = ref(false)
const keyword = ref('')
const error = ref('')

function asRecords(payload: { records?: Course[] } | Course[]) {
  return Array.isArray(payload) ? payload : payload.records ?? []
}

async function loadCourses() {
  loading.value = true
  error.value = ''
  try {
    if (keyword.value) {
      courses.value = asRecords(await getRecommendedCourses({ current: 1, size: 20, name: keyword.value }))
    } else {
      try {
        courses.value = await getMyClassCourses()
      } catch {
        courses.value = asRecords(await getRecommendedCourses({ current: 1, size: 20 }))
      }
    }
  } catch (err: any) {
    error.value = err?.message || '课程加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadCourses)
</script>

<template>
  <main class="page">
    <header class="topbar">
      <div>
        <p>Course Library</p>
        <h1>课程</h1>
      </div>
    </header>

    <form class="search-panel panel" @submit.prevent="loadCourses">
      <input v-model.trim="keyword" placeholder="搜索课程、方向或教师" />
      <button class="primary-button" type="submit">搜索</button>
    </form>

    <p v-if="error" class="error-text">{{ error }}</p>

    <section class="course-list">
      <RouterLink
        v-for="course in courses"
        :key="course.id"
        class="course-row panel"
        :to="{ name: 'CourseDetail', params: { id: course.id }, query: { name: course.name } }"
      >
        <div class="cover">
          <img v-if="course.coverImg" :src="course.coverImg" alt="" />
          <span v-else>{{ course.name.slice(0, 1) }}</span>
        </div>
        <div>
          <h2>{{ course.name }}</h2>
          <p>{{ course.description || course.teacherName || '继续学习并完成配套练习。' }}</p>
          <span>{{ course.teacherName || '平台课程' }}</span>
        </div>
      </RouterLink>

      <div v-if="!loading && !courses.length" class="empty-state panel">没有找到课程。</div>
      <div v-if="loading" class="loading-list">
        <div v-for="item in 4" :key="item" class="skeleton"></div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.topbar p {
  margin: 0 0 6px;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 900;
}

.topbar h1 {
  margin: 0;
  font-size: 32px;
}

.search-panel {
  display: grid;
  grid-template-columns: 1fr 76px;
  gap: 10px;
  margin: 18px 0;
  padding: 10px;
}

input {
  min-width: 0;
  min-height: 44px;
  border: 0;
  border-radius: 8px;
  padding: 0 12px;
  background: rgba(31, 42, 46, 0.06);
  outline: none;
}

.error-text {
  color: #af4a31;
  font-size: 13px;
}

.course-list {
  display: grid;
  gap: 12px;
}

.course-row {
  display: grid;
  grid-template-columns: 86px 1fr;
  gap: 12px;
  padding: 10px;
}

.cover {
  display: grid;
  width: 86px;
  height: 86px;
  place-items: center;
  border-radius: 8px;
  overflow: hidden;
  color: #fffdf8;
  background: linear-gradient(135deg, var(--blue), var(--green));
  font-size: 28px;
  font-weight: 900;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-row h2 {
  margin: 2px 0 6px;
  font-size: 17px;
}

.course-row p {
  display: -webkit-box;
  margin: 0 0 8px;
  overflow: hidden;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.course-row span {
  color: var(--gold);
  font-size: 12px;
  font-weight: 900;
}

.loading-list {
  display: grid;
  gap: 12px;
}

.loading-list .skeleton {
  height: 106px;
  border-radius: 8px;
}
</style>
