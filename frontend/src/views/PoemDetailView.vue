<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { poetryApi, poemFavApi, poemCommentApi, songApi, aiMusicApi } from '../api'
import { usePlayerStore } from '../stores/player'
import { useUserStore } from '../stores/user'
import { confirmAction } from '../stores/ui'
import { toast } from '../utils/toast'
import UserAvatar from '../components/UserAvatar.vue'
import AskDialog from '../components/AskDialog.vue'
import ReciteMode from '../components/ReciteMode.vue'
import { downloadPoemCard } from '../utils/poemCard'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const userStore = useUserStore()

const loading = ref(true)
const poem = ref(null)
const tracks = ref([])
const comments = ref([])
const favorited = ref(false)
const commentText = ref('')
const submitting = ref(false)
const similar = ref([])
const activeTrackId = ref(null)
const aiJob = ref(null)          // 谱曲任务：{id, status, audioUrl...}
const askOpen = ref(false)       // 问诗对话
const reciteOpen = ref(false)    // 背诵模式
const polling = ref(false)

const lines = computed(() => (poem.value?.paragraphs || '').split('\n').filter(Boolean))
/**
 * 竖排短列：按句读（，。？！；、）把诗行切成短句，每列一句。
 * 词的长短句若整行一列，参差会显得凌乱；切成句读单元后
 * 列与列之间等距、列内居中，呈现词牌固有的错落之美。
 */
const phrases = computed(() => {
  const out = []
  for (const line of lines.value) {
    const parts = (line.match(/[^，。？！；、]+[，。？！；、]?/g) || [])
      .map((x) => x.trim())
      .filter(Boolean)
    out.push(...parts)
  }
  return out
})
const fullTitle = computed(() => poem.value
  ? poem.value.title + (poem.value.subtitle ? '·' + poem.value.subtitle : '')
  : '')

const aiTracks = computed(() => tracks.value.filter((t) => t.trackKind === 'ai'))
const recitations = computed(() => tracks.value.filter((t) => t.trackKind === 'recitation'))
const bgms = computed(() => tracks.value.filter((t) => t.trackKind === 'bgm'))

/**
 * 注：原文是一条连写的长串，按「句号 + 短词 + 冒号」切成独立条目，
 * 例如「渭城：……。浥：……。」切成两条。词条本身以句读收尾，
 * 故只在句号后紧跟「词：」时才断开，不会误伤释义中的句读。
 */
const zhuanItems = computed(() => {
  const raw = (poem.value?.annotation || '').trim()
  if (!raw) return []
  return raw
    .split(/(?<=。)(?=[^。：]{1,10}：)/)
    .map((s) => s.trim().replace(/[。；;]$/, ''))
    .filter(Boolean)
})

async function load() {
  loading.value = true
  try {
    const { data } = await poetryApi.poemDetail(route.params.id)
    poem.value = data
    const [t, c, f, s, ai] = await Promise.allSettled([
      songApi.list({ poemId: data.id }),
      poemCommentApi.list(data.id),
      poemFavApi.state(data.id),
      data.mood ? poetryApi.poemList({ mood: data.mood }) : Promise.resolve({ data: [] }),
      aiMusicApi.list(data.id)
    ])
    if (t.status === 'fulfilled') tracks.value = t.value.data
    if (c.status === 'fulfilled') comments.value = c.value.data
    if (f.status === 'fulfilled') favorited.value = f.value.data.favorited
    if (s.status === 'fulfilled') {
      similar.value = s.value.data.filter((m) => m.id !== data.id).slice(0, 6)
    }
    // 页面刷新后回显最近一次谱曲任务（提示词常驻可见）
    if (ai.status === 'fulfilled' && ai.value.data?.length) {
      aiJob.value = ai.value.data[0]
    }
  } catch {
    poem.value = null
  } finally {
    loading.value = false
  }
}

function playTrack(t) {
  if (!t.url) {
    toast.error('音频需部署后端环境，本地请用 real 模式体验')
    return
  }
  activeTrackId.value = t.id
  player.playSong({ ...t, singerName: t.name.match(/（(.+?)）/)?.[1] || poem.value?.poetName })
}

