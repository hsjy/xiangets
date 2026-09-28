<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { songApi } from '../api'
import SongTable from '../components/SongTable.vue'

const route = useRoute()
const loading = ref(true)
const allSongs = ref([])
const keyword = ref(String(route.query.keyword || ''))
const style = ref('')
const sort = ref('pop') // pop 热度 | time 最新 | name 歌名
const page = ref(1)
const pageSize = 10

const STYLES = ['流行', '摇滚', '民谣', '电子', '古风', 'R&B']

async function load() {
  loading.value = true
  try {
    const { data } = await songApi.list()
    allSongs.value = data
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  let list = allSongs.value
  const kw = keyword.value.trim()
  if (kw) list = list.filter((s) => s.name.includes(kw) || s.singerName.includes(kw))
  if (style.value) list = list.filter((s) => s.style === style.value)
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))
const pageSongs = computed(() => {
  const start = (page.value - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

// 分页数字（最多显示 7 个，省略号省略）
const pageList = computed(() => {
  const t = totalPages.value, c = page.value
  if (t <= 7) return Array.from({ length: t }, (_, i) => i + 1)
  if (c <= 4) return [1, 2, 3, 4, 5, '…', t]
  if (c >= t - 3) return [1, '…', t - 4, t - 3, t - 2, t - 1, t]
  return [1, '…', c - 1, c, c + 1, '…', t]
})

function goPage(p) {
  if (p === '…' || p < 1 || p > totalPages.value) return
  page.value = p
}

watch([keyword, style, sort], () => { page.value = 1 })
watch(() => route.query.keyword, (v) => { keyword.value = String(v || '') })

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">歌曲库</h1>
        <p class="page-sub" v-if="!loading">共 {{ filtered.length }} 首歌曲</p>
      </div>
      <div class="toolbar">
        <div class="search-mini">
          <input v-model="keyword" class="input" placeholder="在结果中筛选…" data-test="song-filter" />
        </div>
        <select v-model="style" class="select" style="width: 110px" data-test="song-style">
          <option value="">全部风格</option>
          <option v-for="st in STYLES" :key="st" :value="st">{{ st }}</option>
        </select>
        <select v-model="sort" class="select" style="width: 110px" data-test="song-sort">
          <option value="pop">按热度</option>
          <option value="time">按最新</option>
          <option value="name">按歌名</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="spinner" />
    <template v-else>
      <SongTable :songs="pageSongs" empty-text="没有匹配的歌曲，换个关键词试试" />
      <div class="pagination" v-if="totalPages > 1" data-test="pagination">
        <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">‹</button>
        <button
          v-for="(p, i) in pageList" :key="i"
          class="page-btn" :class="{ active: p === page }"
          :disabled="p === '…'" @click="goPage(p)"
        >{{ p }}</button>
        <button class="page-btn" :disabled="page >= totalPages" @click="goPage(page + 1)">›</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.toolbar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.search-mini .input { width: 200px; border-radius: 999px; }
</style>
