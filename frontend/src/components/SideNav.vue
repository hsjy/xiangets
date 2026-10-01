<script setup>
import { useRoute } from 'vue-router'
import Icon from './Icon.vue'
import { usePlayerStore } from '../stores/player'

const route = useRoute()
const player = usePlayerStore()

const navs = [
  { to: '/discover', icon: 'compass', label: '发现音乐' },
  { to: '/songs', icon: 'music', label: '歌曲库' },
  { to: '/singers', icon: 'mic', label: '歌手' },
  { to: '/playlists', icon: 'list-music', label: '我的歌单', auth: true }
]
</script>

<template>
  <aside class="sidenav">
    <router-link to="/discover" class="brand">
      <span class="brand-logo">弦</span>
      <span class="brand-name">弦歌</span>
    </router-link>

    <nav class="nav-list">
      <router-link
        v-for="n in navs" :key="n.to" :to="n.to"
        class="nav-item" :class="{ active: route.path.startsWith(n.to) }"
      >
        <Icon :name="n.icon" :size="19" />
        <span>{{ n.label }}</span>
      </router-link>
    </nav>

    <div class="now-mini" v-if="player.current">
      <router-link :to="`/song/${player.current.id}`" class="now-cover">
        <Icon name="music" :size="16" />
      </router-link>
      <div class="now-info">
        <div class="now-name">{{ player.current.name }}</div>
        <div class="now-singer">{{ player.current.singerName }}</div>
      </div>
    </div>

    <div class="foot">
      <p>Vue3 · SpringBoot · MySQL</p>
      <p>诗乐雅集 · 古诗配乐欣赏</p>
    </div>
  </aside>
</template>

<style scoped>
.sidenav {
  width: var(--nav-w);
  min-width: var(--nav-w);
  height: 100vh;
  position: sticky;
  top: 0;
  background: var(--bg-soft);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: 20px 14px;
  z-index: 20;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 18px;
  color: var(--text);
}
.brand-logo {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  background: var(--seal);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fbf6ea;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', 'Songti SC', serif;
  font-size: 24px;
  line-height: 1;
  padding-bottom: 2px;
  box-shadow: 0 4px 12px rgba(165, 64, 45, 0.35);
}
.brand-name {
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', 'Songti SC', serif;
  font-size: 24px;
  font-weight: 400;
  letter-spacing: 3px;
  color: var(--text);
}
.nav-list { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; }
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 10px;
  color: var(--text-2);
  font-size: 14.5px;
  transition: all 0.15s;
}
.nav-item:hover { color: var(--text); background: rgba(64, 99, 77, 0.08); }
.nav-item.active {
  color: var(--text);
  background: var(--grad-soft);
  font-weight: 600;
}
.nav-item.active svg { color: var(--primary-2); }

.now-mini {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  background: var(--card);
  border: 1px solid var(--border);
}
.now-cover {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: var(--grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  animation: pulse 2.4s ease infinite;
}
@keyframes pulse { 50% { filter: brightness(1.25); } }
.now-info { min-width: 0; }
.now-name { font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.now-singer { font-size: 11.5px; color: var(--text-3); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.foot { margin-top: 16px; padding: 0 10px; }
.foot p { margin: 2px 0; font-size: 11px; color: var(--text-3); }

@media (max-width: 900px) {
  .sidenav { width: 64px; min-width: 64px; padding: 20px 8px; }
  .brand-name, .nav-item span, .now-info, .foot { display: none; }
  .nav-item { justify-content: center; padding: 11px; }
  .brand { justify-content: center; padding-bottom: 18px; }
  .now-mini { display: none; }
}
</style>
