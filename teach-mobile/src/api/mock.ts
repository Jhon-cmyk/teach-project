import type { CaptchaLoginPayload, CaptchaVO, LoginUser } from '@/types/user'
import type {
  CommunityPost,
  Course,
  CourseChapter,
  HomeworkDetail,
  HomeworkHistory,
  HomeworkPending,
  StudentLearningProfile,
  StudyHeatmapDay
} from '@/types/student'

export const isMobileDemo = import.meta.env.VITE_MOBILE_DEMO === 'true'

export const demoUser: LoginUser & { token: string } = {
  id: 10001,
  userAccount: 'student_demo',
  userName: '林知夏',
  userRole: 'student',
  points: 1260,
  token: 'mobile-demo-token'
}

export const demoCaptcha: CaptchaVO = {
  captchaId: 'mobile-demo-captcha',
  captchaCode: '1234',
  imageBase64:
    'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjQwIiB2aWV3Qm94PSIwIDAgMTIwIDQwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMjAiIGhlaWdodD0iNDAiIHJ4PSI4IiBmaWxsPSIjZjdmNWVlIi8+PHRleHQgeD0iMjIiIHk9IjI3IiBmb250LXNpemU9IjIyIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtd2VpZ2h0PSI3MDAiIGZpbGw9IiMxZjdhNWIiPjEyMzQ8L3RleHQ+PC9zdmc+'
}

export const demoCourses: Course[] = [
  {
    id: 1,
    name: 'Python 数据分析入门',
    teacherName: '周老师',
    description: '用真实校园数据练习表格清洗、可视化和基础统计。'
  },
  {
    id: 2,
    name: 'Web 前端项目实战',
    teacherName: '陈老师',
    description: '从组件拆分、接口联调到移动端适配，完成一个学习工具。'
  },
  {
    id: 3,
    name: '机器学习基础',
    teacherName: '李老师',
    description: '理解分类、回归、评估指标，并完成一组小实验。'
  },
  {
    id: 4,
    name: '职业沟通与展示',
    teacherName: '王老师',
    description: '把技术方案讲清楚，形成简历、作品集和答辩表达。'
  }
]

export const demoChapters: CourseChapter[] = [
  {
    id: 101,
    courseId: 1,
    title: '环境准备与数据读取',
    description: '安装工具，理解 CSV、Excel 与 DataFrame 的基本关系。',
    sortOrder: 1,
    duration: 26
  },
  {
    id: 102,
    courseId: 1,
    title: '缺失值与异常值处理',
    description: '识别缺失数据，完成清洗记录和处理说明。',
    sortOrder: 2,
    duration: 34
  },
  {
    id: 103,
    courseId: 1,
    title: '图表表达与结论撰写',
    description: '选择合适图表，并写出面向业务问题的结论。',
    sortOrder: 3,
    duration: 42
  }
]

export const demoHeatmap: StudyHeatmapDay[] = Array.from({ length: 14 }, (_, index) => {
  const date = new Date()
  date.setDate(date.getDate() - (13 - index))
  const active = index % 3 !== 0
  return {
    date: date.toISOString().slice(0, 10),
    hours: active ? Math.floor(index / 6) : 0,
    minutes: active ? 18 + index * 3 : 0,
    seconds: active ? 20 : 0
  }
})

