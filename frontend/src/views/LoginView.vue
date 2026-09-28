<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { toast } from '../utils/toast'
import Icon from '../components/Icon.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const mode = ref('login') // login | register
const loading = ref(false)
const form = ref({
  username: '',
  password: '',
  password2: '',
  name: ''
})

const isLogin = computed(() => mode.value === 'login')

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
    if (password.length < 6) return toast.error('密码长度至少 6 位')
    if (password !== password2) return toast.error('两次输入的密码不一致')
  }
  loading.value = true
  try {
    if (isLogin.value) {
      await userStore.login(username.trim(), password)
      toast.success('登录成功，欢迎回来！')
    } else {
      await userStore.register({ username: username.trim(), password, name: name.trim() })
      toast.success('注册成功，已自动登录！')
    }
    goBack()
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

function fillDemo() {
  form.value.username = 'demo'
  form.value.password = '123456'
  mode.value = 'login'
}
</script>

<template>
  <div class="login-page">
    <div class="bg-orbs">
      <div class="orb o1" />
      <div class="orb o2" />
      <div class="orb o3" />
    </div>

    <div class="login-card" data-test="login-card">
      <div class="head">
        <div class="logo"><Icon name="disc" :size="26" /></div>
        <h1>悦听</h1>
        <p class="sub">在线音乐播放网站 · Vue3 + SpringBoot</p>
      </div>

      <div class="tabs">
        <button :class="{ on: isLogin }" @click="switchMode('login')">登录</button>
        <button :class="{ on: !isLogin }" @click="switchMode('register')">注册</button>
        <span class="ink" :style="{ left: isLogin ? '0%' : '50%' }" />
      </div>

      <form @submit.prevent="onSubmit" data-test="auth-form">
        <div class="form-item">
          <label class="form-label">用户名</label>
          <input v-model.trim="form.username" class="input" placeholder="3-16 位字母、数字或下划线"
            autocomplete="username" data-test="username" />
        </div>
        <div class="form-item">
          <label class="form-label">密码</label>
          <input v-model="form.password" class="input" type="password" placeholder="至少 6 位"
            autocomplete="current-password" data-test="password" />
        </div>
        <div class="form-item" v-if="!isLogin">
          <label class="form-label">确认密码</label>
          <input v-model="form.password2" class="input" type="password" placeholder="再输入一次密码" />
        </div>
        <div class="form-item" v-if="!isLogin">
          <label class="form-label">昵称（可选）</label>
          <input v-model.trim="form.name" class="input" placeholder="默认与用户名相同" />
        </div>

        <button class="btn btn-primary btn-lg btn-block" type="submit" :disabled="loading" data-test="submit">
          {{ loading ? '请稍候…' : isLogin ? '登 录' : '注 册' }}
        </button>
      </form>

      <div class="demo-tip">
        <p>演示账号：<code>demo</code> / <code>123456</code>（管理员 <code>admin</code> / <code>admin123</code>）</p>
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
    radial-gradient(1000px 500px at 80% -10%, rgba(236, 72, 153, 0.14), transparent),
    radial-gradient(800px 400px at 10% 110%, rgba(139, 92, 246, 0.16), transparent),
    var(--bg);
}
.bg-orbs { position: absolute; inset: 0; pointer-events: none; }
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.35;
  animation: float 12s ease-in-out infinite;
}
.o1 { width: 320px; height: 320px; background: #8b5cf6; top: -60px; left: 12%; }
.o2 { width: 260px; height: 260px; background: #ec4899; bottom: -40px; right: 14%; animation-delay: -4s; }
.o3 { width: 180px; height: 180px; background: #38bdf8; top: 55%; left: 55%; animation-delay: -8s; }
@keyframes float { 50% { transform: translateY(-24px); } }

.login-card {
  position: relative;
  width: min(400px, calc(100vw - 32px));
  background: rgba(21, 25, 36, 0.86);
  backdrop-filter: blur(14px);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 34px 30px 26px;
  box-shadow: var(--shadow);
  z-index: 1;
}
.head { text-align: center; margin-bottom: 20px; }
.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: 16px;
  background: var(--grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(139, 92, 246, 0.45);
}
.head h1 {
  margin: 0;
  font-size: 26px;
  letter-spacing: 6px;
  background: linear-gradient(135deg, #a78bfa, #f472b6);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.sub { margin: 6px 0 0; font-size: 12.5px; color: var(--text-3); }

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
.tabs button.on { color: #fff; font-weight: 600; }
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
</style>
