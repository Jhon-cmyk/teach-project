<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchHomeworkHistory, fetchPendingHomework } from '@/api/student'
import { readHomeworkReminderPreferences, scheduleHomeworkDeadlineReminder } from '@/utils/reminder'
import type { HomeworkHistory, HomeworkPending } from '@/types/student'

const pending = ref<HomeworkPending[]>([])
const history = ref<HomeworkHistory[]>([])
const remindedAssignments = ref<Set<number>>(new Set())
const loading = ref(false)
const reminderLoadingId = ref<number | null>(null)
const error = ref('')
const notice = ref('')

function refreshReminderState() {
  remindedAssignments.value = new Set(readHomeworkReminderPreferences().map((item) => item.assignmentId))
}

async function load() {
  loading.value = true
  error.value = ''
  notice.value = ''
  const results = await Promise.allSettled([fetchPendingHomework(), fetchHomeworkHistory()])
  if (results[0].status === 'fulfilled') pending.value = results[0].value
  if (results[1].status === 'fulfilled') history.value = results[1].value
  const failed = results.find((item) => item.status === 'rejected') as PromiseRejectedResult | undefined
  if (failed) error.value = failed.reason?.message || '作业数据暂时不可用'
  refreshReminderState()
  loading.value = false
}

async function remindHomework(item: HomeworkPending) {
  reminderLoadingId.value = item.assignmentId
  error.value = ''
  notice.value = ''
  try {
    const result = await scheduleHomeworkDeadlineReminder({
      assignmentId: item.assignmentId,
      title: item.title,
      deadline: item.deadline
    })
    refreshReminderState()
    const remindTime = result.remindAt.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
    if (result.scheduled) {
      notice.value = `已设置 ${item.title} 的截止提醒：${remindTime}`
    } else if (result.permission === 'granted') {
      notice.value = `提醒已保存，原生 App 内会在 ${remindTime} 触发`
    } else {
      notice.value = '需要开启通知权限后才能发送作业提醒'
    }
  } catch (err: any) {
    error.value = err?.message || '作业提醒设置失败'
  } finally {
    reminderLoadingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <main class="page">
    <header class="task-head">
      <p>Assignments</p>
      <h1>作业</h1>
    </header>

    <section class="task-summary panel">
      <div>
        <span>待完成</span>
        <strong>{{ pending.length }}</strong>
      </div>
      <div>
        <span>历史记录</span>
        <strong>{{ history.length }}</strong>
      </div>
    </section>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="notice" class="notice-text">{{ notice }}</p>

    <section class="section-title">
      <h2>待完成</h2>
      <span>{{ loading ? '同步中' : '按截止时间处理' }}</span>
    </section>
    <section class="task-list">
      <RouterLink
        v-for="item in pending"
        :key="item.assignmentId"
        class="task-card panel"
        :to="{ name: 'HomeworkDetail', params: { id: item.assignmentId } }"
      >
        <div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.teacherNote || '完成后会进入老师批改或自动批改流程。' }}</p>
          <span>
            {{ item.questionCount || 0 }} 题 · 已尝试 {{ item.attemptCount || 0 }} 次
            <template v-if="item.deadline"> · 截止 {{ item.deadline.slice(5, 16) }}</template>
          </span>
        </div>
        <div class="task-actions">
          <button
            class="remind-button"
            type="button"
            :disabled="reminderLoadingId === item.assignmentId || remindedAssignments.has(item.assignmentId)"
            @click.prevent="remindHomework(item)"
          >
            {{
              remindedAssignments.has(item.assignmentId)
                ? '已提醒'
                : reminderLoadingId === item.assignmentId
                  ? '设置中'
                  : '提醒我'
            }}
          </button>
          <strong>去完成</strong>
        </div>
      </RouterLink>
      <div v-if="!loading && !pending.length" class="empty-state panel">现在没有待完成作业。</div>
    </section>

    <section class="section-title">
      <h2>最近提交</h2>
      <span>学习轨迹</span>
    </section>
    <section class="history-list">
      <article v-for="item in history.slice(0, 6)" :key="item.submissionId" class="history-card panel">
        <div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.courseName || item.submitTime || '已提交' }}</p>
        </div>
        <strong>{{ item.totalScore ?? '-' }}</strong>
      </article>
      <div v-if="!loading && !history.length" class="empty-state panel">暂无提交记录。</div>
    </section>
  </main>
</template>

<style scoped>
.task-head p {
  margin: 0 0 6px;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 900;
}

.task-head h1 {
  margin: 0;
  font-size: 32px;
}

.task-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 18px;
  padding: 16px;
}

.task-summary span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.task-summary strong {
  display: block;
  margin-top: 6px;
  color: var(--green-deep);
  font-size: 30px;
}

.error-text {
  color: #af4a31;
  font-size: 13px;
}

.notice-text {
  color: var(--green-deep);
  font-size: 13px;
  font-weight: 800;
}

.task-list,
.history-list {
  display: grid;
  gap: 12px;
}

.task-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 16px;
}

.task-card h2,
.history-card h3 {
  margin: 0 0 8px;
  font-size: 17px;
}

.task-card p,
.history-card p {
  margin: 0 0 8px;
  color: var(--muted);
  line-height: 1.6;
  font-size: 13px;
}

.task-card span {
  color: var(--gold);
  font-size: 12px;
  font-weight: 900;
}

.task-actions {
  display: grid;
  flex: 0 0 auto;
  justify-items: end;
  gap: 8px;
}

.task-actions strong {
  flex: 0 0 auto;
  color: var(--green-deep);
  font-size: 13px;
}

.remind-button {
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid rgba(31, 122, 91, 0.2);
  border-radius: 8px;
  color: var(--green-deep);
  background: rgba(31, 122, 91, 0.08);
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.remind-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.history-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
}

.history-card strong {
  color: var(--coral);
  font-size: 20px;
}
</style>
