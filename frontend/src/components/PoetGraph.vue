<script setup>
/**
 * 诗人关系图谱：以当前诗人为中心、关系人为环的径向图。
 * 环上位置按关系建立顺序均布，同一环内两两之间保持弧距；
 * 点击关系人进入其诗人页。
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  poet: { type: Object, required: true },   // {id, name, title, dynasty}
  relations: { type: Array, required: true } // [{relatedId, relatedName, relatedTitle, relatedDynasty, relType, note}]
})
const router = useRouter()

const W = 760, H = 420
const CX = W / 2, CY = H / 2
const RX = 268, RY = 148

const nodes = computed(() => {
  const n = props.relations.length
  return props.relations.map((r, i) => {
    const angle = -Math.PI / 2 + (i * 2 * Math.PI) / Math.max(1, n)
    return {
      ...r,
      x: CX + RX * Math.cos(angle),
      y: CY + RY * Math.sin(angle)
    }
  })
})

const typeColor = (t) => ({
  师承: 'var(--accent)',
  挚友: 'var(--seal)',
  交游: '#6b8f7a',
  并称: '#8a6d3b',
  对手: '#8a4b3b',
  父子: '#5b6d8a',
  文脉: '#6d6b8a',
  同志: '#4a7c59',
  知遇: '#9a7b3a',
  倾慕: '#a5402d'
}[t] || 'var(--text-3)')

function go(id) { router.push(`/poet/${id}`) }
</script>

<template>
  <div class="pgraph">
    <svg :viewBox="`0 0 ${W} ${H}`" class="pg-svg" role="img" aria-label="诗人关系图谱">
      <!-- 连线 -->
      <line v-for="(n, i) in nodes" :key="'l' + i"
            :x1="CX" :y1="CY" :x2="n.x" :y2="n.y"
            class="pg-line" :style="{ stroke: typeColor(n.relType) }" />
      <!-- 关系标签（线中点） -->
      <text v-for="(n, i) in nodes" :key="'t' + i"
            :x="(CX + n.x) / 2" :y="(CY + n.y) / 2 - 6"
            class="pg-type" :style="{ fill: typeColor(n.relType) }">{{ n.relType }}</text>

      <!-- 中心：当前诗人 -->
      <g class="pg-center">
        <circle :cx="CX" :cy="CY" r="46" class="c-halo" />
        <circle :cx="CX" :cy="CY" r="38" class="c-core" />
        <text :x="CX" :y="CY + 2" class="c-name font-brush">{{ poet.name }}</text>
        <text :x="CX" :y="CY + 24" class="c-sub">{{ poet.title || poet.dynasty }}</text>
      </g>

      <!-- 关系人 -->
      <g v-for="(n, i) in nodes" :key="'n' + i" class="pg-node" @click="go(n.relatedId)">
        <circle :cx="n.x" :cy="n.y" r="30" class="n-halo" />
        <circle :cx="n.x" :cy="n.y" r="24" class="n-core" />
        <text :x="n.x" :y="n.y + 2" class="n-name">{{ n.relatedName.slice(0, 4) }}</text>
        <text :x="n.x" :y="n.y + 38" class="n-note">{{ n.note.slice(0, 14) }}</text>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.pgraph { width: 100%; }
.pg-svg { width: 100%; height: auto; display: block; }

.pg-line { stroke-width: 1.4; stroke-dasharray: 5 4; opacity: 0.55; }
.pg-type { font-size: 11.5px; text-anchor: middle; paint-order: stroke; stroke: var(--card); stroke-width: 3px; }

.c-halo { fill: var(--seal); opacity: 0.1; }
.c-core { fill: var(--seal); }
.c-name { font-size: 24px; fill: #fbf6ea; text-anchor: middle; }
.c-sub { font-size: 10.5px; fill: rgba(251, 246, 234, 0.75); text-anchor: middle; }

.pg-node { cursor: pointer; }
.n-halo { fill: var(--primary); opacity: 0.1; transition: opacity 0.15s; }
.n-core { fill: var(--card); stroke: var(--primary); stroke-width: 1.4; }
.n-name { font-size: 13px; fill: var(--text); text-anchor: middle; font-weight: 600; }
.n-note { font-size: 10px; fill: var(--text-3); text-anchor: middle; }
.pg-node:hover .n-halo { opacity: 0.24; }
.pg-node:hover .n-core { fill: var(--card-hover); }
</style>
