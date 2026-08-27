import request from '@/utils/request'

export interface AvatarSessionConfig {
  signedUrl: string
  appId: string
  sceneId: string
  avatarId: string
  avatarName: string
  voiceName: string
  welcomeText: string
}

export const getAvatarSession = () =>
  request.get<AvatarSessionConfig, AvatarSessionConfig>('/avatar/session', {
    skipErrorToast: true,
  })
