<script setup>
/**
 * 诗游 —— 古诗词小游戏合集
 * 1. 拼图：打乱名句字序，按原句顺序拼回
 * 2. 交游：给出两位诗人，猜他们之间的关系
 * 3. 地理：看诗人的行旅地点，猜朝代
 */
import { ref, computed, onMounted } from 'vue'
import { poetryApi } from '../api'
import { toast } from '../utils/toast'

/* ========== 全局状态 ========== */
const tab = ref('shuffle')  // shuffle | pair | geo
const loading = ref(true)

const tabs = [
  { id: 'shuffle', label: '拼句', desc: '名句打乱，按序拼回' },
  { id: 'pair', label: '交游', desc: '两位诗人，关系为何' },
  { id: 'geo', label: '地理', desc: '行旅地点，猜其朝代' }
]

/* ========== 数据源 ========== */
const poems = ref([])        // 精选层诗（含 famousLine）
const poets = ref([])        // 全部诗人
const relations = ref([])    // 关系列表
const journeys = ref([])     // 有行旅的诗人 [{name, dynasty, stops:[{place, modern}]}]

onMounted(async () => {
  try {
    const [pl, pt] = await Promise.allSettled([
      poetryApi.poemPage({ tier: 1, page: 1, size: 100 }),
      poetryApi.poetPage({ page: 1, size: 100 })
    ])
    if (pl.status === 'fulfilled') poems.value = pl.value.data.list
    if (pt.status === 'fulfilled') poets.value = pt.value.data.list

    // 拉取关系：取前 40 位精选诗人（带诗人名）
    const tier1 = poets.value.filter(p => p.tier === 1).slice(0, 40)
    const relResults = await Promise.allSettled(
      tier1.map((p) => import('../api').then(m => m.relationApi.byPoet(p.id)))
    )
    relations.value = relResults
      .filter(r => r.status === 'fulfilled')
      .flatMap((r, i) => (r.value.data || []).map(rel => ({ ...rel, poetName: tier1[i]?.name || '' })))

    // 拉取行旅：取前 30 位
    const journeyTargets = poets.value.filter(p => p.tier === 1).slice(0, 30)
    const jResults = await Promise.allSettled(
      journeyTargets.map((p) => poetryApi.poetDetail(p.id))
    )
    journeys.value = jResults
      .filter(r => r.status === 'fulfilled' && r.value.data?.poet?.journey)
      .map(r => {
        let stops = []
        try { stops = JSON.parse(r.value.data.poet.journey) } catch { /* ignore */ }
        return {
          name: r.value.data.poet.name,
          dynasty: r.value.data.poet.dynasty,
          stops
        }
      })
      .filter(j => j.stops.length > 0)
  } finally {
    loading.value = false
  }
})

/* ========== 1. 拼句 ========== */
const shuffleState = ref(null)

function newShuffle() {
  const pool = poems.value.filter(p => p.famousLine)
  if (!pool.length) return
  const p = pool[Math.floor(Math.random() * pool.length)]
  const target = p.famousLine
  const chars = [...target]
  // 打乱（确保不是原序）
  let shuffled
  do {
    shuffled = [...chars].sort(() => Math.random() - 0.5)
  } while (shuffled.join('') === target && chars.length > 1)
  shuffleState.value = {
    target,
    chars,
    shuffled,
    placed: [],       // 已放入槽位的字
    slots: [],        // 槽位内容
    done: false,
    score: 0,
    attempts: 0,
    poet: p.poetName,
    title: p.title + (p.subtitle ? '·' + p.subtitle : '')
  }
}

function shuffleClick(ch, idx) {
  const st = shuffleState.value
  if (!st || st.done) return
  // 从池中移除
  const pool = [...st.shuffled]
  pool.splice(idx, 1)
  st.shuffled = pool
  st.slots.push({ ch, from: idx })

  // 槽满 → 检查
  if (st.slots.length === st.chars.length) {
    st.attempts++
    const answer = st.slots.map(s => s.ch).join('')
    if (answer === st.target) {
      st.done = true
      st.score = Math.max(0, 100 - st.attempts * 10)
      toast.success(`拼对了！${st.score} 分`)
    } else {
      toast.error('顺序不对，再想想')
      // 重置槽位，池恢复
      setTimeout(() => {
        st.slots = []
        st.shuffled = [...st.chars].sort(() => Math.random() - 0.5)
      }, 800)
    }
  }
}

function slotRemove(i) {
  const st = shuffleState.value
  if (!st || st.done) return
  const removed = st.slots.splice(i, 1)[0]
  st.shuffled.push(removed.ch)
}

/* ========== 2. 交游 ========== */
const pairState = ref(null)

function newPair() {
  if (!relations.value.length) return
  const r = relations.value[Math.floor(Math.random() * relations.value.length)]
  const options = [...new Set(relations.value.map(x => x.relType))]
  const shuffledOpts = [...options].sort(() => Math.random() - 0.5).slice(0, 5)
  if (!shuffledOpts.includes(r.relType)) {
    shuffledOpts[Math.floor(Math.random() * shuffledOpts.length)] = r.relType
  }
  pairState.value = {
    a: r.poetName,       // 当前诗人
    b: r.relatedName,    // 关联诗人
    rel: r,
    options: shuffledOpts.sort(() => Math.random() - 0.5),
    selected: null,
    correct: false,
    done: false,
    showNote: false
  }
}

