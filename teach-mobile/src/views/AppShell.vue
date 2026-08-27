<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { to: '/home', label: '首页', icon: '⌂' },
  { to: '/courses', label: '课程', icon: '◫' },
  { to: '/homework', label: '作业', icon: '□' },
  { to: '/learning', label: '诊断', icon: '◇' },
  { to: '/community', label: '互助', icon: '✦' },
  { to: '/profile', label: '我的', icon: '○' }
]

const activePath = computed(() => route.path)

function isActiveTab(path: string) {
  return activePath.value === path || activePath.value.startsWith(`${path}/`)
}
</script>

<template>
  <div class="mobile-shell">
    <RouterView />
    <nav class="tabbar" aria-label="学生端主导航">
      <RouterLink
        v-for="tab in tabs"
        :key="tab.to"
        :class="{ active: isActiveTab(tab.to) }"
        :to="tab.to"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span>{{ tab.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.mobile-shell {
  min-height: 100vh;
}

.tabbar {
  position: fixed;
  z-index: 20;
  left: 12px;
  right: 12px;
  bottom: calc(10px + var(--safe-bottom));
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  min-height: 64px;
  border: 1px solid rgba(31, 42, 46, 0.1);
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.94);
  box-shadow: 0 14px 30px rgba(31, 42, 46, 0.16);
  backdrop-filter: blur(18px);
}

.tabbar a {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 3px;
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.tabbar a.active {
  color: var(--green-deep);
}

.tab-icon {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  font-size: 16px;
}
</style>
