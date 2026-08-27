import { ref } from 'vue'
import {
  getCommunityNotificationUnreadCount,
  isCommunityAuthError
} from '@/api/community'

const unreadCount = ref(0)
const syncing = ref(false)
let initialized = false

async function refreshUnreadCount() {
  if (syncing.value) return unreadCount.value

  syncing.value = true
  try {
    unreadCount.value = await getCommunityNotificationUnreadCount()
    initialized = true
    return unreadCount.value
  } catch (error) {
    if (isCommunityAuthError(error)) {
      unreadCount.value = 0
      initialized = true
      return 0
    }

    console.error('[useCommunityNotificationBadge] refresh failed', error)
    return unreadCount.value
  } finally {
    syncing.value = false
  }
}

function ensureUnreadCount() {
  if (!initialized) {
    refreshUnreadCount()
  }
}

function setUnreadCount(value: number) {
  unreadCount.value = Math.max(0, Number(value) || 0)
  initialized = true
}

function decreaseUnreadCount(step = 1) {
  unreadCount.value = Math.max(0, unreadCount.value - step)
  initialized = true
}

function clearUnreadCount() {
  unreadCount.value = 0
  initialized = true
}

export function useCommunityNotificationBadge() {
  return {
    unreadCount,
    syncing,
    refreshUnreadCount,
    ensureUnreadCount,
    setUnreadCount,
    decreaseUnreadCount,
    clearUnreadCount
  }
}
