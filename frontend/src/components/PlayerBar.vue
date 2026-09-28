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
  <div class="player-bar" data-test="player-bar">
    <audio
      ref="audioRef"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onMeta"
      @ended="onEnded"
      @play="onPlay"
      @pause="onPause"
    />

    <!-- 左：当前歌曲 -->
    <div class="left">
      <template v-if="player.current">
        <router-link :to="`/song/${player.current.id}`" class="cover">
          <Icon name="music" :size="20" />
        </router-link>
        <div class="meta">
          <router-link :to="`/song/${player.current.id}`" class="name" data-test="player-song-name">
            {{ player.current.name }}
          </router-link>
          <router-link :to="`/singer/${player.current.singerId}`" class="singer">
            {{ player.current.singerName }}
          </router-link>
        </div>
        <button class="ctl like" :class="{ active: liked }" :title="liked ? '取消喜欢' : '喜欢'" @click="toggleLike">
          <Icon name="heart" :size="17" />
        </button>
      </template>
      <div v-else class="meta placeholder">
        <span>当前没有播放歌曲</span>
      </div>
    </div>

    <!-- 中：控制 + 进度 -->
    <div class="center">
      <div class="controls">
        <button class="ctl" title="上一首" data-test="prev" :disabled="!player.hasTrack" @click="player.prev()">
          <Icon name="skip-back" :size="18" />
        </button>
        <button class="ctl play" :title="player.playing ? '暂停' : '播放'" data-test="toggle"
          :disabled="!player.hasTrack" @click="player.toggle()">
          <Icon :name="player.playing ? 'pause' : 'play'" :size="22" />
        </button>
        <button class="ctl" title="下一首" data-test="next" :disabled="!player.hasTrack" @click="player.next()">
          <Icon name="skip-forward" :size="18" />
        </button>
      </div>
      <div class="progress-row">
        <span class="time" data-test="current-time">{{ formatDuration(player.currentTime) }}</span>
        <input
          class="slider progress" type="range" min="0" :max="player.duration || 100" step="0.1"
          :value="player.currentTime" :style="{ '--fill': progressFill + '%' }"
          :disabled="!player.hasTrack" @change="onSeek" data-test="progress"
        />
        <span class="time">{{ formatDuration(player.duration) }}</span>
      </div>
    </div>

    <!-- 右：模式 / 音量 / 队列 -->
    <div class="right">
      <button class="ctl" :title="modeInfo.label" data-test="mode" @click="player.cycleMode()">
        <Icon :name="modeInfo.icon" :size="17" />
      </button>
      <div class="vol">
        <button class="ctl" title="静音" data-test="mute" @click="player.toggleMute()">
          <Icon :name="player.muted || player.volume === 0 ? 'volume-x' : 'volume-2'" :size="17" />
        </button>
        <input
          class="slider vol-slider" type="range" min="0" max="1" step="0.01"
          :value="player.muted ? 0 : player.volume" :style="{ '--fill': volFill + '%' }"
          @input="(e) => player.setVolume(e.target.value)" data-test="volume"
        />
      </div>
      <button class="ctl" title="播放列表" data-test="queue-toggle" @click="player.toggleQueue()">
        <Icon name="queue" :size="17" />
        <span v-if="player.queue.length" class="badge">{{ player.queue.length }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.player-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: var(--player-h);
  background: rgba(18, 21, 29, 0.92);
  backdrop-filter: blur(16px);
  border-top: 1px solid var(--border);
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(360px, 2fr) minmax(180px, 1fr);
  align-items: center;
  gap: 16px;
  padding: 0 22px;
  z-index: 100;
}

.left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.cover {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  background: var(--grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.playing-animation .cover { animation: spin-cover 8s linear infinite; }
.meta { min-width: 0; }
.name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.singer { font-size: 12px; color: var(--text-3); }
.placeholder { color: var(--text-3); font-size: 13px; }

.center { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.controls { display: flex; align-items: center; gap: 18px; }
.progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 560px;
}
.time { font-size: 11.5px; color: var(--text-3); font-variant-numeric: tabular-nums; width: 38px; }
.time:first-child { text-align: right; }
.progress { flex: 1; }

.right { display: flex; align-items: center; justify-content: flex-end; gap: 10px; }
.vol { display: flex; align-items: center; gap: 4px; }
.vol-slider { width: 86px; }

.ctl {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.15s;
}
.ctl:hover:not(:disabled) { color: var(--text); background: rgba(255, 255, 255, 0.07); }
.ctl:disabled { opacity: 0.35; cursor: not-allowed; }
.ctl.play {
  width: 44px;
  height: 44px;
  background: var(--grad);
  color: #fff;
  box-shadow: 0 4px 14px rgba(139, 92, 246, 0.4);
}
.ctl.play:hover:not(:disabled) { filter: brightness(1.1); }
.ctl.like.active { color: var(--accent); }
.ctl.like.active svg { fill: currentColor; }
.badge {
  position: absolute;
  top: 2px;
  right: 0;
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
  .player-bar { grid-template-columns: 1fr 2fr 1fr; padding: 0 12px; gap: 8px; }
  .vol-slider { display: none; }
}
</style>
