<script setup>
/**
 * 人生行旅图：古风舆图风格的 SVG 单组件。
 * 底图为生成资产（china-outline.js，等经纬投影 1000x720），
 * 诗人落脚点按时间序连成朱砂虚线；地名旁小字标注该地所作诗题，
 * 点诗题直接进入意境页，点落脚点在看下方行止卡片。
 * 支持滚轮/双指缩放、拖拽平移；放大后淡入诗人朝代的行政区划
 * （唐·道 / 宋·路 / 东晋·州，示意界线，按需懒加载）。
 */
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { CHINA_MAP } from '../assets/china-outline'

const props = defineProps({
  journey: { type: Array, required: true },   // [{place, modern, lon, lat, period, event, poems:[id]}]
  poems: { type: Array, required: true },     // 该诗人存诗（用于把 id 解析成标题）
  dynasty: { type: String, default: '' }      // 诗人朝代（魏晋/唐/宋）→ 对应区划图层
})
const router = useRouter()
const selected = ref(-1)

const VIEW_W = CHINA_MAP.width
const VIEW_H = CHINA_MAP.height
const proj = (lon, lat) => [
  (lon - 73) / (135.2 - 73) * VIEW_W,
  (53.8 - lat) / (53.8 - 17.8) * VIEW_H
]

/* ---------------- 缩放与平移（操作 viewBox） ---------------- */

const MIN_W = 130, MAX_W = VIEW_W          // 最大约 7.7 倍
const vb = ref({ x: 0, y: 0, w: VIEW_W, h: VIEW_H })
const zoom = computed(() => VIEW_W / vb.value.w)        // 1 = 全图
const invK = computed(() => vb.value.w / VIEW_W)        // 叠加层反向缩放系数
const dynOn = ref(false)                                // 手动常显区划层
const REGION_ZOOM = 1.55                                // 放大到该倍率自动淡入区划
const regionShow = computed(() => dynOn.value || zoom.value >= REGION_ZOOM)

function clampVB(v) {
  v.w = Math.min(MAX_W, Math.max(MIN_W, v.w))
  v.h = v.w * VIEW_H / VIEW_W
  v.x = Math.min(VIEW_W - v.w, Math.max(0, v.x))
  v.y = Math.min(VIEW_H - v.h, Math.max(0, v.y))
  return v
}

/** 以 svg 坐标 (px,py) 为锚缩放 f 倍 */
function zoomAt(px, py, f) {
  const v = vb.value
  const w = Math.min(MAX_W, Math.max(MIN_W, v.w / f))
  const k = w / v.w
  vb.value = clampVB({ w, h: 0, x: px - (px - v.x) * k, y: py - (py - v.y) * k })
}

function svgPoint(e) {
  const r = svgEl.value.getBoundingClientRect()
  return [vb.value.x + (e.clientX - r.left) / r.width * vb.value.w,
          vb.value.y + (e.clientY - r.top) / r.height * vb.value.h]
}

function onWheel(e) {
  e.preventDefault()
  const d = e.deltaMode === 1 ? e.deltaY * 33 : e.deltaY
  const [px, py] = svgPoint(e)
  zoomAt(px, py, Math.exp(-d * 0.0022))
}

/* 指针：拖拽平移；双指针捏合缩放 */
const svgEl = ref(null)
const pointers = new Map()
let dragMoved = 0
let pinch = null

