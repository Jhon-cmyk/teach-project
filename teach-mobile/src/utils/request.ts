import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'

export class ApiError extends Error {
  code?: number
  status?: number

  constructor(message: string, code?: number, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  withCredentials: true
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('smartedu_mobile_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === 0) {
        return payload.data
      }
      throw new ApiError(payload.msg || payload.message || '请求失败', payload.code, response.status)
    }
    return payload
  },
  (error: AxiosError<any>) => {
    const message =
      error.response?.data?.msg ||
      error.response?.data?.message ||
      error.message ||
      '网络连接失败'
    throw new ApiError(message, error.response?.data?.code, error.response?.status)
  }
)

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.get<T, T>(url, config)
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request.post<T, T>(url, data, config)
}

export default request
