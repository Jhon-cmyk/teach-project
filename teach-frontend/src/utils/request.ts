import axios from 'axios'
import { message } from 'ant-design-vue'
import { clearTabAuth, getAuthToken, setTabAuthToken } from '@/utils/authStorage'

declare module 'axios' {
  export interface AxiosRequestConfig {
    successMessage?: string
    skipSuccessToast?: boolean
    skipErrorToast?: boolean
  }

  export interface InternalAxiosRequestConfig {
    successMessage?: string
    skipSuccessToast?: boolean
    skipErrorToast?: boolean
  }
}

let authRedirecting = false

const redirectToAuth = () => {
  clearTabAuth()

  if (!authRedirecting && typeof window !== 'undefined') {
    authRedirecting = true
    window.location.href = '/auth'
    setTimeout(() => {
      authRedirecting = false
    }, 1200)
  }
}

const normalizeErrorMessage = (error: any) => {
  return (
    error?.response?.data?.msg ||
    error?.response?.data?.message ||
    error?.message ||
    '网络连接失败，请检查服务'
  )
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api',
  timeout: 30000,
  withCredentials: true
})

request.interceptors.request.use(
  (config) => {
    const token = getAuthToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    const config = response.config
    const issuedToken = response.headers['x-auth-token']
    if (typeof issuedToken === 'string' && issuedToken) {
      setTabAuthToken(issuedToken)
    }

    // 1. 特殊逻辑保持：判断是否为图谱数据结构，直接返回
    if (
      payload &&
      Array.isArray(payload.nodes) &&
      Array.isArray(payload.links) &&
      Array.isArray(payload.categories)
    ) {
      return payload
    }

    // 2. 核心修复：解析 code 和错误信息
    const code = payload?.code
    const data = payload?.data
    // 兼容多种可能的错误字段名，如果都没有则使用兜底文案
    const errorMsg = payload?.msg || payload?.message || '接口请求异常'

    // 业务逻辑成功 (假设 0 是成功码)
    if (code === 0) {
      if (config.successMessage && !config.skipSuccessToast) {
        message.success(config.successMessage)
      }
      return data
    }

    // 3. 业务逻辑失败（比如账号密码错误，code 可能为 40000 或其他）
    const err = new Error(errorMsg) as Error & { code?: number }
    err.code = code

    if (code === 40100) {
      if (!config.skipErrorToast) {
        message.warning('登录状态已失效，请重新登录')
      }
      redirectToAuth()
      return Promise.reject(err)
    }

    // 如果配置了不跳过错误提示，则弹出具体的错误信息
    if (!config.skipErrorToast) {
      message.error(errorMsg)
    }

    return Promise.reject(err)
  },
  (error) => {
    // 4. HTTP 状态码错误处理 (401, 403, 500 等)
    const status = error?.response?.status
    const text = normalizeErrorMessage(error)
    const config = error?.config || {}

    // 处理 401 登录失效
    if (status === 401) {
      message.warning('登录状态已失效，请重新登录')
      redirectToAuth()

      return Promise.reject(error)
    }

    // 处理 403 无权限
    if (status === 403) {
      message.error('当前账号无权访问该内容')
      return Promise.reject(error)
    }

    // 处理其他网络或服务器错误
    if (!config.skipErrorToast) {
      message.error(text)
    }

    return Promise.reject(error)
  }
)

export default request