function onDown(e) {
  svgEl.value.setPointerCapture?.(e.pointerId)
  pointers.set(e.pointerId, [e.clientX, e.clientY])
  dragMoved = 0
  if (pointers.size === 2) {
    const [a, b] = [...pointers.values()]
    pinch = { dist: Math.hypot(a[0] - b[0], a[1] - b[1]) }
  }
}
function onMove(e) {
  if (!pointers.has(e.pointerId)) return
  const prev = pointers.get(e.pointerId)
  pointers.set(e.pointerId, [e.clientX, e.clientY])

  if (pointers.size === 2 && pinch) {
    const [a, b] = [...pointers.values()]
    const dist = Math.hypot(a[0] - b[0], a[1] - b[1])
    const mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2
    const r = svgEl.value.getBoundingClientRect()
    const px = vb.value.x + (mx - r.left) / r.width * vb.value.w
    const py = vb.value.y + (my - r.top) / r.height * vb.value.h
    zoomAt(px, py, dist / (pinch.dist || dist))
    pinch.dist = dist
    dragMoved = 99
    return
  }
  const r = svgEl.value.getBoundingClientRect()
  const dx = (e.clientX - prev[0]) / r.width * vb.value.w
  const dy = (e.clientY - prev[1]) / r.height * vb.value.h
  dragMoved += Math.abs(dx) + Math.abs(dy)
  vb.value.x -= dx
  vb.value.y -= dy
  clampVB(vb.value)
}
function onUp(e) {
  pointers.delete(e.pointerId)
  if (pointers.size < 2) pinch = null
}
/** 拖拽后抑制误触点击 */
function onClickGuard() { return dragMoved > 6 }

const dynLayer = ref(null)
const DYN_KEY = { 魏晋: 'dongjin', 唐: 'tang', 宋: 'song' }
watch(() => props.dynasty, async (d) => {
  dynLayer.value = null
  const key = DYN_KEY[d]
  if (!key) return
  try {
    const mod = await import(`../assets/china-dyn-${key}.js`)
    dynLayer.value = mod.DYNASTY_MAP
  } catch { /* 图层缺失不碍事 */ }
}, { immediate: true })

/* ---------------- 站点与标签 ---------------- */

const stops = computed(() => {
  const list = props.journey.map((s) => {
    const [x, y] = proj(s.lon, s.lat)
    const poems = (s.poems || [])
      .map((id) => props.poems.find((p) => p.id === id))
      .filter(Boolean)
    return {
      ...s,
      x, y,
      poems,
      // 旁注最多两首，其余收入下方行止卡片，保持地图整洁
      shown: poems.slice(0, 2)
    }
  })
  return placeLabels(list)
})

/**
 * 标签避让：地名+旁注诗题整块摆放，按「右→左→上→下」贪心试探，
 * 与已放置的标签块、落脚点、南海小图框都不冲突者为胜；
 * 数据里给了 lx/ly 的点视为手工指定，不再自动摆放。
 */
function placeLabels(list) {
  const blocks = []
  const inset = { x: CHINA_MAP.insetBox.x, y: CHINA_MAP.insetBox.y - 16,
                  w: CHINA_MAP.insetBox.w, h: CHINA_MAP.insetBox.h + 30 }

  for (const s of list) {
    const noW = 18, chW = 15.5, poemW = 12.5, lineH = 14.5
    const w = noW + s.place.length * chW
    const moreW = s.poems.length > s.shown.length ? 44 : 0   // 「…余 N 首」一行
    const h = 21 + s.shown.length * lineH + moreW
    const wMax = Math.max(w, ...s.shown.map((p) => 10 + (p.title.length + (p.subtitle ? p.subtitle.length + 1 : 0) + 2) * poemW))

    let lx, ly
    if (s.lx != null || s.ly != null) {
      lx = s.x + (s.lx ?? 11)
      ly = s.y + (s.ly ?? 5)
    } else {
      // 候选位（标签块左上角相对点心）：八方向 × 常用距离，逐个试探打分
      const L = -(wMax + 12), C = -wMax / 2
      const cands = [
        [11, 5], [11, 20], [11, -12],          // 右 / 右下 / 右上
        [C, 30], [C, -(h + 8)],                // 正下 / 正上
        [L, 5], [L, 20], [L, -12],             // 左 / 左下 / 左上
        [11, 38], [L, 38]                      // 更远的右下 / 左下（同址两站用）
      ]
      const score = (dx, dy) => {
        const r = { x: s.x + dx, y: s.y + dy - 15, w: wMax, h }
        let v = 0
        for (const b of blocks) {
          if (r.x < b.x + b.w && b.x < r.x + r.w && r.y < b.y + b.h && b.y < r.y + r.h) v += 2
        }
        for (const o of list) {
          if (o === s) continue
          // 块不得盖住其他落脚点：圆点半径约 4.5 加文字上伸部，余量取 8
          if (r.x - 8 < o.x && o.x < r.x + r.w + 8 && r.y - 8 < o.y && o.y < r.y + r.h + 8) v += 5
        }
        if (r.x < inset.x + inset.w && inset.x < r.x + r.w &&
            r.y < inset.y + inset.h && inset.y < r.y + r.h) v += 1
        if (r.x < 0 || r.x + r.w > 1000 || r.y < 0 || r.y + r.h > 720) v += 3
        return v
      }
      let bestV = Infinity
      for (const [dx, dy] of cands) {
        const v = score(dx, dy)
        if (v < bestV) { bestV = v; lx = s.x + dx; ly = s.y + dy }
      }
    }
    blocks.push({ x: lx, y: ly - 15, w: wMax, h })
    s.lx = lx
    s.ly = ly
    // 标签改为相对点心坐标，便于随缩放整体反向缩放
    s.lxr = lx - s.x
    s.lyr = ly - s.y
  }
  return list
}