/** 为此诗谱曲：发起任务并轮询，完成后刷新音轨 */
async function compose() {
  if (polling.value) return
  // 请求发出前先上锁：后端要等 DeepSeek 读完诗才返回，这段时间里按钮必须已禁用，
  // 否则连点会各建一个任务，一首诗被谱出两曲
  polling.value = true
  const again = aiJob.value?.status === 'done'
  try {
    const { data: job } = await aiMusicApi.generate(poem.value.id, again)
    aiJob.value = job
    if (job.status === 'done') {
      await refreshTracks()
      toast.success('AI 谱曲完成，可试听')
      return
    }
    if (job.status === 'failed') {
      toast.error('谱曲失败：' + (job.error || '未知原因'))
      return
    }
    for (let i = 0; i < 100; i++) {
      await new Promise((r) => setTimeout(r, 1500))
      const { data: j } = await aiMusicApi.job(job.id)
      aiJob.value = j
      if (j.status === 'done') {
        await refreshTracks()
        toast.success('AI 谱曲完成，可试听')
        break
      }
      if (j.status === 'failed') {
        toast.error('谱曲失败：' + (j.error || '未知原因'))
        break
      }
    }
  } catch (e) {
    toast.error(e.message)
  } finally {
    polling.value = false
  }
}

async function refreshTracks() {
  const { data } = await songApi.list({ poemId: poem.value.id })
  tracks.value = data
}

async function toggleFav() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const { data } = await poemFavApi.toggle(poem.value.id)
    favorited.value = data.favorited
    toast.success(data.favorited ? '已收入书斋' : '已移出书斋')
  } catch (e) {
    toast.error(e.message)
  }
}

