const LOGIN_USER_KEY = 'loginUser'
const LEGACY_SESSION_USER_KEY = 'user_login'
const AUTH_TOKEN_KEY = 'smartedu_token'

const canUseStorage = () => typeof window !== 'undefined'

export const getLoginUserRaw = (): string | null => {
  if (!canUseStorage()) return null
  return (
    sessionStorage.getItem(LOGIN_USER_KEY) ||
    sessionStorage.getItem(LEGACY_SESSION_USER_KEY) ||
    localStorage.getItem(LOGIN_USER_KEY)
  )
}

export const getLoginUser = <T = any>(): T | null => {
  const raw = getLoginUserRaw()
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

export const setTabLoginUser = (user: unknown) => {
  if (!canUseStorage()) return
  const serialized = JSON.stringify(user)
  sessionStorage.setItem(LOGIN_USER_KEY, serialized)
  sessionStorage.setItem(LEGACY_SESSION_USER_KEY, serialized)
  localStorage.removeItem(LOGIN_USER_KEY)
}

export const patchTabLoginUser = (patch: Record<string, unknown>) => {
  const current = getLoginUser<Record<string, unknown>>()
  if (!current) return
  setTabLoginUser({ ...current, ...patch })
}

export const getAuthToken = (): string => {
  if (!canUseStorage()) return ''
  return sessionStorage.getItem(AUTH_TOKEN_KEY) || localStorage.getItem(AUTH_TOKEN_KEY) || ''
}

export const setTabAuthToken = (token: string) => {
  if (!canUseStorage() || !token) return
  sessionStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.removeItem(AUTH_TOKEN_KEY)
}

export const clearTabAuth = () => {
  if (!canUseStorage()) return
  sessionStorage.removeItem(LOGIN_USER_KEY)
  sessionStorage.removeItem(LEGACY_SESSION_USER_KEY)
  sessionStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(LOGIN_USER_KEY)
  localStorage.removeItem(AUTH_TOKEN_KEY)
}
