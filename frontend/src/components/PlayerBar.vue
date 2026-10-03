<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePlayerStore } from '../stores/player'
import { formatDuration } from '../utils/format'
import { toast } from '../utils/toast'
import { songApi } from '../api'
import Icon from './Icon.vue'

const router = useRouter()
const player = usePlayerStore()
const audioRef = ref(null)
const liked = ref(false)
const likePending = ref(false)

onMounted(() => {
  player.attach(audioRef.value)
})

const modeInfo = computed(() => ({
  order: { icon: 'repeat', label: '列表循环' },
  random: { icon: 'shuffle', label: '随机播放' },
  single: { icon: 'repeat-1', label: '单曲循环' }
}[player.mode]))

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
  player.next(true) // 按播放模式自动切下一曲
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

  <!-- 浮动曲签：有曲可播才现身；点主体直达对应古诗页 -->
  <div
    v-if="player.hasTrack"
    class="player-bar"
    :class="{ clickable: !!player.current?.poemId }"
    :title="player.current?.poemId ? '查看此诗' : null"
    data-test="player-bar"
    @click="goPoem"
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

    <!-- 右：模式 / 音量 / 队列 -->
    <div class="right">
      <button class="ctl slim" :title="modeInfo.label" data-test="mode" @click="player.cycleMode()">
        <Icon :name="modeInfo.icon" :size="16" />
      </button>
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
  animation: player-rise 0.5s cubic-bezier(0.21, 0.9, 0.34, 1.12) both;
}
@keyframes player-rise {
  from { opacity: 0; transform: translate(-50%, 34px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}
.player-bar.clickable { cursor: pointer; }
.player-bar.clickable:hover { border-color: var(--primary); }

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
}
</style>
