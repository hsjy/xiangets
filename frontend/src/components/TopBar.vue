<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import Icon from './Icon.vue'
import { useUserStore } from '../stores/user'
import { toast } from '../utils/toast'

const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const menuOpen = ref(false)
const menuRef = ref(null)

function onSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  router.push({ path: '/songs', query: { keyword: kw } })
}

async function onLogout() {
  menuOpen.value = false
  userStore.logout()
  toast.success('已退出登录')
  router.push('/discover')
}

function onClickOutside(e) {
  if (menuRef.value && !menuRef.value.contains(e.target)) menuOpen.value = false
}
onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<template>
  <header class="topbar">
    <form class="search-box" @submit.prevent="onSearch">
      <Icon name="search" :size="16" class="search-icon" />
      <input
        v-model="keyword"
        class="search-input"
        type="text"
        placeholder="搜索歌曲 / 歌手 / 歌单"
        data-test="global-search"
      />
    </form>

    <div class="right" ref="menuRef">
      <template v-if="userStore.isLoggedIn">
        <button class="user-chip" @click.stop="menuOpen = !menuOpen" data-test="user-menu">
          <img :src="userStore.user.avatar" class="avatar" alt="" />
          <span class="uname">{{ userStore.user.name }}</span>
          <Icon name="chevron-down" :size="14" />
        </button>
        <!-- 用 CSS animation 做入场，避免 <transition> 在后台标签页 rAF 停摆时残留 enter-from 类 -->
        <div v-if="menuOpen" class="menu">
          <div class="menu-head">
            <div class="menu-name">{{ userStore.user.name }}</div>
            <div class="menu-role">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</div>
          </div>
          <button class="menu-item" @click="menuOpen = false; router.push('/playlists')">
            <Icon name="list-music" :size="15" /> 我的歌单
          </button>
          <button class="menu-item" @click="menuOpen = false; router.push('/profile')">
            <Icon name="user" :size="15" /> 个人资料
          </button>
          <button class="menu-item danger" @click="onLogout" data-test="logout">
            <Icon name="logout" :size="15" /> 退出登录
          </button>
        </div>
      </template>
      <router-link v-else to="/login" class="btn btn-primary btn-sm" data-test="go-login">
        <Icon name="user" :size="15" /> 登录 / 注册
      </router-link>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 28px;
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(250, 246, 234, 0.88);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}
.search-box {
  position: relative;
  width: min(380px, 100%);
}
.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-3);
  pointer-events: none;
}
.search-input {
  width: 100%;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 9px 16px 9px 38px;
  color: var(--text);
  font-size: 13.5px;
  outline: none;
  transition: border-color 0.18s, box-shadow 0.18s;
}
.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(166, 58, 43, 0.14);
}

.right { position: relative; }
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 999px;
  color: var(--text);
  padding: 5px 12px 5px 6px;
  cursor: pointer;
  font-size: 13.5px;
}
.user-chip:hover { border-color: var(--primary); }
.avatar { width: 28px; height: 28px; border-radius: 50%; background: var(--bg-soft); }
.uname { max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.menu {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: 200px;
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow);
  padding: 6px;
  z-index: 50;
  animation: menu-in 0.15s ease;
}
@keyframes menu-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: none; }
}
.menu-head { padding: 10px 12px 8px; border-bottom: 1px solid var(--border); margin-bottom: 6px; }
.menu-name { font-weight: 600; font-size: 14px; }
.menu-role { font-size: 12px; color: var(--text-3); margin-top: 2px; }
.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: none;
  background: none;
  color: var(--text-2);
  font-size: 13.5px;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
}
.menu-item:hover { background: rgba(166, 58, 43, 0.07); color: var(--text); }
.menu-item.danger:hover { color: var(--danger); background: rgba(244, 63, 94, 0.08); }
</style>
