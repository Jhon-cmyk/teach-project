import request from '../utils/request'

export interface PlatformBannerItem {
  id?: number
  title?: string
  imageUrl: string
  targetUrl?: string
}

export interface PlatformCategoryItem {
  id?: number
  name: string
  iconUrl: string
  sortOrder?: number
  isEnabled?: number
}

export const getBannerList = () => {
  return request.get<PlatformBannerItem[], PlatformBannerItem[]>('/banner/list', {
    skipErrorToast: true
  })
}

export const getCategoryList = () => {
  return request.get<PlatformCategoryItem[], PlatformCategoryItem[]>('/category/list', {
    skipErrorToast: true
  })
}
