<script setup>
/**
 * 管理后台：用户管理 + 感悟管理。
 * 仅管理员可见（路由守卫 requiresAdmin）；后端接口另有 requireAdmin 兜底。
 */
import { ref, computed, onMounted } from 'vue'
import { adminApi, authApi } from '../api'
import { levelOf } from '../utils/points'
import { useUserStore } from '../stores/user'
import { confirmAction } from '../stores/ui'
import { toast } from '../utils/toast'
import { formatDate } from '../utils/format'
import UserAvatar from '../components/UserAvatar.vue'

const userStore = useUserStore()
const tab = ref('users')
const loading = ref(true)
const users = ref([])
const comments = ref([])
const keyword = ref('')

const filteredUsers = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return users.value
  return users.value.filter((u) =>
    (u.username + u.name + (u.role || '')).toLowerCase().includes(k))
})

async function load() {
  loading.value = true
  try {
    if (tab.value === 'users') {
      const { data } = await adminApi.users()
      users.value = data
    } else {
      const { data } = await adminApi.comments()
      comments.value = data
    }
  } finally {
    loading.value = false
  }
}
function switchTab(t) { tab.value = t; load() }

async function setRole(u, role) {
  const label = role === 'admin' ? '设为管理员' : '降为普通用户'
  const yes = await confirmAction(`确定将「${u.name || u.username}」${label}吗？`, label)
  if (!yes) return
  try {
    await adminApi.setRole(u.id, role)
    u.role = role
    toast.success('角色已更新')
  } catch (e) { toast.error(e.message) }
}

async function resetPwd(u) {
  const np = prompt(`为「${u.name || u.username}」设置新密码（至少 8 位，含字母和数字）：`)
  if (np == null) return
  try {
    await adminApi.resetUserPwd(u.id, np)
    toast.success('密码已重置')
  } catch (e) { toast.error(e.message) }
}

async function unlock(u) {
  try {
    await adminApi.unlock(u.id)
    toast.success('已解除锁定')
  } catch (e) { toast.error(e.message) }
}

async function adjPoints(u, delta) {
  const label = delta > 0 ? `奖励「${u.name || u.username}」${delta} 积分` : `扣除「${u.name || u.username}」${-delta} 积分`
  const yes = await confirmAction(`确定${label}吗？`, delta > 0 ? '积分奖励' : '积分扣除')
  if (!yes) return
  try {
    const { data } = await authApi.adjustPoints(u.id, delta)
    u.points = data.points
    toast.success(`已${delta > 0 ? '奖励' : '扣除'}，当前 ${data.points} 分`)
  } catch (e) { toast.error(e.message) }
}

async function deleteUser(u) {
  if (u.id === userStore.user?.id) return toast.error('不能注销自己')
  const yes = await confirmAction(`确定注销「${u.name || u.username}」吗？其收藏与感悟将一并删除。`, '注销账号')
  if (!yes) return
  try {
    await adminApi.deleteUser(u.id)
    users.value = users.value.filter((x) => x.id !== u.id)
    toast.success('已注销')
  } catch (e) { toast.error(e.message) }
}

async function delComment(c) {
  const yes = await confirmAction('确定删除这条感悟吗？', '删除感悟')
  if (!yes) return
  try {
    await adminApi.deleteComment(c.id)
    comments.value = comments.value.filter((x) => x.id !== c.id)
    toast.success('已删除')
  } catch (e) { toast.error(e.message) }
}

