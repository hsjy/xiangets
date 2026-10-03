<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePlayerStore } from '../stores/player'
import { formatDuration } from '../utils/format'
import { toast } from '../utils/toast'
import { confirmAction } from '../stores/ui'
import { songApi } from '../api'
import Icon from './Icon.vue'

const router = useRouter()
const player = usePlayerStore()
const audioRef = ref(null)
const liked = ref(false)
const likePending = ref(false)

/* ---------- 闲置虚化：鼠标离开曲签数秒后淡出，悬停即恢复 ---------- */
const IDLE_MS = 3500
const faded = ref(false)
let fadeTimer = null
function armFade() {
  clearTimeout(fadeTimer)
  fadeTimer = setTimeout(() => { faded.value = true }, IDLE_MS)
}
function wake() {
  clearTimeout(fadeTimer)
  faded.value = false
  armFade()
}
function onBarEnter() { wake() }
function onBarLeave() { armFade() }
onMounted(() => {
  armFade()                       // 页面加载后无悬停，片刻即虚化
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => {
  clearTimeout(fadeTimer)
  document.removeEventListener('click', onDocClick)
})

/* ---------- 播放列表浮层（锚在列表图标上方，点外部关闭） ---------- */
function onDocClick(e) {
  if (!player.queueVisible) return
  if (e.target.closest('.queue-pop') || e.target.closest('[data-test="queue-toggle"]')) return
  player.queueVisible = false
}

async function onClearQueue() {
  const yes = await confirmAction(`确定清空播放列表（共 ${player.queue.length} 首）吗？`, '清空播放列表')
  if (yes) {
    player.clearQueue()
    toast.success('播放列表已清空')
  }
}

onMounted(() => {
  player.attach(audioRef.value)
})

const progressFill = computed(() =>
  player.duration ? (player.currentTime / player.duration) * 100 : 0
)
const volFill = computed(() => (player.muted ? 0 : player.volume * 100))

function onSeek(e) {
  player.seek(Number(e.target.value))
}

function onTimeUpdate() {
  player.currentTime = audioRef.value.currentTime
  if (audioRef.value.buffered.length) {
    player.buffered = audioRef.value.buffered.end(audioRef.value.buffered.length - 1)
  }
}
function onMeta() {
  player.duration = audioRef.value.duration || 0
}
function onEnded() {
  player.handleEnded() // 播一遍即停：不循环、不自动切下一曲
}
function onPlay() { player.playing = true }
function onPause() { player.playing = false }
function onError() {
  if (!player.current) return
  toast.error('音频加载失败——诗之音轨需连接后端（real 模式）体验')
  player.playing = false
}

/** 点击播放器主体（按钮/滑杆除外）→ 进入对应古诗意境页 */
function goPoem(e) {
  if (e.target.closest('button, input, a')) return
  const pid = player.current?.poemId
  if (pid) router.push(`/poem/${pid}`)
}

async function toggleLike() {
  if (!player.current) return
  const userStore = await import('../stores/user').then((m) => m.useUserStore())
  if (!userStore.isLoggedIn) {
    toast.error('登录后即可收藏喜欢的歌曲')
    return
  }
  if (likePending.value) return
  likePending.value = true
  try {
    const { data } = await songApi.like(player.current.id)
    liked.value = data.liked
    toast.success(data.liked ? '已加入我喜欢的音乐' : '已取消喜欢')
  } catch (e) {
    toast.error(e.message)
  } finally {
    likePending.value = false
  }
}

// 切歌时查询是否已喜欢
watch(() => player.currentId, async (id) => {
  liked.value = false
  if (!id) return
  try {
    const { data } = await songApi.likeList()
    liked.value = data.some((s) => s.id === id)
  } catch { /* 未登录时忽略 */ }
})
</script>

<template>
  <!-- <audio> 常驻：播放器 UI 的有无与音频实例生命周期解耦 -->
  <audio
    ref="audioRef"
    @timeupdate="onTimeUpdate"
    @loadedmetadata="onMeta"
    @ended="onEnded"
    @play="onPlay"
    @pause="onPause"
    @error="onError"
  />

  <!-- 浮动曲签：有曲可播才现身；点主体直达对应古诗页；闲置自动虚化 -->
  <div
    v-if="player.hasTrack"
    class="player-bar"
    :class="{ clickable: !!player.current?.poemId, faded }"
    :title="player.current?.poemId && !faded ? '查看此诗' : null"
    data-test="player-bar"
    @click="goPoem"
    @mouseenter="onBarEnter"
    @mouseleave="onBarLeave"
  >
    <!-- 左：曲目 -->
    <div class="left">
      <span class="cover" :class="{ spin: player.playing }">
        <Icon name="music" :size="18" />
      </span>
      <span class="meta">
        <span class="name" data-test="player-song-name">{{ player.current.name }}</span>
        <span class="singer">
          {{ player.current.singerName }}
          <i v-if="player.current.poemId" class="hint-arrow">· 查看此诗</i>
        </span>
      </span>
      <button class="ctl like" :class="{ active: liked }" :title="liked ? '取消喜欢' : '喜欢'" @click="toggleLike">
        <Icon name="heart" :size="16" />
      </button>
    </div>

    <!-- 中：控制 + 进度 -->
    <div class="center">
      <div class="controls">
        <button class="ctl slim" title="上一首" data-test="prev" @click="player.prev()">
          <Icon name="skip-back" :size="17" />
        </button>
        <button class="ctl play" :title="player.playing ? '暂停' : '播放'" data-test="toggle" @click="player.toggle()">
          <Icon :name="player.playing ? 'pause' : 'play'" :size="20" />
        </button>
        <button class="ctl slim" title="下一首" data-test="next" @click="player.next()">
          <Icon name="skip-forward" :size="17" />
        </button>
      </div>
      <div class="progress-row">
        <span class="time" data-test="current-time">{{ formatDuration(player.currentTime) }}</span>
        <input
          class="slider progress" type="range" min="0" :max="player.duration || 100" step="0.1"
          :value="player.currentTime" :style="{ '--fill': progressFill + '%' }"
          @change="onSeek" data-test="progress"
        />
        <span class="time">{{ formatDuration(player.duration) }}</span>
      </div>
    </div>

    <!-- 右：音量 / 队列 -->
    <div class="right">
      <div class="vol">
        <button class="ctl slim" title="静音" data-test="mute" @click="player.toggleMute()">
          <Icon :name="player.muted || player.volume === 0 ? 'volume-x' : 'volume-2'" :size="16" />
        </button>
        <input
          class="slider vol-slider" type="range" min="0" max="1" step="0.01"
          :value="player.muted ? 0 : player.volume" :style="{ '--fill': volFill + '%' }"
          @input="(e) => player.setVolume(e.target.value)" data-test="volume"
        />
      </div>
      <button class="ctl slim" title="播放列表" data-test="queue-toggle" @click="player.toggleQueue()">
        <Icon name="queue" :size="16" />
        <span v-if="player.queue.length" class="badge">{{ player.queue.length }}</span>
      </button>

      <!-- 播放列表浮层：锚在列表图标上方，与曲签同风格 -->
      <div v-if="player.queueVisible" class="queue-pop" data-test="queue-pop" @click.stop>
        <div class="qp-head">
          <span class="qp-title">当前播放 <i>{{ player.queue.length }}</i></span>
          <button class="qp-clear" v-if="player.queue.length" @click="onClearQueue">清空</button>
        </div>
        <div class="qp-body">
          <div v-if="!player.queue.length" class="qp-empty">播放列表还是空的<br />去挑几首喜欢的诗吧</div>
          <div
            v-for="(s, i) in player.queue" :key="s.id"
            class="qp-item" :class="{ current: s.id === player.currentId }"
            @click="player.playSong(s)"
          >
            <span class="qp-idx">{{ i + 1 }}</span>
            <span class="qp-info">
              <span class="qp-name">{{ s.name }}</span>
              <span class="qp-singer">{{ s.singerName }}</span>
            </span>
            <span class="qp-dur">{{ formatDuration(s.duration) }}</span>
            <button class="qp-del" title="移除" @click.stop="player.removeFromQueue(s.id)">
              <Icon name="x" :size="13" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.player-bar {
  position: fixed;
  left: 50%;
  bottom: 16px;
  transform: translateX(-50%);
  width: min(940px, calc(100vw - 28px));
  border-radius: 20px;
  background: rgba(250, 246, 234, 0.96);
  backdrop-filter: blur(18px);
  border: 1px solid var(--border-strong);
  box-shadow: 0 18px 48px rgba(72, 58, 32, 0.22), 0 2px 8px rgba(72, 58, 32, 0.08);
  display: grid;
  grid-template-columns: minmax(170px, 1fr) minmax(320px, 1.9fr) minmax(150px, 1fr);
  align-items: center;
  gap: 14px;
  padding: 10px 16px;
  z-index: 100;
  animation: player-rise 0.5s cubic-bezier(0.21, 0.9, 0.34, 1.12) backwards;
}
@keyframes player-rise {
  from { opacity: 0; transform: translate(-50%, 34px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}
.player-bar.clickable { cursor: pointer; }
.player-bar.clickable:hover { border-color: var(--primary); }
.player-bar.faded {
  opacity: 0.14;
  filter: blur(2.5px) saturate(0.7);
}
.player-bar {
  transition: opacity 0.6s ease, filter 0.6s ease, border-color 0.15s;
}
/* 虚化时禁掉内部交互，避免误触 */
.player-bar.faded * { pointer-events: none; }
.player-bar.faded { pointer-events: auto; }

.left { display: flex; align-items: center; gap: 11px; min-width: 0; }
.cover {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--grad);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 3px 10px rgba(64, 99, 77, 0.3), inset 0 0 0 3px rgba(250, 246, 234, 0.28);
}
.cover.spin { animation: disc-spin 9s linear infinite; }
@keyframes disc-spin { to { transform: rotate(360deg); } }
.meta { min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.singer { font-size: 11.5px; color: var(--text-3); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hint-arrow { font-style: normal; color: var(--accent); }

.center { display: flex; align-items: center; gap: 12px; }
.controls { display: flex; align-items: center; gap: 8px; }
.progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.time { font-size: 11px; color: var(--text-3); font-variant-numeric: tabular-nums; width: 36px; }
.time:first-child { text-align: right; }
.progress { flex: 1; min-width: 60px; }

.right { display: flex; align-items: center; justify-content: flex-end; gap: 4px; }
.vol { display: flex; align-items: center; gap: 2px; }
.vol-slider { width: 78px; }

.ctl {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.15s;
}
.ctl:hover { color: var(--primary); background: rgba(64, 99, 77, 0.1); }
.ctl.play {
  width: 40px;
  height: 40px;
  background: var(--grad);
  color: #fff;
  box-shadow: 0 4px 14px rgba(64, 99, 77, 0.32);
}
.ctl.play:hover { filter: brightness(1.1); background: var(--grad); }
.ctl.like.active { color: var(--accent); }
.ctl.like.active svg { fill: currentColor; }
.badge {
  position: absolute;
  top: 1px;
  right: -1px;
  background: var(--accent);
  color: #fff;
  border-radius: 999px;
  font-size: 10px;
  min-width: 15px;
  height: 15px;
  line-height: 15px;
  text-align: center;
  padding: 0 3px;
}

@media (max-width: 900px) {
  .player-bar {
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-rows: auto auto;
    row-gap: 2px;
    bottom: 10px;
    padding: 9px 12px;
    gap: 8px;
  }
  .center { grid-column: 1 / -1; }
  .progress-row .time { display: none; }
  .right .vol { display: none; }
  .right .slim:not([data-test='queue-toggle']) { display: none; }
  .like { display: none; }
  .queue-pop { right: 0; width: min(320px, calc(100vw - 24px)); }
}

/* ---------- 播放列表浮层 ---------- */
.queue-pop {
  position: absolute;
  right: 10px;
  bottom: calc(100% + 14px);
  width: 340px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
  border-radius: 18px;
  background: rgba(250, 246, 234, 0.97);
  backdrop-filter: blur(18px);
  border: 1px solid var(--border-strong);
  box-shadow: 0 18px 48px rgba(72, 58, 32, 0.24), 0 2px 8px rgba(72, 58, 32, 0.08);
  overflow: hidden;
  cursor: default;
  animation: qp-rise 0.32s cubic-bezier(0.21, 0.9, 0.34, 1.12) backwards;
}
@keyframes qp-rise {
  from { opacity: 0; transform: translateY(12px); }
}
.queue-pop::after {          /* 连接曲签的小尖角 */
  content: '';
  position: absolute;
  right: 26px;
  bottom: -6px;
  width: 12px;
  height: 12px;
  background: rgba(250, 246, 234, 0.97);
  border-right: 1px solid var(--border-strong);
  border-bottom: 1px solid var(--border-strong);
  transform: rotate(45deg);
}
.qp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 10px;
  border-bottom: 1px solid var(--border);
}
.qp-title { font-size: 14px; font-weight: 600; }
.qp-title i { font-style: normal; color: var(--accent); margin-left: 2px; }
.qp-clear {
  border: 1px solid var(--border-strong);
  background: none;
  border-radius: 999px;
  padding: 2px 12px;
  font-size: 12px;
  color: var(--text-2);
  cursor: pointer;
}
.qp-clear:hover { color: var(--seal); border-color: var(--seal); }
.qp-body { overflow-y: auto; padding: 6px 8px 10px; }
.qp-empty { padding: 44px 0; text-align: center; font-size: 12.5px; color: var(--text-3); line-height: 2; }
.qp-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
}
.qp-item:hover { background: rgba(64, 99, 77, 0.07); }
.qp-item.current { background: rgba(165, 64, 45, 0.08); }
.qp-item.current .qp-name { color: var(--seal); }
.qp-idx { font-size: 11px; color: var(--text-3); width: 18px; text-align: center; font-variant-numeric: tabular-nums; }
.qp-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.qp-name { font-size: 13px; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qp-singer { font-size: 11px; color: var(--text-3); }
.qp-dur { font-size: 11px; color: var(--text-3); font-variant-numeric: tabular-nums; }
.qp-del {
  border: none;
  background: none;
  color: var(--text-3);
  cursor: pointer;
  display: inline-flex;
  padding: 4px;
  border-radius: 50%;
}
.qp-del:hover { color: var(--seal); background: rgba(165, 64, 45, 0.1); }
</style>
