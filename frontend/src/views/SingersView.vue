<script setup>
import { ref, onMounted } from 'vue'
import { singerApi } from '../api'
import Icon from '../components/Icon.vue'

const loading = ref(true)
const singers = ref([])
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const { data } = await singerApi.list(keyword.value.trim() ? { keyword: keyword.value.trim() } : {})
    singers.value = data
  } finally {
    loading.value = false
  }
}

const GENDER = ['男歌手', '女歌手', '乐团/组合']

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">歌手</h1>
        <p class="page-sub" v-if="!loading">共 {{ singers.length }} 位歌手</p>
      </div>
      <form class="toolbar" @submit.prevent="load">
        <input v-model="keyword" class="input" style="width: 220px" placeholder="搜索歌手名 / 风格 / 地区" />
        <button class="btn btn-primary" type="submit"><Icon name="search" :size="15" /> 搜索</button>
      </form>
    </div>

    <div v-if="loading" class="spinner" />
    <div v-else-if="singers.length" class="grid-cards">
      <router-link v-for="s in singers" :key="s.id" :to="`/singer/${s.id}`" class="singer-card">
        <img :src="`https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(s.name)}&backgroundColor=b6e3f4,c0aede,d1d4f9`" :alt="s.name" loading="lazy" />
        <div class="sc-body">
          <div class="sc-name">{{ s.name }}</div>
          <div class="sc-meta">{{ GENDER[s.gender] || '歌手' }} · {{ s.style }}</div>
          <div class="sc-intro">{{ s.intro }}</div>
          <div class="sc-count"><Icon name="music" :size="13" /> {{ s.songCount }} 首作品</div>
        </div>
      </router-link>
    </div>
    <div v-else class="empty"><div class="empty-icon">🎤</div>没有找到匹配的歌手</div>
  </div>
</template>

<style scoped>
.toolbar { display: flex; gap: 8px; }
.singer-card {
  display: flex;
  gap: 14px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  color: var(--text);
  transition: all 0.18s;
}
.singer-card:hover { border-color: var(--primary); transform: translateY(-2px); }
.singer-card img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--bg-soft);
  flex-shrink: 0;
}
.sc-body { min-width: 0; }
.sc-name { font-size: 15px; font-weight: 700; }
.sc-meta { font-size: 12px; color: var(--primary-2); margin: 3px 0 6px; }
.sc-intro {
  font-size: 12px;
  color: var(--text-3);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.sc-count {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  font-size: 11.5px;
  color: var(--text-2);
  background: var(--grad-soft);
  padding: 2px 8px;
  border-radius: 999px;
}
</style>
