<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { singerApi, songApi } from '../api'
import { usePlayerStore } from '../stores/player'
import { openAddToPlaylist } from '../stores/ui'
import SongTable from '../components/SongTable.vue'
import Icon from '../components/Icon.vue'

const route = useRoute()
const player = usePlayerStore()
const loading = ref(true)
const singer = ref(null)
const songs = ref([])

const GENDER = ['男歌手', '女歌手', '乐团/组合']

onMounted(async () => {
  loading.value = true
  try {
    const [a, b] = await Promise.all([
      singerApi.detail(route.params.id),
      songApi.list({ singerId: route.params.id, sort: 'pop' })
    ])
    singer.value = a.data
    songs.value = b.data
  } finally {
    loading.value = false
  }
})

function playAll() {
  if (songs.value.length) player.playSong(songs.value[0], songs.value)
}
</script>

<template>
  <div class="page">
    <div v-if="loading" class="spinner" />
    <template v-else-if="singer">
      <section class="hero">
        <img class="hero-avatar" :src="`https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(singer.name)}&backgroundColor=b6e3f4,c0aede,d1d4f9&size=160`" :alt="singer.name" />
        <div class="hero-info">
          <h1>{{ singer.name }}</h1>
          <p class="tags">
            <span class="tag" style="cursor: default">{{ GENDER[singer.gender] || '歌手' }}</span>
            <span class="tag" style="cursor: default">{{ singer.style }}</span>
            <span v-if="singer.location" class="tag" style="cursor: default">{{ singer.location }}</span>
            <span v-if="singer.birth" class="tag" style="cursor: default">{{ singer.birth }}</span>
          </p>
          <p class="intro">{{ singer.intro }}</p>
          <div class="acts">
            <button class="btn btn-primary" @click="playAll" data-test="play-all">
              <Icon name="play" :size="15" /> 播放全部
            </button>
            <button class="btn btn-ghost" @click="songs[0] && openAddToPlaylist(songs[0])" :disabled="!songs.length">
              <Icon name="plus" :size="15" /> 收藏歌曲
            </button>
          </div>
        </div>
      </section>

      <section class="block">
        <h2 class="page-title" style="margin-bottom: 14px">全部作品（{{ songs.length }}）</h2>
        <SongTable :songs="songs" />
      </section>
    </template>
    <div v-else class="empty"><div class="empty-icon">🫥</div>歌手不存在</div>
  </div>
</template>

<style scoped>
.hero {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 26px;
  margin-bottom: 26px;
}
.hero-avatar {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  background: var(--bg-soft);
  border: 3px solid rgba(139, 92, 246, 0.4);
  flex-shrink: 0;
}
.hero-info h1 { margin: 4px 0 10px; font-size: 24px; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; margin: 0 0 12px; }
.intro { color: var(--text-2); font-size: 13.5px; line-height: 1.8; margin: 0 0 16px; max-width: 640px; }
.acts { display: flex; gap: 10px; }
.block { margin-bottom: 24px; }
</style>
