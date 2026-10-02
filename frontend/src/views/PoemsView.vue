<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { poetryApi } from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const poems = ref([])
const facets = ref(null)
const dynasty = ref(route.query.dynasty || '')
const mood = ref(route.query.mood || '')
const keyword = ref(route.query.keyword || '')

async function load() {
  loading.value = true
  try {
    const { data } = await poetryApi.poemList({
      dynasty: dynasty.value || undefined,
      mood: mood.value || undefined,
      keyword: keyword.value || undefined
    })
    poems.value = data
  } finally {
    loading.value = false
  }
}

function setFilter(key, val) {
  if (key === 'dynasty') dynasty.value = dynasty.value === val ? '' : val
  if (key === 'mood') mood.value = mood.value === val ? '' : val
}
function pickDynasty(d) { setFilter('dynasty', d) }
function pickMood(m) { setFilter('mood', m) }

const firstLine = (p) => (p.paragraphs || '').split('\n')[0] || ''
const fullTitle = (p) => p.title + (p.subtitle ? '·' + p.subtitle : '')

onMounted(async () => {
  const [f] = await Promise.allSettled([poetryApi.facets()])
  if (f.status === 'fulfilled') facets.value = f.value.data
  load()
})
watch([dynasty, mood], load)
watch(keyword, () => {
  clearTimeout(keyword._t)
  keyword._t = setTimeout(load, 350)
})
watch(() => route.query, (q) => {
  dynasty.value = q.dynasty || ''
  mood.value = q.mood || ''
  load()
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">诗选</h1>
        <p class="page-sub">共 {{ poems.length }} 首 · 循朝代与心境</p>
      </div>
      <input
        v-model.trim="keyword" class="input search" placeholder="搜诗题、诗句、诗人…"
        data-test="poem-search"
      />
    </div>

    <div class="filter-row" v-if="facets">
      <div class="filter-group">
        <span class="f-label">朝代</span>
        <button
          v-for="d in facets.dynasties" :key="d"
          class="tag" :class="{ active: dynasty === d }"
          @click="pickDynasty(d)"
        >{{ d }}</button>
      </div>
      <div class="filter-group">
        <span class="f-label">心境</span>
        <button
          v-for="m in facets.moods" :key="m"
          class="tag" :class="{ active: mood === m }"
          @click="pickMood(m)"
        >{{ m }}</button>
      </div>
    </div>

    <div v-if="loading" class="spinner" />
    <div v-else-if="!poems.length" class="empty">
      <div class="empty-icon">🎞️</div>
      未寻得此诗——换一组心境试试
    </div>
    <div v-else class="poem-grid">
      <router-link
        v-for="p in poems" :key="p.id"
        :to="`/poem/${p.id}`" class="poem-card"
        :data-test="'poem-card-' + p.id"
      >
        <div class="p-head">
          <span class="p-mood tag" v-if="p.mood">{{ p.mood }}</span>
          <span class="p-dynasty">{{ p.dynasty }}{{ p.genre ? ' · ' + p.genre : '' }}</span>
        </div>
        <h3 class="p-title">《{{ fullTitle(p) }}》</h3>
        <p class="p-author">{{ p.poetName }}</p>
        <p class="p-line">{{ firstLine(p) }}</p>
        <p class="p-famous" v-if="p.famousLine">{{ p.famousLine }}</p>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.search { width: 260px; }
.filter-row { display: flex; flex-direction: column; gap: 10px; margin-bottom: 22px; }
.filter-group { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.f-label { font-size: 12.5px; color: var(--text-3); width: 34px; }

.poem-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
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
  position: relative;
  overflow: hidden;
}
.poem-card::before {
  content: '';
  position: absolute;
  top: 0; right: 0;
  width: 46px; height: 46px;
  background: linear-gradient(225deg, rgba(184, 134, 11, 0.12), transparent 55%);
}
.poem-card:hover {
  border-color: var(--primary);
  transform: translateY(-3px);
  box-shadow: 0 8px 22px rgba(64, 99, 77, 0.16);
}
.p-head { display: flex; justify-content: space-between; align-items: center; }
.p-dynasty { font-size: 11.5px; color: var(--text-3); }
.p-title { margin: 0; font-size: 16.5px; }
.p-author { margin: 0; font-size: 12.5px; color: var(--text-2); }
.p-line { margin: 0; font-size: 13.5px; color: var(--text-2); line-height: 1.8; }
.p-famous {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--accent);
  border-top: 1px dashed var(--border);
  padding-top: 8px;
}
</style>
