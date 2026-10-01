<script setup>
/**
 * 通用歌曲列表（表格视图）：供歌曲库、歌单详情、歌曲详情、我的喜欢复用
 */
import { usePlayerStore } from '../stores/player'
import { openAddToPlaylist } from '../stores/ui'
import { formatDuration, formatPlays } from '../utils/format'
import Icon from './Icon.vue'

const props = defineProps({
  songs: { type: Array, default: () => [] },
  showPlays: { type: Boolean, default: true },
  removable: { type: Boolean, default: false }, // 显示"从歌单移除"按钮
  emptyText: { type: String, default: '暂无歌曲' }
})
const emit = defineEmits(['remove'])

const player = usePlayerStore()

function playRow(song) {
  player.playSong(song, props.songs)
}

function fmtTime(ts) {
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<template>
  <div class="table-wrap">
    <table class="song-table" v-if="songs.length" data-test="song-table">
      <thead>
        <tr>
          <th class="col-index">#</th>
          <th>歌曲</th>
          <th>歌手</th>
          <th>风格</th>
          <th v-if="showPlays" class="col-plays">播放量</th>
          <th class="col-duration">时长</th>
          <th style="width: 110px; text-align: center">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(s, i) in songs" :key="s.id"
          :class="{ playing: s.id === player.currentId }"
          @dblclick="playRow(s)"
        >
          <td class="col-index">
            <span v-if="s.id !== player.currentId" class="idx">{{ i + 1 }}</span>
            <Icon v-else name="volume-2" :size="14" class="eq" />
          </td>
          <td class="song-cell">
            <router-link :to="`/song/${s.id}`" class="song-name" @click.stop>{{ s.name }}</router-link>
          </td>
          <td>
            <router-link :to="`/singer/${s.singerId}`" class="singer-link" @click.stop>
              {{ s.singerName }}
            </router-link>
          </td>
          <td><span class="tag" style="cursor: default">{{ s.style }}</span></td>
          <td v-if="showPlays" class="col-plays">{{ formatPlays(s.playCount) }}</td>
          <td class="col-duration">{{ formatDuration(s.duration) }}</td>
          <td class="actions">
            <button class="act play" title="播放" @click.stop="playRow(s)"><Icon name="play" :size="14" /></button>
            <button class="act" title="加入歌单" @click.stop="openAddToPlaylist(s)"><Icon name="plus" :size="14" /></button>
            <button v-if="removable" class="act danger" title="从歌单移除" @click.stop="emit('remove', s)">
              <Icon name="trash" :size="14" />
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty">
      <div class="empty-icon">🎧</div>{{ emptyText }}
    </div>
  </div>
</template>

<style scoped>
.table-wrap { overflow-x: auto; }
.song-cell { min-width: 140px; }
.song-name { color: var(--text); font-weight: 500; }
.song-name:hover { color: var(--primary-2); }
.singer-link { color: var(--text-2); }
.singer-link:hover { color: var(--primary-2); }
tbody tr.playing { background: var(--grad-soft); }
tbody tr.playing .song-name { color: var(--primary-2); }
.eq { color: var(--primary-2); animation: eq 1s ease infinite; }
@keyframes eq { 50% { opacity: 0.35; } }

.actions { text-align: center; white-space: nowrap; }
.act {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-3);
  cursor: pointer;
  margin: 0 2px;
  opacity: 0;
  transition: all 0.15s;
}
tr:hover .act { opacity: 1; }
.act:hover { color: var(--primary); background: rgba(64, 99, 77, 0.1); }
.act.play { color: var(--primary-2); }
.act.play:hover { background: var(--grad); color: #fff; }
.act.danger:hover { color: var(--danger); background: rgba(244, 63, 94, 0.12); }
@media (hover: none) { .act { opacity: 1; } }
</style>
