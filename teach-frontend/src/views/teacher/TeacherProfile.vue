<template>
  <div class="profile-page modern-page">
    <div class="page-header">
      <div class="title-group">
        <h2 class="page-title">个人设置中心</h2>
        <p class="page-subtitle">管理您的基础信息、头像与账户安全。</p>
      </div>
    </div>

    <div class="content-layout">
      <aside class="left-sidebar">
        <div class="user-info-card glass-panel">
          <div class="avatar-upload-wrapper">
            <a-upload
              name="file"
              list-type="picture-card"
              class="avatar-uploader"
              :show-upload-list="false"
              :action="`${API_BASE_URL}/file/upload`"
              :with-credentials="true"
              :before-upload="beforeUpload"
              @change="handleAvatarChange"
            >
              <img
                v-if="userInfo.userAvatar"
                :src="normalizeServerAssetUrl(userInfo.userAvatar)"
                alt="avatar"
                class="avatar-img"
              />
              <div v-else class="upload-placeholder">
                <loading-outlined v-if="uploadLoading" />
                <plus-outlined v-else />
                <div class="ant-upload-text" style="margin-top: 8px">上传头像</div>
              </div>
              <div class="avatar-hover-mask">
                <span class="hover-text">更换头像</span>
              </div>
            </a-upload>
          </div>

          <h3 class="card-name">{{ userInfo.userName || '未设置昵称' }}</h3>
          <p class="card-role">{{ userInfo.title || '教师' }}</p>
        </div>

        <nav class="nav-menu glass-panel">
          <div
            class="nav-item"
            :class="{ active: activeMenu === 'basic' }"
            @click="activeMenu = 'basic'"
          >
            <user-outlined />
            <span class="nav-text">基本信息</span>
          </div>
          <div
            class="nav-item"
            :class="{ active: activeMenu === 'security' }"
            @click="activeMenu = 'security'"
          >
            <safety-certificate-outlined />
            <span class="nav-text">安全设置</span>
          </div>
        </nav>
      </aside>

      <main class="main-content">
        <div v-if="pageLoading" class="glass-panel content-section loading-wrap">
          <a-spin size="large" />
          <p class="loading-text">正在加载个人信息...</p>
        </div>

        <template v-else>
          <div v-show="activeMenu === 'basic'" class="glass-panel content-section fade-in">
            <div class="section-header">
              <h3>基本信息</h3>
              <p>完善您的基础信息，让学生更好地了解您。</p>
            </div>

            <a-form layout="vertical" :model="userInfo" class="modern-form">
              <div class="form-row">
                <a-form-item label="真实姓名" class="half-width">
                  <a-input
                    v-model:value="userInfo.userName"
                    placeholder="请输入您的姓名"
                    size="large"
                    :maxlength="20"
                  />
                </a-form-item>

                <a-form-item label="教工号 / 账号" class="half-width">
                  <a-input v-model:value="userInfo.userAccount" disabled size="large" />
                </a-form-item>
              </div>

              <div class="form-row">
                <a-form-item label="所属院系" class="half-width" extra="由管理员端统一设置，教师端不可修改">
                  <a-input
                    v-model:value="userInfo.department"
                    placeholder="管理员暂未设置"
                    size="large"
                    :maxlength="50"
                    disabled
                  />
                </a-form-item>

                <a-form-item label="职称" class="half-width" extra="由管理员端统一设置，教师端不可修改">
                  <a-input
                    v-model:value="userInfo.title"
                    placeholder="管理员暂未设置"
                    size="large"
                    disabled
                  />
                </a-form-item>
              </div>

              <a-form-item label="个人简介">
                <a-textarea
                  v-model:value="userInfo.bio"
                  :rows="4"
                  :maxlength="300"
                  show-count
                  placeholder="写一段简短的个人介绍，展示您的教学理念与研究方向..."
                />
              </a-form-item>

              <div class="form-actions">
                <a-button
                  type="primary"
                  size="large"
                  @click="handleSaveBasic"
                  :loading="savingBasic"
                >
                  保存基本信息
                </a-button>
              </div>
            </a-form>
          </div>

          <div
            v-show="activeMenu === 'security'"
            class="glass-panel content-section fade-in"
          >
            <div class="section-header">
              <h3>安全设置</h3>
              <p>定期更新密码，保障您的账号安全。</p>
            </div>

            <a-form layout="vertical" :model="pwdForm" class="modern-form" style="max-width: 480px">
              <a-form-item label="当前密码" required>
                <a-input-password
                  v-model:value="pwdForm.oldPassword"
                  placeholder="请输入当前密码"
                  size="large"
                />
              </a-form-item>

              <a-form-item label="新密码" required>
                <a-input-password
                  v-model:value="pwdForm.newPassword"
                  placeholder="请输入新密码（至少 6 位）"
                  size="large"
                />
              </a-form-item>

              <a-form-item label="确认新密码" required>
                <a-input-password
                  v-model:value="pwdForm.confirmPassword"
                  placeholder="请再次输入新密码"
                  size="large"
                />
              </a-form-item>

              <div class="form-actions">
                <a-button type="primary" size="large" @click="handleSavePwd" :loading="savingPwd">
                  更新密码
                </a-button>
              </div>
            </a-form>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import { getAuthToken, setTabLoginUser } from '@/utils/authStorage'
