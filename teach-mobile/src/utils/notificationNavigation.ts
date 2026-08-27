import { LocalNotifications, type ActionPerformed } from '@capacitor/local-notifications'
import type { Router } from 'vue-router'

function readRoute(action: ActionPerformed) {
  const extra = action.notification.extra as { route?: unknown } | undefined
  const route = extra?.route
  if (typeof route !== 'string') return null
  if (!route.startsWith('/')) return null
  return route
}

export function setupNotificationNavigation(router: Router) {
  LocalNotifications.addListener('localNotificationActionPerformed', async (action) => {
    const route = readRoute(action)
    if (!route) return

    try {
      await router.isReady()
      await router.push(route)
    } catch (error) {
      console.warn('[mobile] notification navigation failed', error)
    }
  }).catch((error) => {
    console.warn('[mobile] local notification listener unavailable', error)
  })
}
