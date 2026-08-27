/**
 * 学习交流模块 - 类型定义
 *
 * 使用位置：
 *   - 首页学习交流区块 (LearningCommunitySection.vue)
 *   - api/community.ts
 *   - 学习交流子页面 (community/*.vue)
 */

// ========== 讨论项（列表用） ==========
export interface DiscussionItem {
  /** 讨论 ID */
  id: number | string
  /** 标题 */
  title: string
  /** 所属课程 ID（可选，用于跳转课程页） */
  courseId?: number | string
  /** 所属课程名称（展示用） */
  courseName: string
  /** 回复数 */
  replyCount: number
  /** 浏览数 */
  viewCount: number
  /** 最后活跃时间（展示文本，如 "10 分钟前"） */
  lastActiveTime: string
  /** 最后活跃时间戳（可选，用于排序） */
  lastActiveTimestamp?: number
  /** 是否热门 */
  isHot?: boolean
  /** 是否有老师回答 */
  isTeacherAnswered?: boolean
}

// ========== 回复项 ==========
export interface DiscussionReply {
  /** 回复 ID */
  id: number | string
  /** 回复人名称 */
  userId?: number | string
  authorName: string
  /** 回复内容 */
  content: string
  /** 发布时间（展示文本） */
  createdAt: string
  /** 是否是教师回复 */
  isTeacher?: boolean
  teacher?: boolean
}

// ========== 讨论详情（详情页用，继承列表字段） ==========
export interface DiscussionDetail extends DiscussionItem {
  /** 正文内容 */
  content: string
  /** 作者名称 */
  authorName: string
  /** 发布时间（展示文本） */
  createdAt: string
  /** 回复列表 */
  replies: DiscussionReply[]

  /** very small 补充：详情页用于老师轻操作判断 */
  status?: 'open' | 'resolved'
  postType?: 'discussion' | 'homework'
}

// ========== 作业互助 - 提问项 ==========
export interface HomeworkQuestionItem extends DiscussionItem {
  /** 问题状态 */
  status?: 'open' | 'resolved'
  /** 问题摘要（展示用） */
  excerpt?: string
  /** 提问人 */
  authorName?: string
}

// ========== 答疑精选 - 精选项 ==========
export interface FeaturedAnswerItem {
  /** 精选条目 ID */
  id: number | string
  /** 对应讨论 ID（点击跳转详情页用） */
  discussionId: number | string
  /** 问题标题 */
  title: string
  /** 所属课程 ID */
  courseId?: number | string
  /** 所属课程名称 */
  courseName: string
  /** 精选回答摘要 */
  excerpt: string
  /** 答疑老师/助教名称 */
  teacherName: string
  /** 更新时间 */
  updatedAt: string
  /** 是否推荐 */
  isRecommended?: boolean
}

// ========== 作业互助摘要 ==========
export interface HomeworkHelpSummary {
  /** 今日新增提问数 */
  todayQuestionCount: number
}

// ========== 答疑精选摘要 ==========
export interface FeaturedAnswersSummary {
  /** 本周精选回答数 */
  weeklySelectedCount: number
}

// ========== 首页学习交流模块聚合数据 ==========
export interface CommunityOverview {
  /** 讨论列表（首页只展示前 5 条） */
  discussions: DiscussionItem[]
  /** 作业互助摘要 */
  homeworkHelp: HomeworkHelpSummary
  /** 答疑精选摘要 */
  featuredAnswers: FeaturedAnswersSummary
}

export interface MyCommunityPostItem {
  id: number | string
  title: string
  courseId?: number | string
  courseName: string
  postType?: 'discussion' | 'homework'
  status?: 'open' | 'resolved'
  replyCount: number
  viewCount: number
  lastActiveTime: string
  lastActiveTimestamp?: number
  isTeacherAnswered?: boolean
}

export interface MyCommunityReplyItem {
  discussionId: number | string
  title: string
  courseId?: number | string
  courseName: string
  myLastReplyTime: string
  replyCount: number
  lastActiveTime: string
  lastActiveTimestamp?: number
}

export type CommunityNotificationType =
  | 'post_replied'
  | 'post_resolved'
  | 'post_featured'
  | 'followed_discussion_updated'

export interface CommunityNotificationItem {
  id: number | string
  type: CommunityNotificationType
  postId: number | string
  replyId?: number | string
  title: string
  content: string
  isRead: boolean
  createdAt: string
}