export const demoProfile: StudentLearningProfile = {
  insight: {
    title: '节奏稳定，练习复盘还可以更及时',
    body: '你最近保持了较好的学习连续性，课程观看和作业完成都在推进。建议把错题复盘提前到当天完成。',
    riskLevel: 'low',
    riskLabel: '状态良好',
    overallScore: 84,
    weakPointCount: 2,
    wrongQuestionCount: 6,
    confidence: 0.82,
    confidenceLabel: '可信',
    trendLabel: '稳中上升',
    recentActivityCount: 18
  },
  recommendations: [
    {
      id: 1,
      courseId: 1,
      courseName: 'Python 数据分析入门',
      resourceTitle: '缺失值处理专项练习',
      resourceType: 'practice',
      knowledgeName: '数据清洗',
      recommendationReason: '你在最近两次作业中对空值判断还不够稳定，适合用短练习补齐。'
    },
    {
      id: 2,
      courseId: 2,
      courseName: 'Web 前端项目实战',
      resourceTitle: '移动端表单交互案例',
      resourceType: 'case',
      knowledgeName: '移动端适配',
      recommendationReason: '作业提交页需要兼顾输入、上传和反馈，这个案例可以直接迁移思路。'
    }
  ],
  advices: [
    {
      title: '每天留 10 分钟复盘错题',
      body: '先写出错因，再标记是概念不清、步骤遗漏还是粗心。',
      tone: 'steady'
    },
    {
      title: '把课程笔记和作业放在同一条线看',
      body: '遇到作业卡点时，回到最近相关章节重新做一遍示例。',
      tone: 'practical'
    }
  ],
  actionPlans: [
    {
      title: '完成数据清洗短练习',
      target: '缺失值处理',
      reason: '这是你当前最容易提分的薄弱点。',
      actionText: '开始练习',
      minutes: 18,
      priority: 1
    },
    {
      title: '复看移动端表单案例',
      target: '交互设计',
      reason: '能帮助你理解作业提交流程中的状态反馈。',
      actionText: '去学习',
      minutes: 22,
      priority: 2
    }
  ]
}

export const demoCommunityPosts: CommunityPost[] = [
  {
    id: 1,
    title: 'Pandas 分组统计后怎么保留原始列？',
    content: '我在做成绩分析时 groupby 后丢了班级字段，想知道更清晰的写法。',
    authorName: '许同学',
    answerCount: 4,
    viewCount: 68,
    createdAt: '2026-07-04 19:20:00'
  },
  {
    id: 2,
    title: '移动端上传图片后要不要压缩？',
    content: '作业照片原图很大，想了解前端和后端分别适合做什么处理。',
    authorName: '林知夏',
    answerCount: 2,
    viewCount: 41,
    createdAt: '2026-07-05 10:05:00'
  }
]

export const demoPendingHomework: HomeworkPending[] = [
  {
    assignmentId: 1,
    title: '数据清洗练习 02',
    teacherNote: '请提交处理步骤和最终结果截图。',
    deadline: '2026-07-08 22:00:00',
    questionCount: 5,
    attemptCount: 0,
    maxAttemptCount: 3,
    allowRedo: true
  },
  {
    assignmentId: 2,
    title: '移动端表单体验观察',
    teacherNote: '任选一个 App，记录输入、上传和错误提示流程。',
    deadline: '2026-07-10 20:00:00',
    questionCount: 3,
    attemptCount: 1,
    maxAttemptCount: 2,
    allowRedo: true
  }
]

export const demoHomeworkHistory: HomeworkHistory[] = [
  {
    assignmentId: 3,
    submissionId: 3001,
    title: 'Python 基础测验',
    courseName: 'Python 数据分析入门',
    submitStatus: 'graded',
    totalScore: 88,
    submitTime: '2026-07-03 21:10:00'
  },
  {
    assignmentId: 4,
    submissionId: 3002,
    title: '组件拆分作业',
    courseName: 'Web 前端项目实战',
    submitStatus: 'graded',
    totalScore: 91,
    submitTime: '2026-07-02 18:45:00'
  }
]

export const demoHomeworkDetail: HomeworkDetail = {
  assignmentId: 1,
  title: '数据清洗练习 02',
  teacherNote: '重点写清楚为什么这样处理缺失值。',
  deadline: '2026-07-08 22:00:00',
  contentSnapshot:
    '1. 读取提供的 score_sample.csv。\n2. 找出缺失值最多的两列，并说明处理策略。\n3. 输出清洗后的统计摘要。\n4. 用一张图展示处理前后的差异。',
  answerMode: 'mixed',
  assignmentType: 'practice',
  questionCount: 4,
  totalScore: 100,
  allowRedo: true,
  maxAttemptCount: 3,
  durationMinutes: 45,
  attemptCount: 0,
  completed: false
}

export function demoLogin(_payload: CaptchaLoginPayload) {
  return Promise.resolve(demoUser)
}

export function demoSubmitHomework() {
  return Promise.resolve(Date.now())
}
