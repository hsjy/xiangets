<script setup>
import { ref, onMounted } from 'vue'
import { poetryApi, songApi } from '../api'
import { usePlayerStore } from '../stores/player'
import Icon from '../components/Icon.vue'
import UserAvatar from '../components/UserAvatar.vue'

const player = usePlayerStore()
const loading = ref(true)
const today = ref(null)
const facets = ref(null)
const poets = ref([])
const showcase = ref([])   // [{poem, aiTrack}]
const heroQuote = ref('')
onMounted(load)

async function load() {
  try {
    const [t, f, g] = await Promise.allSettled([
      poetryApi.today(),
      poetryApi.facets(),
      poetryApi.poetList()
    ])
    if (t.status === 'fulfilled') {
      today.value = t.value.data
      heroQuote.value = today.value?.poem?.famousLine || '弦歌不辍'
    }
    if (f.status === 'fulfilled') facets.value = f.value.data
    if (g.status === 'fulfilled') poets.value = g.value.data

    /* AI 谱曲 showcase：取三首已有 AI 轨的诗 */
    const demo = [18, 8, 24]
    const got = await Promise.allSettled(
      demo.map((id) =>
        Promise.all([poetryApi.poemDetail(id), songApi.list({ poemId: id })])
      )
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
}

function playTrack(t, poem) {
  player.playSong({ ...t, singerName: poem.poetName })
}
const firstLine = (p) => (p.paragraphs || '').split('\n')[0] || ''
const fullTitle = (p) => p.title + (p.subtitle ? '·' + p.subtitle : '')
</script>

<template>
  <div class="home">
    <!-- ============ 动态古图 hero ============ -->
    <section class="hero" data-test="hero">
      <div class="sky"></div>
      <div class="moon"></div>

      <!-- 三层远山，视差漂移 -->
      <svg class="mount m1" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path d="M0 320 L0 200 Q160 110 340 168 T700 128 T1060 176 T1440 112 L1440 320 Z" fill="#3f4a42" opacity="0.16"/>
      </svg>
      <svg class="mount m2" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path d="M0 320 L0 244 Q220 156 460 214 T900 184 T1440 226 L1440 320 Z" fill="#3f4a42" opacity="0.24"/>
      </svg>
      <svg class="mount m3" viewBox="0 0 1440 320" preserveAspectRatio="none">
        <path d="M0 320 L0 282 Q260 210 520 254 T1040 240 T1440 266 L1440 320 Z" fill="#333f38" opacity="0.4"/>
      </svg>

      <!-- 云雾 -->
      <div class="mist w1"></div>
      <div class="mist w2"></div>

      <!-- 飞鸟 -->
      <svg class="birds b1" viewBox="0 0 120 40">
        <path d="M4 26 Q14 14 24 26 Q34 14 44 26" fill="none" stroke="#3a3630" stroke-width="2.4" stroke-linecap="round" opacity="0.55"/>
        <path d="M58 18 Q66 8 74 18 Q82 8 90 18" fill="none" stroke="#3a3630" stroke-width="2" stroke-linecap="round" opacity="0.4"/>
      </svg>
      <svg class="birds b2" viewBox="0 0 120 40">
        <path d="M10 22 Q20 12 30 22 Q40 12 50 22" fill="none" stroke="#3a3630" stroke-width="2" stroke-linecap="round" opacity="0.35"/>
      </svg>

      <!-- 竖排题字 -->
      <div class="hero-word">
        <p class="hw-line font-brush">诗为经</p>
        <p class="hw-line dim font-brush">乐为纬</p>
      </div>
      <div class="hero-title">
        <p class="ht-sub">弦歌 · 诗乐雅集</p>
        <h1 class="ht-main font-brush">{{ heroQuote || '弦歌不辍' }}</h1>
        <p class="ht-hint">一首诗，一阙乐，一卷古人的心事</p>
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
    <!-- 今日一诗 -->
    <section v-reveal class="sec" v-if="today?.poem">
      <div class="today-card">
        <div class="tc-left">
          <p class="tc-date">{{ today.date }} · 今日一诗</p>
          <h2 class="tc-famous font-brush">「{{ today.poem.famousLine || today.poem.title }}」</h2>
          <p class="tc-meta">
            <router-link :to="`/poem/${today.poem.id}`">《{{ fullTitle(today.poem) }}》</router-link>
            · {{ today.poem.poetName }} · {{ today.poem.dynasty }}
          </p>
          <p class="tc-line">{{ firstLine(today.poem) }}</p>
          <router-link :to="`/poem/${today.poem.id}`" class="btn btn-primary">入其意境 →</router-link>
        </div>
        <div class="tc-seal font-brush">{{ today.poem.mood?.slice(0, 1) || '诗' }}</div>
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
          <p>{{ facets?.poemCount || 24 }} 首名篇 · 朝代与心境 · 注译赏俱全</p>
        </router-link>
        <router-link to="/poets" class="go-card">
          <h3>诗人 <Icon name="chevron-right" :size="16" /></h3>
          <p>{{ poets.length }} 位诗人 · 生平故事 · 存诗目录</p>
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
.sky {
  position: absolute; inset: 0;
  background:
    radial-gradient(900px 480px at 78% 18%, rgba(232, 192, 106, 0.28), transparent 60%),
    linear-gradient(180deg, #f4eeda 0%, #ece5cd 58%, #e3d9bd 100%);
}
.moon {
  position: absolute;
  top: 14%; right: 18%;
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
.w2 { width: 38%; right: 2%; bottom: 30%; animation-delay: -14s; }
@keyframes mistdrift { from { transform: translateX(-5%); opacity: 0.7; } to { transform: translateX(7%); opacity: 1; } }

.birds { position: absolute; width: 110px; opacity: 0.9; }
.b1 { top: 24%; left: -12%; animation: fly 34s linear infinite; }
.b2 { top: 33%; left: -18%; animation: fly 46s linear infinite; animation-delay: -12s; }
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

.hero-title { position: relative; text-align: center; padding: 0 20px; }
.ht-sub { margin: 0 0 14px; letter-spacing: 6px; font-size: 14px; color: rgba(58, 54, 48, 0.6); }
.ht-main { margin: 0 0 16px; font-size: clamp(30px, 5.4vw, 52px); color: #2f2a20; letter-spacing: 6px; }
.ht-hint { margin: 0; font-size: 14px; color: rgba(58, 54, 48, 0.55); letter-spacing: 2px; }

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
</style>
