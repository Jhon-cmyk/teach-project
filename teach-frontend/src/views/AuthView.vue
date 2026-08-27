<template>
  <div class="auth-container">
    <div class="auth-panel">
      <div class="brand-header">
        <img src="/favicon.png" alt="Logo" class="logo-img" />
        <div class="title-group">
          <span class="system-name">智慧教育</span>
          <span class="system-subtitle">多模态教学过程分析与数字化资源制作</span>
        </div>
      </div>

      <div class="auth-card">
        <div class="header">
          <h1>{{ isLogin ? '欢迎回来' : '创建账号' }}</h1>
          <p class="subtitle">{{ isLogin ? '登录账号继续使用平台' : '教师注册需填写管理端发放的注册号' }}</p>
        </div>

        <a-form
          :model="formState"
          name="auth_form"
          layout="vertical"
          class="auth-form"
          @finish="handleSubmit"
        >
          <a-form-item v-if="!isLogin">
            <a-radio-group v-model:value="formState.userRole" button-style="solid" class="role-selector">
              <a-radio-button value="student">学生注册</a-radio-button>
              <a-radio-button value="teacher">教师注册</a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item
            v-if="!isLogin && formState.userRole === 'teacher'"
            name="teacherRegisterCode"
            :rules="[{ required: true, message: '请输入教师注册号' }]"
          >
            <a-input v-model:value="formState.teacherRegisterCode" placeholder="教师注册号" size="large" />
          </a-form-item>

          <a-form-item v-if="!isLogin" name="userName" :rules="[{ required: true, message: '请输入昵称' }]">
            <a-input v-model:value="formState.userName" placeholder="昵称" size="large" />
          </a-form-item>

          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input v-model:value="formState.userAccount" placeholder="账号 / 学号 / 工号" size="large" />
          </a-form-item>

          <a-form-item name="userPassword" :rules="[{ required: true, message: '请输入密码' }, { min: 6, message: '密码不少于 6 位' }]">
            <a-input-password v-model:value="formState.userPassword" placeholder="密码" size="large" />
          </a-form-item>

          <a-form-item
            v-if="!isLogin"
            name="checkPassword"
            :rules="[{ required: true, message: '请再次输入密码' }, { validator: validatePass2, trigger: 'change' }]"
          >
            <a-input-password v-model:value="formState.checkPassword" placeholder="确认密码" size="large" />
          </a-form-item>

          <a-form-item v-if="isLogin" name="captchaCode" :rules="[{ required: true, message: '请输入图形验证码' }]">
            <div class="captcha-row">
              <a-input v-model:value="formState.captchaCode" placeholder="图形验证码" size="large" :maxlength="6" />
              <div class="captcha-img-wrap" title="点击刷新" @click="loadCaptcha">
                <img v-if="captchaImg" :src="captchaImg" alt="图形验证码" class="captcha-img" />
                <span v-else class="captcha-placeholder">加载中</span>
              </div>
            </div>
          </a-form-item>

          <div class="form-options" v-if="isLogin">
            <a-checkbox v-model:checked="formState.rememberMe">记住账号</a-checkbox>
          </div>

          <a-form-item>
            <a-button type="primary" html-type="submit" block size="large" class="submit-btn" :loading="loading">
              {{ isLogin ? '立即登录' : '立即注册' }}
            </a-button>
          </a-form-item>
        </a-form>

        <div class="toggle-auth">
          <span>{{ isLogin ? '还没有账号？' : '已经有账号了？' }}</span>
          <a @click="toggleAuthState">{{ isLogin ? '免费注册' : '直接登录' }}</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import { getCaptcha, loginByCaptcha } from '@/api/auth'
import { setTabLoginUser } from '@/utils/authStorage'

const router = useRouter()
const isLogin = ref(true)
const loading = ref(false)
const DEFAULT_LOGIN_PASSWORD = '1234567'

const formState = reactive({
  userAccount: '',
  userPassword: DEFAULT_LOGIN_PASSWORD,
  checkPassword: '',
  userName: '',
  userRole: 'student',
  teacherRegisterCode: '',
  rememberMe: false,
  captchaCode: ''
})

const captchaId = ref('')
const captchaImg = ref('')

const loadCaptcha = async () => {
  try {
    const vo = await getCaptcha()
    captchaId.value = vo.captchaId
    captchaImg.value = vo.imageBase64
    formState.captchaCode = vo.captchaCode ?? ''
  } catch (e) {
    console.error('加载图形验证码失败', e)
  }
}

onMounted(() => {
  document.documentElement.classList.add('auth-no-scroll')

  const savedAccount = localStorage.getItem('rememberedAccount')
  const isRemember = localStorage.getItem('isRememberMe') === 'true'
  if (isRemember && savedAccount) {
    formState.userAccount = savedAccount
    formState.rememberMe = true
  }
  if (isLogin.value) loadCaptcha()
})

onUnmounted(() => {
  document.documentElement.classList.remove('auth-no-scroll')
})

const toggleAuthState = () => {
  isLogin.value = !isLogin.value
  formState.userAccount = ''
  formState.userPassword = isLogin.value ? DEFAULT_LOGIN_PASSWORD : ''
  formState.checkPassword = ''
  formState.userName = ''
  formState.userRole = 'student'
  formState.teacherRegisterCode = ''
  formState.captchaCode = ''
  if (isLogin.value) loadCaptcha()
}

