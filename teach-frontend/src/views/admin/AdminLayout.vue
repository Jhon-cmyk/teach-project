<template>
  <a-layout class="admin-layout">
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      width="248"
      class="admin-sider"
    >
      <div class="brand-block" @click="goDashboard">
        <div class="brand-mark">管</div>
        <div v-if="!collapsed" class="brand-copy">
          <span class="brand-title">智慧教育管理端</span>
          <span class="brand-subtitle">平台运营与内容管理</span>
        </div>
      </div>

      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="inline"
        theme="light"
        class="admin-menu"
      >
        <a-menu-item key="dashboard" @click="router.push('/admin/dashboard')">
          <template #icon><AppstoreOutlined /></template>
          <span>管理工作台</span>
        </a-menu-item>

        <a-menu-item key="courses" @click="router.push('/admin/courses')">
          <template #icon><BookOutlined /></template>
          <span>平台课程</span>
        </a-menu-item>

        <a-menu-item key="tutorials" @click="router.push('/admin/tutorials')">
          <template #icon><ReadOutlined /></template>
          <span>图文教程</span>
        </a-menu-item>

        <a-menu-item key="ai-resources" @click="router.push('/admin/ai-resources')">
          <template #icon><RobotOutlined /></template>
          <span>AI资源</span>
        </a-menu-item>

        <a-menu-item key="model-configs" @click="router.push('/admin/model-configs')">
          <template #icon><SettingOutlined /></template>
          <span>接口服务</span>
        </a-menu-item>

        <a-menu-item key="knowledge-base" @click="router.push('/admin/knowledge-base')">
          <template #icon><DatabaseOutlined /></template>
          <span>星火知识库</span>
        </a-menu-item>

        <a-menu-item key="cases" @click="router.push('/admin/cases')">
          <template #icon><FileTextOutlined /></template>
          <span>平台案例</span>
        </a-menu-item>

        <a-menu-item key="assets" @click="router.push('/admin/assets')">
          <template #icon><PictureOutlined /></template>
          <span>运营素材</span>
        </a-menu-item>

        <a-menu-item key="users" @click="router.push('/admin/users')">
          <template #icon><TeamOutlined /></template>
          <span>用户管理</span>
        </a-menu-item>

        <a-menu-item key="classes" @click="router.push('/admin/classes')">
          <template #icon><ApartmentOutlined /></template>
          <span>班级专业</span>
        </a-menu-item>

        <a-menu-item key="teacher-tracking" @click="router.push('/admin/teacher-tracking')">
          <template #icon><SolutionOutlined /></template>
          <span>教师跟踪</span>
        </a-menu-item>

        <a-menu-item key="data-transfer" @click="router.push('/admin/data-transfer')">
          <template #icon><ImportOutlined /></template>
          <span>导入导出</span>
        </a-menu-item>

        <a-menu-item key="audit-logs" @click="router.push('/admin/audit-logs')">
          <template #icon><SafetyCertificateOutlined /></template>
          <span>审计日志</span>
        </a-menu-item>
      </a-menu>

      <div class="sider-footer" @click="collapsed = !collapsed">
        <span v-if="!collapsed" class="collapse-text">收起导航</span>
        <MenuUnfoldOutlined v-if="collapsed" />
        <MenuFoldOutlined v-else />
      </div>
    </a-layout-sider>

    <a-layout class="admin-main">
      <a-layout-header class="admin-header">
        <div class="header-left">
          <div class="page-copy">
            <h1 class="page-title">{{ pageTitle }}</h1>
            <p class="page-desc">{{ pageDescription }}</p>
          </div>
        </div>

        <div class="header-right">
          <div class="admin-user">
            <a-avatar :size="40" :src="avatarUrl" class="avatar-box">
              {{ adminInitial }}
            </a-avatar>
            <div class="user-copy">
              <span class="user-name">{{ adminName }}</span>
              <span class="user-role">{{ userRoleLabel }}</span>
            </div>
          </div>

          <a-button type="primary" class="logout-btn" @click="handleLogout">
            <template #icon><LogoutOutlined /></template>
            退出登录
          </a-button>
        </div>
      </a-layout-header>

      <a-layout-content class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { getLoginUserRaw } from '@/utils/authStorage'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  ApartmentOutlined,
  AppstoreOutlined,
  BookOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  ImportOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PictureOutlined,
  ReadOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  SolutionOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const selectedKeys = ref<string[]>(['dashboard'])
const loginUser = ref<any>({})
const tabletSidebarQuery = '(max-width: 1180px)'

const pageDescriptions: Record<string, string> = {
  '/admin/dashboard': '集中查看平台整体运行概况与待处理状态',
  '/admin/courses': '统一管理平台课程内容、封面信息与发布状态',
  '/admin/tutorials': '维护学生端图文教程、章节目录和 Markdown 正文内容',
  '/admin/ai-resources': '整理教师 AI 备课室产出，建立统一资源视图',
  '/admin/model-configs': '维护 AI 模型、OSS、ASR、Judge0 等服务配置，并查看实时健康检测',
  '/admin/knowledge-base': '维护课程私有知识资料、向量状态与课程检索路由，让 AI 回答有据可查',
  '/admin/cases': '统一维护平台教学案例，供教师检索、保存与教案生成引用',
  '/admin/assets': '管理广告图、分类图标与前台展示类运营素材',
  '/admin/users': '查看学生、教师与管理员账号的整体分布与管理入口',
  '/admin/classes': '维护班级、专业、学院和学生绑定基础数据',
  '/admin/teacher-tracking': '跟踪教师是否已创建课程、AI资源和教学案例',
  '/admin/data-transfer': '导入导出学生、班级和账号基础数据',
  '/admin/audit-logs': '追踪管理员关键操作记录'
}