/** 行旅路线：按时间序把落脚点连成平滑曲线 */
const routePath = computed(() => {
  const pts = stops.value
  if (pts.length < 2) return ''
  let d = `M${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)}`
  for (let i = 1; i < pts.length; i++) d += `L${pts[i].x.toFixed(1)} ${pts[i].y.toFixed(1)}`
  return d
})

const current = computed(() => stops.value[selected.value] || null)

function gotoPoem(id) {
  router.push(`/poem/${id}`)
}

function toggleSelect(i) {
  if (!onClickGuard()) selected.value = selected.value === i ? -1 : i
}

function resetView() {
  dynOn.value = false
  vb.value = { x: 0, y: 0, w: VIEW_W, h: VIEW_H }
}

/** 站点序号用汉字数字，与舆图气质一致 */
function cnNum(n) {
  const cn = '一二三四五六七八九十'
  if (n <= 10) return cn[n - 1]
  if (n < 20) return '十' + cn[n - 11]
  return String(n)
}

const cnNo = (n) => cnNum(n) + '\u2002'
</script>

<template>
  <div class="jmap">
    <div class="map-wrap">
    <svg ref="svgEl"
         :viewBox="`${vb.x} ${vb.y} ${vb.w} ${vb.h}`"
         class="map" role="img" aria-label="诗人行旅图"
         @wheel="onWheel" @pointerdown="onDown" @pointermove="onMove"
         @pointerup="onUp" @pointercancel="onUp"
         @click.capture="onClickGuard() && $event.stopPropagation()">
      <!-- 舆图底：陆域淡墨、江河淡青 -->
      <path :d="CHINA_MAP.land" class="land" />
      <path v-for="(r, i) in CHINA_MAP.rivers" :key="'r' + i" :d="r" class="river" />

      <!-- 朝代行政区划（放大淡入；界线系示意） -->
      <g v-if="dynLayer" class="dyn" :class="{ show: regionShow }" aria-hidden="true">
        <g v-for="(u, i) in dynLayer.units" :key="i" class="unit">
          <path :d="u.d" class="unit-land" :class="{ alt: i % 2 }" />
          <path :d="u.d" class="unit-edge" />
          <text :x="u.label[0]" :y="u.label[1]" class="unit-name"
                :font-size="13 * invK" :letter-spacing="2 * invK">{{ u.name }}</text>
        </g>
      </g>

      <!-- 南海诸岛小图框 -->
      <g class="inset">
        <rect :x="CHINA_MAP.insetBox.x" :y="CHINA_MAP.insetBox.y"
              :width="CHINA_MAP.insetBox.w" :height="CHINA_MAP.insetBox.h" class="inset-frame" />
        <path :d="CHINA_MAP.inset" class="land inset-land" />
        <text :x="CHINA_MAP.insetBox.x + CHINA_MAP.insetBox.w / 2"
              :y="CHINA_MAP.insetBox.y + CHINA_MAP.insetBox.h + 14"
              class="inset-label">南海诸岛</text>
      </g>

      <!-- 行旅路线 -->
      <path :d="routePath" class="route" />

      <!-- 落脚点（随缩放反向缩放，保持圆点与文字观感恒定） -->
      <g v-for="(s, i) in stops" :key="i" class="stop" :class="{ on: selected === i }"
         :transform="`translate(${s.x} ${s.y}) scale(${invK})`"
         @click="toggleSelect(i)">
        <circle r="10" class="halo" />
        <circle r="4.5" class="dot" />
        <!-- 地名 + 旁注诗题（每地至多两首，可点进诗；其余见行止卡片） -->
        <g class="labels">
          <text :x="s.lxr" :y="s.lyr" class="place">
            <tspan class="no">{{ cnNo(i + 1) }}</tspan>{{ s.place }}
          </text>
          <text v-for="(p, k) in s.shown" :key="k" class="poem"
                :x="s.lxr + 10" :y="s.lyr + 14 + k * 14"
                @click.stop="gotoPoem(p.id)">《{{ p.title }}{{ p.subtitle ? '·' + p.subtitle : '' }}》</text>
          <text v-if="s.poems.length > s.shown.length" class="poem more"
                :x="s.lxr + 10" :y="s.lyr + 14 + s.shown.length * 14">…余 {{ s.poems.length - s.shown.length }} 首</text>
        </g>
      </g>
    </svg>

    <!-- 缩放与图层控件 -->
    <div class="ctrl">
      <button class="cbtn" title="放大" @click="zoomAt(vb.x + vb.w / 2, vb.y + vb.h / 2, 1.5)" data-test="zoom-in">＋</button>
      <button class="cbtn" title="缩小" @click="zoomAt(vb.x + vb.w / 2, vb.y + vb.h / 2, 1 / 1.5)" data-test="zoom-out">－</button>
      <button class="cbtn" title="复位" @click="resetView()" data-test="zoom-reset">⌂</button>
      <button v-if="dynLayer" class="cbtn wide" :class="{ on: dynOn }"
              :title="`${dynLayer.title}常显`" @click="dynOn = !dynOn" data-test="dyn-toggle">
        {{ dynLayer.title }}
      </button>
    </div>
    <span class="tip" v-if="!dynOn && dynLayer">滚轮缩放 · 拖拽平移 · 放大后现{{ dynLayer.dynasty === 'tang' ? '唐道' : dynLayer.dynasty === 'song' ? '宋路' : '东晋州' }}</span>
    </div>

    <!-- 行止卡片 -->
    <div class="detail card" :class="{ empty: !current }">
      <template v-if="current">
        <h3>
          <span class="stamp">{{ current.place.slice(0, 1) }}</span>
          {{ current.place }}
          <small class="modern">{{ current.modern }}</small>
        </h3>
        <p class="period">{{ current.period }}</p>
        <p class="event">{{ current.event }}</p>
        <p class="dp" v-if="current.poems.length">
          <template v-for="(p, k) in current.poems" :key="p.id">
            <a @click.prevent="gotoPoem(p.id)" :href="`#/poem/${p.id}`">《{{ p.title }}{{ p.subtitle ? '·' + p.subtitle : '' }}》</a><span v-if="k < current.poems.length - 1"> · </span>
          </template>
        </p>
      </template>
      <p v-else class="hint">点地图上的地名，读诗人一程行止；旁注小字可直达该地所作之诗。</p>
    </div>
  </div>
