import { get, post } from '@/utils/request'
import { demoCaptcha, demoLogin, demoUser, isMobileDemo } from '@/api/mock'
import type { CaptchaLoginPayload, CaptchaVO, LoginUser } from '@/types/user'

export const getCaptcha = () => {
  if (isMobileDemo) return Promise.resolve(demoCaptcha)
  return get<CaptchaVO>('/user/captcha')
}

export const loginByCaptcha = (payload: CaptchaLoginPayload) => {
  if (isMobileDemo) return demoLogin(payload)
  return post<LoginUser & { token?: string; accessToken?: string }>('/user/login/captcha', payload)
}

export const getCurrentUser = () => {
  if (isMobileDemo) return Promise.resolve(demoUser)
  return get<LoginUser>('/user/get/login')
}
