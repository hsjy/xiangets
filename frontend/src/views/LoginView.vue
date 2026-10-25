<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'
import { toast } from '../utils/toast'
import Icon from '../components/Icon.vue'

// 与后端 AuthService 相同的用户名规则
const USERNAME_RE = /^[A-Za-z0-9_]{3,20}$/

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const mode = ref('login') // login | register | forgot
const loading = ref(false)
const remember = ref(true)
const form = ref({
  username: '',
  password: '',
  password2: '',
  name: '',
  pwdQuestion: '出生城市',
  pwdAnswer: ''
})

// 忘记密码三步：用户名 → 答密保 → 设新密码
const forgot = ref({ step: 1, username: '', question: '', answer: '', newPassword: '', newPassword2: '' })

const isLogin = computed(() => mode.value === 'login')
const PWD_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/   // 与后端强度规则一致

function switchMode(m) {
  mode.value = m
}

function goBack() {
  const target = route.query.redirect
  router.push(typeof target === 'string' && target.startsWith('/') ? target : '/discover')
}

async function onSubmit() {
  const { username, password, password2, name } = form.value
  if (!username.trim() || !password) {
    toast.error('请输入用户名和密码')
    return
  }
  if (!isLogin.value) {
    if (!USERNAME_RE.test(username.trim())) {
      return toast.error('用户名需为 3-20 位字母、数字或下划线')
    }
    if (!PWD_RE.test(password)) return toast.error('密码需至少 8 位，且同时包含字母和数字')
    if (password !== password2) return toast.error('两次输入的密码不一致')
    if (!form.value.pwdQuestion || !form.value.pwdAnswer) return toast.error('请设置密保问题与答案')
  }
  loading.value = true
  try {
    if (isLogin.value) {
      await userStore.login(username.trim(), password, remember.value)
      toast.success('登录成功，欢迎回来！')
      goBack()
    } else {
      // 注册成功后回到登录页：预填用户名，清空密码，由用户主动登录
      await authApi.register({
        username: username.trim(), password, name: name.trim(),
        pwdQuestion: form.value.pwdQuestion, pwdAnswer: form.value.pwdAnswer
      })
      toast.success('注册成功，请使用新账号登录')
      mode.value = 'login'
      form.value = { username: username.trim(), password: '', password2: '', name: '', pwdQuestion: '出生城市', pwdAnswer: '' }
    }
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

/* ---- 忘记密码 ---- */
async function forgotStep1() {
  const u = forgot.value.username.trim()
  if (!u) return toast.error('请输入用户名')
  loading.value = true
  try {
    const { data } = await authApi.pwdQuestion(u)
    forgot.value.question = data.question
    forgot.value.step = 2
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function forgotStep2() {
  const f = forgot.value
  if (!f.answer) return toast.error('请输入密保答案')
  if (!PWD_RE.test(f.newPassword)) return toast.error('新密码需至少 8 位，且同时包含字母和数字')
  if (f.newPassword !== f.newPassword2) return toast.error('两次输入的密码不一致')
  loading.value = true
  try {
    await authApi.pwdReset({ username: f.username.trim(), answer: f.answer, newPassword: f.newPassword })
    toast.success('密码已重置，请用新密码登录')
    form.value.username = f.username.trim()
    form.value.password = ''
    mode.value = 'login'
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

function fillDemo() {
  form.value.username = 'demo'
  form.value.password = 'demo123456'
  mode.value = 'login'
}
</script>

<template>
  <div class="login-page">
    <div class="bg-ink">
      <div class="orb o1" />
      <div class="orb o2" />
      <div class="orb o3" />
      <div class="mountains" />
    </div>

    <div class="login-card" data-test="login-card">
      <div class="head">
        <div class="seal">弦歌</div>
        <h1 class="font-brush">弦歌</h1>
        <p class="sub">诗乐雅集 · 古诗配乐欣赏</p>
      </div>

      <div class="tabs" v-if="mode !== 'forgot'">
        <button :class="{ on: isLogin }" @click="switchMode('login')">登录</button>
        <button :class="{ on: !isLogin }" @click="switchMode('register')">注册</button>
        <span class="ink" :style="{ left: isLogin ? '0%' : '50%' }" />
      </div>

      <!-- 忘记密码流程 -->
      <div v-if="mode === 'forgot'" class="forgot" data-test="forgot-flow">
        <template v-if="forgot.step === 1">
          <div class="form-item">
            <label class="form-label">用户名</label>
            <input v-model.trim="forgot.username" class="input" placeholder="输入注册时的用户名" data-test="forgot-username" />
          </div>
          <button class="btn btn-primary btn-lg btn-block" :disabled="loading" @click="forgotStep1" data-test="forgot-next">
            {{ loading ? '查询中…' : '下一步' }}
          </button>
        </template>
        <template v-else>
          <p class="forgot-q">密保问题：{{ forgot.question }}</p>
          <div class="form-item">
            <label class="form-label">密保答案</label>
            <input v-model="forgot.answer" class="input" data-test="forgot-answer" />
          </div>
          <div class="form-item">
            <label class="form-label">新密码</label>
            <input v-model="forgot.newPassword" class="input" type="password" placeholder="至少 8 位，含字母和数字" data-test="forgot-new" />
          </div>
          <div class="form-item">
            <label class="form-label">确认新密码</label>
            <input v-model="forgot.newPassword2" class="input" type="password" />
          </div>
          <button class="btn btn-primary btn-lg btn-block" :disabled="loading" @click="forgotStep2" data-test="forgot-reset">
            {{ loading ? '重置中…' : '重置密码' }}
          </button>
        </template>
        <button class="back-login" @click="mode = 'login'">返回登录</button>
      </div>

      <form v-else @submit.prevent="onSubmit" data-test="auth-form">
        <div class="form-item">
          <label class="form-label">用户名</label>
          <input v-model.trim="form.username" class="input" placeholder="3-20 位字母、数字或下划线"
            autocomplete="username" data-test="username" />
        </div>
        <div class="form-item">
          <label class="form-label">密码</label>
          <input v-model="form.password" class="input" type="password" placeholder="至少 8 位，含字母和数字"
            autocomplete="current-password" data-test="password" />
        </div>
        <div class="form-item" v-if="!isLogin">
          <label class="form-label">确认密码</label>
          <input v-model="form.password2" class="input" type="password" placeholder="再输入一次密码" />
        </div>
        <div class="form-item" v-if="!isLogin">
          <label class="form-label">密保问题</label>
          <select v-model="form.pwdQuestion" class="input" data-test="pwd-question">
            <option>出生城市</option>
            <option>母亲的名字</option>
            <option>小学的名字</option>
            <option>第一只宠物的名字</option>
            <option>最喜欢的一首诗</option>
          </select>
        </div>
        <div class="form-item" v-if="!isLogin">
          <label class="form-label">密保答案</label>
          <input v-model="form.pwdAnswer" class="input" placeholder="用于找回密码，请勿遗忘" data-test="pwd-answer" />
        </div>
        <div class="form-item" v-if="!isLogin">
          <label class="form-label">昵称（可选）</label>
          <input v-model.trim="form.name" class="input" placeholder="默认与用户名相同" />
        </div>

        <div class="form-item" v-if="isLogin">
          <label class="remember-row">
            <input type="checkbox" v-model="remember" data-test="remember-me" />
            <span>记住我（30 天内免登录）</span>
          </label>
          <button type="button" class="forgot-link" @click="mode = 'forgot'" data-test="go-forgot">忘记密码？</button>
        </div>

        <button class="btn btn-primary btn-lg btn-block" type="submit" :disabled="loading" data-test="submit">
          {{ loading ? '请稍候…' : isLogin ? '登 录' : '注 册' }}
        </button>
      </form>

      <div class="demo-tip">
        <p>演示账号：<code>demo</code> / <code>demo123456</code></p>
        <button class="btn btn-ghost btn-sm" @click="fillDemo">一键填入演示账号</button>
      </div>
    </div>

    <button class="btn btn-ghost back" @click="goBack">
      <Icon name="arrow-left" :size="15" /> 先去逛逛
    </button>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(1000px 500px at 80% -10%, rgba(74, 124, 89, 0.1), transparent),
    radial-gradient(800px 400px at 10% 110%, rgba(58, 54, 48, 0.12), transparent),
    var(--bg);
}
/* 水墨背景：淡墨晕 + 远山剪影 */
.bg-ink { position: absolute; inset: 0; pointer-events: none; }
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 14s ease-in-out infinite;
}
.o1 { width: 340px; height: 340px; background: rgba(58, 54, 48, 0.16); top: -80px; left: 10%; }
.o2 { width: 280px; height: 280px; background: rgba(64, 99, 77, 0.14); bottom: -60px; right: 12%; animation-delay: -5s; }
.o3 { width: 200px; height: 200px; background: rgba(47, 79, 79, 0.14); top: 52%; left: 56%; animation-delay: -9s; }
@keyframes float { 50% { transform: translateY(-24px); } }
.mountains {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 38vh;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 420' preserveAspectRatio='none'%3E%3Cpath d='M0 420 L0 300 Q120 220 260 268 T540 236 T820 276 T1100 226 T1440 262 L1440 420 Z' fill='%233a3630' opacity='0.08'/%3E%3Cpath d='M0 420 L0 336 Q180 260 360 306 T720 286 T1080 316 T1440 286 L1440 420 Z' fill='%233a3630' opacity='0.12'/%3E%3Cpath d='M0 420 L0 372 Q240 310 480 348 T960 336 T1440 356 L1440 420 Z' fill='%233a3630' opacity='0.16'/%3E%3C/svg%3E");
  background-size: cover;
}

.login-card {
  position: relative;
  width: min(400px, calc(100vw - 32px));
  background: rgba(250, 246, 234, 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border-strong);
  border-radius: 12px;
  padding: 34px 30px 26px;
  box-shadow: var(--shadow);
  z-index: 1;
}
.head { text-align: center; margin-bottom: 20px; }
.seal {
  width: 54px;
  height: 54px;
  margin: 0 auto 12px;
  border-radius: 6px;
  background: var(--primary);
  color: #fbf6ea;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', 'Songti SC', serif;
  font-size: 26px;
  line-height: 1.1;
  letter-spacing: 2px;
  text-indent: 2px;
  box-shadow: 0 8px 22px rgba(166, 58, 43, 0.4);
}
.head h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: 10px;
  text-indent: 10px;
  color: var(--ink);
}
.sub { margin: 6px 0 0; font-size: 12.5px; color: var(--text-3); letter-spacing: 2px; }

.tabs {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: var(--bg-soft);
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 22px;
}
.tabs button {
  position: relative;
  z-index: 1;
  border: none;
  background: none;
  color: var(--text-2);
  padding: 8px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 8px;
  transition: color 0.2s;
}
.tabs button.on { color: #fbf6ea; font-weight: 600; }
.ink {
  position: absolute;
  top: 4px;
  bottom: 4px;
  width: calc(50% - 4px);
  margin-left: 4px;
  border-radius: 8px;
  background: var(--grad);
  transition: left 0.25s ease;
}

.demo-tip {
  margin-top: 18px;
  text-align: center;
}
.demo-tip p { font-size: 12px; color: var(--text-3); margin: 0 0 8px; }
.demo-tip code {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 5px;
  padding: 1px 6px;
  color: var(--primary-2);
}

.back {
  position: absolute;
  top: 20px;
  left: 20px;
}

.remember-row {
  display: flex; align-items: center; gap: 6px;
  font-size: 12.5px; color: var(--text-2); cursor: pointer;
}
.forgot-link {
  margin-left: auto; border: none; background: none;
  font-size: 12.5px; color: var(--accent); cursor: pointer;
}
.forgot-link:hover { text-decoration: underline; }
.forgot-q {
  margin: 0 0 14px; padding: 10px 14px;
  border-radius: 8px; background: var(--card-hover);
  font-size: 13.5px; color: var(--text);
}
.back-login {
  margin-top: 12px; border: none; background: none;
  font-size: 12.5px; color: var(--text-3); cursor: pointer; width: 100%;
}
.back-login:hover { color: var(--primary); }
select.input { appearance: auto; }
</style>
