<script setup>
/**
 * 诗人关系图谱：以当前诗人为中心、关系人为环的径向图。
 * 点击关系人 → 进入其诗人页；点击连线/关系标签 → 侧栏展开两人来往故事。
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  poet: { type: Object, required: true },   // {id, name, title, dynasty}
  relations: { type: Array, required: true } // [{relatedId, relatedName, relatedTitle, relatedDynasty, relType, note}]
})
const router = useRouter()

const W = 760, H = 420
const CX = W / 2, CY = H / 2
const RX = 268, RY = 148

const selected = ref(null)   // 被点选的关系 {relatedName, relType, note...}

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

function pickRelation(n) {
  // 点的是关系人节点则进入其页；点的是连线/标签则展开故事
  selected.value = selected.value?.relatedId === n.relatedId ? null : n
}
function go(id) { router.push(`/poet/${id}`) }
</script>

<template>
  <div class="pgraph">
    <svg :viewBox="`0 0 ${W} ${H}`" class="pg-svg" role="img" aria-label="诗人关系图谱">
      <!-- 连线：可见线 + 加宽透明命中区 -->
      <g v-for="(n, i) in nodes" :key="'l' + i" class="pg-edge" @click="pickRelation(n)">
        <line :x1="CX" :y1="CY" :x2="n.x" :y2="n.y"
              class="pg-line" :class="{ on: selected?.relatedId === n.relatedId }"
              :style="{ stroke: typeColor(n.relType) }" />
        <line :x1="CX" :y1="CY" :x2="n.x" :y2="n.y" class="pg-hit" />
      </g>
      <!-- 关系标签（线中点，可点） -->
      <g v-for="(n, i) in nodes" :key="'t' + i" class="pg-edge" @click="pickRelation(n)">
        <text :x="(CX + n.x) / 2" :y="(CY + n.y) / 2 - 6"
              class="pg-type" :class="{ on: selected?.relatedId === n.relatedId }"
              :style="{ fill: typeColor(n.relType) }">{{ n.relType }}</text>
      </g>

      <!-- 中心：当前诗人 -->
      <g class="pg-center">
        <circle :cx="CX" :cy="CY" r="46" class="c-halo" />
        <circle :cx="CX" :cy="CY" r="38" class="c-core" />
        <text :x="CX" :y="CY + 2" class="c-name font-brush">{{ poet.name }}</text>
        <text :x="CX" :y="CY + 24" class="c-sub">{{ poet.title || poet.dynasty }}</text>
      </g>

      <!-- 关系人 -->
      <g v-for="(n, i) in nodes" :key="'n' + i" class="pg-node"
         :class="{ sel: selected?.relatedId === n.relatedId }" @click="go(n.relatedId)">
        <circle :cx="n.x" :cy="n.y" r="30" class="n-halo" />
        <circle :cx="n.x" :cy="n.y" r="24" class="n-core" />
        <text :x="n.x" :y="n.y + 2" class="n-name">{{ n.relatedName.slice(0, 4) }}</text>
      </g>
    </svg>

    <!-- 关系故事：侧栏面板 -->
    <div class="pg-story card" v-if="selected" data-test="poet-story">
      <div class="ps-head">
        <span class="ps-type" :style="{ color: typeColor(selected.relType), borderColor: typeColor(selected.relType) }">{{ selected.relType }}</span>
        <span class="ps-pair">
          <b>{{ poet.name }}</b>
          <i class="ps-arrow">↔</i>
          <b>{{ selected.relatedName }}</b>
        </span>
        <button class="ps-close" @click="selected = null" title="收起">✕</button>
      </div>
      <p class="ps-note">{{ selected.note }}</p>
      <button class="btn btn-ghost btn-sm ps-go" @click="go(selected.relatedId)">
        走进{{ selected.relatedName }}的诗人页 →
      </button>
    </div>
  </div>
</template>

<style scoped>
.pgraph { display: grid; grid-template-columns: minmax(0, 1fr) 250px; gap: 14px; align-items: start; }
.pg-svg { width: 100%; height: auto; display: block; }

.pg-edge { cursor: pointer; }
.pg-line { stroke-width: 1.4; stroke-dasharray: 5 4; opacity: 0.5; transition: opacity 0.15s, stroke-width 0.15s; }
.pg-edge:hover .pg-line, .pg-line.on { opacity: 1; stroke-width: 2.4; stroke-dasharray: none; }
.pg-hit { stroke: transparent; stroke-width: 16; }
.pg-type { font-size: 11.5px; text-anchor: middle; paint-order: stroke; stroke: var(--card); stroke-width: 3px; cursor: pointer; opacity: 0.85; }
.pg-type.on, .pg-edge:hover .pg-type { opacity: 1; font-weight: 600; }

.c-halo { fill: var(--seal); opacity: 0.1; }
.c-core { fill: var(--seal); }
.c-name { font-size: 24px; fill: #fbf6ea; text-anchor: middle; }
.c-sub { font-size: 10.5px; fill: rgba(251, 246, 234, 0.75); text-anchor: middle; }

.pg-node { cursor: pointer; }
.n-halo { fill: var(--primary); opacity: 0.1; transition: opacity 0.15s; }
.n-core { fill: var(--card); stroke: var(--primary); stroke-width: 1.4; }
.n-name { font-size: 13px; fill: var(--text); text-anchor: middle; font-weight: 600; }
.pg-node:hover .n-halo, .pg-node.sel .n-halo { opacity: 0.24; }
.pg-node:hover .n-core { fill: var(--card-hover); }

/* 关系故事侧栏 */
.pg-story {
  padding: 16px;
  border-radius: 12px;
  background: var(--card);
  border: 1px solid var(--border-strong);
  animation: story-in 0.28s ease backwards;
}
@keyframes story-in { from { opacity: 0; transform: translateX(10px); } }
.ps-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
.ps-type {
  font-size: 11.5px; font-weight: 600;
  border: 1px solid; border-radius: 999px; padding: 2px 10px;
}
.ps-pair { font-size: 13.5px; color: var(--text); display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.ps-arrow { font-style: normal; color: var(--text-3); }
.ps-close { margin-left: auto; border: none; background: none; color: var(--text-3); cursor: pointer; }
.ps-close:hover { color: var(--seal); }
.ps-note { margin: 0 0 12px; font-size: 13.5px; line-height: 2.1; color: var(--text-2); }
.ps-go { border: none; }

@media (max-width: 1020px) {
  .pgraph { grid-template-columns: 1fr; }
}
</style>
