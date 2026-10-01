<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SideNav from './components/SideNav.vue'
import TopBar from './components/TopBar.vue'
import PlayerBar from './components/PlayerBar.vue'
import QueueDrawer from './components/QueueDrawer.vue'
import ToastHost from './components/ToastHost.vue'
import ConfirmHost from './components/ConfirmHost.vue'
import AddToPlaylistModal from './components/AddToPlaylistModal.vue'

const route = useRoute()
// 登录/注册页使用极简布局（无侧边栏与播放器）
const isBare = computed(() => route.meta.bare === true)
</script>

<template>
  <router-view v-if="isBare" />
  <div v-else class="app-shell">
    <SideNav />
    <div class="main-col">
      <TopBar />
      <main class="content">
        <router-view />
      </main>
    </div>
    <PlayerBar />
    <QueueDrawer />
    <AddToPlaylistModal />
  </div>
  <!-- Toast / 确认框必须在所有布局分支之外：登录/注册页（bare）也要能弹提示 -->
  <ToastHost />
  <ConfirmHost />
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
}
.main-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.content {
  flex: 1;
  padding: 24px 28px 120px; /* 底部给播放器留出空间 */
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
}
</style>
