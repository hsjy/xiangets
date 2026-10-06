<script setup>
import { ref, computed, onMounted } from 'vue'
import { poetryApi, songApi, collectionApi } from '../api'
import { usePlayerStore } from '../stores/player'
import Icon from '../components/Icon.vue'
import UserAvatar from '../components/UserAvatar.vue'

const player = usePlayerStore()
const loading = ref(true)
const heroPoem = ref(null)   // 此刻：随机一诗，古图依其情绪而生
const today = ref(null)      // 今日一诗：按日期轮换
const facets = ref(null)
const poets = ref([])
const showcase = ref([])     // [{poem, aiTrack}]
const collections = ref([])  // 主题选集
const poetTotal = ref(0)     // 诗人总数（语料库全量）
const todayTrack = ref(null) // 今日诗的朗诵轨

/* ---- 依情绪的场景调色盘：天空 / 山色 / 云雾 / 日月 ---- */
const SCENES = {
  '月夜':  { sky: ['#dfe3d2', '#b7c2b2'], mount: ['#3c4a44', '#435550', '#4a5c52'], mist: 0.85, moon: true,  birds: 2, label: '月夜' },
  '思乡':  { sky: ['#f0e4c8', '#d9c49a'], mount: ['#4a4640', '#54504a', '#5c5850'], mist: 0.7,  moon: true,  birds: 1, label: '暮色' },
  '送别':  { sky: ['#f2e3c4', '#d8bf94'], mount: ['#4f4a40', '#585247', '#615a4d'], mist: 0.8,  moon: false, birds: 1, label: '暮别' },
  '怀人':  { sky: ['#ece7d6', '#cdc9b4'], mount: ['#41453e', '#4a4f46', '#53584e'], mist: 0.9,  moon: false, birds: 2, label: '烟雨' },
  '田园':  { sky: ['#f6f0dc', '#e7dcb8'], mount: ['#43573f', '#4c6147', '#566c50'], mist: 0.6,  moon: false, birds: 2, label: '晨晓' },
  '山水':  { sky: ['#eef0e0', '#d3dcc4'], mount: ['#3f5248', '#475c50', '#506658'], mist: 0.7,  moon: false, birds: 2, label: '青绿' },
  '闲适':  { sky: ['#f6f0dc', '#e9e0c4'], mount: ['#46583f', '#4f6249', '#586c52'], mist: 0.55, moon: false, birds: 2, label: '春晓' },
  '家国':  { sky: ['#e4e0d0', '#c2c2b2'], mount: ['#3e423c', '#464a43', '#4e524a'], mist: 0.75, moon: false, birds: 1, label: '苍茫' },
  '悲秋':  { sky: ['#efe6d2', '#d6c8ac'], mount: ['#4a463c', '#534e42', '#5c564a'], mist: 0.8,  moon: false, birds: 1, label: '秋暮' },
  '豪放':  { sky: ['#e6e4d0', '#c0c4b0'], mount: ['#36403a', '#3d4842', '#44504a'], mist: 0.6,  moon: false, birds: 2, label: '长风' },
  '壮志':  { sky: ['#e2e2d2', '#bec4b2'], mount: ['#333e38', '#3a463f', '#424e47'], mist: 0.65, moon: false, birds: 2, label: '金戈' },
  '哲理':  { sky: ['#eff0e2', '#d0d6c4'], mount: ['#414b44', '#49544b', '#525d53'], mist: 0.7,  moon: false, birds: 1, label: '山霭' },
}
const DEFAULT_SCENE = SCENES['月夜']

/* ---- 过程化山脊：每次刷新 / 换一首，山形都不重样 ---- */
function makeRidge(rand, baseY, amp, seg = 6) {
  let d = `M0 320 L0 ${(baseY + rand() * amp * 0.4).toFixed(0)}`
  const w = 1440 / seg
  for (let i = 0; i < seg; i++) {
    const x1 = (i * w + w * (0.3 + rand() * 0.4)).toFixed(0)
    const y1 = (baseY - rand() * amp).toFixed(0)
    const x2 = ((i + 1) * w).toFixed(0)
    const y2 = (baseY - rand() * amp * 0.55).toFixed(0)
    d += ` Q${x1} ${y1} ${x2} ${y2}`
  }
  d += ' L1440 320 Z'
  return d
}

