import request from '@/utils/request';

// 接口类型定义
export interface Course {
  id: number;
  name: string;
  teacherName?: string;
  coverImg?: string;
  price?: number;
  description?: string;
  rating?: number;
  categoryId?: number;
}

export interface Homework {
  taskId: string;
  title: string;
  course: string;
  deadline: string;
  status: 'pending' | 'completed' | 'overdue';
}

export interface StudyHeatmapDay {
  date: string;
  hours: number;
  minutes: number;
  seconds: number;
}

// 获取课程列表
export const getRecommendedCourses = (
  current: number = 1,
  size: number = 8,
  name?: string,
  categoryId?: number
) => {
  return request.get('/course/list/all', {
    params: {
      current,
      size,
      ...(name ? { name } : {}),
      ...(categoryId ? { categoryId } : {})
    }
  });
};

export const userCheckIn = (): Promise<number> => request.post('/plan/checkin');
export const addStudyPlan = (data: { title: string }): Promise<number> => request.post('/plan/add', data);
export const completeStudyPlan = (id: string | number): Promise<number> => request.post(`/plan/complete/${id}`);
export const getStudyHeatmap = (
  userId?: string | number,
  days: number = 180
): Promise<StudyHeatmapDay[]> => request.get('/learning/heatmap', {
  params: { days }
});

// 查询今日签到状态
export const getCheckInStatus = () => {
  return request.get('/plan/checkin/status');
};

// 获取当前登录用户信息
export const getLoginUserInfo = () => {
  return request.get('/user/get/login');
};
