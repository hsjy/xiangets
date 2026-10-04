<script setup>
/**
 * 飞花令：给定令字，双方轮流报出含令字的诗句。
 * 系统从 34 万首诗库随机抽句（行进卡片展示出处）；
 * 用户对句经后端校验（含令字 + 库中可查 + 未用过）。
 */
import { ref, nextTick } from 'vue'
import { feihuaApi, poetryApi } from '../api'
import { toast } from '../utils/toast'

const CHAR_POOL = ['月', '花', '风', '山', '水', '云', '酒', '春', '夜', '江', '雪', '雨', '秋', '日', '天', '人']
const char = ref('')
const customChar = ref('')
const started = ref(false)
const loading = ref(false)
const submitting = ref(false)

const used = ref([])          // 全部已用句（归一化文本）
const rounds = ref([])        // [{who:'me'|'sys', line, title, poetName}]
const myLine = ref('')
const over = ref(false)
const overReason = ref('')
const inputRef = ref(null)

const norm = (s) => (s || '').replace(/[，。？！；、：""''\s]/g, '')

function start(ch) {
  const c = (ch || '').trim()
  if (!/^[\u4e00-\u9fa5]$/.test(c)) {
    toast.error('请选一个汉字作为令字')
    return
  }
  char.value = c
  used.value = []
  rounds.value = []
  over.value = false
  overReason.value = ''
  started.value = true
  systemTurn()
}

async function systemTurn() {
  loading.value = true
  try {
    const { data } = await feihuaApi.draw(char.value, used.value)
    used.value.push(data.line)
    rounds.value.push({ who: 'sys', line: data.line, title: data.title, poetName: data.poetName })
    await nextTick()
    inputRef.value?.focus()
  } catch (e) {
    over.value = true
    overReason.value = e.message
  } finally {
    loading.value = false
  }
}

async function submit() {
  const line = myLine.value.trim()
  if (!line || submitting.value || over.value) return
  submitting.value = true
  try {
    await feihuaApi.check(char.value, line, used.value)
    used.value.push(norm(line))
    rounds.value.push({ who: 'me', line })
    myLine.value = ''
    await systemTurn()
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}

function giveUp() {
  over.value = true
  overReason.value = '你认输了——「' + char.value + '」字句，诗库记你这一败。'
}

async function randomChar() {
  const c = CHAR_POOL[Math.floor(Math.random() * CHAR_POOL.length)]
  start(c)
}
</script>

<template>
  <div class="page feihua">
    <div class="page-head">
      <div>
        <h1 class="page-title">飞花令</h1>
        <p class="page-sub">与三十四万首诗对令——你说一句含令字的诗，库中回你一句</p>
      </div>
    </div>

    <!-- 未开局：选令字 -->
    <section v-if="!started" class="setup card">
      <h3 class="setup-title">择一令字，或让它替你拈一个</h3>
      <div class="chars">
        <button v-for="c in CHAR_POOL" :key="c" class="char-tag" @click="start(c)">{{ c }}</button>
      </div>
      <div class="custom-row">
        <input v-model.trim="customChar" class="input" maxlength="1" placeholder="自定义一个字" @keyup.enter="start(customChar)" />
        <button class="btn btn-primary" @click="start(customChar)">以此开局</button>
        <button class="btn btn-ghost" @click="randomChar">随机令字</button>
      </div>
    </section>

    <!-- 对局 -->
    <template v-else>
      <section class="board card">
        <div class="board-head">
          <span class="cur-char font-brush" :data-test="'feihua-char'">{{ char }}</span>
          <span class="board-meta">令字「{{ char }}」 · 已行 {{ rounds.length }} 句</span>
          <button v-if="!over" class="btn btn-ghost btn-sm" @click="giveUp">认输</button>
          <button class="btn btn-ghost btn-sm" @click="started = false">换令字</button>
        </div>

        <div class="rounds">
          <div v-for="(r, i) in rounds" :key="i" class="round" :class="r.who">
            <span class="who">{{ r.who === 'sys' ? '诗库' : '你' }}</span>
            <div class="said">
              <p class="line">{{ r.line }}</p>
              <p class="from" v-if="r.who === 'sys'">—— 《{{ r.title }}》 · {{ r.poetName }}</p>
            </div>
          </div>
          <div v-if="loading" class="round sys"><span class="who">诗库</span><div class="said"><p class="line thinking">正在翻检诗库…</p></div></div>
        </div>

        <div v-if="over" class="over">
          <p>{{ overReason }}</p>
          <button class="btn btn-primary" @click="start(char)">再来一局</button>
        </div>

        <form v-else class="my-turn" @submit.prevent="submit">
          <input
            ref="inputRef" v-model="myLine" class="input"
            :placeholder="`写出一句含「${char}」的诗…`" data-test="feihua-input"
          />
          <button class="btn btn-primary" type="submit" :disabled="submitting || loading" data-test="feihua-submit">对令</button>
        </form>
      </section>
    </template>
  </div>
</template>

<style scoped>
.setup { padding: 26px 28px; }
.setup-title { margin: 0 0 16px; font-size: 16px; }
.chars { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 20px; }
.char-tag {
  width: 44px; height: 44px;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--card);
  color: var(--text);
  font-size: 19px;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', serif;
  cursor: pointer;
  transition: all 0.15s;
}
.char-tag:hover { border-color: var(--seal); color: var(--seal); transform: translateY(-2px); }
.custom-row { display: flex; gap: 10px; align-items: center; }
.custom-row .input { width: 180px; text-align: center; font-size: 16px; }

.board { padding: 22px 26px; }
.board-head { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.cur-char {
  width: 52px; height: 52px;
  border-radius: 10px;
  background: var(--seal); color: #fbf6ea;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 30px;
}
.board-meta { flex: 1; font-size: 13px; color: var(--text-3); }

.rounds { display: flex; flex-direction: column; gap: 14px; margin-bottom: 20px; max-height: 46vh; overflow-y: auto; }
.round { display: flex; gap: 12px; }
.who {
  width: 44px; height: 30px; flex-shrink: 0;
  border-radius: 999px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 12.5px;
}
.round.sys .who { background: rgba(64, 99, 77, 0.12); color: var(--primary-2); }
.round.me .who { background: rgba(165, 64, 45, 0.1); color: var(--seal); }
.said { flex: 1; min-width: 0; border-radius: 10px; padding: 10px 14px; background: var(--card-hover); }
.round.me .said { background: rgba(165, 64, 45, 0.06); }
.line { margin: 0; font-size: 15px; line-height: 1.9; }
.line.thinking { color: var(--text-3); font-size: 13px; }
.from { margin: 4px 0 0; font-size: 12px; color: var(--accent); }

.my-turn { display: flex; gap: 10px; }
.my-turn .input { flex: 1; }
.over { text-align: center; padding: 16px 0 6px; }
.over p { margin: 0 0 14px; color: var(--text-2); }
</style>