const validatePass2 = async (_rule: any, value: string) => {
  if (value !== formState.userPassword) return Promise.reject('两次输入的密码不一致')
  return Promise.resolve()
}

const handleSubmit = async (values: any) => {
  loading.value = true
  try {
    if (isLogin.value) {
      try {
        const user: any = await loginByCaptcha({
          userAccount: values.userAccount,
          userPassword: values.userPassword,
          captchaId: captchaId.value,
          captchaCode: formState.captchaCode
        })

        localStorage.removeItem('user')
        setTabLoginUser(user)

        if (formState.rememberMe) {
          localStorage.setItem('rememberedAccount', values.userAccount)
          localStorage.setItem('isRememberMe', 'true')
        } else {
          localStorage.removeItem('rememberedAccount')
          localStorage.removeItem('isRememberMe')
        }

        message.success('登录成功')
        if (user.userRole === 'admin') router.push('/admin/dashboard')
        else if (user.userRole === 'teacher') router.push('/teacher/dashboard')
        else router.push('/student/dashboard')
      } catch (err) {
        formState.captchaCode = ''
        loadCaptcha()
        throw err
      }
    } else {
      await request.post('/user/register', {
        userAccount: values.userAccount,
        userPassword: values.userPassword,
        checkPassword: values.checkPassword,
        userName: values.userName,
        userRole: formState.userRole,
        teacherRegisterCode: formState.userRole === 'teacher' ? formState.teacherRegisterCode : undefined
      })

      message.success('注册成功，请直接登录')
      toggleAuthState()
      formState.userAccount = values.userAccount
    }
  } catch (error) {
    console.error('认证过程终止:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 32px 10vw 32px 24px;
  box-sizing: border-box;
  background: url('@/assets/bg.webp') no-repeat center center fixed;
  background-size: cover;
}

.auth-panel {
  width: min(528px, 100%);
}

.brand-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.logo-img {
  width: 98px;
  height: 98px;
  object-fit: contain;
}

.title-group {
  display: flex;
  flex-direction: column;
}

.system-name {
  color: #ffffff;
  font-size: 38px;
  font-weight: 800;
  line-height: 1.25;
}

.system-subtitle {
  margin-top: 5px;
  color: #e2e8f0;
  font-size: 22px;
  line-height: 1.5;
  font-weight: 500;
}

.auth-card {
  width: 100%;
  padding: 34px 31px;
  border-radius: 0;
  background: transparent;
  border: none;
  box-sizing: border-box;
}

.header h1 {
  margin: 0;
  color: #ffffff;
  font-size: 34px;
  font-weight: 800;
}

.subtitle {
  margin: 10px 0 25px;
  color: #cbd5e1;
  font-size: 17px;
  line-height: 1.6;
}

.role-selector {
  display: flex;
  width: 100%;
  gap: 12px;
}

:deep(.role-selector .ant-radio-button-wrapper) {
  flex: 1;
  height: 49px;
  line-height: 47px;
  text-align: center;
  border-radius: 8px !important;
  background: rgba(255, 255, 255, 0.08) !important;
  color: #dbeafe !important;
  border: 1px solid rgba(255, 255, 255, 0.14) !important;
}

:deep(.role-selector .ant-radio-button-wrapper::before) {
  display: none !important;
}

:deep(.role-selector .ant-radio-button-wrapper-checked) {
  background: #2563eb !important;
  color: #ffffff !important;
  border-color: #3b82f6 !important;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-input),
:deep(.ant-input-affix-wrapper) {
  height: 53px;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.18);
  color: #ffffff;
}

:deep(.ant-input-affix-wrapper .ant-input) {
  height: 100%;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

:deep(.ant-input::placeholder) {
  color: #94a3b8;
}

:deep(.ant-input-password-icon) {
  color: #cbd5e1;
}

.captcha-row {
  display: flex;
  gap: 14px;
}

.captcha-row :deep(.ant-input) {
  flex: 1;
}

.captcha-img-wrap {
  width: 143px;
  height: 53px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  color: #94a3b8;
  font-size: 13px;
}

.form-options {
  margin-bottom: 14px;
}

:deep(.ant-checkbox-wrapper) {
  color: #e2e8f0;
  font-size: 16px;
}

.submit-btn {
  height: 53px;
  border-radius: 8px;
  font-size: 19px;
  font-weight: 700;
  box-shadow: none;
}

.toggle-auth {
  text-align: center;
  color: #cbd5e1;
  font-size: 16px;
}

.toggle-auth a {
  margin-left: 6px;
  color: #ffffff;
  font-weight: 700;
}

@media (max-width: 900px) {
  .auth-container {
    justify-content: center;
    padding: 24px;
  }

  .auth-card {
    padding: 26px 20px;
  }
}
</style>

<style>
html.auth-no-scroll,
html.auth-no-scroll body {
  overflow: hidden !important;
  scrollbar-width: none !important;
  user-select: none;
}

html.auth-no-scroll::-webkit-scrollbar,
html.auth-no-scroll body::-webkit-scrollbar {
  display: none !important;
}

html.auth-no-scroll input,
html.auth-no-scroll textarea,
html.auth-no-scroll select {
  user-select: auto;
}
</style>
