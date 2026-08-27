import { Capacitor } from '@capacitor/core'
import { LocalNotifications } from '@capacitor/local-notifications'

export interface StudyReminderPreference {
  enabled: boolean
  time: string
}

export interface HomeworkReminderPreference {
  assignmentId: number
  title: string
  remindAt: string
  deadline?: string
}

export type ReminderPermissionState = 'granted' | 'denied' | 'prompt' | 'unsupported'

const STORAGE_KEY = 'smartedu_mobile_study_reminder'
const HOMEWORK_STORAGE_KEY = 'smartedu_mobile_homework_reminders'
const DAILY_REMINDER_ID = 260701
const HOMEWORK_REMINDER_OFFSET_MINUTES = 120
const HOMEWORK_REMINDER_ID_BASE = 270000
const CHANNEL_ID = 'smartedu-study-reminders'

export function readStudyReminderPreference(): StudyReminderPreference {
  const fallback = { enabled: false, time: '20:30' }
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return fallback

  try {
    const parsed = JSON.parse(raw) as Partial<StudyReminderPreference>
    return {
      enabled: Boolean(parsed.enabled),
      time: typeof parsed.time === 'string' && /^\d{2}:\d{2}$/.test(parsed.time) ? parsed.time : fallback.time
    }
  } catch {
    return fallback
  }
}

export function saveStudyReminderPreference(preference: StudyReminderPreference) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(preference))
}

export function readHomeworkReminderPreferences(): HomeworkReminderPreference[] {
  const raw = localStorage.getItem(HOMEWORK_STORAGE_KEY)
  if (!raw) return []

  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.filter((item): item is HomeworkReminderPreference => {
      return Boolean(item?.assignmentId && item?.title && item?.remindAt)
    })
  } catch {
    return []
  }
}

function saveHomeworkReminderPreferences(preferences: HomeworkReminderPreference[]) {
  localStorage.setItem(HOMEWORK_STORAGE_KEY, JSON.stringify(preferences))
}

function getHomeworkReminderId(assignmentId: number) {
  return HOMEWORK_REMINDER_ID_BASE + assignmentId
}

function parseLocalDateTime(value?: string) {
  if (!value) return null
  const normalized = value.trim().replace(' ', 'T')
  const parsed = new Date(normalized)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function resolveHomeworkReminderDate(deadline?: string) {
  const parsedDeadline = parseLocalDateTime(deadline)
  if (!parsedDeadline) return null
  const remindAt = new Date(parsedDeadline.getTime() - HOMEWORK_REMINDER_OFFSET_MINUTES * 60 * 1000)
  if (remindAt.getTime() <= Date.now()) return parsedDeadline
  return remindAt
}

export async function getReminderPermissionState(): Promise<ReminderPermissionState> {
  try {
    const permission = await LocalNotifications.checkPermissions()
    return permission.display
  } catch {
    return 'unsupported'
  }
}

export async function requestReminderPermission(): Promise<ReminderPermissionState> {
  try {
    const permission = await LocalNotifications.requestPermissions()
    return permission.display
  } catch {
    return 'unsupported'
  }
}

async function ensureAndroidChannel() {
  if (Capacitor.getPlatform() !== 'android') return

  await LocalNotifications.createChannel({
    id: CHANNEL_ID,
    name: '学习提醒',
    description: '每日学习计划和作业提醒',
    importance: 3,
    visibility: 1,
    vibration: true
  })
}

export async function cancelStudyReminder() {
  if (Capacitor.getPlatform() === 'web') return
  await LocalNotifications.cancel({ notifications: [{ id: DAILY_REMINDER_ID }] })
}

export async function clearAllReminderPreferences() {
  const homeworkNotifications = readHomeworkReminderPreferences().map((item) => ({
    id: getHomeworkReminderId(item.assignmentId)
  }))

  localStorage.removeItem(STORAGE_KEY)
  localStorage.removeItem(HOMEWORK_STORAGE_KEY)

  if (Capacitor.getPlatform() === 'web') return

  await LocalNotifications.cancel({
    notifications: [{ id: DAILY_REMINDER_ID }, ...homeworkNotifications]
  })
}

export async function scheduleStudyReminder(preference: StudyReminderPreference) {
  saveStudyReminderPreference(preference)

  if (!preference.enabled) {
    await cancelStudyReminder()
    return { scheduled: false, permission: await getReminderPermissionState() }
  }

  const permission = await requestReminderPermission()
  if (permission !== 'granted') {
    return { scheduled: false, permission }
  }

  if (Capacitor.getPlatform() === 'web') {
    return { scheduled: false, permission }
  }

  const [hour, minute] = preference.time.split(':').map(Number)
  await ensureAndroidChannel()
  await cancelStudyReminder()
  await LocalNotifications.schedule({
    notifications: [
      {
        id: DAILY_REMINDER_ID,
        title: '该回到学习计划了',
        body: '花 15 分钟处理今日待办或复盘一道错题。',
        channelId: CHANNEL_ID,
        schedule: {
          on: { hour, minute },
          repeats: true
        },
        extra: {
          route: '/home'
        }
      }
    ]
  })

  return { scheduled: true, permission }
}

export async function scheduleHomeworkDeadlineReminder(homework: {
  assignmentId: number
  title: string
  deadline?: string
}) {
  const remindAt = resolveHomeworkReminderDate(homework.deadline)
  if (!remindAt) {
    throw new Error('这个作业没有可用的截止时间')
  }

  const permission = await requestReminderPermission()
  if (permission !== 'granted') {
    return { scheduled: false, permission, remindAt }
  }

  const preference: HomeworkReminderPreference = {
    assignmentId: homework.assignmentId,
    title: homework.title,
    deadline: homework.deadline,
    remindAt: remindAt.toISOString()
  }
  const nextPreferences = readHomeworkReminderPreferences().filter((item) => item.assignmentId !== homework.assignmentId)
  nextPreferences.push(preference)
  saveHomeworkReminderPreferences(nextPreferences)

  if (Capacitor.getPlatform() === 'web') {
    return { scheduled: false, permission, remindAt }
  }

  await ensureAndroidChannel()
  await LocalNotifications.cancel({ notifications: [{ id: getHomeworkReminderId(homework.assignmentId) }] })
  await LocalNotifications.schedule({
    notifications: [
      {
        id: getHomeworkReminderId(homework.assignmentId),
        title: '作业快到截止时间了',
        body: `${homework.title} 还有待完成，记得提交。`,
        channelId: CHANNEL_ID,
        schedule: {
          at: remindAt
        },
        extra: {
          route: `/homework/${homework.assignmentId}`
        }
      }
    ]
  })

  return { scheduled: true, permission, remindAt }
}
