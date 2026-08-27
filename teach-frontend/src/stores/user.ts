import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { userCheckIn, addStudyPlan, completeStudyPlan } from '@/api/dashboard';
import { message } from 'ant-design-vue';
import request from '@/utils/request'
import { clearTabAuth, getLoginUserRaw, patchTabLoginUser } from '@/utils/authStorage'


interface DailyPlan {
  id: string;
  content: string;
  time: string;
  completed: boolean;
}

export const useUserStore = defineStore('user', () => {
  // --- 状态 (State) ---
  const userInfo = ref({
    avatar: '',
    name: '同学你好',
    points: 0,
  });
  const isCheckedIn = ref(false);
  const checkInLoading = ref(false);
  const dailyPlans = ref<DailyPlan[]>([]);

  // --- 计算属性 (Getters) ---
  const completedPlanCount = computed(() =>
    dailyPlans.value.filter(p => p.completed).length
  );

  // --- 内部辅助方法 ---
  const saveLocalData = () => {
    localStorage.setItem('smartedu_daily_plans', JSON.stringify(dailyPlans.value));
  };

  // 同步更新本地登录缓存中的积分 (防止刷新页面后积分回退)
  const syncLocalPoints = (newPoints: number) => {
    userInfo.value.points = newPoints;
    patchTabLoginUser({ points: newPoints })
  };

  // --- 动作 (Actions) ---
  const loadLocalData = () => {
    const s = localStorage.getItem('smartedu_daily_plans');
    if (s) dailyPlans.value = JSON.parse(s);

    const u = getLoginUserRaw();
    if (u) {
      try {
        const parsedUser = JSON.parse(u);
        userInfo.value.avatar = parsedUser.userAvatar || '';
        userInfo.value.name = parsedUser.userName || parsedUser.name || '同学你好';
        if (parsedUser.points !== undefined) {
          userInfo.value.points = parsedUser.points;
        }
      } catch (e) {
        console.error('解析本地用户信息失败', e);
      }
    }
  };


  // 🌟 核心改造：调用后端接口添加计划
  const addPlan = async (content: string) => {
    try {
      // 1. 发送给后端，后端实体类期望的字段是 title
      const backendId = await addStudyPlan({ title: content });

      // 2. 拿到后端生成的真实 ID 后，再存入前端列表
      const now = new Date();
      const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
      dailyPlans.value.unshift({
        id: backendId.toString(), // 使用数据库真实的 ID
        content,
        time,
        completed: false
      });
      saveLocalData();
      message.success('计划添加成功');
    } catch (error: any) {
      message.error('计划添加失败: ' + (error.message || '网络异常'));
    }
  };

  // 🌟 新增：进入页面时，静默查询今天的签到状态
  const fetchCheckInStatus = async () => {
    // 从本地缓存读取签到状态，避免 404 报错
    try {
      const today = new Date().toISOString().slice(0, 10);
      const lastCheckin = localStorage.getItem('smartedu_checkin_date');
      isCheckedIn.value = lastCheckin === today;
    } catch {
      // 静默
    }
  };

  type LoginUserInfo = {
    points?: number
    userAvatar?: string
    userName?: string
    name?: string
  }

  const fetchFreshUserInfo = async () => {
    try {
      const data = await request.get<any, LoginUserInfo>('/user/get/login', {
        skipErrorToast: true
      })

      if (data?.points !== undefined) {
        userInfo.value.points = data.points
        syncLocalPoints(data.points)
      }
    } catch {
      // 静默：session 过期不影响页面
    }
  }

  // 签到打卡
  const handleCheckIn = async () => {
    if (isCheckedIn.value) return;
    checkInLoading.value = true;
    try {
      const newPoints = await userCheckIn();
      syncLocalPoints(newPoints);
      isCheckedIn.value = true;
      localStorage.setItem('smartedu_checkin_date', new Date().toISOString().slice(0, 10));
      message.success('签到成功！积分 +5');
    } catch (error: any) {
      // 🌟 优化报错体验：如果捕获到报错，说明极有可能在别的设备签到过了，或者后端拦截了
      // 我们直接把按钮置灰，不再弹出吓人的红色 Error
      isCheckedIn.value = true;
      localStorage.setItem('smartedu_checkin_date', new Date().toISOString().slice(0, 10));
      message.warning('您今天好像已经签到过啦，明天再来吧！');
    } finally {
      checkInLoading.value = false;
    }
  };

  // 🌟 核心改造：调用后端接口完成计划并获取积分
  const togglePlanStatus = async (id: string) => {
    const plan = dailyPlans.value.find(p => p.id === id);
    if (!plan) return;

    if (plan.completed) {
      message.warning('已经完成的计划无法取消哦');
      return; // 后端不支持取消完成退扣积分，前端予以拦截
    }

    try {
      // 调用完成计划接口，返回的是加分后的最新总积分
      const newPoints = await completeStudyPlan(id);

      plan.completed = true;
      syncLocalPoints(newPoints); // 🌟 实时把加了10分的最新分数刷到右上角和本地缓存
      saveLocalData();

      message.success('太棒了！完成任务，积分 +10 🎉');
    } catch (error: any) {
      message.error('提交失败: ' + (error.message || '网络异常'));
    }
  };

  // 删除计划 (由于我们只是演示排行榜，删除就不必非得调后端了，前端干掉即可)
  const deletePlan = (id: string) => {
    dailyPlans.value = dailyPlans.value.filter(p => p.id !== id);
    saveLocalData();
  };

  const logout = async () => {
    try {
      await request.post('/user/logout', undefined, {
        skipErrorToast: true
      });
    } catch {
      // Session 可能已经过期，本地状态仍需清理。
    } finally {
      // 清掉所有用户相关的 localStorage
      localStorage.removeItem('user');               // 历史遗留 key
      clearTabAuth();
      localStorage.removeItem('smartedu_daily_plans');
      localStorage.removeItem('smartedu_checkin_date');
      // 🌟 关键：清掉污染源 sessionStorage
      // 注意：rememberedAccount / isRememberMe 不清（"记住我"下次登录还要用）

      // 重置 Pinia state
      userInfo.value = { avatar: '', name: '同学你好', points: 0 };
      isCheckedIn.value = false;
      dailyPlans.value = [];
    }
  };

  return {
    userInfo,
    isCheckedIn,
    checkInLoading,
    dailyPlans,
    completedPlanCount,
    fetchCheckInStatus,
    fetchFreshUserInfo,
    loadLocalData,
    handleCheckIn,
    addPlan,
    togglePlanStatus,
    deletePlan,
    logout
  };
});