const scene = computed(() => SCENES[heroPoem.value?.mood] || DEFAULT_SCENE)
const ridges = ref([])
const birds = ref([])

function rollScene() {
  const r = Math.random
  ridges.value = [
    makeRidge(r, 205, 130),
    makeRidge(r, 248, 95),
    makeRidge(r, 285, 62),
  ]
  birds.value = Array.from({ length: scene.value.birds }, () => ({
    top: 18 + Math.round(r() * 22),
    delay: -Math.round(r() * 40),
    dur: 30 + Math.round(r() * 24),
    scale: 0.7 + r() * 0.6,
  }))
}

async function rollPoem() {
  const { data } = await poetryApi.random()
  heroPoem.value = data
  rollScene()
}

onMounted(async () => {
  try {
    const [h, t, f, g] = await Promise.allSettled([
      poetryApi.random(),
      poetryApi.today(),
      poetryApi.facets(),
      poetryApi.poetPage({ page: 1, size: 14 })
    ])
    if (h.status === 'fulfilled') { heroPoem.value = h.value.data; rollScene() }
    if (t.status === 'fulfilled') today.value = t.value.data
    if (f.status === 'fulfilled') facets.value = f.value.data
    if (g.status === 'fulfilled') poets.value = g.value.data.list

    /* 主题选集 + 诗人总数 */
    collectionApi.list().then(({ data }) => { collections.value = data }).catch(() => {})
    poetryApi.poetPage({ page: 1, size: 1 }).then(({ data }) => { poetTotal.value = Number(data.total) }).catch(() => {})

    /* 今日诗朗诵轨（一键收听） */
    if (t.status === 'fulfilled' && t.value.data?.poem) {
      songApi.list({ poemId: t.value.data.poem.id }).then(({ data: tracks }) => {
        todayTrack.value = tracks.find((x) => x.trackKind === 'recitation') || null
      }).catch(() => {})
    }

    /* AI 谱曲 showcase：取三首已有 AI 轨的诗 */
    const demo = [18, 8, 3]
    const got = await Promise.allSettled(
      demo.map((id) => Promise.all([poetryApi.poemDetail(id), songApi.list({ poemId: id })]))
    )
    showcase.value = got
      .filter((g2) => g2.status === 'fulfilled')
      .map((g2) => {
        const [{ data: poem }, { data: tracks }] = g2.value
        return { poem, ai: tracks.find((x) => x.trackKind === 'ai') }
      })
      .filter((x) => x.ai)
  } finally {
    loading.value = false
  }
})

function playTrack(t, poem) {
  player.playSong({ ...t, singerName: poem.poetName })
}
function playToday() {
  if (todayTrack.value && today.value?.poem) playTrack(todayTrack.value, today.value.poem)
}
const firstLine = (p) => (p.paragraphs || '').split('\n')[0] || ''
const fullTitle = (p) => p.title + (p.subtitle ? '·' + p.subtitle : '')
</script>

