<script setup>
/**
 * 问诗：以 DeepSeek 为该诗作注脚——悬浮对话笺。
 * 支持多轮（最近 6 轮回传给后端作为上下文）。
 */
import { ref, nextTick, watch } from 'vue'
import { askApi } from '../api'

const props = defineProps({
  open: { type: Boolean, default: false },
  poem: { type: Object, required: true }
})
const emit = defineEmits(['close'])

const QUESTION_POOL = ['这首诗好在哪里？', '诗人当时是什么处境？', '最妙的是哪一句？', '有哪些意象值得注意？']
const question = ref('')
const messages = ref([])   // [{role:'user'|'assistant', content}]
const sending = ref(false)
const listRef = ref(null)

async function send(text) {
  const q = (text ?? question.value).trim()
  if (!q || sending.value) return
  question.value = ''
  messages.value.push({ role: 'user', content: q })
  sending.value = true
  await scrollBottom()
  try {
    const history = messages.value.slice(0, -1).slice(-6)
    const { data } = await askApi.ask(props.poem.id, q, history)
    messages.value.push({ role: 'assistant', content: data.answer })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: e.message || '问诗暂时不可用，请稍后再试' })
  } finally {
    sending.value = false
    scrollBottom()
  }
}

async function scrollBottom() {
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}

watch(() => props.open, (v) => { if (v) scrollBottom() })
</script>

<template>
  <div v-if="open" class="ask-mask" @click.self="emit('close')">
    <div class="ask-panel" data-test="ask-panel">
      <div class="ask-head">
        <span class="ask-seal font-brush">问</span>
        <div class="ask-title">
          <span>问诗</span>
          <small>《{{ poem.title }}{{ poem.subtitle ? '·' + poem.subtitle : '' }}》</small>
        </div>
        <button class="ask-close" @click="emit('close')" title="收起">✕</button>
      </div>

      <div ref="listRef" class="ask-body">
        <div v-if="!messages.length" class="ask-empty">
          <p>与先生共读此诗。或先问：</p>
          <div class="pool">
            <button v-for="q in QUESTION_POOL" :key="q" class="pool-q" @click="send(q)">{{ q }}</button>
          </div>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <span class="msg-who">{{ m.role === 'user' ? '我' : '先生' }}</span>
          <p class="msg-text">{{ m.content }}</p>
        </div>
        <div v-if="sending" class="msg assistant">
          <span class="msg-who">先生</span>
          <p class="msg-text thinking">沉吟中…</p>
        </div>
      </div>

      <form class="ask-foot" @submit.prevent="send()">
        <input
          v-model="question" class="input" :disabled="sending"
          placeholder="就此诗提问…" data-test="ask-input"
        />
        <button class="btn btn-primary" type="submit" :disabled="sending || !question.trim()" data-test="ask-send">问</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.ask-mask {
  position: fixed;
  inset: 0;
  background: rgba(46, 38, 24, 0.34);
  z-index: 210;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 0 16px 18px;
}
.ask-panel {
  width: min(560px, 100%);
  max-height: min(72vh, 640px);
  display: flex;
  flex-direction: column;
  border-radius: 18px;
  background: var(--panel);
  border: 1px solid var(--border-strong);
  box-shadow: 0 24px 64px rgba(60, 48, 26, 0.3);
  overflow: hidden;
  animation: ask-rise 0.32s cubic-bezier(0.21, 0.9, 0.34, 1.12) backwards;
}
@keyframes ask-rise { from { opacity: 0; transform: translateY(26px); } }

.ask-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 18px;
  border-bottom: 1px solid var(--border);
}
.ask-seal {
  width: 32px; height: 32px;
  border-radius: 6px;
  background: var(--seal); color: #fbf6ea;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 19px;
}
.ask-title { flex: 1; display: flex; align-items: baseline; gap: 8px; }
.ask-title span { font-size: 15px; font-weight: 600; }
.ask-title small { font-size: 12px; color: var(--text-3); }
.ask-close { border: none; background: none; color: var(--text-3); font-size: 15px; cursor: pointer; }
.ask-close:hover { color: var(--seal); }

.ask-body { flex: 1; overflow-y: auto; padding: 16px 18px; display: flex; flex-direction: column; gap: 14px; }
.ask-empty p { margin: 0 0 12px; font-size: 13px; color: var(--text-3); }
.pool { display: flex; flex-wrap: wrap; gap: 8px; }
.pool-q {
  border: 1px solid var(--border-strong);
  background: var(--card);
  color: var(--text-2);
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 12.5px;
  cursor: pointer;
}
.pool-q:hover { border-color: var(--primary); color: var(--primary); }

.msg { display: flex; gap: 10px; }
.msg-who {
  width: 40px; flex-shrink: 0; text-align: right;
  font-size: 12px; color: var(--text-3);
  padding-top: 8px;
}
.msg-text {
  flex: 1;
  margin: 0;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 2;
  white-space: pre-wrap;
}
.msg.assistant .msg-text { background: var(--card-hover); color: var(--text); }
.msg.user .msg-text { background: rgba(64, 99, 77, 0.1); color: var(--text); }
.msg-text.thinking { color: var(--text-3); font-size: 13px; }

.ask-foot { display: flex; gap: 10px; padding: 12px 16px; border-top: 1px solid var(--border); }
.ask-foot .input { flex: 1; }

@media (max-width: 720px) {
  .ask-panel { max-height: 78vh; }
}
</style>
