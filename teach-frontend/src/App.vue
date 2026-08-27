<script setup lang="ts">
import { computed, defineAsyncComponent, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import { useTutorContextStore } from '@/stores/tutorContext'

const GlobalFloatTools = defineAsyncComponent(
  () => import('@/components/GlobalFloatTools.vue')
)

const route = useRoute()
const tutorContext = useTutorContextStore()

const isStudentPage = computed(() => {
  if (!route.path) return false
  return route.path.startsWith('/student') || route.path.startsWith('/learn')
})

watch(
  () => route.path,
  (path) => {
    if (!path) return
    const isCourseLearn = path.startsWith('/learn/')
    const isCodingProblem = path.startsWith('/student/coding/') && path !== '/student/coding'
    if (path.startsWith('/student') && !isCourseLearn && !isCodingProblem) {
      tutorContext.setGeneralContext('学生端')
    }
  },
  { immediate: true }
)
</script>

<template>
  <RouterView />

  <GlobalFloatTools v-if="isStudentPage" />
</template>

<style>
/* 全局重置样式 */
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
}

/* 隐藏浏览器窗口级滚动条（各页面按需通过 JS 添加此 class） */
html.auth-no-scroll,
html.auth-no-scroll body {
  overflow: hidden !important;
  scrollbar-width: none !important;
}
html.auth-no-scroll::-webkit-scrollbar,
html.auth-no-scroll body::-webkit-scrollbar {
  display: none !important;
}
</style>
