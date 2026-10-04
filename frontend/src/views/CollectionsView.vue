<script setup>
import { ref, onMounted } from 'vue'
import { collectionApi } from '../api'

const loading = ref(true)
const list = ref([])

onMounted(async () => {
  try {
    const { data } = await collectionApi.list()
    list.value = data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">主题选集</h1>
        <p class="page-sub">以意相聚，以境相从——策展式组诗</p>
      </div>
    </div>

    <div v-if="loading" class="spinner" />
    <div v-else class="col-grid">
      <router-link v-for="c in list" :key="c.id" :to="`/collection/${c.id}`" class="col-card">
        <div class="cc-seal font-brush">{{ c.title.slice(0, 1) }}</div>
        <div class="cc-body">
          <h3>{{ c.title }}</h3>
          <p class="cc-sub">{{ c.subtitle }}</p>
          <p class="cc-intro">{{ c.intro }}</p>
          <p class="cc-count">{{ c.poemCount }} 首</p>
        </div>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.col-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.col-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text);
  transition: all 0.18s;
}
.col-card:hover { border-color: var(--primary); transform: translateY(-3px); box-shadow: 0 8px 22px rgba(64, 99, 77, 0.16); }
.cc-seal {
  width: 46px; height: 46px; flex-shrink: 0;
  border-radius: 8px;
  background: var(--seal); color: #fbf6ea;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px;
}
.cc-body { min-width: 0; }
.cc-body h3 { margin: 0 0 4px; font-size: 17px; letter-spacing: 1px; }
.cc-sub { margin: 0 0 8px; font-size: 12.5px; color: var(--accent); }
.cc-intro { margin: 0 0 8px; font-size: 13px; line-height: 1.9; color: var(--text-2); }
.cc-count { margin: 0; font-size: 12px; color: var(--text-3); }
</style>
