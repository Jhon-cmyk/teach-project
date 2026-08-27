import { Capacitor } from '@capacitor/core'
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera'

export type HomeworkImageSource = 'camera' | 'photos'

const MAX_IMAGE_WIDTH = 1800
const IMAGE_QUALITY = 82

function normalizeImageType(format?: string, blobType?: string) {
  if (blobType) return blobType
  if (!format) return 'image/jpeg'
  const normalized = format.toLowerCase() === 'jpg' ? 'jpeg' : format.toLowerCase()
  return `image/${normalized}`
}

async function imageUrlToFile(url: string, format: string | undefined, prefix: string) {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error('图片读取失败，请重新选择')
  }
  const blob = await response.blob()
  const type = normalizeImageType(format, blob.type)
  const extension = type.split('/')[1] || 'jpg'
  return new File([blob], `${prefix}-${Date.now()}.${extension}`, { type })
}

export function isImagePickCanceled(error: unknown) {
  const message = error instanceof Error ? error.message : String(error || '')
  return /cancel|cancelled|canceled|user cancelled|user canceled|用户取消/i.test(message)
}

export async function pickHomeworkImages(source: HomeworkImageSource) {
  if (source === 'photos') {
    const picked = await Camera.pickImages({
      quality: IMAGE_QUALITY,
      width: MAX_IMAGE_WIDTH,
      correctOrientation: true,
      limit: 6,
      presentationStyle: Capacitor.getPlatform() === 'ios' ? 'fullscreen' : undefined
    })

    return Promise.all(
      picked.photos.map((photo, index) => imageUrlToFile(photo.webPath, photo.format, `homework-gallery-${index + 1}`))
    )
  }

  const photo = await Camera.getPhoto({
    quality: IMAGE_QUALITY,
    width: MAX_IMAGE_WIDTH,
    correctOrientation: true,
    resultType: CameraResultType.Uri,
    source: CameraSource.Camera,
    saveToGallery: false,
    webUseInput: true,
    promptLabelHeader: '上传作答图片',
    promptLabelPhoto: '从相册选择',
    promptLabelPicture: '拍照'
  })

  if (!photo.webPath) {
    throw new Error('没有读取到图片，请重新选择')
  }

  return [await imageUrlToFile(photo.webPath, photo.format, 'homework-camera')]
}
