<script setup>
import { ref } from 'vue'
import { useUserStore } from '../stores/user'
import { toast } from '../utils/toast'
import { formatDate } from '../utils/format'
import Icon from '../components/Icon.vue'

const userStore = useUserStore()
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
        <img :src="userStore.user?.avatar" class="avatar" alt="" />
        <div>
          <div class="uname">{{ userStore.user?.name }}</div>
          <div class="u-sub">
            @{{ userStore.user?.username }} · {{ userStore.isAdmin ? '管理员' : '普通用户' }}
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
    </div>
  </div>
</template>

<style scoped>
.profile-card { max-width: 640px; padding: 26px; }
.top { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.avatar { width: 64px; height: 64px; border-radius: 50%; background: var(--bg-soft); border: 2px solid rgba(139, 92, 246, 0.4); }
.uname { font-size: 18px; font-weight: 700; }
.u-sub { font-size: 12.5px; color: var(--text-3); margin-top: 4px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
</style>