const lockedNow = (u) => u.lockedUntil && new Date(u.lockedUntil) > new Date()

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">管理后台</h1>
        <p class="page-sub">用户与感悟治理 · 仅管理员可见</p>
      </div>
      <div class="tabs">
        <button :class="{ on: tab === 'users' }" @click="switchTab('users')" data-test="tab-users">用户管理</button>
        <button :class="{ on: tab === 'comments' }" @click="switchTab('comments')" data-test="tab-comments">感悟管理</button>
      </div>
    </div>

    <div v-if="loading" class="spinner" />

    <!-- 用户管理 -->
    <template v-else-if="tab === 'users'">
      <input v-model.trim="keyword" class="input search" placeholder="搜用户名 / 昵称 / 角色" />
      <div class="table-wrap card">
        <table class="tbl" data-test="admin-users">
          <thead>
            <tr><th>用户</th><th>角色</th><th>积分</th><th>注册时间</th><th>状态</th><th class="th-ops">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.id">
              <td>
                <div class="u-cell">
                  <UserAvatar :name="u.name || u.username" :size="34" circle />
                  <div>
                    <div class="u-name">{{ u.name || u.username }}</div>
                    <div class="u-uname">@{{ u.username }}</div>
                  </div>
                </div>
              </td>
              <td><span class="role" :class="u.role">{{ u.role === 'admin' ? '管理员' : '普通用户' }}</span></td>
              <td class="pts-cell">
                <span class="pts-num">{{ u.points ?? 0 }}</span>
                <span class="pts-title">{{ levelOf(u.points ?? 0).title }}</span>
              </td>
              <td>{{ formatDate(u.createTime) }}</td>
              <td>
                <span v-if="lockedNow(u)" class="badge-lock">锁定中</span>
                <span v-else class="badge-ok">正常</span>
              </td>
              <td class="ops">
                <button class="mini" @click="resetPwd(u)" title="重置密码">重置密码</button>
                <button class="mini" v-if="u.role !== 'admin'" @click="setRole(u, 'admin')">设为管理员</button>
                <button class="mini" v-else-if="u.id !== userStore.user?.id" @click="setRole(u, 'user')">降为普通用户</button>
                <button class="mini" v-if="lockedNow(u)" @click="unlock(u)">解锁</button>
                <button class="mini" @click="adjPoints(u, 50)" title="奖励 50 分">+50</button>
                <button class="mini danger" v-if="(u.points ?? 0) >= 50" @click="adjPoints(u, -50)" title="扣回 50 分">-50</button>
                <button class="mini danger" v-if="u.id !== userStore.user?.id" @click="deleteUser(u)">注销</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- 感悟管理 -->
    <template v-else-if="tab === 'comments'">
      <div class="table-wrap card">
        <table class="tbl" data-test="admin-comments">
          <thead>
            <tr><th>用户</th><th>诗</th><th>感悟</th><th>时间</th><th class="th-ops">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in comments" :key="c.id">
              <td>{{ c.userName }}<div class="u-uname">@{{ c.username }}</div></td>
              <td>
                <router-link v-if="c.poemId" :to="`/poem/${c.poemId}`" class="poem-link">《{{ c.poemTitle }}》</router-link>
                <span v-else class="muted">—</span>
              </td>
              <td class="content">{{ c.content }}</td>
              <td>{{ formatDate(c.createTime) }}</td>
              <td class="ops"><button class="mini danger" @click="delComment(c)">删除</button></td>
            </tr>
            <tr v-if="!comments.length"><td colspan="5" class="empty-row">暂无感悟</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<style scoped>
.tabs { display: flex; gap: 6px; }
.tabs button {
  border: 1px solid var(--border-strong);
  background: var(--card); color: var(--text-2);
  border-radius: 999px; padding: 6px 18px;
  font-size: 13px; cursor: pointer;
}
.tabs button.on { background: var(--primary); border-color: var(--primary); color: #fbf6ea; }
.search { width: 260px; margin-bottom: 14px; }

.table-wrap { overflow-x: auto; }
.tbl { width: 100%; border-collapse: collapse; font-size: 13.5px; }
.tbl th, .tbl td { text-align: left; padding: 12px 14px; border-bottom: 1px solid var(--border); vertical-align: top; }
.tbl th { font-size: 12px; color: var(--text-3); font-weight: 600; letter-spacing: 1px; }
.tbl tbody tr:hover { background: var(--card-hover); }
.th-ops { width: 260px; }
.ops { display: flex; gap: 6px; flex-wrap: wrap; }
.mini {
  border: 1px solid var(--border-strong);
  background: var(--card); color: var(--text-2);
  border-radius: 6px; padding: 4px 10px;
  font-size: 12px; cursor: pointer; white-space: nowrap;
}
.mini:hover { border-color: var(--primary); color: var(--primary); }
.mini.danger:hover { border-color: var(--danger); color: var(--danger); }

.u-cell { display: flex; align-items: center; gap: 10px; }
.u-name { font-weight: 600; }
.u-uname { font-size: 11.5px; color: var(--text-3); }
.role { font-size: 12.5px; }
.role.admin { color: var(--accent); font-weight: 600; }
.badge-lock { color: var(--danger); font-size: 12.5px; }
.badge-ok { color: var(--success); font-size: 12.5px; }
.poem-link { color: var(--primary-2); }
.muted { color: var(--text-3); }
.content { max-width: 420px; line-height: 1.8; }
.empty-row { text-align: center; color: var(--text-3); padding: 30px 0; }

.pwd-card { margin-top: 18px; }
.pc-sec { margin: 0 0 12px; font-size: 15px; }

.pts-cell { white-space: nowrap; }
.pts-num { font-weight: 600; color: var(--accent); font-size: 14px; }
.pts-title { display: block; font-size: 11px; color: var(--text-3); }
</style>