</template>

<style scoped>
.jmap { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 18px; align-items: start; }
.map-wrap { min-width: 0; position: relative; }
.map { width: 100%; height: auto; display: block; border-radius: 10px; touch-action: none; cursor: grab; }
.map:active { cursor: grabbing; }

.land { fill: #e9e3d2; stroke: #b9ae94; stroke-width: 1; vector-effect: non-scaling-stroke; }
.inset-frame { fill: none; stroke: #b9ae94; stroke-width: 1; vector-effect: non-scaling-stroke; }
.inset-label { font-size: 11px; fill: var(--text-3); text-anchor: middle; letter-spacing: 2px; }
.river { fill: none; stroke: #9db4a5; stroke-width: 2; opacity: 0.65; stroke-linecap: round; vector-effect: non-scaling-stroke; }

/* 朝代区划层：淡入 */
.dyn { opacity: 0; transition: opacity 0.5s; pointer-events: none; }
.dyn.show { opacity: 1; }
.unit-land { fill: rgba(64, 99, 77, 0.05); stroke: none; }
.unit-land.alt { fill: rgba(184, 134, 11, 0.05); }
.unit-edge { fill: none; stroke: #a89c7e; stroke-width: 1; stroke-dasharray: 5 4; vector-effect: non-scaling-stroke; }
.unit-name {
  fill: #7a6f57; text-anchor: middle; font-weight: 600;
  paint-order: stroke; stroke: rgba(241, 234, 217, 0.8); stroke-width: 3px;
}

.route {
  fill: none;
  stroke: var(--seal);
  stroke-width: 2.2;
  stroke-dasharray: 7 6;
  stroke-linejoin: round;
  stroke-linecap: round;
  opacity: 0.75;
  vector-effect: non-scaling-stroke;
  animation: march 30s linear infinite;
}
@keyframes march { to { stroke-dashoffset: -650; } }

.stop { cursor: pointer; }
.halo { fill: var(--seal); opacity: 0.14; }
.dot { fill: var(--seal); stroke: var(--card); stroke-width: 1.5; }
.stop:hover .dot, .stop.on .dot { r: 6; }
.stop:hover .halo, .stop.on .halo { opacity: 0.24; }

.place { font-size: 15px; fill: var(--text); font-weight: 600; letter-spacing: 1px; paint-order: stroke; stroke: rgba(241, 234, 217, 0.85); stroke-width: 3px; }
.no { fill: var(--seal); font-weight: 700; margin-right: 2px; }
.poem {
  font-size: 11.5px;
  fill: var(--accent);
  cursor: pointer;
  paint-order: stroke;
  stroke: rgba(241, 234, 217, 0.85);
  stroke-width: 3px;
}
.poem:hover { fill: var(--seal); text-decoration: underline; }
.poem.more { color: var(--text-3); cursor: default; }
.poem.more:hover { fill: var(--text-3); text-decoration: none; }

.ctrl { position: absolute; right: 10px; top: 10px; display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
.cbtn {
  width: 30px; height: 30px;
  border: 1px solid var(--border-strong);
  border-radius: 7px;
  background: rgba(250, 246, 234, 0.92);
  color: var(--text-2);
  font-size: 15px;
  cursor: pointer;
  line-height: 1;
  backdrop-filter: blur(4px);
}
.cbtn:hover { color: var(--seal); border-color: var(--seal); }
.cbtn.wide { width: auto; padding: 0 10px; font-size: 11.5px; letter-spacing: 1px; }
.cbtn.on { background: var(--seal); border-color: var(--seal); color: #fbf6ea; }
.tip {
  position: absolute; left: 12px; bottom: 10px;
  font-size: 11px; color: var(--text-3); letter-spacing: 1px;
  background: rgba(250, 246, 234, 0.8); padding: 2px 8px; border-radius: 999px;
}

.detail { padding: 16px 18px; min-height: 180px; }
.detail.empty { display: flex; align-items: center; }
.hint { margin: 0; font-size: 13px; line-height: 2; color: var(--text-3); }
.detail h3 { display: flex; align-items: baseline; gap: 8px; margin: 0 0 6px; font-size: 17px; }
.modern { font-size: 11.5px; color: var(--text-3); font-weight: 400; }
.period { margin: 0 0 8px; font-size: 12px; color: var(--accent); }
.event { margin: 0 0 8px; font-size: 13.5px; line-height: 2; color: var(--text-2); }
.dp { margin: 0; font-size: 13px; }
.dp a { color: var(--accent); cursor: pointer; }
.dp a:hover { color: var(--seal); }
.stamp {
  width: 26px; height: 26px; border-radius: 5px;
  background: var(--seal); color: #fbf6ea;
  display: inline-flex; align-items: center; justify-content: center;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', serif; font-size: 16px;
}

@media (max-width: 1020px) {
  .jmap { grid-template-columns: 1fr; }
}
</style>
