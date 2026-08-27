<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCaptcha } from '@/api/auth'
import { isMobileDemo } from '@/api/mock'
import { useSessionStore } from '@/stores/session'

const router = useRouter()
const route = useRoute()
const session = useSessionStore()

const form = reactive({
  userAccount: isMobileDemo ? 'student_demo' : '',
  userPassword: isMobileDemo ? 'demo123456' : '',
  captchaCode: ''
})

const captchaId = ref('')
const captchaImage = ref('')
const captchaLoading = ref(false)
const notice = ref('')

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const captcha = await getCaptcha()
    captchaId.value = captcha.captchaId
    captchaImage.value = captcha.imageBase64.startsWith('data:')
      ? captcha.imageBase64
      : `data:image/png;base64,${captcha.imageBase64}`
    if (isMobileDemo && captcha.captchaCode) {
      form.captchaCode = captcha.captchaCode
    }
  } catch (error: any) {
    notice.value = error?.message || '验证码加载失败'
  } finally {
    captchaLoading.value = false
  }
}

async function submit() {
  notice.value = ''
  if (!form.userAccount || !form.userPassword || !form.captchaCode || !captchaId.value) {
    notice.value = '请完整填写账号、密码和验证码'
    return
  }
  try {
    await session.login({
      userAccount: form.userAccount,
      userPassword: form.userPassword,
      captchaId: captchaId.value,
      captchaCode: form.captchaCode
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.replace(redirect)
  } catch {
    refreshCaptcha()
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <main class="login-page">
    <section class="brand-block">
      <p>Smart Learning Companion</p>
      <h1>智教学生端</h1>
      <span>把课程、练习、诊断和互助收进一部手机。</span>
    </section>

    <form class="login-card" @submit.prevent="submit">
      <label>
        <span>账号</span>
        <input v-model.trim="form.userAccount" autocomplete="username" placeholder="请输入学生账号" />
      </label>

      <label>
        <span>密码</span>
        <input
          v-model="form.userPassword"
          autocomplete="current-password"
          placeholder="请输入密码"
          type="password"
        />
      </label>

      <label>
        <span>验证码</span>
        <div class="captcha-row">
          <input v-model.trim="form.captchaCode" inputmode="text" placeholder="图形验证码" />
          <button class="captcha" type="button" @click="refreshCaptcha">
            <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
            <span v-else>{{ captchaLoading ? '加载中' : '刷新' }}</span>
          </button>
        </div>
      </label>

      <p v-if="notice || session.error" class="notice">{{ notice || session.error }}</p>
      <p v-if="isMobileDemo" class="demo-tip">演示模式已开启，账号和验证码已自动填入。</p>

      <button class="primary-button submit-button" type="submit" :disabled="session.loading">
        {{ session.loading ? '登录中' : '进入学习' }}
      </button>
    </form>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 28px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 28px;
  background:
    linear-gradient(160deg, rgba(31, 122, 91, 0.16), transparent 34%),
    radial-gradient(circle at 88% 20%, rgba(222, 115, 86, 0.16), transparent 24%),
    #f7f5ef;
}

.brand-block p {
  margin: 0 0 8px;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.brand-block h1 {
  margin: 0;
  color: var(--ink);
  font-size: 42px;
  line-height: 1;
  letter-spacing: 0;
}

.brand-block span {
  display: block;
  max-width: 280px;
  margin-top: 12px;
  color: var(--muted);
  line-height: 1.75;
}

.login-card {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 253, 248, 0.9);
  box-shadow: var(--shadow);
}

label {
  display: grid;
  gap: 8px;
}

label span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

input {
  width: 100%;
  min-height: 46px;
  border: 1px solid rgba(31, 42, 46, 0.14);
  border-radius: 8px;
  padding: 0 12px;
  color: var(--ink);
  background: #fff;
  outline: none;
}

input:focus {
  border-color: rgba(31, 122, 91, 0.65);
  box-shadow: 0 0 0 3px rgba(31, 122, 91, 0.12);
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 116px;
  gap: 10px;
}

.captcha {
  min-height: 46px;
  border: 1px solid rgba(31, 42, 46, 0.14);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.captcha img {
  width: 100%;
  height: 44px;
  object-fit: cover;
  display: block;
}

.notice {
  margin: 0;
  color: #af4a31;
  font-size: 13px;
}

.demo-tip {
  margin: -4px 0 0;
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 800;
}

.submit-button {
  width: 100%;
}
</style>
