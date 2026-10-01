<script setup>
import { ref, onMounted } from 'vue'
import { statsApi, playlistApi, songApi, singerApi } from '../api'
import { formatNumber } from '../utils/format'
import PlaylistCard from '../components/PlaylistCard.vue'
import SongTable from '../components/SongTable.vue'
import Icon from '../components/Icon.vue'

const loading = ref(true)
const stats = ref(null)
const playlists = ref([])
const hotSongs = ref([])
const singers = ref([])
const activeStyle = ref('')

const STYLES = ['流行', '摇滚', '民谣', '电子', '古风', 'R&B', '学习', '通勤', '助眠'] // P1 换诗词内容时改为情绪标签

async function load() {
  loading.value = true
  try {
    const [s, p, h, g] = await Promise.allSettled([
      statsApi.overview(),
      playlistApi.list(activeStyle.value ? { style: activeStyle.value } : {}),
      songApi.hot(8),
      singerApi.list()
    ])
    if (s.status === 'fulfilled') stats.value = s.value.data
    if (p.status === 'fulfilled') playlists.value = p.value.data
    if (h.status === 'fulfilled') hotSongs.value = h.value.data
    if (g.status === 'fulfilled') singers.value = g.value.data.slice(0, 8)
  } finally {
    loading.value = false
  }
}

function pickStyle(st) {
  activeStyle.value = activeStyle.value === st ? '' : st
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 顶部横幅 -->
    <section class="hero">
      <div class="hero-text">
        <h1 class="font-brush">弦歌不辍 · 与君共听千年诗意</h1>
        <p>古诗配乐与朗诵 · 基于 Vue3 + SpringBoot 前后端分离架构</p>
        <div class="hero-stats" v-if="stats">
          <div class="stat"><strong>{{ formatNumber(stats.songs) }}</strong><span>歌曲</span></div>
          <div class="stat"><strong>{{ formatNumber(stats.singers) }}</strong><span>歌手</span></div>
          <div class="stat"><strong>{{ formatNumber(stats.playlists) }}</strong><span>歌单</span></div>
          <div class="stat"><strong>{{ formatNumber(stats.totalPlays) }}</strong><span>总播放</span></div>
        </div>
      </div>
      <div class="hero-disc"><Icon name="disc" :size="120" /></div>
    </section>

    <!-- 歌单广场 -->
    <section class="block">
      <div class="page-head">
        <h2 class="page-title">歌单广场</h2>
        <div class="style-filter">
          <span class="tag" :class="{ active: !activeStyle }" @click="activeStyle = ''; load()">全部</span>
          <span v-for="st in STYLES" :key="st" class="tag" :class="{ active: activeStyle === st }" @click="pickStyle(st)">
            {{ st }}
          </span>
        </div>
      </div>
      <div v-if="loading" class="grid-cards">
        <div v-for="i in 6" :key="i" class="skeleton" style="aspect-ratio: 1.2" />
      </div>
      <div v-else-if="playlists.length" class="grid-cards">
        <PlaylistCard v-for="p in playlists" :key="p.id" :playlist="p" />
      </div>
      <div v-else class="empty"><div class="empty-icon">📀</div>该风格暂无歌单</div>
    </section>

    <!-- 热门歌曲 -->
    <section class="block">
      <div class="page-head">
        <h2 class="page-title"><Icon name="flame" :size="18" /> 热门歌曲</h2>
        <router-link to="/songs" class="btn btn-ghost btn-sm">查看全部 →</router-link>
      </div>
      <SongTable v-if="!loading" :songs="hotSongs" />
    </section>

    <!-- 歌手推荐 -->
    <section class="block">
      <div class="page-head">
        <h2 class="page-title">歌手推荐</h2>
        <router-link to="/singers" class="btn btn-ghost btn-sm">全部歌手 →</router-link>
      </div>
      <div class="singer-row">
        <router-link v-for="s in singers" :key="s.id" :to="`/singer/${s.id}`" class="singer-chip">
          <img :src="`https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(s.name)}&backgroundColor=b6e3f4,c0aede,d1d4f9`" :alt="s.name" loading="lazy" />
          <div>
            <div class="sc-name">{{ s.name }}</div>
            <div class="sc-style">{{ s.style }}</div>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background:
    radial-gradient(600px 300px at 85% 20%, rgba(184, 134, 11, 0.1), transparent),
    linear-gradient(135deg, rgba(166, 58, 43, 0.08), rgba(58, 54, 48, 0.85)),
    #3a3630;
  color: #efe8d6;
  border: 1px solid var(--border-strong);
  border-radius: 12px;
  padding: 34px 38px;
  margin-bottom: 30px;
  overflow: hidden;
  position: relative;
}
/* 水墨山影横幅 */
.hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 300' preserveAspectRatio='none'%3E%3Cpath d='M0 300 L0 190 Q140 120 300 168 T640 130 T980 176 T1440 120 L1440 300 Z' fill='%23efe8d6' opacity='0.06'/%3E%3Cpath d='M0 300 L0 236 Q220 160 440 210 T880 190 T1440 220 L1440 300 Z' fill='%23efe8d6' opacity='0.09'/%3E%3C/svg%3E");
  background-size: cover;
  pointer-events: none;
}
.hero-text h1 { margin: 0 0 8px; font-size: 30px; letter-spacing: 4px; color: #f5efdd; position: relative; z-index: 1; }
.hero-text p { margin: 0; color: rgba(239, 232, 214, 0.72); font-size: 13.5px; position: relative; z-index: 1; }
.hero-stats { display: flex; gap: 28px; margin-top: 22px; flex-wrap: wrap; }
.stat strong { display: block; font-size: 22px; color: #e8c06a; position: relative; z-index: 1; }
.stat span { font-size: 12px; color: rgba(239, 232, 214, 0.55); position: relative; z-index: 1; }
.hero-disc { color: rgba(232, 192, 106, 0.3); animation: spin 20s linear infinite; flex-shrink: 0; position: relative; z-index: 1; }
@keyframes spin { to { transform: rotate(360deg); } }

.block { margin-bottom: 34px; }
.style-filter { display: flex; gap: 8px; flex-wrap: wrap; }

.singer-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}
.singer-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 12px 14px;
  color: var(--text);
  transition: all 0.18s;
}
.singer-chip:hover { border-color: var(--primary); transform: translateY(-2px); }
.singer-chip img { width: 44px; height: 44px; border-radius: 50%; background: var(--bg-soft); }
.sc-name { font-size: 14px; font-weight: 600; }
.sc-style { font-size: 12px; color: var(--text-3); }

@media (max-width: 700px) {
  .hero-disc { display: none; }
  .hero { padding: 24px; }
}
</style>
