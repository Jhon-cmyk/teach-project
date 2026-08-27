export interface LoginUser {
  id?: number
  userAccount?: string
  userName?: string
  userAvatar?: string
  userRole?: 'student' | 'teacher' | 'admin' | string
  points?: number
}

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
