import { createRouter, createWebHistory } from 'vue-router'
import { getLoginUser } from '@/utils/authStorage'
// 引入布局组件
// 引入学习页 (保持原样)

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth'
      }
    }
    return { top: 0, left: 0 }
  },
  routes: [
    { path: '/', redirect: '/auth' },
    {
      path: '/auth',
      name: 'Auth',
      component: () => import('../views/AuthView.vue')
    },

    // ==========================================
    // 👩‍🏫 教师端路由
    // ==========================================
    {
      path: '/teacher',
      component: () => import('../views/teacher/TeacherLayout.vue'),
      meta: { requiresAuth: true, role: 'teacher' },
      redirect: '/teacher/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'TeacherDashboard',
          component: () => import('../views/teacher/Dashboard.vue')
        },
        {
          path: 'graph',
          name: 'TeacherGraph',
          component: () => import('../views/teacher/GraphView.vue')
        },
        {
          path: 'graph/stats',
          name: 'TeacherGraphStats',
          component: () => import('../views/teacher/graph-stats/GraphStatsView.vue'),
          meta: { title: '知识图谱统计' }
        },
        {
          path: 'graph/node/:id',
          name: 'TeacherNodeDetail',
          component: () => import('../views/teacher/NodeDetailView.vue'),
          meta: { title: '知识点详情' }
        },
        {
          path: 'courses',
          name: 'TeacherCourses',
          component: () => import('../views/teacher/CourseList.vue')
        },
        {
          path: 'resources',
          name: 'ResourceLibrary',
          component: () => import('../views/teacher/ResourceLibrary.vue')
        },
        {
          path: 'visual',
          name: 'TeacherVisual',
          component: () => import('../views/teacher/DataVisual.vue')
        },
        {
          path: 'analysis',
          name: 'ClassAnalysis',
          component: () => import('../views/teacher/ClassAnalysis.vue'),
          meta: { title: '课堂录音分析' }
        },
        {
          path: 'profile',
          name: 'TeacherProfile',
          component: () => import('@/views/teacher/TeacherProfile.vue'),
          meta: { title: '个人中心' }
        },
        {
          path: 'ai',
          name: 'TeacherAi',
          component: () => import('../views/teacher/AiRoom.vue')
        },
        {
          path: 'writing',
          name: 'SmartWriting',
          component: () => import('../views/teacher/SmartWriting.vue'),
          meta: { title: '智能编写' }
        },
        {
          path: 'community',
          name: 'TeacherCommunityDesk',
          component: () => import('../views/teacher/TeacherCommunityDesk.vue'),
          meta: { title: '社区处理台' }
        },
        {
          path: 'monitor',
          name: 'TaskMonitor',
          component: () => import('../views/teacher/TaskMonitor.vue')
        },
        {
          path: 'search',
          name: 'ResourceSearch',
          component: () => import('../views/teacher/ResourceSearch.vue')
        },
        {
          path: 'coding-bank',
          name: 'CodingProblemBank',
          component: () => import('../views/teacher/CodingProblemBank.vue'),
          meta: { title: '题库' }
        },
        {
          path: 'case-management',
          name: 'CaseManagement',
          component: () => import('../views/teacher/CaseManagement.vue'),
          meta: { title: '案例管理' }
        },
        {
          path: 'schedule',
          name: 'TeacherSchedule',
          component: () => import('../views/teacher/ScheduleView.vue'),
          meta: { title: '教务安排' }
        }
      ]
    },

    // ==========================================
    // 👨‍🎓 学生端路由
    // ==========================================
    {
      path: '/student',
      component: () => import('@/views/student/StudentLayout.vue'),
      meta: { requiresAuth: true, role: 'student' },
      redirect: '/student/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'studentDashboard',
          component: () => import('../views/student/Dashboard.vue')
        },
        {
          path: 'analysis',
          name: 'CourseAnalysis',
          component: () => import('../views/student/CourseAnalysisView.vue'),
          meta: { title: '智能课表分析' }
        },
        {
          path: 'search',
          name: 'StudentSearch',
          component: () => import('../views/student/SearchResult.vue'),
          meta: { title: '课程搜索' }
        },
        {
          path: 'profile',
          name: 'StudentProfile',
          component: () => import('../views/student/Profile.vue')
        },
        {
          path: 'tutorial',
          name: 'TutorialList',
          component: () => import('../views/student/tutorial/TutorialListView.vue'),
          meta: { title: '文字教程' }
        },
        {
          path: 'tutorial/:courseId/read',
          name: 'TutorialRead',
          component: () => import('../views/student/tutorial/TutorialReadView.vue'),
          meta: { title: '教程阅读' }
        },
        {
          path: '/learn/:id',
          name: 'CourseLearn',
          component: () => import('../views/student/CourseLearnView.vue')
        },
        {
          path: 'diagnosis',
          name: 'StudentDiagnosis',
          component: () => import('@/views/student/LearningDiagnosis.vue'),
          meta: { title: '学习画像' }
        },
        {
          path: 'my-courses',
          name: 'StudentMyCourses',
          component: () => import('../views/student/MyCourses.vue'),
          meta: { title: '我的课程' }
        },
        {
          path: 'mental-state',
          name: 'MentalState',
          component: () => import('../views/student/MentalStateView.vue'),
          meta: { title: '状态充能' }
        },
        {
          path: 'community',
          name: 'CommunityList',
          component: () => import('../views/student/community/CommunityList.vue'),
          meta: { title: '学习交流' }
        },
        {
          path: 'community/homework-help',
          name: 'HomeworkHelp',
          component: () => import('../views/student/community/HomeworkHelp.vue'),
          meta: { title: '作业互助' }
        },
        {
          path: 'community/featured-answers',
          name: 'FeaturedAnswers',
          redirect: { name: 'CommunityList' },
          meta: { title: '学习交流' }
        },
        {
          path: 'community/mine',
          name: 'MyCommunity',
          component: () => import('../views/student/community/MyCommunity.vue'),
          meta: { title: '我的社区' }
        },
        {
          path: 'community/notifications',
          name: 'CommunityNotifications',
          redirect: { name: 'MyCommunity', query: { tab: 'notifications' } },
          meta: { title: '我的动态' }
        },
        {
          path: 'coding',
          name: 'CodingList',
          component: () => import('../views/student/CodingList.vue'),
          meta: { title: '编程练习' }
        },
        {
          path: 'community/:id',
          name: 'CommunityDetail',
          component: () => import('../views/student/community/CommunityDetail.vue'),
          meta: { title: '讨论详情' }
        },
      ]
    },

    // 新增：沉浸式答题与批改室路由
    {
      path: '/student/homework/:id',
      name: 'StudentHomework',
      component: () => import('../views/student/Homework.vue')
    },
    {
      path: '/student/coding/:id',
      name: 'CodingProblem',
      component: () => import('../views/student/CodingProblem.vue'),
      meta: { title: '编程做题' }
    },
    {
      path: '/student/exam/:id',
      name: 'StudentExam',
      component: () => import('../views/student/ExamView.vue'),
      meta: { title: '在线考试' }
    },
    // ==========================================
    // 🔧 管理端路由
    // ==========================================
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'admin' },
      redirect: '/admin/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('../views/admin/Dashboard.vue'),
          meta: { title: '管理工作台' }
        },
        {
          path: 'courses',
          name: 'AdminCourseManage',
          component: () => import('../views/admin/CourseManage.vue'),
          meta: { title: '平台课程' }
        },
        {
          path: 'tutorials',
          name: 'AdminTutorialManage',
          component: () => import('../views/admin/TutorialManage.vue'),
          meta: { title: '图文教程' }
        },
        {
          path: 'ai-resources',
          name: 'AdminAiResourceManage',
          component: () => import('../views/admin/AiResourceManage.vue'),
          meta: { title: 'AI资源' }
        },
        {
          path: 'model-configs',
          name: 'AdminModelConfigManage',
          component: () => import('../views/admin/ModelConfigManage.vue'),
          meta: { title: '接口服务配置' }
        },
        {
          path: 'knowledge-base',
          name: 'AdminKnowledgeBaseManage',
          component: () => import('../views/admin/KnowledgeBaseManage.vue'),
          meta: { title: '星火知识库' }
        },
        {
          path: 'cases',
          name: 'AdminPlatformCaseManage',
          component: () => import('../views/admin/PlatformCaseManage.vue'),
          meta: { title: '平台案例' }
        },
        {
          path: 'assets',
          name: 'AdminAssetManage',
          component: () => import('../views/admin/AssetManage.vue'),
          meta: { title: '运营素材' }
        },
        {
          path: 'users',
          name: 'AdminUserManage',
          component: () => import('../views/admin/UserManage.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'audit-logs',
          name: 'AdminAuditLogManage',
          component: () => import('../views/admin/AuditLogManage.vue'),
          meta: { title: '审计日志' }
        },
        {
          path: 'system-health',
          name: 'AdminSystemHealthManage',
          redirect: '/admin/model-configs',
          meta: { title: '系统健康检测' }
        },
        {
          path: 'classes',
          name: 'AdminClassMajorManage',
          component: () => import('../views/admin/ClassMajorManage.vue'),
          meta: { title: '班级专业管理' }
        },
        {
          path: 'data-transfer',
          name: 'AdminDataTransferManage',
          component: () => import('../views/admin/DataTransferManage.vue'),
          meta: { title: '导入导出中心' }
        },
        {
          path: 'teacher-tracking',
          name: 'AdminTeacherTrackingManage',
          component: () => import('../views/admin/TeacherTrackingManage.vue'),
          meta: { title: '教师配置跟踪' }
        }
      ]
    }
  ]
})

// ✅ 全局路由守卫
router.beforeEach((to, from, next) => {
  const user = getLoginUser<any>()

  if (to.meta.requiresAuth && !user) {
    return next('/auth')
  }

  if (to.meta.role && user?.userRole !== to.meta.role) {
    alert('无权访问该页面')

    if (user?.userRole === 'teacher') return next('/teacher/dashboard')
    if (user?.userRole === 'student') return next('/student/dashboard')
    if (user?.userRole === 'admin') return next('/admin/dashboard')

    return next('/auth')
  }

  next()
})

export default router