import type { UploadChangeParam, UploadProps } from 'ant-design-vue'
import {
  UserOutlined,
  SafetyCertificateOutlined,
  LoadingOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const normalizeServerAssetUrl = (url?: string) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:image')) return url
  return `${SERVER_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
}

type UserProfileExtra = {
  department?: string
  title?: string
  bio?: string
}

type LoginUser = {
  id?: number
  userName?: string
  userAccount?: string
  userAvatar?: string
  teacherTitle?: string
  userProfile?: string
}

type ApiResponse<T = any> = {
  code?: number
  data?: T
  message?: string
  description?: string
}

const activeMenu = ref<'basic' | 'security'>('basic')
const pageLoading = ref(false)
const uploadLoading = ref(false)
const savingBasic = ref(false)
const savingPwd = ref(false)

const userInfo = reactive({
  userName: '',
  userAccount: '',
  department: '',
  title: '教师',
  bio: '',
  userAvatar: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

function getApiBase() {
  return '/api'
}

async function requestJson<T = any>(url: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
  const response = await fetch(url, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
      ...(options.headers || {})
    },
    ...options
  })

  const data = await response.json().catch(() => ({}))

  if (!response.ok) {
    throw new Error(data?.message || '请求失败')
  }

  return data
}

function safeParseUserProfile(userProfile?: string): UserProfileExtra {
  if (!userProfile) {
    return {}
  }

  try {
    const parsed = JSON.parse(userProfile)
    if (parsed && typeof parsed === 'object') {
      return {
        department: typeof parsed.department === 'string' ? parsed.department : '',
        title: typeof parsed.title === 'string' ? parsed.title : '',
        bio: typeof parsed.bio === 'string' ? parsed.bio : ''
      }
    }
  } catch (error) {
    return {
      bio: userProfile
    }
  }

  return {}
}

function fillUserInfoByServerData(user: LoginUser) {
  const profileExtra = safeParseUserProfile(user.userProfile)

  userInfo.userName = user.userName || ''
  userInfo.userAccount = user.userAccount || ''
  userInfo.userAvatar = user.userAvatar || ''
  userInfo.department = profileExtra.department || ''
  userInfo.title = user.teacherTitle || profileExtra.title || '教师'
  userInfo.bio = profileExtra.bio || ''
}

async function fetchLoginUser() {
  pageLoading.value = true
  try {
    const data = await request.get<any, any>('/user/get/login', {
      skipErrorToast: true
    })

    if (data) {
      fillUserInfoByServerData(data)
      setTabLoginUser(data)
    } else {
      message.error('获取当前用户失败')
    }
  } catch (error) {
    console.error(error)
    message.error('获取当前用户失败')
  } finally {
    pageLoading.value = false
  }
}

function buildUserProfileString() {
  return JSON.stringify({
    bio: userInfo.bio?.trim() || ''
  })
}

async function saveBasicInfo(showSuccess = true) {
  const trimmedName = userInfo.userName.trim()

  if (!trimmedName) {
    message.warning('请输入真实姓名')
    return false
  }

  savingBasic.value = true

  try {
    await request.post<any, any>(
      '/user/update/my',
      {
        userName: trimmedName,
        userProfile: buildUserProfileString(),
        userAvatar: userInfo.userAvatar || ''
      },
      {
        skipErrorToast: true
      }
    )

    if (showSuccess) {
      message.success('基本信息保存成功')
    }
    await fetchLoginUser()
    return true
  } catch (error: any) {
    console.error(error)
    message.error(error?.message || '保存基本信息失败')
    return false
  } finally {
    savingBasic.value = false
  }
}

async function handleSaveBasic() {
  await saveBasicInfo(true)
}

async function handleSavePwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
    message.warning('请完整填写密码信息')
    return
  }

  if (pwdForm.newPassword.length < 6) {
    message.warning('新密码至少需要 6 位')
    return
  }

  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    message.error('两次输入的新密码不一致')
    return
  }

  savingPwd.value = true

  try {
    await request.post<any, any>(
      '/user/update/password',
      {
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      },
      {
        skipErrorToast: true
      }
    )

    message.success('密码更新成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (error: any) {
    console.error(error)
    message.error(error?.message || '密码更新失败')
  } finally {
    savingPwd.value = false
  }
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  if (!isImage) {
    message.error('只能上传 JPG / JPEG / PNG 格式的图片')
  }

  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小必须小于 2MB')
  }

  return !!(isImage && isLt2M)
}

function extractUploadUrl(response: any): string {
  if (!response) return ''

  if (typeof response === 'string') return response
  if (typeof response?.data === 'string') return response.data
  if (typeof response?.url === 'string') return response.url
  if (typeof response?.data?.url === 'string') return response.data.url

  return ''
}

async function handleAvatarChange(info: any) {
  if (info.file.status === 'uploading') {
    uploadLoading.value = true
    return
  }

  if (info.file.status === 'done') {
    uploadLoading.value = false

    const res = info.file.response
    const avatarUrl =
      typeof res === 'string'
        ? res
        : typeof res?.data === 'string'
          ? res.data
          : typeof res?.url === 'string'
            ? res.url
            : typeof res?.data?.url === 'string'
              ? res.data.url
              : ''

    if (!avatarUrl) {
      message.error('头像上传成功，但未获取到图片地址')
      return
    }

    userInfo.userAvatar = avatarUrl
    const ok = await saveBasicInfo(false)
    if (ok) {
      message.success('头像更新成功')
    }
    return
  }

  if (info.file.status === 'error') {
    uploadLoading.value = false
    message.error('头像上传失败')
  }
}

onMounted(() => {
  fetchLoginUser()
})
</script>

<style scoped>
.modern-page {
  font-family: 'Plus Jakarta Sans', 'Microsoft YaHei', sans-serif;
  padding: 32px;
  min-height: 85vh;
  background: #f4f6fb;
  animation: fadeIn 0.4s ease;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  margin-bottom: 32px;
  flex-shrink: 0;
}

.page-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
}

.page-subtitle {
  margin: 0;
  color: #64748b;
  font-size: 15px;
}

.content-layout {
  display: flex;
  gap: 24px;
  flex: 1;
  max-width: 1200px;
}

.glass-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.03);
  border-radius: 5px;
}

.left-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-info-card {
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.avatar-upload-wrapper {
  position: relative;
  margin-bottom: 16px;
}

:deep(.ant-upload.ant-upload-select-picture-card) {
  width: 100px !important;
  height: 100px !important;
  border-radius: 50% !important;
  overflow: hidden !important;
  background: #f8fafc !important;
  padding: 0 !important;
  border: 1px solid #e2e8f0 !important;
}

:deep(.ant-upload.ant-upload-select-picture-card > .ant-upload) {
  padding: 0 !important;
  display: block !important;
  position: relative;
  width: 100%;
  height: 100%;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #94a3b8;
}

.avatar-hover-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  cursor: pointer;
}

.hover-text {
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.avatar-upload-wrapper:hover .avatar-hover-mask {
  opacity: 1;
}

.card-name {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.card-role {
  margin: 0;
  padding: 4px 12px;
  background: #eef2ff;
  color: #4f46e5;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
}

.nav-menu {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.nav-item:hover {
  background: #f8fafc;
  color: #3b82f6;
}

.nav-item.active {
  background: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.08);
}

.main-content {
  flex: 1;
  min-width: 0;
}

.content-section {
  padding: 32px;
  min-height: 520px;
  box-sizing: border-box;
}

.fade-in {
  animation: fadeIn 0.4s ease;
}

.section-header {
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.section-header h3 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.section-header p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.modern-form {
  max-width: 800px;
}

.form-row {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.half-width {
  flex: 1;
  min-width: 0;
}

.form-actions {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
  display: flex;
}

.loading-wrap {
  display: flex;
  min-height: 300px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 14px;
}

.loading-text {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

:deep(.ant-btn) {
  border-radius: 5px !important;
  font-weight: 600;
}

:deep(.ant-input),
:deep(.ant-input-password),
:deep(.ant-select-selector),
:deep(.ant-input-textarea textarea) {
  border-radius: 5px !important;
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #334155;
}

@media (max-width: 960px) {
  .content-layout {
    flex-direction: column;
  }

  .left-sidebar {
    width: 100%;
  }

  .content-section {
    min-height: auto;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
