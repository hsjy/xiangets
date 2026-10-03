<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { poetryApi } from '../api'
import UserAvatar from '../components/UserAvatar.vue'
import JourneyMap from '../components/JourneyMap.vue'
import { usePlayerStore } from '../stores/player'

const route = useRoute()
const player = usePlayerStore()
const loading = ref(true)
const poet = ref(null)
const poems = ref([])

/** 行旅数据是 JSON 字符串，解析失败则视作无行旅图 */
const journey = computed(() => {
  try {
    const j = JSON.parse(poet.value?.journey || 'null')
    return Array.isArray(j) && j.length ? j : null
  } catch {
    return null
  }
})

function playPoem(p) {
  // 意境页负责音轨；诗人页提供快捷朗诵入口：取该诗第一条朗诵轨
  import('../api').then(async ({ songApi }) => {
    try {
      const { data: tracks } = await songApi.list({ poemId: p.id })
      const recite = tracks.find((t) => t.trackKind === 'recitation') || tracks[0]
      if (!recite) return
      await player.playSong({ ...recite, name: `《${p.title}》朗诵`, singerName: poet.value.name })
    } catch { /* 静默 */ }
  })
}

async function load(id) {
  loading.value = true
  try {
    const { data } = await poetryApi.poetDetail(id)
    poet.value = data.poet
    poems.value = data.poems
  } finally {
    loading.value = false
  }
}

onMounted(() => load(route.params.id))
// 同文档导航（如诗人页之间跳转）不会重建组件，须监听参数重取
watch(() => route.params.id, (id) => { if (id && route.path.startsWith('/poet/')) load(id) })
</script>

<template>
  <div class="page">
    <div v-if="loading" class="spinner" />
    <template v-else-if="poet">
      <!-- 诗人卷首 -->
      <section class="poet-hero">
        <UserAvatar :name="poet.name" :size="120" circle />
        <div class="hero-info">
          <h1 class="font-brush">{{ poet.name }}<span class="honor" v-if="poet.title">· {{ poet.title }}</span></h1>
          <p class="sub">
            {{ poet.dynasty }}
            <template v-if="poet.years"> · {{ poet.years }}</template>
            <template v-if="poet.zi"> · 字{{ poet.zi }}</template>
            <template v-if="poet.hao"> · 号{{ poet.hao }}</template>
          </p>
          <p class="intro">{{ poet.intro }}</p>
        </div>
      </section>

      <!-- 诗人故事 -->
      <section class="story card" v-if="poet.story">
        <h2 class="story-title">
          <span class="stamp">{{ poet.name.slice(-1) }}</span>
          {{ poet.storyTitle || '诗人故事' }}
        </h2>
        <div class="story-body">
          <p v-for="(para, i) in poet.story.split('\n')" :key="i">{{ para }}</p>
        </div>
      </section>

      <!-- 人生行旅图 -->
      <section class="journey card" v-if="journey">
        <h2 class="story-title">
          <span class="stamp">{{ poet.name.slice(0, 1) }}</span>
          人生行旅图
        </h2>
        <JourneyMap :journey="journey" :poems="poems" :dynasty="poet.dynasty" />
      </section>

      <!-- 存诗 -->
      <section class="works">
        <h2 class="works-head">存诗（{{ poems.length }}）</h2>
        <div class="poem-grid">
          <router-link v-for="p in poems" :key="p.id" :to="`/poem/${p.id}`" class="poem-card">
            <div class="p-head">
              <span class="tag" v-if="p.mood">{{ p.mood }}</span>
              <span class="p-genre">{{ p.genre }}</span>
            </div>
            <h3>《{{ p.title }}{{ p.subtitle ? '·' + p.subtitle : '' }}》</h3>
            <p class="p-line">{{ p.paragraphs.split('\n')[0] }}</p>
            <p class="p-famous" v-if="p.famousLine">{{ p.famousLine }}</p>
            <button class="mini-play" title="听朗诵" @click.prevent="playPoem(p)">
              ▶ 朗诵
            </button>
          </router-link>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.poet-hero {
  display: flex;
  gap: 26px;
  align-items: center;
  padding: 30px;
  border-radius: 14px;
  border: 1px solid var(--border-strong);
  background:
    radial-gradient(600px 260px at 90% 0%, rgba(184, 134, 11, 0.12), transparent),
    var(--card);
  margin-bottom: 26px;
}
.hero-info h1 { margin: 0 0 6px; font-size: 30px; letter-spacing: 4px; }
.honor { font-size: 16px; color: var(--accent); letter-spacing: 2px; }
.sub { margin: 0 0 10px; font-size: 13px; color: var(--text-3); }
.intro { margin: 0; font-size: 14px; line-height: 1.9; color: var(--text-2); max-width: 680px; }

.story {
  margin-bottom: 30px;
  background: linear-gradient(180deg, var(--card), var(--card-hover));
}
.story-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 16px;
  font-size: 19px;
}
.stamp {
  width: 34px; height: 34px;
  border-radius: 5px;
  background: var(--seal);
  color: #fbf6ea;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', serif;
  font-size: 22px;
}
.story-body p {
  margin: 0 0 14px;
  font-size: 14.5px;
  line-height: 2.1;
  color: var(--text);
  text-indent: 2em;
}
.story-body p:last-child { margin-bottom: 0; }

.journey { margin-bottom: 30px; }

.works-head { margin: 0 0 14px; font-size: 18px; }
.poem-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 14px;
}
.poem-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  color: var(--text);
  transition: all 0.18s;
  position: relative;
}
.poem-card:hover { border-color: var(--primary); transform: translateY(-2px); }
.p-head { display: flex; justify-content: space-between; margin-bottom: 8px; }
.p-genre { font-size: 11.5px; color: var(--text-3); }
.poem-card h3 { margin: 0 0 8px; font-size: 15.5px; }
.p-line { margin: 0 0 8px; font-size: 13px; color: var(--text-2); line-height: 1.8; }
.p-famous { margin: 0 0 10px; font-size: 12px; color: var(--accent); }
.mini-play {
  border: 1px solid var(--border-strong);
  background: none;
  color: var(--text-2);
  border-radius: 999px;
  padding: 3px 12px;
  font-size: 12px;
  cursor: pointer;
}
.mini-play:hover { color: var(--primary); border-color: var(--primary); }
</style>
