<template>
  <a-layout class="modern-layout">
    <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible theme="dark" class="modern-sider" width="240">
      <div class="logo-area">
        <div class="logo-icon">
          <img :src="teacherLogo" alt="智慧教育教师端" class="teacher-logo-image" />
        </div>
        <span v-if="!collapsed" class="logo-text">智慧教育教师端</span>
      </div>

      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        class="custom-menu"
      >
        <a-menu-item key="dashboard" @click="router.push('/teacher/dashboard')">
          <template #icon><appstore-outlined class="menu-icon" /></template>
          <span>工作台</span>
        </a-menu-item>
        <a-menu-item key="schedule" @click="router.push('/teacher/schedule')">
          <template #icon><calendar-outlined class="menu-icon" /></template>
          <span>教务安排</span>
        </a-menu-item>
        <a-menu-item key="ai" @click="router.push('/teacher/ai')">
          <template #icon><robot-outlined class="menu-icon" /></template>
          <span>AI 备课室</span>
        </a-menu-item>
        <a-menu-item key="writing" @click="router.push('/teacher/writing')">
          <template #icon><edit-outlined class="menu-icon" /></template>
          <span>智能编写</span>
        </a-menu-item>
        <a-menu-item key="resources" @click="router.push('/teacher/resources')">
          <template #icon><folder-open-outlined class="menu-icon" /></template>
          <span>我的资源</span>
        </a-menu-item>
        <a-menu-item key="search" @click="router.push('/teacher/search')">
          <template #icon><search-outlined class="menu-icon" /></template>
          <span>资源检索</span>
        </a-menu-item>
        <a-menu-item key="case-management" @click="router.push('/teacher/case-management')">
          <template #icon><snippets-outlined class="menu-icon" /></template>
          <span>案例管理</span>
        </a-menu-item>
        <a-menu-item key="coding-bank" @click="router.push('/teacher/coding-bank')">
          <template #icon><book-outlined class="menu-icon" /></template>
          <span>题库管理</span>
        </a-menu-item>
        <a-menu-item key="courses" @click="router.push('/teacher/courses')">
          <template #icon><book-outlined class="menu-icon" /></template>
          <span>教学管理</span>
        </a-menu-item>
        <a-menu-item key="monitor" @click="router.push('/teacher/monitor')">
          <template #icon><dashboard-outlined class="menu-icon" /></template>
          <span>学情监控</span>
        </a-menu-item>
        <a-menu-item key="visual" @click="router.push('/teacher/visual')">
          <template #icon><line-chart-outlined class="menu-icon" /></template>
          <span>数据分析</span>
        </a-menu-item>
        <a-menu-item key="graph" @click="router.push('/teacher/graph')">
          <template #icon><share-alt-outlined class="menu-icon" /></template>
          <span>知识图谱</span>
        </a-menu-item>
        <a-menu-item key="analysis" @click="router.push('/teacher/analysis')">
          <template #icon><audio-outlined class="menu-icon" /></template>
          <span>评课系统</span>
        </a-menu-item>
        <a-menu-item key="community" @click="router.push('/teacher/community')">
          <template #icon><message-outlined class="menu-icon" /></template>
          <span>社区处理</span>
        </a-menu-item>
      </a-menu>

      <div class="sider-footer" @click="collapsed = !collapsed">
        <span v-if="!collapsed" class="collapse-text">收起侧边栏</span>
        <menu-unfold-outlined v-if="collapsed" class="collapse-icon" />
        <menu-fold-outlined v-else class="collapse-icon" />
      </div>
    </a-layout-sider>

    <a-layout class="main-workspace">
      <a-layout-header class="modern-header">
        <div class="header-left">
          <div class="greeting-box">
            <div>
              <div class="greeting-main">您好，{{ displayTeacherName }}</div>
              <div class="greeting-sub">{{ currentDateText }} · 今日教学工作台</div>
            </div>
          </div>
        </div>

        <div class="header-right">
          <a-dropdown placement="bottomRight" :trigger="['click', 'hover']">
            <div class="user-profile">
              <a-avatar :size="40" :src="userInfo.userAvatar || defaultAvatar" class="premium-avatar" />
              <div class="user-info">
                <span class="name">{{ displayTeacherName }}</span>
                <span class="role">教师端</span>
              </div>
              <down-outlined class="dropdown-trigger" />
            </div>

            <template #overlay>
              <a-menu class="profile-dropdown-menu">
                <a-menu-item @click="goToProfile" class="dropdown-item">
                  <user-outlined /> 个人中心
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="handleLogout" class="dropdown-item text-danger">
                  <logout-outlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="modern-content">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide" mode="out-in">
            <component
              :is="Component"
              :key="route.fullPath"
              v-if="Component"
            />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
  <TeacherGlobalAssistant />