function pairPick(opt) {
  const st = pairState.value
  if (!st || st.done) return
  st.selected = opt
  st.correct = opt === st.rel.relType
  st.done = true
  if (st.correct) toast.success('答对了！')
  else toast.error(`正确答案是「${st.rel.relType}」`)
}

/* ========== 3. 地理 ========== */
const geoState = ref(null)

function newGeo() {
  if (!journeys.value.length) return
  const j = journeys.value[Math.floor(Math.random() * journeys.value.length)]
  const stop = j.stops[Math.floor(Math.random() * j.stops.length)]
  // 朝代选项：从全体诗人朝代中取
  const allDynasties = [...new Set(poets.value.map(p => p.dynasty).filter(Boolean))]
  const opts = [...allDynasties].sort(() => Math.random() - 0.5).slice(0, 4)
  if (!opts.includes(j.dynasty)) {
    opts[Math.floor(Math.random() * opts.length)] = j.dynasty
  }
  geoState.value = {
    poet: j.name,
    place: stop.place,
    modern: stop.modern || '',
    period: stop.period || '',
    dynasty: j.dynasty,
    options: opts.sort(() => Math.random() - 0.5),
    selected: null,
    correct: false,
    done: false
  }
}

function geoPick(opt) {
  const st = geoState.value
  if (!st || st.done) return
  st.selected = opt
  st.correct = opt === st.dynasty
  st.done = true
  if (st.correct) toast.success('答对了！')
  else toast.error(`正确答案是「${st.dynasty}」`)
}
</script>

<template>
  <div class="page games-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">诗游</h1>
        <p class="page-sub">拼句 · 交游 · 地理——三款古诗小游戏</p>
      </div>
    </div>

    <div v-if="loading" class="spinner" />
    <template v-else>
      <!-- Tab 切换 -->
      <div class="game-tabs">
        <button v-for="t in tabs" :key="t.id"
          class="game-tab" :class="{ on: tab === t.id }"
          @click="tab = t.id; if (t.id === 'shuffle') newShuffle(); if (t.id === 'pair') newPair(); if (t.id === 'geo') newGeo()"
          :data-test="'tab-' + t.id"
        >
          <span class="gt-label">{{ t.label }}</span>
          <span class="gt-desc">{{ t.desc }}</span>
        </button>
      </div>

      <!-- ===== 拼句 ===== -->
      <section v-if="tab === 'shuffle'" class="game-area card">
        <div v-if="!shuffleState" class="game-empty">
          <button class="btn btn-primary" @click="newShuffle" data-test="shuffle-start">开始拼句</button>
        </div>
        <template v-else>
          <div class="gs-head">
            <span class="gs-meta">《{{ shuffleState.title }}》· {{ shuffleState.poet }}</span>
            <button class="btn btn-ghost btn-sm" @click="newShuffle">换一句</button>
          </div>
          <!-- 槽位 -->
          <div class="slots" data-test="shuffle-slots">
            <div v-for="(s, i) in shuffleState.slots" :key="i"
              class="slot" :class="{ ok: shuffleState.done }"
              @click="slotRemove(i)"
            >{{ s.ch }}</div>
            <div v-for="i in shuffleState.chars.length - shuffleState.slots.length" :key="'e' + i"
              class="slot empty"
            ></div>
          </div>
          <!-- 字池 -->
          <div class="pool" data-test="shuffle-pool">
            <button v-for="(ch, i) in shuffleState.shuffled" :key="i"
              class="pool-ch" :disabled="shuffleState.done"
              @click="shuffleClick(ch, i)"
            >{{ ch }}</button>
          </div>
          <div class="gs-foot">
            <span>已尝试 {{ shuffleState.attempts }} 次 · <b :style="{color: shuffleState.done ? 'var(--accent)' : ''}">{{ shuffleState.done ? '拼对了！' : '' }}</b></span>
          </div>
        </template>
      </section>

      <!-- ===== 交游 ===== -->
      <section v-if="tab === 'pair'" class="game-area card">
        <div v-if="!pairState" class="game-empty">
          <button class="btn btn-primary" @click="newPair" data-test="pair-start">开始交游</button>
        </div>
        <template v-else>
          <div class="gs-head">
            <span class="gs-meta">两位诗人之间是什么关系？</span>
            <button class="btn btn-ghost btn-sm" @click="newPair">换一题</button>
          </div>
          <div class="pair-names">
            <span class="pn font-brush">{{ pairState.a }}</span>
            <span class="pn-arrow">↔</span>
            <span class="pn font-brush">{{ pairState.b }}</span>
          </div>
          <div class="pair-options" data-test="pair-options">
            <button v-for="opt in pairState.options" :key="opt"
              class="pair-opt"
              :class="{ picked: pairState.selected === opt, right: pairState.done && opt === pairState.rel.relType, wrong: pairState.done && pairState.selected === opt && opt !== pairState.rel.relType }"
              :disabled="pairState.done"
              @click="pairPick(opt)"
            >{{ opt }}</button>
          </div>
          <div class="gs-note" v-if="pairState.done && pairState.showNote">
            <p>{{ pairState.rel.note }}</p>
          </div>
          <div class="gs-foot">
            <button v-if="pairState.done && !pairState.showNote" class="btn btn-ghost btn-sm"
              @click="pairState.showNote = true">看故事 →</button>
          </div>
        </template>
      </section>

      <!-- ===== 地理 ===== -->
      <section v-if="tab === 'geo'" class="game-area card">
        <div v-if="!geoState" class="game-empty">
          <button class="btn btn-primary" @click="newGeo" data-test="geo-start">开始地理</button>
        </div>
        <template v-else>
          <div class="gs-head">
            <span class="gs-meta">{{ geoState.poet }} 曾旅居此地，这是哪个朝代？</span>
            <button class="btn btn-ghost btn-sm" @click="newGeo">换一题</button>
          </div>
          <div class="geo-place">
            <span class="gp-name font-brush">{{ geoState.place }}</span>
            <span class="gp-modern" v-if="geoState.modern">今 {{ geoState.modern }}</span>
          </div>
          <div class="pair-options" data-test="geo-options">
            <button v-for="opt in geoState.options" :key="opt"
              class="pair-opt"
              :class="{ picked: geoState.selected === opt, right: geoState.done && opt === geoState.dynasty, wrong: geoState.done && geoState.selected === opt && opt !== geoState.dynasty }"
              :disabled="geoState.done"
              @click="geoPick(opt)"
            >{{ opt }}</button>
          </div>
          <div class="gs-foot" v-if="geoState.done">
            <span :class="geoState.correct ? 'ok-text' : 'bad-text'">
              {{ geoState.correct ? '答对了！' : `正确答案是「${geoState.dynasty}」` }}
            </span>
            <span v-if="geoState.period" class="gp-period">{{ geoState.period }}</span>
          </div>
        </template>
      </section>
    </template>
  </div>
