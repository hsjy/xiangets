<script setup>
import { usePlayerStore } from '../stores/player'
import { formatDuration } from '../utils/format'
import { confirmAction } from '../stores/ui'
import { toast } from '../utils/toast'
import Icon from './Icon.vue'

const player = usePlayerStore()

async function onClear() {
  const yes = await confirmAction(`确定清空播放列表（共 ${player.queue.length} 首）吗？`, '清空播放列表')
  if (yes) {
    player.clearQueue()
    toast.success('播放列表已清空')
  }
}
</script>

<template>
  <Teleport to="body">
    <!-- 用 CSS animation 做入场，不依赖 <transition> 的帧回调（后台标签页 rAF 停摆会让遮罩类残留） -->
    <div v-if="player.queueVisible" class="drawer-mask" @click.self="player.toggleQueue()">
      <div class="drawer">
        <div class="drawer-head">
          <h3>当前播放 <span class="count">({{ player.queue.length }})</span></h3>
          <div style="display: flex; gap: 4px">
            <button class="mini" v-if="player.queue.length" @click="onClear">清空</button>
            <button class="mini icon" @click="player.toggleQueue()"><Icon name="x" :size="15" /></button>
          </div>
        </div>
        <div class="drawer-body">
          <div v-if="!player.queue.length" class="empty" style="padding: 60px 0">
            <div class="empty-icon">🎵</div>
            播放列表还是空的<br />去挑几首喜欢的歌吧
          </div>
          <div
            v-for="(s, i) in player.queue" :key="s.id"
            class="q-item" :class="{ current: s.id === player.currentId }"
            @click="player.playSong(s)"
          >
            <span class="q-idx">{{ i + 1 }}</span>
            <div class="q-info">
              <div class="q-name">{{ s.name }}</div>
              <div class="q-singer">{{ s.singerName }}</div>
            </div>
            <span class="q-dur">{{ formatDuration(s.duration) }}</span>
            <button class="q-del" title="移除" @click.stop="player.removeFromQueue(s.id)">
              <Icon name="x" :size="14" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(46, 38, 24, 0.38);
  z-index: 200;
}
.drawer {
  position: absolute;
  right: 0;
  bottom: var(--player-h);
  top: 0;
  width: min(380px, 92vw);
  background: var(--panel);
  border-left: 1px solid var(--border);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  animation: drawer-in 0.25s ease;
}
@keyframes drawer-in {
  from { transform: translateX(48px); opacity: 0.4; }
  to { transform: none; opacity: 1; }
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid var(--border);
}
.drawer-head h3 { margin: 0; font-size: 16px; }
.count { color: var(--text-3); font-weight: 400; font-size: 13px; }
.mini {
  border: 1px solid var(--border);
  background: none;
  color: var(--text-2);
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}
.mini:hover { color: var(--text); border-color: var(--primary); }
.mini.icon { padding: 4px 6px; }
.drawer-body { flex: 1; overflow-y: auto; padding: 8px; }

.q-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.q-item:hover { background: rgba(64, 99, 77, 0.07); }
.q-item.current { background: var(--grad-soft); }
.q-item.current .q-name { color: var(--primary-2); }
.q-idx { width: 22px; text-align: center; color: var(--text-3); font-size: 12px; flex-shrink: 0; }
.q-info { flex: 1; min-width: 0; }
.q-name { font-size: 13.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.q-singer { font-size: 11.5px; color: var(--text-3); }
.q-dur { font-size: 11.5px; color: var(--text-3); font-variant-numeric: tabular-nums; }
.q-del {
  border: none;
  background: none;
  color: var(--text-3);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  opacity: 0;
}
.q-item:hover .q-del { opacity: 1; }
.q-del:hover { color: var(--danger); background: rgba(244, 63, 94, 0.1); }
</style>