<template>
  <div class="home">
    <!-- ============ 动态古图 hero：依「此刻」之诗而生 ============ -->
    <section class="hero" data-test="hero">
      <div class="sky" :style="{ background: `linear-gradient(180deg, ${scene.sky[0]} 0%, ${scene.sky[1]} 62%, ${scene.mount[2]}33 100%)` }"></div>
      <div v-if="scene.moon" class="moon" :style="{ top: heroPoem?.mood === '月夜' ? '13%' : '20%' }"></div>

      <!-- 三层过程化山脊，视差漂移 -->
      <svg class="mount m1" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path :d="ridges[0]" :fill="scene.mount[0]" opacity="0.16"/>
      </svg>
      <svg class="mount m2" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path :d="ridges[1]" :fill="scene.mount[1]" opacity="0.26"/>
      </svg>
      <svg class="mount m3" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path :d="ridges[2]" :fill="scene.mount[2]" opacity="0.42"/>
      </svg>

      <!-- 云雾 -->
      <div class="mist w1" :style="{ opacity: scene.mist }"></div>
      <div class="mist w2" :style="{ opacity: scene.mist * 0.8, animationDelay: '-14s' }"></div>

      <!-- 飞鸟 -->
      <svg
        v-for="(b, i) in birds" :key="i"
        class="birds" :class="'b' + i"
        :style="{ top: b.top + '%', animationDuration: b.dur + 's', animationDelay: b.delay + 's' }"
        viewBox="0 0 120 40"
      >
        <path d="M4 26 Q14 14 24 26 Q34 14 44 26" fill="none" stroke="#3a3630" stroke-width="2.4" stroke-linecap="round" opacity="0.55"/>
        <path d="M58 18 Q66 8 74 18 Q82 8 90 18" fill="none" stroke="#3a3630" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
      </svg>

      <!-- 竖排题字 -->
      <div class="hero-word">
        <p class="hw-line font-brush">诗为经</p>
        <p class="hw-line dim font-brush">乐为纬</p>
      </div>

      <div class="hero-title">
        <p class="ht-sub">弦歌 · 诗乐雅集 · 此刻〔{{ scene.label }}〕</p>
        <h1 class="ht-main font-brush" data-test="hero-line">{{ heroPoem?.famousLine || heroPoem?.title || '弦歌不辍' }}</h1>
        <p class="ht-poem" v-if="heroPoem">
          <router-link :to="`/poem/${heroPoem.id}`">《{{ fullTitle(heroPoem) }}》</router-link>
          · {{ heroPoem.poetName }} · {{ heroPoem.dynasty }}
        </p>
        <button class="reroll" data-test="hero-reroll" @click="rollPoem">
          <Icon name="compass" :size="14" /> 换一首 · 换一景
        </button>
      </div>

      <div class="reeds">
        <svg viewBox="0 0 1440 160" preserveAspectRatio="none">
          <path d="M120 160 Q124 96 116 60 M132 160 Q140 110 138 78 M300 160 Q292 100 302 66 M980 160 Q988 104 980 70 M1330 160 Q1322 108 1332 74 M1350 160 Q1358 118 1352 90"
            fill="none" stroke="#333f38" stroke-width="3" stroke-linecap="round" opacity="0.5"/>
        </svg>
      </div>
      <div class="scroll-hint">
        <span>向下展卷</span>
        <Icon name="chevron-down" :size="16" />
      </div>
    </section>

    <!-- ============ 卷下内容（滚动渐现） ============ -->
    <!-- 数据卷轴 -->
    <section v-reveal class="sec stats-strip" v-if="facets?.poemCount">
      <div class="stat"><span class="st-num">{{ Number(facets.poemCount).toLocaleString() }}</span><span class="st-label">首诗</span></div>
      <span class="st-dot">·</span>
      <div class="stat"><span class="st-num">{{ poetTotal.toLocaleString() }}</span><span class="st-label">位诗人</span></div>
      <span class="st-dot">·</span>
      <div class="stat"><span class="st-num">{{ facets.dynasties?.length || 0 }}</span><span class="st-label">个朝代</span></div>
    </section>

    <!-- 今日一诗 -->
    <section v-reveal class="sec" v-if="today?.poem">
      <div class="today-card">
        <div class="tc-left">
          <p class="tc-date">{{ today.date }} · 今日一诗 · 与「此刻」不同，今日只此一首</p>
          <h2 class="tc-famous font-brush">「{{ today.poem.famousLine || today.poem.title }}」</h2>
          <p class="tc-meta">
            <router-link :to="`/poem/${today.poem.id}`">《{{ fullTitle(today.poem) }}》</router-link>
            · {{ today.poem.poetName }} · {{ today.poem.dynasty }}
          </p>
          <p class="tc-line">{{ firstLine(today.poem) }}</p>
          <div class="tc-actions">
            <router-link :to="`/poem/${today.poem.id}`" class="btn btn-primary">入其意境 →</router-link>
            <button v-if="todayTrack" class="btn btn-ghost" @click="playToday" data-test="today-recite">
              <Icon name="play" :size="14" /> 听今日朗诵
            </button>
          </div>
        </div>
        <div class="tc-seal font-brush">{{ today.poem.mood?.slice(0, 1) || '诗' }}</div>
      </div>
    </section>

    <!-- 游于艺：功能入口 -->
    <section v-reveal class="sec">
      <h2 class="sec-title">游于艺</h2>
      <p class="sec-sub">诗之外，还有这些玩法</p>
      <div class="fun-grid">
        <router-link to="/feihua" class="fun-card">
          <span class="fu-seal font-brush">令</span>
          <div><h3>飞花令</h3><p>择一字为令，与三十四万首诗轮流对句</p></div>
        </router-link>
        <router-link :to="`/poem/${today?.poem?.id || ''}`" class="fun-card" v-if="today?.poem">
          <span class="fu-seal font-brush">问</span>
          <div><h3>问诗</h3><p>DeepSeek 先生伴读，就今日之诗有问必答</p></div>
        </router-link>
        <router-link to="/collections" class="fun-card">
          <span class="fu-seal font-brush">集</span>
          <div><h3>主题选集</h3><p>策展式组诗，边塞明月、田园归隐各成一番</p></div>
        </router-link>
        <router-link to="/games" class="fun-card">
          <span class="fu-seal font-brush">游</span>
          <div><h3>诗游</h3><p>拼句、交游、地理——三款古诗小游戏</p></div>
        </router-link>
        <router-link :to="`/poem/${today?.poem?.id || ''}`" class="fun-card" v-if="today?.poem">
          <span class="fu-seal font-brush">笺</span>
          <div><h3>诗笺</h3><p>竖排笺纸配朱砂印，一首诗制成一张可存的笺</p></div>
        </router-link>
      </div>
    </section>

    <!-- 主题选集精选 -->
    <section v-reveal class="sec" v-if="collections.length">
      <h2 class="sec-title">主题选集</h2>
      <p class="sec-sub">以意相聚，以境相从</p>
      <div class="col-strip">
        <router-link v-for="c in collections" :key="c.id" :to="`/collection/${c.id}`" class="cs-card">
          <span class="cs-count">{{ c.poemCount }} 首</span>
          <h3>{{ c.title }}</h3>
          <p class="cs-sub">{{ c.subtitle }}</p>
        </router-link>
      </div>
    </section>

    <!-- 情绪入口 -->
    <section v-reveal class="sec">
      <h2 class="sec-title">以心入境</h2>
      <p class="sec-sub">循情绪寻诗</p>
      <div class="chips">
        <router-link v-for="m in facets?.moods || []" :key="m"
          :to="`/poems?mood=${encodeURIComponent(m)}`" class="chip">{{ m }}</router-link>
      </div>
      <h2 class="sec-title" style="margin-top: 34px">以时观诗</h2>
      <p class="sec-sub">循朝代寻诗</p>
      <div class="chips">
        <router-link v-for="d in facets?.dynasties || []" :key="d"
          :to="`/poems?dynasty=${encodeURIComponent(d)}`" class="chip dynasty">{{ d }}</router-link>
      </div>
    </section>

    <!-- 诗人印廊 -->
    <section v-reveal class="sec">
      <h2 class="sec-title">诗人印廊</h2>
      <p class="sec-sub">观其人，方懂其诗</p>
      <div class="strip">
        <router-link v-for="p in poets" :key="p.id" :to="`/poet/${p.id}`" class="strip-item">
          <UserAvatar :name="p.name" :size="64" circle />
          <span class="s-name">{{ p.name }}</span>
          <span class="s-title">{{ p.title || p.dynasty }}</span>
        </router-link>
      </div>
    </section>

    <!-- AI 谱曲 showcase -->
    <section v-reveal class="sec ai-sec">
      <h2 class="sec-title">AI 谱曲 · 为诗而作</h2>
      <p class="sec-sub">依诗之意象，即席成曲——乐为佐诗之茶</p>
      <div class="ai-grid">
        <div v-for="x in showcase" :key="x.poem.id" class="ai-card">
          <div class="ai-head">
            <span class="ai-badge">AI</span>
            <span class="ai-mood">{{ x.poem.mood }}</span>
          </div>
          <h3 class="font-brush">《{{ fullTitle(x.poem) }}》</h3>
          <p class="ai-poet">{{ x.poem.poetName }} · {{ x.poem.dynasty }}</p>
          <p class="ai-prompt">{{ x.ai.name }}</p>
          <button class="btn btn-primary btn-sm" @click="playTrack(x.ai, x.poem)">
            <Icon name="play" :size="14" /> 听此曲
          </button>
        </div>
      </div>
      <p class="ai-note">
        MusicGen 依「情绪 → 意象提示词」生成器乐曲 · 配乐朗诵由 TTS 呈现 · 配置 GPU 推理服务可实时生成
      </p>
    </section>

    <!-- 诗选预览 -->
    <section v-reveal class="sec last">
      <h2 class="sec-title">诗选</h2>
      <p class="sec-sub">以时观诗，以心入境</p>
      <div class="go-grid">
        <router-link to="/poems" class="go-card">
          <h3>诗选 <Icon name="chevron-right" :size="16" /></h3>
          <p>{{ Number(facets?.poemCount || 0).toLocaleString() }} 首诗 · 朝代与心境 · 注译赏俱全</p>
        </router-link>
        <router-link to="/poets" class="go-card">
          <h3>诗人 <Icon name="chevron-right" :size="16" /></h3>
          <p>{{ poetTotal.toLocaleString() }} 位诗人 · 生平故事 · 行旅图 · 交游图谱</p>
        </router-link>
        <router-link to="/study" class="go-card">
          <h3>书斋 <Icon name="chevron-right" :size="16" /></h3>
          <p>收藏的诗 · 你写下的感悟</p>
        </router-link>
      </div>
      <footer class="home-foot">
        弦歌 · 诗乐雅集 —— Vue3 + SpringBoot + MySQL · 音乐为佐诗之茶
      </footer>
    </section>
  </div>