</template>

<script setup lang="ts">
import { getLoginUserRaw } from '@/utils/authStorage'
import { computed, ref, watch, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/user'
import TeacherGlobalAssistant from '@/components/TeacherGlobalAssistant.vue'
import teacherLogo from '@/assets/teacher-logo.png'
import {
  UserOutlined, LogoutOutlined, DownOutlined,
  AppstoreOutlined, ShareAltOutlined, BookOutlined,
  LineChartOutlined, FolderOpenOutlined, SearchOutlined,
  DashboardOutlined, RobotOutlined, MenuUnfoldOutlined, MenuFoldOutlined,
  AudioOutlined, MessageOutlined, EditOutlined, SnippetsOutlined,
  CalendarOutlined
} from '@ant-design/icons-vue'

const router = useRouter();
const route = useRoute();
const collapsed = ref(false);
const selectedKeys = ref<string[]>(['dashboard']);
const tabletSidebarQuery = '(max-width: 1180px)';

const userInfo = ref<any>({});
const defaultAvatar = 'https://api.dicebear.com/7.x/notionists/svg?seed=Teacher';

const getSelectedMenuKey = (path: string) => {
  if (path.includes('/teacher/coding-bank')) return 'coding-bank';
  if (path.includes('/teacher/case-management')) return 'case-management';
  if (path.includes('/teacher/schedule')) return 'schedule';
  if (path.includes('/teacher/graph')) return 'graph';
  if (path.includes('/teacher/courses')) return 'courses';
  if (path.includes('/teacher/visual')) return 'visual';
  if (path.includes('/teacher/resources')) return 'resources';
  if (path.includes('/teacher/search')) return 'search';
  if (path.includes('/teacher/monitor')) return 'monitor';
  if (path.includes('/teacher/community')) return 'community';
  if (path.includes('/teacher/analysis')) return 'analysis';
  if (path.includes('/teacher/writing')) return 'writing';
  if (path.includes('/teacher/ai')) return 'ai';
  if (path.includes('/teacher/dashboard')) return 'dashboard';
  return '';
};

const displayTeacherName = computed(() => {
  return userInfo.value?.userName || userInfo.value?.name || userInfo.value?.userAccount || '老师'
})

const currentDateText = computed(() => {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  }).format(new Date())
})

onMounted(() => {
  document.documentElement.classList.add('auth-no-scroll')

  const userStr = getLoginUserRaw();
  if (userStr) {
    userInfo.value = JSON.parse(userStr);
  }

  syncSidebarForViewport()
  window.addEventListener('resize', syncSidebarForViewport)
});

watch(() => route.path, (path) => {
  const selectedKey = getSelectedMenuKey(path);
  selectedKeys.value = selectedKey ? [selectedKey] : [];
}, { immediate: true });

const goToProfile = () => {
  router.push('/teacher/profile');
  selectedKeys.value = [];
};

onUnmounted(() => {
  window.removeEventListener('resize', syncSidebarForViewport)
  document.documentElement.classList.remove('auth-no-scroll')
})

const syncSidebarForViewport = () => {
  collapsed.value = window.matchMedia(tabletSidebarQuery).matches
}

const userStore = useUserStore()

const handleLogout = async () => {
  await userStore.logout()
  router.push('/auth')
}
</script>

<style scoped>
.modern-layout {
  height: 100vh;
  min-height: 100vh;
  font-family:
    "PingFang SC",
    "Microsoft YaHei",
    "Noto Sans CJK SC",
    -apple-system,
    BlinkMacSystemFont,
    "Segoe UI",
    sans-serif;
  background: #f6f8fb;
}

