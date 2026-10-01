<script setup>
import { computed } from 'vue'

/**
 * 古风印章头像：以用户名首字入印，印色由名字哈希决定。
 * 取代卡通头像，风格与站内「弦歌」朱印一脉相承。
 */
const props = defineProps({
  name: { type: String, default: '' },
  size: { type: [Number, String], default: 32 },
  circle: { type: Boolean, default: false }
})

/* 印色盘：朱砂 / 松绿 / 黛青 / 泥金 / 紫檀 */
const SEALS = [
  { bg: '#a5402d', fg: '#fbf6ea' },
  { bg: '#40634d', fg: '#fbf6ea' },
  { bg: '#3a5a6b', fg: '#fbf6ea' },
  { bg: '#a67c1b', fg: '#fbf8ec' },
  { bg: '#5d3a4a', fg: '#fbf6ea' }
]

const displayName = computed(() => (props.name || '').trim() || '客')
const char = computed(() => [...displayName.value][0] || '客')
const idx = computed(() => {
  let h = 0
  for (const c of displayName.value) h = (h * 31 + c.codePointAt(0)) >>> 0
  return h % SEALS.length
})
const style = computed(() => ({
  width: props.size + 'px',
  height: props.size + 'px',
  background: SEALS[idx.value].bg,
  color: SEALS[idx.value].fg,
  fontSize: Math.round(props.size * 0.5) + 'px'
}))
</script>

<template>
  <span class="user-seal" :class="{ circle }" :style="style">{{ char }}</span>
</template>

<style scoped>
.user-seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 22%;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', 'Kaiti SC', 'STKaiti', serif;
  line-height: 1;
  flex-shrink: 0;
  user-select: none;
  /* 内描印边，仿朱文印 */
  box-shadow: inset 0 0 0 1.5px rgba(251, 246, 234, 0.5);
}
.user-seal.circle { border-radius: 50%; }
</style>