</template>

<style scoped>
/* ============ 动态古图 hero ============ */
.hero {
  position: relative;
  height: 100vh;
  min-height: 560px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sky { position: absolute; inset: 0; }
.moon {
  position: absolute;
  right: 18%;
  width: 84px; height: 84px;
  border-radius: 50%;
  background: radial-gradient(circle at 38% 34%, #fef9ec, #e8d894 78%);
  box-shadow: 0 0 0 1px rgba(184, 134, 11, 0.25), 0 0 70px 30px rgba(232, 192, 106, 0.45);
  animation: moonrise 2.4s ease both;
}
@keyframes moonrise { from { transform: translateY(26px); opacity: 0; } to { transform: none; opacity: 1; } }

.mount { position: absolute; left: -6%; right: -6%; bottom: -4%; width: 112%; height: 46%; }
.m1 { animation: drift 90s ease-in-out infinite alternate; }
.m2 { animation: drift 60s ease-in-out infinite alternate-reverse; }
.m3 { animation: drift 44s ease-in-out infinite alternate; bottom: -8%; height: 38%; }
@keyframes drift { from { transform: translateX(-2.4%); } to { transform: translateX(2.4%); } }

.mist {
  position: absolute;
  height: 120px;
  border-radius: 50%;
  filter: blur(42px);
  background: rgba(250, 246, 234, 0.75);
  animation: mistdrift 38s ease-in-out infinite alternate;
}
.w1 { width: 46%; left: 4%; bottom: 22%; }
.w2 { width: 38%; right: 2%; bottom: 30%; }
@keyframes mistdrift { from { transform: translateX(-5%); } to { transform: translateX(7%); } }

.birds { position: absolute; left: -12%; width: 110px; opacity: 0.9; animation: fly linear infinite; }
@keyframes fly { from { transform: translateX(0); } to { transform: translateX(130vw); } }

.reeds { position: absolute; left: 0; right: 0; bottom: -2px; height: 130px; }
.reeds svg { width: 100%; height: 100%; transform-origin: bottom; animation: sway 7s ease-in-out infinite alternate; }
@keyframes sway { from { transform: rotate(-1.1deg); } to { transform: rotate(1.1deg); } }

/* 竖排题字 */
.hero-word {
  position: absolute;
  right: 13%;
  top: 22%;
  display: flex;
  gap: 16px;
  animation: moonrise 2s 0.3s ease both;
  text-shadow: 0 0 18px rgba(250, 246, 234, 0.9), 0 0 4px rgba(250, 246, 234, 0.7);
}
.hw-line {
  writing-mode: vertical-rl;
  text-orientation: upright;
  margin: 0;
  font-size: 34px;
  letter-spacing: 15px;
  color: #2f2a20;
  opacity: 0.92;
}
.hw-line.dim { font-size: 24px; opacity: 0.7; margin-top: 44px; }

.hero-title { position: relative; text-align: center; padding: 0 20px; max-width: 900px; }
.ht-sub { margin: 0 0 14px; letter-spacing: 6px; font-size: 14px; color: rgba(58, 54, 48, 0.62); }
.ht-main { margin: 0 0 14px; font-size: clamp(30px, 5.4vw, 52px); color: #2f2a20; letter-spacing: 6px; text-shadow: 0 0 22px rgba(250, 246, 234, 0.75); }
.ht-poem { margin: 0 0 18px; font-size: 15px; }
.ht-poem a { color: var(--primary-2); }
.reroll {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 20px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  background: rgba(251, 246, 234, 0.75);
  color: var(--text-2);
  font-size: 13.5px;
  letter-spacing: 2px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.reroll:hover { color: var(--primary); border-color: var(--primary); transform: translateY(-2px); box-shadow: 0 6px 18px rgba(64, 99, 77, 0.18); }

.scroll-hint {
  position: absolute;
  bottom: 26px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  letter-spacing: 4px;
  color: rgba(58, 54, 48, 0.5);
  animation: bob 2.2s ease-in-out infinite;
}
@keyframes bob { 50% { transform: translate(-50%, 7px); } }

/* ============ 卷下内容 ============ */
.sec {
  max-width: 1080px;
  margin: 0 auto;
  padding: 74px 28px 8px;
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.8s ease, transform 0.8s ease;
}
.sec.revealed { opacity: 1; transform: none; }
.sec-title { margin: 0; font-size: 26px; letter-spacing: 3px; }
.sec-sub { margin: 6px 0 22px; font-size: 13px; color: var(--text-3); letter-spacing: 2px; }
.last { padding-bottom: 40px; }

.today-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 30px;
  background: linear-gradient(135deg, rgba(64, 99, 77, 0.1), rgba(58, 54, 48, 0.9)), #2e4034;
  color: #efe8d6;
  border-radius: 16px;
  padding: 40px 44px;
  overflow: hidden;
}
.tc-left { flex: 1; position: relative; }
.tc-date { margin: 0 0 12px; font-size: 12px; letter-spacing: 3px; color: rgba(239, 232, 214, 0.55); }
.tc-famous { margin: 0 0 14px; font-size: clamp(24px, 3.4vw, 34px); color: #f2ecd9; }
.tc-meta, .tc-meta a { color: rgba(239, 232, 214, 0.8); font-size: 13.5px; }
.tc-line { margin: 12px 0 20px; font-size: 14px; color: rgba(239, 232, 214, 0.65); }
.tc-seal {
  width: 88px; height: 88px;
  border-radius: 10px;
  background: rgba(232, 192, 106, 0.16);
  border: 1.5px solid rgba(232, 192, 106, 0.45);
  color: #e8c06a;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  writing-mode: vertical-rl;
  letter-spacing: 4px;
  flex-shrink: 0;
}

.chips { display: flex; gap: 10px; flex-wrap: wrap; }
.chip {
  padding: 9px 20px;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  background: var(--card);
  color: var(--text-2);
  font-size: 14px;
  letter-spacing: 1px;
  transition: all 0.2s;
}
.chip:hover { border-color: var(--primary); color: var(--primary); transform: translateY(-2px); box-shadow: 0 6px 16px rgba(64, 99, 77, 0.16); }

.strip { display: flex; gap: 24px; overflow-x: auto; padding-bottom: 10px; }
.strip-item { display: flex; flex-direction: column; align-items: center; gap: 8px; min-width: 80px; color: var(--text); }
.s-name { font-size: 14px; }
.s-title { font-size: 11.5px; color: var(--text-3); }

.ai-sec {
  background: linear-gradient(180deg, transparent, rgba(64, 99, 77, 0.05) 30%, transparent);
  border-radius: 16px;
}
.ai-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; }
.ai-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px;
  transition: all 0.2s;
}
.ai-card:hover { border-color: var(--accent); transform: translateY(-3px); box-shadow: 0 10px 26px rgba(88, 70, 40, 0.14); }
.ai-head { display: flex; gap: 8px; align-items: center; margin-bottom: 10px; }
.ai-badge {
  background: var(--accent); color: #fbf6ea;
  font-size: 10px; font-weight: 700;
  padding: 2px 7px; border-radius: 4px;
  letter-spacing: 1px;
}
.ai-mood { font-size: 12px; color: var(--text-3); }
.ai-card h3 { margin: 0 0 6px; font-size: 18px; }
.ai-poet { margin: 0 0 10px; font-size: 12.5px; color: var(--text-2); }
.ai-prompt {
  margin: 0 0 14px;
  font-size: 11.5px;
  color: var(--text-3);
  font-style: italic;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ai-note { margin: 18px 0 0; font-size: 12px; color: var(--text-3); text-align: center; }

.go-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
.go-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
  color: var(--text);
  transition: all 0.2s;
}
.go-card:hover { border-color: var(--primary); transform: translateY(-3px); }
.go-card h3 { margin: 0 0 8px; font-size: 17px; display: flex; align-items: center; gap: 4px; }
.go-card p { margin: 0; font-size: 13px; color: var(--text-2); line-height: 1.7; }
.home-foot { margin-top: 46px; text-align: center; font-size: 12px; color: var(--text-3); letter-spacing: 2px; padding-bottom: 120px; }

@media (max-width: 860px) {
  .hero-word { display: none; }
  .today-card { flex-direction: column; align-items: flex-start; padding: 28px; }
  .tc-seal { display: none; }
}

/* ============ 数据卷轴 ============ */
.stats-strip {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 18px;
  padding: 6px 0 2px;
}
.stat { display: flex; align-items: baseline; gap: 8px; }
.st-num { font-size: 26px; font-weight: 700; color: var(--primary-2); font-variant-numeric: tabular-nums; }
.st-label { font-size: 13px; color: var(--text-3); letter-spacing: 2px; }
.st-dot { color: var(--text-3); }

/* ============ 今日一诗快捷操作 ============ */
.tc-actions { display: flex; gap: 10px; margin-top: 4px; }

/* ============ 游于艺 ============ */
.fun-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; }
.fun-card {
  display: flex; gap: 14px; align-items: flex-start;
  padding: 18px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text);
  transition: all 0.18s;
}
.fun-card:hover { border-color: var(--primary); transform: translateY(-3px); box-shadow: 0 8px 22px rgba(64, 99, 77, 0.16); }
.fu-seal {
  width: 40px; height: 40px; flex-shrink: 0;
  border-radius: 7px;
  background: var(--seal); color: #fbf6ea;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 22px;
}
.fun-card h3 { margin: 0 0 4px; font-size: 15.5px; }
.fun-card p { margin: 0; font-size: 12.5px; line-height: 1.8; color: var(--text-2); }

/* ============ 主题选集精选 ============ */
.col-strip { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; }
.cs-card {
  padding: 16px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  color: var(--text);
  transition: all 0.18s;
  position: relative;
}
.cs-card:hover { border-color: var(--primary); transform: translateY(-3px); }
.cs-count {
  position: absolute; top: 12px; right: 12px;
  font-size: 11px; color: var(--text-3);
}
.cs-card h3 { margin: 0 0 6px; font-size: 15.5px; letter-spacing: 1px; }
.cs-sub { margin: 0; font-size: 12px; color: var(--text-2); line-height: 1.7; }
</style>
