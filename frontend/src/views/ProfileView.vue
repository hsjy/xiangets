<script setup>
import { ref } from 'vue'
import { authApi } from '../api'
import { useUserStore } from '../stores/user'
import { toast } from '../utils/toast'
import { formatDate } from '../utils/format'
import { levelOf } from '../utils/points'
import Icon from '../components/Icon.vue'
import UserAvatar from '../components/UserAvatar.vue'

const userStore = useUserStore()

// 挂载时刷新用户信息（积分可能被飞花令/管理员调整）
userStore.fetchProfile()

/* ---- 修改密码 ---- */
const pwd = ref({ old: '', next: '', confirm: '' })
const pwdSaving = ref(false)
async function changePwd() {
  if (!pwd.value.old || !pwd.value.next) return toast.error('请填写当前密码与新密码')
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(pwd.value.next)) {
    return toast.error('新密码需至少 8 位，且同时包含字母和数字')
  }
  if (pwd.value.next !== pwd.value.confirm) return toast.error('两次输入的新密码不一致')
  pwdSaving.value = true
  try {
    await authApi.changePassword({ oldPassword: pwd.value.old, newPassword: pwd.value.next })
    toast.success('密码已更新')
    pwd.value = { old: '', next: '', confirm: '' }
  } catch (e) {
    toast.error(e.message)
  } finally {
    pwdSaving.value = false
  }
}
const form = ref({
  name: userStore.user?.name || '',
  sex: userStore.user?.sex ?? 0,
  birth: userStore.user?.birth || '',
  intro: userStore.user?.intro || '',
  phone: userStore.user?.phone || '',
  email: userStore.user?.email || ''
})
const saving = ref(false)

async function save() {
  if (!form.value.name.trim()) return toast.error('昵称不能为空')
  saving.value = true
  try {
    await userStore.updateProfile(userStore.user.id, { ...form.value })
    toast.success('资料已更新')
  } catch (e) {
    toast.error(e.message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <h1 class="page-title">个人资料</h1>
    </div>

    <div class="card profile-card">
      <div class="top">
        <UserAvatar :name="userStore.user?.name" :size="64" />
        <div>
          <div class="uname">{{ userStore.user?.name }}</div>
          <div class="u-sub">
            @{{ userStore.user?.username }} · {{ userStore.isAdmin ? '管理员' : '普通用户' }}
            <template v-if="userStore.user?.points != null"> · {{ levelOf(userStore.user.points).title }} · {{ userStore.user.points }} 分</template>
            <template v-if="userStore.user?.createTime"> · 注册于 {{ formatDate(userStore.user.createTime) }}</template>
          </div>
        </div>
      </div>

      <form @submit.prevent="save" data-test="profile-form">
        <div class="two-col">
          <div class="form-item">
            <label class="form-label">昵称</label>
            <input v-model="form.name" class="input" maxlength="20" />
          </div>
          <div class="form-item">
            <label class="form-label">性别</label>
            <select v-model.number="form.sex" class="select">
              <option :value="0">保密</option>
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
          </div>
          <div class="form-item">
            <label class="form-label">生日</label>
            <input v-model="form.birth" class="input" type="date" />
          </div>
          <div class="form-item">
            <label class="form-label">手机号</label>
            <input v-model="form.phone" class="input" maxlength="15" placeholder="选填" />
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">邮箱</label>
          <input v-model="form.email" class="input" type="email" placeholder="选填" />
        </div>
        <div class="form-item">
          <label class="form-label">个性签名</label>
          <textarea v-model="form.intro" class="textarea" maxlength="120" placeholder="介绍一下自己吧" />
        </div>
        <button class="btn btn-primary" type="submit" :disabled="saving" data-test="profile-save">
          <Icon name="check" :size="15" /> {{ saving ? '保存中…' : '保存修改' }}
        </button>
      </form>

      <form @submit.prevent="changePwd" class="card pwd-card" data-test="pwd-form">
        <h3 class="pc-sec">修改密码</h3>
        <div class="two-col">
          <div class="form-item">
            <label class="form-label">当前密码</label>
            <input v-model="pwd.old" class="input" type="password" autocomplete="current-password" data-test="pwd-old" />
          </div>
          <div class="form-item">
            <label class="form-label">新密码</label>
            <input v-model="pwd.next" class="input" type="password" placeholder="至少 8 位，含字母和数字" data-test="pwd-new" />
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">确认新密码</label>
          <input v-model="pwd.confirm" class="input" type="password" data-test="pwd-confirm" />
        </div>
        <button class="btn btn-primary" type="submit" :disabled="pwdSaving">{{ pwdSaving ? '保存中…' : '更新密码' }}</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.profile-card { max-width: 640px; padding: 26px; }
.top { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.avatar { width: 64px; height: 64px; border-radius: 50%; background: var(--bg-soft); border: 2px solid rgba(64, 99, 77, 0.45); }
.uname { font-size: 18px; font-weight: 700; }
.u-sub { font-size: 12.5px; color: var(--text-3); margin-top: 4px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
</style>