</template>

<style scoped>
.game-tabs { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.game-tab {
  display: flex; flex-direction: column; gap: 2px;
  padding: 10px 20px;
  border: 1px solid var(--border-strong);
  border-radius: 10px;
  background: var(--card);
  cursor: pointer;
  text-align: left;
  min-width: 140px;
  transition: all 0.15s;
}
.game-tab.on { border-color: var(--primary); background: var(--grad-soft); }
.gt-label { font-size: 15px; font-weight: 600; color: var(--text); }
.gt-desc { font-size: 11.5px; color: var(--text-3); }

.game-area { padding: 24px 28px; min-height: 300px; }
.game-empty { display: flex; align-items: center; justify-content: center; min-height: 200px; }
.gs-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.gs-meta { font-size: 13.5px; color: var(--text-2); }
.gs-foot { margin-top: 16px; font-size: 13px; color: var(--text-3); }
.gs-note { margin-top: 14px; padding: 12px 16px; background: var(--card-hover); border-radius: 8px; font-size: 13.5px; line-height: 2; color: var(--text-2); }

/* 拼句 */
.slots { display: flex; gap: 6px; margin-bottom: 20px; justify-content: center; flex-wrap: wrap; }
.slot {
  width: 38px; height: 38px;
  border: 2px dashed var(--border-strong);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700;
  background: var(--panel);
  cursor: pointer;
  transition: all 0.15s;
}
.slot.ok { border-color: var(--accent); background: rgba(184, 134, 11, 0.08); }
.slot.empty { opacity: 0.4; cursor: default; }

.pool { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; }
.pool-ch {
  width: 40px; height: 40px;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--card);
  font-size: 20px; font-weight: 600;
  cursor: pointer;
  transition: all 0.12s;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', serif;
}
.pool-ch:hover:not(:disabled) { border-color: var(--primary); transform: translateY(-2px); }
.pool-ch:disabled { opacity: 0.35; cursor: not-allowed; }

/* 交游 */
.pair-names { display: flex; align-items: center; justify-content: center; gap: 20px; margin: 30px 0; }
.pn { font-size: 32px; letter-spacing: 3px; }
.pn-arrow { font-size: 22px; color: var(--accent); }
.pair-options { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }
.pair-opt {
  padding: 10px 24px;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--card);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.15s;
}
.pair-opt:hover:not(:disabled) { border-color: var(--primary); }
.pair-opt.right { border-color: var(--accent); background: rgba(184, 134, 11, 0.12); color: var(--accent); }
.pair-opt.wrong { border-color: var(--danger); background: rgba(127, 29, 29, 0.08); color: var(--danger); }
.pair-opt:disabled { cursor: not-allowed; }

/* 地理 */
.geo-place { text-align: center; margin: 30px 0; }
.gp-name { font-size: 34px; letter-spacing: 4px; display: block; margin-bottom: 6px; }
.gp-modern { font-size: 13px; color: var(--text-3); }
.gp-period { margin-left: 14px; font-size: 12px; color: var(--accent); }
.ok-text { color: var(--accent); }
.bad-text { color: var(--danger); }
</style>
