<script setup>
import { ref, onMounted } from 'vue'
import { poetryApi } from '../api'
import { usePlayerStore } from '../stores/player'
import Icon from '../components/Icon.vue'
import UserAvatar from '../components/UserAvatar.vue'

const player = usePlayerStore()
const loading = ref(true)
const today = ref(null)   // { poem, date, index, total }
const facets = ref(null)
const poets = ref([])

onMounted(async () => {
  try {
    const [t, f, g] = await Promise.allSettled([
      poetryApi.today(),
      poetryApi.facets(),
      poetryApi.poetList()
    ])
    if (t.status === 'fulfilled') today.value = t.value.data
    if (f.status === 'fulfilled') facets.value = f.value.data
    if (g.status === 'fulfilled') poets.value = g.value.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div v-if="loading" class="spinner" />
    <template v-else-if="today?.poem">
      <!-- 今日一诗长卷 -->
      <section class="today-hero">
        <div class="hero-inner">
          <p class="date-line">{{ today.date }} · 今日一诗 · 第 {{ today.index + 1 }} / {{ today.total }} 首</p>
          <h1 class="famous-line font-brush">「{{ today.poem.famousLine || today.poem.title }}」</h1>
          <div class="meta">
            <router-link :to="`/poem/${today.poem.id}`" class="poem-title-link">
              《{{ today.poem.title }}{{ today.poem.subtitle ? '·' + today.poem.subtitle : '' }}》
            </router-link>
            <span class="dot">·</span>
            <router-link :to="`/poet/${today.poem.poetId}`">{{ today.poem.poetName }}</router-link>
            <span class="dot">·</span>
            <span>{{ today.poem.dynasty }}</span>
            <span v-if="today.poem.mood" class="tag mood">{{ today.poem.mood }}</span>
          </div>
          <p class="first-lines">
            {{ today.poem.paragraphs.split('\n').slice(0, 2).join('  ') }}
          </p>
          <router-link :to="`/poem/${today.poem.id}`" class="btn btn-primary enter-btn">
            <Icon name="compass" :size="16" /> 入其意境
          </router-link>
        </div>
      </section>

      <!-- 情绪与朝代入口 -->
      <section class="entries">
        <div class="block-head">
          <h2>以心入境</h2>
          <span class="sub">循情绪寻诗</span>
        </div>
        <div class="chips">
          <router-link
            v-for="m in facets?.moods || []" :key="m"
            :to="`/poems?mood=${encodeURIComponent(m)}`" class="chip"
          >{{ m }}</router-link>
        </div>
        <div class="block-head">
          <h2>以时观诗</h2>
          <span class="sub">循朝代寻诗</span>
        </div>
        <div class="chips">
          <router-link
            v-for="d in facets?.dynasties || []" :key="d"
            :to="`/poems?dynasty=${encodeURIComponent(d)}`" class="chip dynasty"
          >{{ d }}</router-link>
        </div>
      </section>

      <!-- 诗人印廊 -->
      <section class="poet-strip">
        <div class="block-head">
          <h2>诗人印廊</h2>
          <router-link to="/poets" class="more">全部诗人 →</router-link>
        </div>
        <div class="strip">
          <router-link v-for="p in poets" :key="p.id" :to="`/poet/${p.id}`" class="strip-item">
            <UserAvatar :name="p.name" :size="58" circle />
            <span class="s-name">{{ p.name }}</span>
            <span class="s-title">{{ p.title || p.dynasty }}</span>
          </router-link>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.today-hero {
  position: relative;
  border-radius: 14px;
  border: 1px solid var(--border-strong);
  padding: 54px 48px;
  margin-bottom: 34px;
  overflow: hidden;
  background:
    radial-gradient(700px 340px at 88% 12%, rgba(184, 134, 11, 0.14), transparent),
    linear-gradient(135deg, rgba(64, 99, 77, 0.55), rgba(34, 50, 40, 0.94)),
    #2e4034;
  color: #efe8d6;
}
.today-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 320' preserveAspectRatio='none'%3E%3Cpath d='M0 320 L0 200 Q150 130 320 176 T660 140 T1000 186 T1440 128 L1440 320 Z' fill='%23efe8d6' opacity='0.07'/%3E%3Cpath d='M0 320 L0 248 Q220 176 440 222 T880 200 T1440 232 L1440 320 Z' fill='%23efe8d6' opacity='0.1'/%3E%3C/svg%3E");
  background-size: cover;
  pointer-events: none;
}
.hero-inner { position: relative; z-index: 1; }
.date-line { margin: 0 0 18px; font-size: 12.5px; letter-spacing: 3px; color: rgba(239, 232, 214, 0.6); }
.famous-line {
  margin: 0 0 20px;
  font-size: 34px;
  line-height: 1.5;
  color: #f2ecd9;
  letter-spacing: 2px;
}
.meta { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; font-size: 14px; }
.meta a { color: #e8c06a; }
.meta .dot { color: rgba(239, 232, 214, 0.4); }
.tag.mood {
  background: rgba(232, 192, 106, 0.14);
  border-color: rgba(232, 192, 106, 0.4);
  color: #e8c06a;
  cursor: default;
}
.first-lines {
  margin: 18px 0 26px;
  font-size: 15px;
  line-height: 2;
  color: rgba(239, 232, 214, 0.82);
  max-width: 640px;
}
.enter-btn { background: var(--seal); }
.enter-btn:hover:not(:disabled) { background: var(--seal); filter: brightness(1.1); }

.entries { margin-bottom: 34px; }
.block-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; }
.block-head h2 { margin: 0; font-size: 18px; }
.block-head .sub { font-size: 12.5px; color: var(--text-3); }
.block-head .more { font-size: 13px; margin-left: auto; }
.chips { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 24px; }
.chip {
  padding: 8px 18px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  background: var(--card);
  color: var(--text-2);
  font-size: 14px;
  letter-spacing: 1px;
  transition: all 0.18s;
}
.chip:hover {
  border-color: var(--primary);
  color: var(--primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 99, 77, 0.18);
}

.poet-strip .strip { display: flex; gap: 22px; overflow-x: auto; padding-bottom: 8px; }
.strip-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 76px;
  color: var(--text);
}
.s-name { font-size: 14px; }
.s-title { font-size: 11.5px; color: var(--text-3); }
</style>