const routeKeyMap: Array<[string, string]> = [
  ['/admin/dashboard', 'dashboard'],
  ['/admin/courses', 'courses'],
  ['/admin/tutorials', 'tutorials'],
  ['/admin/ai-resources', 'ai-resources'],
  ['/admin/model-configs', 'model-configs'],
  ['/admin/knowledge-base', 'knowledge-base'],
  ['/admin/cases', 'cases'],
  ['/admin/assets', 'assets'],
  ['/admin/users', 'users'],
  ['/admin/classes', 'classes'],
  ['/admin/teacher-tracking', 'teacher-tracking'],
  ['/admin/data-transfer', 'data-transfer'],
  ['/admin/audit-logs', 'audit-logs']
]

onMounted(() => {
  try {
    const raw = getLoginUserRaw()
    loginUser.value = raw ? JSON.parse(raw) : {}
  } catch {
    loginUser.value = {}
  }

  syncSidebarForViewport()
  window.addEventListener('resize', syncSidebarForViewport)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncSidebarForViewport)
})

const syncSidebarForViewport = () => {
  collapsed.value = window.matchMedia(tabletSidebarQuery).matches
}

watch(
  () => route.path,
  (path) => {
    const matched = routeKeyMap.find(([prefix]) => path.startsWith(prefix))
    selectedKeys.value = [matched?.[1] || 'dashboard']
  },
  { immediate: true }
)

const adminName = computed(() => {
  return (
    loginUser.value?.userName ||
    loginUser.value?.name ||
    loginUser.value?.userAccount ||
    '平台管理员'
  )
})

const adminInitial = computed(() => String(adminName.value).slice(0, 1) || '管')
const avatarUrl = computed(() => loginUser.value?.userAvatar || '')
const userRoleLabel = computed(() => loginUser.value?.userRole === 'admin' ? '系统管理员' : '管理账号')
const pageTitle = computed(() => (route.meta.title as string) || '管理工作台')
const pageDescription = computed(() => pageDescriptions[route.path] || '平台内容、资源与账号管理入口')

const goDashboard = () => {
  router.push('/admin/dashboard')
}

const handleLogout = async () => {
  await userStore.logout()
  router.push('/auth')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f5f7fb;
}

.admin-sider {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%) !important;
  border-right: 1px solid #e7edf5;
  box-shadow: 12px 0 32px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  z-index: 20;
}

.brand-block {
  height: 78px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  border-bottom: 1px solid #eef3f8;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, #2f6bff 0%, #75a9ff 100%);
  color: #ffffff;
  font-size: 18px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(47, 107, 255, 0.24);
  flex-shrink: 0;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-title {
  font-size: 17px;
  font-weight: 700;
  color: #182230;
  line-height: 1.2;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #7a8699;
  line-height: 1.2;
}

.admin-menu {
  flex: 1;
  padding: 16px 12px;
  border-inline-end: none !important;
  background: transparent !important;
}

:deep(.admin-menu .ant-menu-item) {
  height: 46px;
  line-height: 46px;
  margin-bottom: 8px;
  border-radius: 12px;
  color: #445268;
  font-weight: 500;
}

:deep(.admin-menu .ant-menu-item .ant-menu-title-content) {
  font-size: 14px;
}

:deep(.admin-menu .ant-menu-item:hover) {
  color: #1e4ed8;
  background: #eef4ff;
}

:deep(.admin-menu .ant-menu-item-selected) {
  color: #1e4ed8 !important;
  background: linear-gradient(90deg, #edf4ff 0%, #f6faff 100%) !important;
  box-shadow: inset 0 0 0 1px #d9e6ff;
}

:deep(.admin-menu .ant-menu-item .anticon) {
  font-size: 16px;
}

.sider-footer {
  height: 64px;
  padding: 0 20px;
  border-top: 1px solid #eef3f8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #6b7a90;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sider-footer:hover {
  background: #f7faff;
  color: #1e4ed8;
}

.collapse-text {
  font-size: 14px;
  font-weight: 500;
}

.admin-main {
  height: 100vh;
  overflow: hidden;
  background: #f5f7fb;
  min-width: 0;
}

.admin-header {
  height: 80px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid #e7edf5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  overflow: hidden;
  line-height: normal;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.header-left {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.page-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  width: 100%;
  line-height: normal;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #182230;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.page-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #7a8699;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-right {
  flex-shrink: 0;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 14px;
}

.admin-user {
  min-width: 0;
  max-width: 260px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 6px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #edf1f7;
  flex-shrink: 1;
}

.avatar-box {
  background: linear-gradient(135deg, #2f6bff 0%, #75a9ff 100%);
  color: #ffffff;
  font-weight: 700;
  flex-shrink: 0;
}

.user-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #182230;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  margin-top: 2px;
  font-size: 12px;
  color: #7a8699;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logout-btn {
  border-radius: 10px;
  box-shadow: none;
  flex-shrink: 0;
}

.admin-content {
  height: calc(100vh - 80px);
  overflow: auto;
  padding: 24px;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.24s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 1200px) {
  .page-desc {
    display: none;
  }

  .admin-user {
    max-width: 200px;
  }
}

@media (max-width: 900px) {
  .admin-header {
    padding: 0 16px;
    gap: 12px;
  }

  .user-copy {
    display: none;
  }

  .admin-user {
    max-width: none;
    padding-right: 6px;
  }
}
</style>
