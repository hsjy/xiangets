<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { playlistApi, playlistSongApi } from '../api'
import { usePlayerStore } from '../stores/player'
import { useUserStore } from '../stores/user'
import { confirmAction } from '../stores/ui'
import { formatDate } from '../utils/format'
import { toast } from '../utils/toast'
import SongTable from '../components/SongTable.vue'
import Icon from '../components/Icon.vue'

const route = useRoute()
const router = useRouter()
const player = usePlayerStore()
const userStore = useUserStore()

const loading = ref(true)
const playlist = ref(null)
const songs = ref([])
const editing = ref(false)
const form = ref({ name: '', style: '', intro: '' })

const isCreator = computed(() =>
  playlist.value && userStore.user && playlist.value.creatorId === userStore.user.id
)
const canManage = computed(() => isCreator.value || userStore.isAdmin)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const { data } = await playlistApi.detail(route.params.id)
    playlist.value = data
    const list = await playlistSongApi.list(route.params.id)
    songs.value = list.data
  } catch (e) {
    playlist.value = null
  } finally {
    loading.value = false
  }
}

function playAll() {
  if (songs.value.length) player.playSong(songs.value[0], songs.value)
  else toast.error('歌单里还没有歌曲')
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) return toast.error('登录后即可收藏歌单')
  try {
    const { data } = await playlistApi.favorite(playlist.value.id)
    playlist.value.favorited = data.favorited
    toast.success(data.favorited ? '已收藏歌单' : '已取消收藏')
  } catch (e) {
    toast.error(e.message)
  }
}

function startEdit() {
  form.value = {
    name: playlist.value.name,
    style: playlist.value.style,
    intro: playlist.value.intro
  }
  editing.value = true
}

async function saveEdit() {
  if (!form.value.name.trim()) return toast.error('歌单名不能为空')
  try {
    const { data } = await playlistApi.update(playlist.value.id, { ...form.value })
    Object.assign(playlist.value, data)
    editing.value = false
    toast.success('歌单信息已更新')
  } catch (e) {
    toast.error(e.message)
  }
}

async function removePlaylist() {
  const yes = await confirmAction(`确定删除歌单「${playlist.value.name}」吗？该操作不可恢复。`, '删除歌单')
  if (!yes) return
  try {
    await playlistApi.remove(playlist.value.id)
    toast.success('歌单已删除')
    router.push('/playlists')
  } catch (e) {
    toast.error(e.message)
  }
}

async function removeSong(song) {
  try {
    await playlistSongApi.remove(playlist.value.id, song.id)
    songs.value = songs.value.filter((s) => s.id !== song.id)
    playlist.value.songCount = songs.value.length
    toast.success(`已移除「${song.name}」`)
  } catch (e) {
    toast.error(e.message)
  }
}

async function moveSong(song, direction) {
  try {
    await playlistSongApi.order(playlist.value.id, song.id, direction)
    await load()
  } catch (e) {
    toast.error(e.message)
  }
}

const STYLES = ['我的收藏', '流行', '摇滚', '民谣', '电子', '古风', 'R&B', '学习', '通勤', '助眠']
</script>

<template>
  <div class="page">
    <div v-if="loading" class="spinner" />
    <template v-else-if="playlist">
      <section class="head">
        <img class="cover" :src="playlist.cover" :alt="playlist.name" />
        <div class="info">
          <p class="kind">
            <Icon name="list-music" :size="14" />
            {{ playlist.creatorId ? '用户歌单' : '官方歌单' }}
          </p>
          <h1 data-test="playlist-name">{{ playlist.name }}</h1>
          <div class="meta">
            <span class="tag" style="cursor: default">{{ playlist.style }}</span>
            <span>{{ playlist.songCount }} 首</span>
            <span v-if="playlist.creatorId">创建者：{{ playlist.creatorName }}</span>
            <span>创建于 {{ formatDate(playlist.createTime) }}</span>
          </div>
          <p class="intro" v-if="playlist.intro">{{ playlist.intro }}</p>
          <div class="acts">
            <button class="btn btn-primary" @click="playAll" data-test="playlist-play-all">
              <Icon name="play" :size="15" /> 播放全部
            </button>
            <button class="btn btn-ghost" :class="{ faved: playlist.favorited }" @click="toggleFavorite" data-test="playlist-favorite">
              <Icon name="heart" :size="15" :style="playlist.favorited ? 'fill: currentColor' : ''" />
              {{ playlist.favorited ? '已收藏' : '收藏' }}
            </button>
            <button v-if="canManage" class="btn btn-ghost" @click="startEdit">
              <Icon name="edit" :size="15" /> 编辑
            </button>
            <button v-if="canManage" class="btn btn-danger" @click="removePlaylist">
              <Icon name="trash" :size="15" /> 删除
            </button>
          </div>
        </div>
      </section>

      <!-- 编辑弹窗 -->
      <Teleport to="body">
        <div v-if="editing" class="modal-mask" @click.self="editing = false">
          <div class="modal">
            <h3 class="modal-title">编辑歌单</h3>
            <div class="form-item">
              <label class="form-label">歌单名</label>
              <input v-model="form.name" class="input" maxlength="30" />
            </div>
            <div class="form-item">
              <label class="form-label">风格标签</label>
              <select v-model="form.style" class="select">
                <option v-for="st in STYLES" :key="st" :value="st">{{ st }}</option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">简介</label>
              <textarea v-model="form.intro" class="textarea" maxlength="120" />
            </div>
            <div class="modal-actions">
              <button class="btn btn-ghost" @click="editing = false">取消</button>
              <button class="btn btn-primary" @click="saveEdit">保存</button>
            </div>
          </div>
        </div>
      </Teleport>

      <section class="list">
        <div class="list-head">
          <h2 class="page-title">歌曲列表</h2>
          <router-link to="/songs" class="btn btn-ghost btn-sm"><Icon name="plus" :size="14" /> 去歌曲库添加</router-link>
        </div>
        <SongTable :songs="songs" :removable="canManage" empty-text="歌单还是空的，去歌曲库挑几首吧"
          @remove="removeSong" />
        <!-- 排序操作提示 -->
        <p v-if="canManage && songs.length" class="order-tip">
          提示：进入「歌曲详情 → 加入歌单」可继续添加歌曲；播放列表抽屉（右下角）支持临时调整播放顺序。
        </p>
      </section>
    </template>
    <div v-else class="empty"><div class="empty-icon">🫥</div>歌单不存在或已被删除</div>
  </div>
</template>

<style scoped>
.head { display: flex; gap: 26px; margin-bottom: 26px; }
.cover {
  width: 180px;
  height: 180px;
  border-radius: 16px;
  object-fit: cover;
  background: var(--card);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  flex-shrink: 0;
}
.info { min-width: 0; display: flex; flex-direction: column; }
.kind {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-3);
}
.info h1 { margin: 8px 0 12px; font-size: 26px; }
.meta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; color: var(--text-3); font-size: 12.5px; margin-bottom: 12px; }
.intro { color: var(--text-2); font-size: 13.5px; line-height: 1.8; margin: 0 0 16px; max-width: 620px; }
.acts { display: flex; gap: 10px; flex-wrap: wrap; margin-top: auto; }
.acts .faved { color: var(--accent); border-color: rgba(166, 58, 43, 0.45); }
.list-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.order-tip { color: var(--text-3); font-size: 12px; margin-top: 12px; }
</style>
