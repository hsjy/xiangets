<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TopNav from './components/TopNav.vue'
import PlayerBar from './components/PlayerBar.vue'
import QueueDrawer from './components/QueueDrawer.vue'
import ToastHost from './components/ToastHost.vue'
import ConfirmHost from './components/ConfirmHost.vue'

const route = useRoute()
// 登录/注册页使用极简布局（无导航与播放器）
const isBare = computed(() => route.meta.bare === true)
// 首页是全幅沉浸式长卷，不用内页容器
const isHome = computed(() => route.path === '/')
</script>

<template>
  <router-view v-if="isBare" />
  <div v-else class="app-shell">
    <TopNav />
    <main class="content" :class="{ full: isHome }">
      <router-view />
    </main>
    <PlayerBar />
    <QueueDrawer />
  </div>
  <!-- Toast / 确认框必须在所有布局分支之外：登录/注册页（bare）也要能弹提示 -->
  <ToastHost />
  <ConfirmHost />
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}
.content {
  padding: 92px 28px 120px; /* 顶部给固定导航留出空间，底部给播放器 */
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
}
.content.full {
  max-width: none;
  padding: 0 0 120px;
}
</style>