async function submitComment() {
  if (!commentText.value.trim()) return toast.error('写点什么再发吧')
  submitting.value = true
  try {
    await poemCommentApi.add(poem.value.id, commentText.value.trim())
    commentText.value = ''
    const { data } = await poemCommentApi.list(poem.value.id)
    comments.value = data
    toast.success('感悟已录')
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function removeComment(c) {
  const yes = await confirmAction('确定删除这条感悟吗？', '删除感悟')
  if (!yes) return
  try {
    await poemCommentApi.remove(c.id)
    comments.value = comments.value.filter((x) => x.id !== c.id)
    toast.success('已删除')
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(load)
watch(() => route.params.id, () => { if (route.name === 'poemDetail') load() })
</script>

<template>
  <div class="page">
    <div v-if="loading" class="spinner" />
    <div v-else-if="!poem" class="empty">诗不存在</div>
    <template v-else>
      <!-- 意境卷首 -->
      <section class="jing-hero">
        <div class="mountain" />
        <div class="jing-grid">
          <!-- 竖排诗行 -->
          <div class="scroll-pane">
            <div class="poem-vertical" data-test="poem-vertical">
              <p v-for="(ln, i) in phrases" :key="i" class="v-line">{{ ln }}</p>
            </div>
            <p class="v-sign">{{ poem.poetName }}<span class="v-dyn">{{ poem.dynasty }}</span></p>
          </div>

          <!-- 题注与音轨 -->
          <div class="side-pane">
            <h1 class="p-title font-brush">《{{ fullTitle }}》</h1>
            <div class="p-meta">
              <router-link :to="`/poet/${poem.poetId}`" class="poet-link">
                {{ poem.poetName }}<span v-if="poem.poetTitle"> · {{ poem.poetTitle }}</span>
              </router-link>
              <span class="tag" v-if="poem.mood">{{ poem.mood }}</span>
              <span class="tag plain" v-if="poem.genre">{{ poem.genre }}</span>
            </div>

            <!-- 音轨（乐为辅助） -->
            <div class="tracks card" v-if="tracks.length">
              <p class="t-label">以乐佐诗</p>
              <button
                v-for="t in [...aiTracks, ...recitations, ...bgms]" :key="t.id"
                class="track" :class="{ playing: activeTrackId === t.id }"
                @click="playTrack(t)"
              >
                <span class="t-kind" :class="'k-' + t.trackKind">{{ { recitation: '诵', bgm: '乐', ai: 'AI' }[t.trackKind] }}</span>
                <span class="t-name">{{ t.name }}</span>
                <span class="t-dur">{{ Math.round(t.duration) }}″</span>
              </button>
            </div>

            <div class="actions">
              <button class="btn compose" :disabled="polling" @click="compose" data-test="ai-compose">
                {{ polling ? '谱曲中…' : aiJob?.status === 'done' ? '✦ 再谱一曲' : '✦ 为此诗谱曲' }}
              </button>
              <button class="btn fav" :class="{ on: favorited }" @click="toggleFav" data-test="poem-fav">
                {{ favorited ? '✓ 已入书斋' : '收入书斋' }}
              </button>
              <button class="btn ghost-act" @click="askOpen = true" data-test="poem-ask">✦ 问诗</button>
              <button class="btn ghost-act" @click="downloadPoemCard(poem)" data-test="poem-card">制成诗笺</button>
              <button class="btn ghost-act" @click="reciteOpen = true" data-test="poem-recite">试背</button>
            </div>
            <p class="compose-note" v-if="aiJob">
              {{ aiJob.status === 'done' ? `已按「${poem.mood}」谱成 · ${aiJob.provider === 'acestep' ? 'AI 读诗实时生成' : '本地预生成兜底'}` : aiJob.status === 'failed' ? aiJob.error : 'AI 正在读诗…' }}
            </p>
            <p class="compose-prompt" v-if="aiJob?.status === 'done'" :title="aiJob.prompt">
              ✧ {{ aiJob.prompt }}
            </p>
          </div>
        </div>
      </section>

      <!-- 注 译 赏 -->
      <section class="sanlan">
        <div class="lan card" v-if="poem.annotation">
          <h3>注</h3>
          <div class="zhuan">
            <span v-for="(t, i) in zhuanItems" :key="i" class="zhuan-item">{{ t }}<i v-if="i < zhuanItems.length - 1">；</i></span>
          </div>
        </div>
        <div class="lan card" v-if="poem.translation">
          <h3>译</h3>
          <p>{{ poem.translation }}</p>
        </div>
        <div class="lan card" v-if="poem.appreciation">
          <h3>赏</h3>
          <p>{{ poem.appreciation }}</p>
        </div>
      </section>

      <!-- 同心境 -->
      <section class="similar" v-if="similar.length">
        <h2 class="sec-title">同心境 · 再读</h2>
        <div class="sim-chips">
          <router-link v-for="m in similar" :key="m.id" :to="`/poem/${m.id}`" class="sim-chip">
            《{{ m.title }}》<span>{{ m.poetName }}</span>
          </router-link>
        </div>
      </section>

      <AskDialog :open="askOpen" :poem="poem || {}" @close="askOpen = false" />
      <ReciteMode :open="reciteOpen" :poem="poem || {}" @close="reciteOpen = false" />

      <!-- 感悟 -->
      <section class="comments">
        <h2 class="sec-title">感悟（{{ comments.length }}）</h2>

        <div class="c-editor" v-if="userStore.isLoggedIn">
          <UserAvatar :name="userStore.user.name" :size="38" circle />
          <div class="c-wrap">
            <textarea v-model="commentText" class="textarea" rows="2" maxlength="500"
              placeholder="读罢此诗，写下你的所感所想…" data-test="poem-comment-input" />
            <div class="c-foot">
              <span class="counter">{{ commentText.length }}/500</span>
              <button class="btn btn-primary btn-sm" :disabled="submitting" @click="submitComment" data-test="poem-comment-submit">
                {{ submitting ? '落笔中…' : '写下感悟' }}
              </button>
            </div>
          </div>
        </div>
        <div v-else class="login-tip">
          <router-link :to="{ name: 'login', query: { redirect: route.fullPath } }">登录</router-link>
          后写下你的感悟
        </div>

        <div class="c-list">
          <div v-for="c in comments" :key="c.id" class="c-item">
            <UserAvatar :name="c.username" :size="38" circle />
            <div class="c-body">
              <div class="c-head">
                <span class="c-name">{{ c.username }}</span>
                <button class="c-del" v-if="c.userId === userStore.user?.id" @click="removeComment(c)">删除</button>
              </div>
              <p class="c-text">{{ c.content }}</p>
            </div>
          </div>
          <div v-if="!comments.length" class="empty" style="padding: 26px 0">尚无感悟，做第一个题笔的人</div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
/* 意境卷首：依情绪调色的宣纸卷 */
.jing-hero {
  position: relative;
  border: 1px solid var(--border-strong);
  border-radius: 14px;
  padding: 36px 38px;
  margin-bottom: 26px;
  overflow: hidden;
  background:
    radial-gradient(560px 300px at 86% 8%, rgba(184, 134, 11, 0.13), transparent),
    linear-gradient(160deg, rgba(64, 99, 77, 0.16), rgba(58, 54, 48, 0.06)),
    var(--card);
}
.mountain {
  position: absolute;
  left: 0; right: 0; bottom: 0;
  height: 30%;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 240' preserveAspectRatio='none'%3E%3Cpath d='M0 240 L0 150 Q180 84 380 128 T760 96 T1140 140 T1440 108 L1440 240 Z' fill='%233a3630' opacity='0.07'/%3E%3Cpath d='M0 240 L0 186 Q240 122 480 160 T960 148 T1440 172 L1440 240 Z' fill='%233a3630' opacity='0.11'/%3E%3C/svg%3E");
  background-size: cover;
  pointer-events: none;
}
.jing-grid {
  position: relative;
  display: flex;
  justify-content: center;      /* 诗与操作区作为整体居中，剩余空间留给两侧 */
  align-items: flex-start;
  gap: clamp(36px, 5vw, 72px);  /* 诗区与操作区间距恒定，不随诗的长短变化 */
}
/* 竖排诗行：宽度随诗的列数收缩 */
.scroll-pane {
  flex: none;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  max-width: min(62%, 780px);
  overflow-x: auto;             /* 超长词牌（如水调歌头）横向滚动兜底 */
  padding-bottom: 6px;
}
.poem-vertical {
  writing-mode: vertical-rl;
  text-orientation: upright;
  display: flex;
  flex-direction: column;   /* vertical-rl 中 flex 主轴即纵向（inline 向） */
  align-items: center;
  gap: 16px;
  min-height: 300px;
  max-height: 460px;
}
.v-line {
  margin: 0;
  font-size: 20px;
  line-height: 1;
  letter-spacing: 9px;
  color: var(--ink);
}
.v-sign {
  writing-mode: vertical-rl;
  text-orientation: upright;
  margin: 0;
  font-size: 14px;
  color: var(--text-3);
  letter-spacing: 6px;
  padding-top: 8px;
}
.v-dyn { font-size: 11px; color: var(--text-3); }

.side-pane {
  flex: none;
  width: 380px;
  max-width: 42%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.p-title { margin: 0; font-size: 30px; letter-spacing: 4px; line-height: 1.4; }
.p-meta { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; font-size: 13.5px; }
.poet-link { color: var(--primary-2); }
.tag.plain { background: none; color: var(--text-3); border-color: var(--border); cursor: default; }

.tracks { padding: 14px; }
.t-label { margin: 0 0 8px; font-size: 12px; color: var(--text-3); letter-spacing: 2px; }
.track {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--panel);
  color: var(--text);
  cursor: pointer;
  margin-bottom: 6px;
  transition: all 0.15s;
  text-align: left;
}
.track:last-child { margin-bottom: 0; }
.track:hover { border-color: var(--primary); }
.track.playing { border-color: var(--primary); background: var(--grad-soft); }
.t-kind {
  width: 22px; height: 22px;
  border-radius: 4px;
  background: var(--seal);
  color: #fbf6ea;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.t-kind.k-bgm { background: var(--primary); }
.t-kind.k-ai { background: var(--accent); font-size: 9px; font-weight: 700; }
.track .t-kind:last-child { background: var(--primary); }
.t-name { flex: 1; font-size: 13px; }
.t-dur { font-size: 11.5px; color: var(--text-3); font-variant-numeric: tabular-nums; }

.actions { display: flex; flex-wrap: wrap; gap: 10px; row-gap: 10px; align-items: center; }
.compose { background: var(--accent); color: #fbf6ea; border: none; }
.compose:hover:not(:disabled) { filter: brightness(1.1); }
.compose:disabled { opacity: 0.7; }
.compose-note { margin: -6px 0 0; font-size: 12px; color: var(--text-3); }
.ghost-act { background: none; border: 1px solid var(--border-strong); color: var(--text-2); }
.ghost-act:hover { border-color: var(--primary); color: var(--primary); }
.compose-prompt {
  margin: 2px 0 0;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--text-3);
  font-style: italic;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fav { border: 1px solid var(--border-strong); background: var(--card); color: var(--text-2); }
.fav:hover { border-color: var(--seal); color: var(--seal); }
.fav.on { background: var(--seal); border-color: var(--seal); color: #fbf6ea; }

/* 注译赏 */
.sanlan { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; margin-bottom: 28px; }
.lan h3 {
  margin: 0 0 10px;
  font-size: 15px;
  color: var(--primary);
  display: flex;
  align-items: center;
  gap: 8px;
}
.lan h3::after { content: ''; flex: 1; height: 1px; background: var(--border); }
.lan p { margin: 0; font-size: 13.5px; line-height: 2; color: var(--text-2); }

/* 注：一条一行，行末分号；左列排满框高后，余下的条目自动流入右列 */
.zhuan {
  height: 9.7em;           /* 约五行；内容少时只占左列，不留空白列 */
  font-size: 13px;
  line-height: 1.95;
  color: var(--text-2);
  column-count: 2;
  column-gap: 18px;
  column-fill: auto;
}
.zhuan-item { display: block; break-inside: avoid; }
.zhuan-item i { font-style: normal; color: var(--text-3); }

.sec-title { margin: 0 0 14px; font-size: 18px; }
.sim-chips { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 28px; }
.sim-chip {
  padding: 8px 14px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 8px;
  color: var(--text-2);
  font-size: 13px;
  transition: all 0.15s;
}
.sim-chip span { color: var(--text-3); font-size: 12px; }
.sim-chip:hover { border-color: var(--primary); color: var(--primary); }

/* 感悟 */
.comments { margin-bottom: 30px; }
.c-editor { display: flex; gap: 12px; margin-bottom: 20px; }
.c-wrap { flex: 1; }
.c-foot { display: flex; justify-content: flex-end; align-items: center; gap: 10px; margin-top: 8px; }
.counter { font-size: 12px; color: var(--text-3); }
.login-tip { font-size: 13px; color: var(--text-3); margin-bottom: 20px; }
.c-list { display: flex; flex-direction: column; gap: 14px; }
.c-item { display: flex; gap: 12px; }
.c-body { flex: 1; background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 12px 14px; }
.c-head { display: flex; justify-content: space-between; margin-bottom: 6px; }
.c-name { font-size: 13px; font-weight: 600; }
.c-del { border: none; background: none; color: var(--text-3); font-size: 12px; cursor: pointer; }
.c-del:hover { color: var(--danger); }
.c-text { margin: 0; font-size: 13.5px; line-height: 1.9; color: var(--text); }

/* 窄屏：竖排退化为横排 */
@media (max-width: 860px) {
  .jing-grid { flex-direction: column; }
  .poem-vertical {
    writing-mode: horizontal-tb;
    text-orientation: mixed;
    max-height: none;
  }
  .v-line { font-size: 16px; letter-spacing: 2px; line-height: 2.2; }
  .v-sign { writing-mode: horizontal-tb; text-orientation: mixed; }
  .side-pane { max-width: none; }
  /* 窄屏一列太窄，分列反而更难读 */
  .zhuan { height: auto; column-count: 1; }
}
</style>