.modern-sider {
  background: linear-gradient(180deg, #142235 0%, #101b2b 54%, #0d1725 100%) !important;
  border-right: 1px solid rgba(203, 217, 232, 0.14);
  box-shadow: 12px 0 30px rgba(15, 34, 56, 0.18);
  z-index: 20;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

:deep(.modern-sider .ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logo-area {
  height: 78px;
  flex: 0 0 78px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 22px;
  color: #edf5fb;
  font-size: 17px;
  font-weight: 800;
  border-bottom: 1px solid rgba(203, 217, 232, 0.12);
}

.logo-icon {
  width: 44px;
  height: 44px;
  background: #f8fbff;
  border: 1px solid rgba(203, 217, 232, 0.72);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 10px 22px rgba(5, 15, 28, 0.22), 0 0 0 4px rgba(203, 217, 232, 0.08);
}

.teacher-logo-image {
  width: 34px;
  height: 34px;
  object-fit: contain;
  display: block;
}

.logo-text {
  color: #eef5fb;
  letter-spacing: 0;
  white-space: nowrap;
  text-shadow: none;
}

.custom-menu {
  background: transparent !important;
  border-right: none !important;
  padding: 14px 10px 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

:deep(.custom-menu::-webkit-scrollbar) {
  width: 0;
}

:deep(.custom-menu .ant-menu-item) {
  position: relative;
  height: 40px;
  border-radius: 8px;
  margin: 0 0 4px;
  color: #b6c4d3;
  font-weight: 600;
  display: flex;
  align-items: center;
  padding-left: 14px !important;
  padding-inline-start: 14px !important;
  border: 1px solid transparent;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

:deep(.custom-menu .ant-menu-title-content) {
  font-weight: 600;
}

.menu-icon {
  font-size: 17px;
  margin-right: 10px;
  color: #94a8bc;
  transition: color 0.18s ease;
}

:deep(.custom-menu .ant-menu-item:hover) {
  color: #f8fbff;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(203, 217, 232, 0.14);
  transform: translateX(2px);
}

:deep(.custom-menu .ant-menu-item:hover .menu-icon) {
  color: #c7d7e5;
}

:deep(.custom-menu .ant-menu-item-selected) {
  background: rgba(120, 184, 201, 0.16) !important;
  color: #ffffff !important;
  border: 1px solid rgba(120, 184, 201, 0.28);
  box-shadow: none;
}

:deep(.custom-menu .ant-menu-item-selected::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 3px;
  height: 24px;
  border-radius: 999px;
  background: #78b8c9;
  box-shadow: none;
}

:deep(.custom-menu .ant-menu-item-selected .menu-icon) {
  color: #d8edf3;
}

:deep(.custom-menu.ant-menu-inline-collapsed .ant-menu-item) {
  padding-inline: calc(50% - 8px) !important;
}

:deep(.custom-menu.ant-menu-inline-collapsed .menu-icon) {
  margin-right: 0;
}

.sider-footer {
  margin-top: auto;
  height: 62px;
  padding: 0 22px;
  border-top: 1px solid rgba(203, 217, 232, 0.12);
  color: #9dadbe;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: color 0.18s ease, background 0.18s ease;
}

.sider-footer:hover {
  color: #eef5fb;
  background: rgba(255, 255, 255, 0.05);
}

.collapse-icon { font-size: 16px; }

.main-workspace {
  background: #f6f8fb;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.modern-header {
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  height: 78px;
  min-height: 78px;
  line-height: normal;
  padding: 0 34px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #d1dce9;
  box-shadow: 0 8px 22px rgba(15, 34, 56, 0.04);
  z-index: 10;
  flex: 0 0 78px;
}

.greeting-box {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
}

.greeting-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #b7ddfb;
  border-radius: 7px;
  background: #eaf6ff;
  color: #0e6fac;
  font-size: 12px;
  font-weight: 700;
}

.greeting-main {
  color: #142237;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.25;
}

.greeting-sub {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 500;
  color: #68798c;
}

.header-right { display: flex; align-items: center; }

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 5px 12px 5px 5px;
  border-radius: 8px;
  transition: background 0.18s ease, border-color 0.18s ease;
  background: #ffffff;
  border: 1px solid #d5e0eb;
  box-shadow: 0 8px 18px rgba(20, 42, 68, 0.05);
}

.user-profile:hover {
  background: #f7fbff;
  border-color: #a9cbe9;
}

.premium-avatar {
  border: 1px solid #e2e8f0;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-info .name {
  font-weight: 700;
  font-size: 14px;
  color: #1f2937;
}

.user-info .role {
  font-size: 12px;
  color: #7b8796;
  margin-top: 2px;
}

.dropdown-trigger {
  font-size: 10px;
  color: #8a96a6;
  margin-left: 2px;
  transition: transform 0.18s ease, color 0.18s ease;
}

.user-profile:hover .dropdown-trigger {
  color: #2563eb;
  transform: translateY(1px);
}

.profile-dropdown-menu {
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  border: 1px solid #e7edf5;
}

.dropdown-item {
  padding: 10px 18px;
  border-radius: 7px;
  font-weight: 500;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}

.dropdown-item:hover { background: #f8fafc; color: #1e293b; }
.text-danger { color: #ef4444 !important; }
.text-danger:hover { background: #fef2f2 !important; color: #dc2626 !important; }

.modern-content {
  flex: 1;
  padding: 26px 34px 28px;
  overflow: hidden;
  min-height: 0;
  background:
    linear-gradient(180deg, #fbfcfe 0%, rgba(246, 248, 251, 0) 180px),
    #f6f8fb;
}

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
