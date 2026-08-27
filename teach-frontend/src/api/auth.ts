import request from '@/utils/request'

export interface CaptchaVO {
  captchaId: string
  imageBase64: string
  captchaCode?: string
}

export interface CaptchaLoginPayload {
  userAccount: string
  userPassword: string
  captchaId: string
  captchaCode: string
}

export const getCaptcha = (): Promise<CaptchaVO> => request.get('/user/captcha')

export const loginByCaptcha = (data: CaptchaLoginPayload): Promise<any> =>
  request.post('/user/login/captcha', data)
