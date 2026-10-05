<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from './Icon.vue'
import UserAvatar from './UserAvatar.vue'
import { useUserStore } from '../stores/user'
import { toast } from '../utils/toast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const menuOpen = ref(false)
const menuRef = ref(null)
const scrolled = ref(false)

const navs = [
  { to: '/today', label: '今日' },
  { to: '/poems', label: '诗选' },
  { to: '/poets', label: '诗人' },
  { to: '/collections', label: '选集' },
  { to: '/feihua', label: '飞花令' },
  { to: '/study', label: '书斋' }
]

/* 夜读模式：主题写在 <html data-theme>，localStorage 记忆 */
const night = ref(localStorage.getItem('yuetin_theme') === 'dark')
function applyTheme() {
  document.documentElement.dataset.theme = night.value ? 'dark' : ''
  localStorage.setItem('yuetin_theme', night.value ? 'dark' : 'light')
}
function toggleNight() {
  night.value = !night.value
  applyTheme()
}
applyTheme()

// 首页顶部透出动态古画，滚动后或进入内页时变为纸质实底
const overHero = computed(() => route.path === '/' && !scrolled.value)

function onScroll() {
  scrolled.value = window.scrollY > 40
}

function onSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  router.push({ path: '/poems', query: { keyword: kw } })
}

function isActive(to) {
  if (to === '/today') return route.path === '/' || route.path === '/today'
  return route.path.startsWith(to)
}

async function onLogout() {
  menuOpen.value = false
  userStore.logout()
  toast.success('已退出登录')
  router.push('/')
}

function onClickOutside(e) {
  if (menuRef.value && !menuRef.value.contains(e.target)) menuOpen.value = false
}
onMounted(() => {
  document.addEventListener('click', onClickOutside)
  window.addEventListener('scroll', onScroll, { passive: true })
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <header class="topnav" :class="{ solid: !overHero }">
    <router-link to="/" class="brand">
      <span class="brand-seal">弦</span>
      <span class="brand-name font-brush">弦歌</span>
    </router-link>

    <nav class="nav-links">
      <router-link
        v-for="n in navs" :key="n.to" :to="n.to"
        class="nav-link" :class="{ active: isActive(n.to) }"
      >{{ n.label }}</router-link>
    </nav>

    <form class="search-box" @submit.prevent="onSearch">
      <Icon name="search" :size="15" class="search-icon" />
      <input
        v-model="keyword"
        class="search-input"
        type="text"
        placeholder="搜诗题 / 诗句 / 诗人"
        data-test="global-search"
      />
    </form>

    <div class="right" ref="menuRef">
      <button class="theme-btn" :title="night ? '切换到日间' : '切换到夜读'" @click="toggleNight" data-test="night-toggle">
        {{ night ? '☀' : '☾' }}
      </button>
      <template v-if="userStore.isLoggedIn">
        <button class="user-chip" @click.stop="menuOpen = !menuOpen" data-test="user-menu">
          <UserAvatar :name="userStore.user.name" :size="28" circle />
          <span class="uname">{{ userStore.user.name }}</span>
          <Icon name="chevron-down" :size="14" />
        </button>
        <div v-if="menuOpen" class="menu">
          <div class="menu-head">
            <div class="menu-name">{{ userStore.user.name }}</div>
            <div class="menu-role">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</div>
          </div>
          <button class="menu-item" @click="menuOpen = false; router.push('/study')">
            <Icon name="heart" :size="15" /> 我的书斋
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
.topnav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 120;
  display: flex;
  align-items: center;
  gap: 26px;
  padding: 14px 34px;
  transition: background 0.35s ease, border-color 0.35s ease, box-shadow 0.35s ease;
}
.topnav.solid {
  background: rgba(250, 246, 234, 0.92);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 4px 20px rgba(88, 70, 40, 0.08);
}
.brand { display: flex; align-items: center; gap: 10px; color: var(--text); }
.brand-seal {
  width: 34px; height: 34px;
  border-radius: 6px;
  background: var(--seal);
  color: #fbf6ea;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', serif;
  font-size: 22px;
  padding-bottom: 2px;
  box-shadow: 0 4px 12px rgba(165, 64, 45, 0.35);
}
.brand-name { font-size: 21px; letter-spacing: 3px; }

.nav-links { display: flex; gap: 6px; margin-left: 8px; }
.nav-link {
  padding: 7px 16px;
  border-radius: 999px;
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--text-2);
  transition: all 0.18s;
}
.nav-link:hover { color: var(--primary); }
.nav-link.active { background: var(--grad-soft); color: var(--primary); }
.search-box { position: relative; margin-left: auto; width: min(240px, 30vw); }
.search-icon { position: absolute; left: 10px; top: 8px; color: var(--text-3); }
.search-input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: rgba(255, 253, 244, 0.9);
  color: var(--text);
  font-size: 13px;
  outline: none;
  font-family: inherit;
  transition: all 0.2s;
}
.search-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(64, 99, 77, 0.14); }
.right { display: flex; align-items: center; }
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--border-strong);
  background: var(--card);
  border-radius: 999px;
  padding: 5px 12px 5px 6px;
  cursor: pointer;
  color: var(--text);
  font-size: 13px;
  transition: border-color 0.18s;
}
.user-chip:hover { border-color: var(--primary); }
.uname { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.menu {
  position: absolute;
  right: 34px;
  top: calc(100% + 8px);
  width: 200px;
  background: var(--panel);
  border: 1px solid var(--border-strong);
  border-radius: 12px;
  box-shadow: var(--shadow);
  padding: 6px;
  z-index: 130;
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
.menu-item:hover { background: rgba(64, 99, 77, 0.08); color: var(--text); }
.menu-item.danger:hover { color: var(--danger); background: rgba(127, 29, 29, 0.07); }

@media (max-width: 860px) {
  .search-box { display: none; }
  .topnav { padding: 12px 18px; gap: 12px; }
}
.theme-btn {
  width: 32px; height: 32px;
  border: 1px solid var(--border-strong);
  border-radius: 50%;
  background: transparent;
  color: var(--text-2);
  font-size: 15px;
  cursor: pointer;
  margin-right: 10px;
  transition: all 0.2s;
}
.theme-btn:hover { color: var(--accent); border-color: var(--accent); }
</style>
