<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { poetryApi } from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const poems = ref([])
const facets = ref(null)
const total = ref(0)
const page = ref(Number(route.query.page) || 1)
const dynasty = ref(route.query.dynasty || '')
const mood = ref(route.query.mood || '')
const keyword = ref(route.query.keyword || '')
const SIZE = 48

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / SIZE)))

async function load() {
  loading.value = true
  try {
    const { data } = await poetryApi.poemPage({
      dynasty: dynasty.value || undefined,
      mood: mood.value || undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      size: SIZE
    })
    poems.value = data.list
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

/** 筛选与页码写回 URL：刷新、分享、后退都成立 */
function syncQuery() {
  const q = {}
  if (dynasty.value) q.dynasty = dynasty.value
  if (mood.value) q.mood = mood.value
  if (keyword.value) q.keyword = keyword.value
  if (page.value > 1) q.page = String(page.value)
  router.replace({ query: q })
}

function setFilter(key, val) {
  if (key === 'dynasty') dynasty.value = dynasty.value === val ? '' : val
  if (key === 'mood') mood.value = mood.value === val ? '' : val
  page.value = 1
  syncQuery()
  load()
}
function pickDynasty(d) { setFilter('dynasty', d) }
function pickMood(m) { setFilter('mood', m) }

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  syncQuery()
  load()
  window.scrollTo({ top: 0 })
}

/** 页码窗口：当前页前后各一页，首末页恒在 */
const pageWindow = computed(() => {
  const t = totalPages.value, c = page.value
  const out = [...new Set([1, t, c - 1, c, c + 1].filter((x) => x >= 1 && x <= t))].sort((a, b) => a - b)
  const res = []
  out.forEach((v, i) => {
    if (i && v - out[i - 1] > 1) res.push('…')
    res.push(v)
  })
  return res
})

const firstLine = (p) => (p.paragraphs || '').split('\n')[0] || ''
const fullTitle = (p) => p.title + (p.subtitle ? '·' + p.subtitle : '')
/** 名句与首句重复时不再展示第二遍 */
const famousShown = (p) => p.famousLine && p.famousLine !== firstLine(p)

onMounted(async () => {
  const [f] = await Promise.allSettled([poetryApi.facets()])
  if (f.status === 'fulfilled') facets.value = f.value.data
  load()
})
watch([dynasty, mood], () => { page.value = 1; syncQuery(); load() })
watch(keyword, () => {
  clearTimeout(keyword._t)
  keyword._t = setTimeout(() => { page.value = 1; syncQuery(); load() }, 350)
})
watch(() => route.query, (q) => {
  const np = Number(q.page) || 1
  const nd = q.dynasty || ''
  const nm = q.mood || ''
  if (np === page.value && nd === dynasty.value && nm === mood.value) return
  page.value = np
  dynasty.value = nd
  mood.value = nm
  load()
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">诗选</h1>
        <p class="page-sub">共 {{ total.toLocaleString() }} 首 · 循朝代与心境</p>
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
    <template v-else>
      <div class="poem-grid">
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
          <p class="p-famous" v-if="famousShown(p)">{{ p.famousLine }}</p>
        </router-link>
      </div>

      <div class="pager" v-if="totalPages > 1" data-test="poem-pager">
        <button class="pg" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <template v-for="(it, i) in pageWindow" :key="i">
          <span v-if="it === '…'" class="pg-dots">…</span>
          <button v-else class="pg" :class="{ active: it === page }" @click="goPage(it)">{{ it }}</button>
        </template>
        <button class="pg" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
        <span class="pg-info">第 {{ page }} / {{ totalPages.toLocaleString() }} 页</span>
      </div>
    </template>
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

/* 分页 */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 30px 0 8px;
  flex-wrap: wrap;
}
.pg {
  min-width: 34px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--card);
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
}
.pg:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.pg.active { background: var(--primary); border-color: var(--primary); color: #fbf6ea; }
.pg:disabled { opacity: 0.4; cursor: not-allowed; }
.pg-dots { color: var(--text-3); padding: 0 2px; }
.pg-info { margin-left: 10px; font-size: 12px; color: var(--text-3); }
</style>
