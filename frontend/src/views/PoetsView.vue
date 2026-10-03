<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { poetryApi } from '../api'
import UserAvatar from '../components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const poets = ref([])
const facets = ref(null)
const total = ref(0)
const page = ref(Number(route.query.page) || 1)
const dynasty = ref(route.query.dynasty || '')
const keyword = ref(route.query.keyword || '')
const SIZE = 48

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / SIZE)))

async function load() {
  loading.value = true
  try {
    const { data } = await poetryApi.poetPage({
      dynasty: dynasty.value || undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      size: SIZE
    })
    poets.value = data.list
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function syncQuery() {
  const q = {}
  if (dynasty.value) q.dynasty = dynasty.value
  if (keyword.value) q.keyword = keyword.value
  if (page.value > 1) q.page = String(page.value)
  router.replace({ query: q })
}

function pickDynasty(d) {
  dynasty.value = dynasty.value === d ? '' : d
  page.value = 1
  syncQuery()
  load()
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  syncQuery()
  load()
  window.scrollTo({ top: 0 })
}

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

/** 卡片称号显示收敛：字/号过长时只取第一个 */
const shortHao = (h) => (h || '').split('、')[0]

onMounted(async () => {
  const [f] = await Promise.allSettled([poetryApi.facets()])
  if (f.status === 'fulfilled') facets.value = f.value.data
  load()
})
watch(dynasty, () => { page.value = 1; syncQuery(); load() })
watch(keyword, () => {
  clearTimeout(keyword._t)
  keyword._t = setTimeout(() => { page.value = 1; syncQuery(); load() }, 350)
})
watch(() => route.query, (q) => {
  const np = Number(q.page) || 1
  const nd = q.dynasty || ''
  if (np === page.value && nd === dynasty.value) return
  page.value = np
  dynasty.value = nd
  load()
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">诗人</h1>
        <p class="page-sub">共 {{ total.toLocaleString() }} 位 · 观其人，方懂其诗</p>
      </div>
      <input
        v-model.trim="keyword" class="input search" placeholder="搜诗人姓名、字号、称号…"
        data-test="poet-search"
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
    </div>

    <div v-if="loading" class="spinner" />
    <template v-else>
      <div class="poet-grid">
        <router-link v-for="p in poets" :key="p.id" :to="`/poet/${p.id}`" class="poet-card">
          <div class="pc-top">
            <UserAvatar :name="p.name" :size="56" circle />
            <div class="pc-names">
              <h3>{{ p.name }}<span class="zi" v-if="p.zi">（{{ p.zi }}{{ shortHao(p.hao) ? '，号' + shortHao(p.hao) : '' }}）</span></h3>
              <p class="pc-sub">{{ p.dynasty }}<template v-if="p.years"> · {{ p.years }}</template></p>
            </div>
            <span class="pc-title tag" v-if="p.title">{{ p.title }}</span>
          </div>
          <p class="pc-intro" v-if="p.intro">{{ p.intro }}</p>
          <p class="pc-count"><template v-if="p.poemCount">存诗 {{ p.poemCount.toLocaleString() }} 首</template></p>
        </router-link>
      </div>

      <div class="pager" v-if="totalPages > 1" data-test="poet-pager">
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
.filter-row { display: flex; gap: 10px; margin-bottom: 22px; flex-wrap: wrap; }
.filter-group { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.f-label { font-size: 12.5px; color: var(--text-3); width: 34px; }

.poet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.poet-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 20px;
  color: var(--text);
  transition: all 0.18s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.poet-card:hover {
  border-color: var(--primary);
  transform: translateY(-3px);
  box-shadow: 0 8px 22px rgba(64, 99, 77, 0.16);
}
.pc-top { display: flex; align-items: center; gap: 14px; }
.pc-names { flex: 1; min-width: 0; }
.pc-names h3 { margin: 0; font-size: 17px; }
.zi { font-size: 12px; color: var(--text-3); font-weight: 400; }
.pc-sub { margin: 4px 0 0; font-size: 12px; color: var(--text-3); }
.pc-intro {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-2);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pc-count { margin: 0; font-size: 12px; color: var(--accent); }

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
