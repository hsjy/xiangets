<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { collectionApi, songApi } from '../api'
import { usePlayerStore } from '../stores/player'
import { toast } from '../utils/toast'

const route = useRoute()
const player = usePlayerStore()
const loading = ref(true)
const col = ref(null)
const poems = ref([])
const playingAll = ref(false)

const fullTitle = (p) => p.title + (p.subtitle ? '·' + p.subtitle : '')
const firstLine = (p) => (p.paragraphs || '').split('\n')[0] || ''

async function load(id) {
  loading.value = true
  try {
    const { data } = await collectionApi.detail(id)
    col.value = data.collection
    poems.value = data.poems
  } finally {
    loading.value = false
  }
}

/** 选集连播：按策展顺序取每首的朗诵轨，组成队列 */
async function playAll() {
  if (playingAll.value) return
  playingAll.value = true
  try {
    const tracks = []
    for (const p of poems.value) {
      const { data } = await songApi.list({ poemId: p.id })
      const rec = data.find((t) => t.trackKind === 'recitation') || data[0]
      if (rec) tracks.push({ ...rec, name: rec.name || `《${fullTitle(p)}》朗诵`, singerName: p.poetName })
    }
    if (!tracks.length) {
      toast.error('本选集暂无朗诵音轨')
      return
    }
    // playSong 带上下文队列：上一首/下一首即按选集顺序
    await player.playSong(tracks[0], tracks)
    toast.success(`开始连播《${col.value.title}》${tracks.length} 首`)
  } finally {
    playingAll.value = false
  }
}

onMounted(() => load(route.params.id))
watch(() => route.params.id, (id) => { if (id) load(id) })
</script>

<template>
  <div class="page">
    <div v-if="loading" class="spinner" />
    <template v-else-if="col">
      <section class="col-hero">
        <div class="ch-seal font-brush">{{ col.title.slice(0, 1) }}</div>
        <div class="ch-body">
          <h1 class="font-brush">{{ col.title }}</h1>
          <p class="ch-sub">{{ col.subtitle }}</p>
          <p class="ch-intro">{{ col.intro }}</p>
        </div>
        <button class="btn btn-primary play-all" :disabled="playingAll" @click="playAll" data-test="collection-play">
          {{ playingAll ? '准备中…' : '▶ 连播此集' }}
        </button>
      </section>

      <div class="poem-grid">
        <router-link v-for="p in poems" :key="p.id" :to="`/poem/${p.id}`" class="poem-card">
          <div class="p-head">
            <span class="p-idx">{{ poems.indexOf(p) + 1 }}</span>
            <span class="p-dynasty">{{ p.dynasty }}</span>
          </div>
          <h3 class="p-title">《{{ fullTitle(p) }}》</h3>
          <p class="p-author">{{ p.poetName }}</p>
          <p class="p-line">{{ firstLine(p) }}</p>
          <p class="p-famous" v-if="p.famousLine">{{ p.famousLine }}</p>
        </router-link>
      </div>
    </template>
    <div v-else class="empty"><div class="empty-icon">📜</div>选集不存在</div>
  </div>
</template>

<style scoped>
.col-hero {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 26px 28px;
  border-radius: 14px;
  border: 1px solid var(--border-strong);
  background:
    radial-gradient(600px 240px at 92% 0%, rgba(184, 134, 11, 0.12), transparent),
    var(--card);
  margin-bottom: 26px;
}
.ch-seal {
  width: 62px; height: 62px; flex-shrink: 0;
  border-radius: 10px;
  background: var(--seal); color: #fbf6ea;
  display: flex; align-items: center; justify-content: center;
  font-size: 34px;
}
.ch-body { flex: 1; min-width: 0; }
.ch-body h1 { margin: 0 0 4px; font-size: 26px; letter-spacing: 3px; }
.ch-sub { margin: 0 0 8px; font-size: 13px; color: var(--accent); letter-spacing: 1px; }
.ch-intro { margin: 0; font-size: 13.5px; line-height: 1.9; color: var(--text-2); max-width: 640px; }
.play-all { flex-shrink: 0; }

.poem-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.poem-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 18px;
  color: var(--text);
  transition: all 0.18s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.poem-card:hover { border-color: var(--primary); transform: translateY(-3px); box-shadow: 0 8px 22px rgba(64, 99, 77, 0.16); }
.p-head { display: flex; justify-content: space-between; align-items: center; }
.p-idx { font-size: 12px; color: var(--seal); font-weight: 700; }
.p-dynasty { font-size: 11.5px; color: var(--text-3); }
.p-title { margin: 0; font-size: 16.5px; }
.p-author { margin: 0; font-size: 12.5px; color: var(--text-2); }
.p-line { margin: 0; font-size: 13.5px; color: var(--text-2); line-height: 1.8; }
.p-famous { margin: 4px 0 0; font-size: 12px; color: var(--accent); border-top: 1px dashed var(--border); padding-top: 8px; }
</style>
