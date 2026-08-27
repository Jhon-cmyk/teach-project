<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import {
  clearAllReminderPreferences,
  getReminderPermissionState,
  readStudyReminderPreference,
  saveStudyReminderPreference,
  scheduleStudyReminder,
  type ReminderPermissionState
} from '@/utils/reminder'

const router = useRouter()
const session = useSessionStore()
const reminder = ref(readStudyReminderPreference())
const reminderStatus = ref<ReminderPermissionState>('prompt')
const savingReminder = ref(false)
const reminderMessage = ref('')

const reminderStatusLabel = computed(() => {
  if (reminderStatus.value === 'granted') return '已授权'
  if (reminderStatus.value === 'denied') return '已关闭'
  if (reminderStatus.value === 'unsupported') return '当前环境不支持'
  return '待授权'
})

async function logout() {
  try {
    await clearAllReminderPreferences()
  } finally {
    session.logout()
    router.replace('/login')
  }
}

async function saveReminder() {
  savingReminder.value = true
  reminderMessage.value = ''
  try {
    const result = await scheduleStudyReminder(reminder.value)
    reminderStatus.value = result.permission
    if (!reminder.value.enabled) {
      reminderMessage.value = '学习提醒已关闭'
    } else if (result.scheduled) {
      reminderMessage.value = `每天 ${reminder.value.time} 提醒你回到学习计划`
    } else if (result.permission === 'granted') {
      reminderMessage.value = '提醒偏好已保存，原生 App 内会安排系统通知'
    } else {
      reminderMessage.value = '需要开启通知权限后才能发送提醒'
    }
  } catch (err: any) {
    reminderMessage.value = err?.message || '提醒设置保存失败'
  } finally {
    saveStudyReminderPreference(reminder.value)
    savingReminder.value = false
  }
}

onMounted(async () => {
  reminderStatus.value = await getReminderPermissionState()
})
</script>

<template>
  <main class="page">
    <header class="profile-card panel">
      <div class="avatar">
        <img v-if="session.user?.userAvatar" :src="session.user.userAvatar" alt="" />
        <span v-else>{{ session.displayName.slice(0, 1) }}</span>
      </div>
      <div>
        <p>Student Profile</p>
        <h1>{{ session.displayName }}</h1>
        <span>{{ session.user?.userAccount || '学生账号' }}</span>
      </div>
    </header>

    <section class="setting-list panel">
      <button type="button">
        <span>个人资料</span>
        <strong>›</strong>
      </button>
      <button type="button">
        <span>学习记录</span>
        <strong>›</strong>
      </button>
      <button type="button">
        <span>通知设置</span>
        <strong>{{ reminderStatusLabel }}</strong>
      </button>
    </section>

    <section class="reminder-panel panel">
      <div class="reminder-head">
        <div>
          <strong>学习提醒</strong>
          <p>每天固定时间提醒你处理今日待办、复盘错题或继续课程。</p>
        </div>
        <label class="switch">
          <input v-model="reminder.enabled" type="checkbox" />
          <span></span>
        </label>
      </div>

      <label class="time-row">
        <span>提醒时间</span>
        <input v-model="reminder.time" type="time" :disabled="!reminder.enabled" />
      </label>

      <button class="primary-button reminder-save" type="button" :disabled="savingReminder" @click="saveReminder">
        {{ savingReminder ? '保存中' : '保存提醒' }}
      </button>
      <p v-if="reminderMessage" class="reminder-message">{{ reminderMessage }}</p>
    </section>

    <section class="mobile-boundary panel">
      <strong>移动端开发边界</strong>
      <p>本 App 使用独立工程、独立路由、独立样式和原生插件，不复写 Web 端页面。后续新增功能也优先接入已有 API。</p>
    </section>

    <button class="secondary-button logout" type="button" @click="logout">退出登录</button>
  </main>
</template>

<style scoped>
.profile-card {
  display: grid;
  grid-template-columns: 78px 1fr;
  gap: 14px;
  align-items: center;
  padding: 16px;
}

.avatar {
  display: grid;
  width: 78px;
  height: 78px;
  place-items: center;
  border-radius: 8px;
  overflow: hidden;
  color: #fffdf8;
  background: linear-gradient(135deg, var(--green), var(--blue));
  font-size: 32px;
  font-weight: 900;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-card p {
  margin: 0 0 6px;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 900;
}

.profile-card h1 {
  margin: 0 0 6px;
  font-size: 28px;
}

.profile-card span {
  color: var(--muted);
  font-size: 13px;
}

.setting-list {
  display: grid;
  margin-top: 18px;
  overflow: hidden;
}

.setting-list button {
  display: flex;
  min-height: 54px;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--line);
  color: var(--ink);
  background: transparent;
  font-weight: 800;
}

.setting-list button:last-child {
  border-bottom: 0;
}

.setting-list strong {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.2;
}

.reminder-panel {
  display: grid;
  gap: 14px;
  margin-top: 18px;
  padding: 16px;
}

.reminder-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.reminder-head strong {
  color: var(--green-deep);
  font-size: 17px;
}

.reminder-head p,
.reminder-message {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.7;
  font-size: 13px;
}

.switch {
  position: relative;
  flex: 0 0 auto;
  width: 52px;
  height: 30px;
}

.switch input {
  position: absolute;
  opacity: 0;
}

.switch span {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(31, 42, 46, 0.12);
  border-radius: 999px;
  background: rgba(31, 42, 46, 0.12);
  transition:
    background 0.18s ease,
    border-color 0.18s ease;
}

.switch span::after {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fffdf8;
  box-shadow: 0 4px 10px rgba(31, 42, 46, 0.18);
  content: "";
  transition: transform 0.18s ease;
}

.switch input:checked + span {
  border-color: rgba(31, 122, 91, 0.48);
  background: var(--green);
}

.switch input:checked + span::after {
  transform: translateX(22px);
}

.time-row {
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.76);
}

.time-row span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.time-row input {
  width: 118px;
  border: 0;
  color: var(--ink);
  background: transparent;
  text-align: right;
  outline: none;
  font-weight: 900;
}

.reminder-save {
  width: 100%;
}

.reminder-save:disabled {
  opacity: 0.58;
}

.mobile-boundary {
  margin-top: 18px;
  padding: 16px;
}

.mobile-boundary strong {
  color: var(--gold);
}

.mobile-boundary p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.7;
  font-size: 13px;
}

.logout {
  width: 100%;
  margin-top: 18px;
}
</style>
