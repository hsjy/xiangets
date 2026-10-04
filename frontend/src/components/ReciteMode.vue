<script setup>
/**
 * 背诵模式：按句读挖空，逐字填入。
 * 挖空位置由诗 id 作种子伪随机（同一首诗每次进入一致）；
 * 「提示」逐格揭示，「核对」标出正误。
 */
import { ref, computed, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  poem: { type: Object, required: true }
})
const emit = defineEmits(['close'])

/** 简易确定性随机（mulberry32）：让同一首诗的挖空固定 */
function rng(seed) {
  let a = seed >>> 0
  return () => {
    a |= 0; a = (a + 0x6D2B79F5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const rows = ref([])
const checked = ref(false)

const allBlanks = computed(() => rows.value.flatMap((r) => r.cells.filter((c) => c.blank)))
const okCount = computed(() => allBlanks.value.filter((b) => b.ok).length)

function build() {
  const rand = rng(Number(props.poem.id) * 7919 + 13)
  const lines = (props.poem.paragraphs || '').split('\n').filter(Boolean)
  rows.value = lines.map((line) => {
    const chars = [...line]
    const positions = chars.map((ch, i) => ({ ch, i }))
      .filter(({ ch }) => /[\u4e00-\u9fa5]/.test(ch))
    const n = Math.min(positions.length, 2 + Math.floor(rand() * 2))
    const picked = new Set()
    while (picked.size < n) picked.add(positions[Math.floor(rand() * positions.length)].i)
    return {
      cells: chars.map((ch, i) => picked.has(i)
        ? { blank: true, answer: ch, input: '', shown: false, ok: null }
        : { ch })
    }
  })
  checked.value = false
}

watch(() => [props.open, props.poem?.id], () => { if (props.open) build() }, { immediate: true })

function check() {
  checked.value = true
  for (const b of allBlanks.value) b.ok = b.input.trim() === b.answer
}

function hintOne() {
  const target = allBlanks.value.find((b) => !b.ok)
  if (target) { target.shown = true; target.input = target.answer; target.ok = true }
}
</script>

<template>
  <div v-if="open" class="recite-mask" @click.self="emit('close')">
    <div class="recite-panel" data-test="recite-panel">
      <div class="rc-head">
        <span class="rc-seal font-brush">诵</span>
        <div class="rc-title">
          <span>背诵</span>
          <small>《{{ poem.title }}{{ poem.subtitle ? '·' + poem.subtitle : '' }}》 · 填出空缺的字</small>
        </div>
        <span class="rc-score" v-if="checked">答对 {{ okCount }} / {{ allBlanks.length }}</span>
        <button class="rc-close" @click="emit('close')">✕</button>
      </div>

      <div class="rc-body">
        <p v-for="(row, ri) in rows" :key="ri" class="rc-line">
          <template v-for="(cell, ci) in row.cells" :key="ci">
            <input
              v-if="cell.blank"
              v-model="cell.input"
              class="rc-input" :class="{ ok: cell.ok === true, bad: cell.ok === false }"
              maxlength="1" :placeholder="cell.shown ? cell.answer : ''"
              :data-test="`recite-${ri}-${ci}`"
            />
            <span v-else class="rc-char">{{ cell.ch }}</span>
          </template>
        </p>
      </div>

      <div class="rc-foot">
        <button class="btn btn-ghost btn-sm" @click="hintOne">提示一字</button>
        <button class="btn btn-ghost btn-sm" @click="build">重来</button>
        <button class="btn btn-primary" @click="check" data-test="recite-check">核对</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recite-mask {
  position: fixed;
  inset: 0;
  background: rgba(46, 38, 24, 0.36);
  z-index: 210;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.recite-panel {
  width: min(660px, 100%);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  border-radius: 18px;
  background: var(--panel);
  border: 1px solid var(--border-strong);
  box-shadow: 0 24px 64px rgba(60, 48, 26, 0.3);
  overflow: hidden;
  animation: rc-rise 0.3s cubic-bezier(0.21, 0.9, 0.34, 1.12) backwards;
}
@keyframes rc-rise { from { opacity: 0; transform: translateY(20px) scale(0.98); } }

.rc-head { display: flex; align-items: center; gap: 12px; padding: 13px 18px; border-bottom: 1px solid var(--border); }
.rc-seal {
  width: 32px; height: 32px; border-radius: 6px;
  background: var(--seal); color: #fbf6ea;
  display: inline-flex; align-items: center; justify-content: center; font-size: 19px;
}
.rc-title { flex: 1; display: flex; align-items: baseline; gap: 8px; }
.rc-title span { font-size: 15px; font-weight: 600; }
.rc-title small { font-size: 12px; color: var(--text-3); }
.rc-score { font-size: 13px; color: var(--accent); }
.rc-close { border: none; background: none; color: var(--text-3); font-size: 15px; cursor: pointer; }
.rc-close:hover { color: var(--seal); }

.rc-body { flex: 1; overflow-y: auto; padding: 20px 24px; }
.rc-line { margin: 0 0 12px; font-size: 19px; line-height: 2.1; letter-spacing: 2px; }
.rc-char { color: var(--text); }
.rc-input {
  width: 26px; height: 30px;
  margin: 0 2px;
  border: none;
  border-bottom: 1.6px solid var(--border-strong);
  background: transparent;
  text-align: center;
  font-size: 19px;
  font-family: inherit;
  color: var(--text);
  outline: none;
}
.rc-input:focus { border-bottom-color: var(--primary); }
.rc-input.ok { border-bottom-color: var(--success); color: var(--success); }
.rc-input.bad { border-bottom-color: var(--danger); color: var(--danger); }
.rc-input::placeholder { color: var(--text-3); }

.rc-foot { display: flex; justify-content: flex-end; gap: 10px; padding: 12px 16px; border-top: 1px solid var(--border); }
</style>
