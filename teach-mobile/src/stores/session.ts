import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, loginByCaptcha } from '@/api/auth'
import type { CaptchaLoginPayload, LoginUser } from '@/types/user'

const USER_CACHE_KEY = 'smartedu_mobile_user'
const TOKEN_CACHE_KEY = 'smartedu_mobile_token'
const STUDENT_ONLY_MESSAGE = '当前移动端优先开放学生端，请使用学生账号登录'

export const useSessionStore = defineStore('session', () => {
  const user = ref<LoginUser | null>(null)
  const bootstrapped = ref(false)
  const loading = ref(false)
  const error = ref('')

  const isLoggedIn = computed(() => Boolean(user.value?.id))
  const isStudent = computed(() => !user.value?.userRole || user.value.userRole === 'student')
  const displayName = computed(() => user.value?.userName || user.value?.userAccount || '同学')

  function isAllowedStudent(nextUser: LoginUser | null) {
    return Boolean(nextUser?.id) && (!nextUser?.userRole || nextUser.userRole === 'student')
  }

  function persist(nextUser: LoginUser | null, token?: string) {
    user.value = nextUser
    if (nextUser) {
      localStorage.setItem(USER_CACHE_KEY, JSON.stringify(nextUser))
    } else {
      localStorage.removeItem(USER_CACHE_KEY)
      localStorage.removeItem(TOKEN_CACHE_KEY)
    }
    if (token) {
      localStorage.setItem(TOKEN_CACHE_KEY, token)
    }
  }

  function restore() {
    if (bootstrapped.value) return
    bootstrapped.value = true
    const raw = localStorage.getItem(USER_CACHE_KEY)
    if (!raw) return
    try {
      const restoredUser = JSON.parse(raw) as LoginUser
      if (isAllowedStudent(restoredUser)) {
        user.value = restoredUser
      } else {
        persist(null)
      }
    } catch {
      localStorage.removeItem(USER_CACHE_KEY)
    }
  }

  async function fetchCurrentUser() {
    restore()
    try {
      const fresh = await getCurrentUser()
      if (!isAllowedStudent(fresh)) {
        persist(null)
        return false
      }
      persist(fresh)
      return Boolean(fresh?.id)
    } catch {
      return isAllowedStudent(user.value)
    }
  }

  async function login(payload: CaptchaLoginPayload) {
    loading.value = true
    error.value = ''
    try {
      const result = await loginByCaptcha(payload)
      if (!isAllowedStudent(result)) {
        throw new Error(STUDENT_ONLY_MESSAGE)
      }
      const token = result.token || result.accessToken
      persist(result, token)
      return result
    } catch (err: any) {
      error.value = err?.message || '登录失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  function logout() {
    persist(null)
  }

  return {
    user,
    loading,
    error,
    isLoggedIn,
    isStudent,
    displayName,
    restore,
    fetchCurrentUser,
    login,
    logout
  }
})
