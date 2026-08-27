import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior() {
    return { top: 0, left: 0 }
  },
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/',
      component: () => import('@/views/AppShell.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'home',
          name: 'Home',
          component: () => import('@/views/HomeView.vue')
        },
        {
          path: 'courses',
          name: 'Courses',
          component: () => import('@/views/CoursesView.vue')
        },
        {
          path: 'courses/:id',
          name: 'CourseDetail',
          component: () => import('@/views/CourseDetailView.vue')
        },
        {
          path: 'homework',
          name: 'Homework',
          component: () => import('@/views/HomeworkView.vue')
        },
        {
          path: 'homework/:id',
          name: 'HomeworkDetail',
          component: () => import('@/views/HomeworkDetailView.vue')
        },
        {
          path: 'learning',
          name: 'Learning',
          component: () => import('@/views/LearningView.vue')
        },
        {
          path: 'community',
          name: 'Community',
          component: () => import('@/views/CommunityView.vue')
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/ProfileView.vue')
        }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const session = useSessionStore()
  session.restore()

  if (to.meta.requiresAuth && !session.isLoggedIn) {
    const loaded = await session.fetchCurrentUser()
    if (!loaded) {
      return { name: 'Login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.guestOnly && session.isLoggedIn) {
    return { name: 'Home' }
  }

  return true
})

export default router
