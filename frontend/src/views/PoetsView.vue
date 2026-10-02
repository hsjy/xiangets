<script setup>
import { ref, onMounted } from 'vue'
import { poetryApi } from '../api'
import UserAvatar from '../components/UserAvatar.vue'

const loading = ref(true)
const poets = ref([])

onMounted(async () => {
  try {
    const { data } = await poetryApi.poetList()
    poets.value = data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">诗人</h1>
        <p class="page-sub">共 {{ poets.length }} 位 · 观其人，方懂其诗</p>
      </div>
    </div>

    <div v-if="loading" class="spinner" />
    <div v-else class="poet-grid">
      <router-link v-for="p in poets" :key="p.id" :to="`/poet/${p.id}`" class="poet-card">
        <div class="pc-top">
          <UserAvatar :name="p.name" :size="56" circle />
          <div class="pc-names">
            <h3>{{ p.name }}<span class="zi" v-if="p.zi">（{{ p.zi }}{{ p.hao ? '，号' + p.hao : '' }}）</span></h3>
            <p class="pc-sub">{{ p.dynasty }}<template v-if="p.years"> · {{ p.years }}</template></p>
          </div>
          <span class="pc-title tag" v-if="p.title">{{ p.title }}</span>
        </div>
        <p class="pc-intro">{{ p.intro }}</p>
        <p class="pc-count"><template v-if="p.poemCount">存诗 {{ p.poemCount }} 首</template></p>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
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
</style>
