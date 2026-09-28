<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { songApi, commentApi } from '../api'
import { usePlayerStore } from '../stores/player'
import { useUserStore } from '../stores/user'
import { openAddToPlaylist, confirmAction } from '../stores/ui'
import { formatDuration, formatDate, formatPlays } from '../utils/format'
import { toast } from '../utils/toast'
import SameSingerSongs from '../components/SameSingerSongs.vue'
import Icon from '../components/Icon.vue'

const route = useRoute()
const player = usePlayerStore()
const userStore = useUserStore()

const loading = ref(true)
const song = ref(null)
const comments = ref([])
const commentText = ref('')
const submitting = ref(false)
const liked = ref(false)

const isCurrent = computed(() => player.currentId === song.value?.id)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await songApi.detail(route.params.id)
    song.value = data
    await Promise.all([loadComments(), loadLikeState()])
  } catch {
    song.value = null
  } finally {
    loading.value = false
  }
})

async function loadComments() {
  const { data } = await commentApi.list(song.value.id)
  comments.value = [...data].sort((a, b) => b.createTime - a.createTime)
}

async function loadLikeState() {
  try {
    const { data } = await songApi.likeList()
    liked.value = data.some((s) => s.id === song.value.id)
  } catch { /* 未登录 */ }
}

function play() {
  player.playSong(song.value)
}

async function toggleLike() {
  if (!userStore.isLoggedIn) return toast.error('登录后即可收藏喜欢的歌曲')
  try {
    const { data } = await songApi.like(song.value.id)
    liked.value = data.liked
    toast.success(data.liked ? '已加入我喜欢的音乐' : '已取消喜欢')
  } catch (e) {
    toast.error(e.message)
  }
}

async function submitComment() {
  const content = commentText.value.trim()
  if (!content) return toast.error('说点什么吧~')
  if (!userStore.isLoggedIn) return toast.error('登录后才能评论')
  submitting.value = true
  try {
    await commentApi.add(song.value.id, content)
    commentText.value = ''
    toast.success('评论成功')
    await loadComments()
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function removeComment(c) {
  const yes = await confirmAction('确定删除这条评论吗？', '删除评论')
  if (!yes) return
  try {
    await commentApi.remove(c.id)
    toast.success('评论已删除')
    await loadComments()
  } catch (e) {
    toast.error(e.message)
  }
}
</script>

<template>
  <div class="page">
    <div v-if="loading" class="spinner" />
    <template v-else-if="song">
      <section class="head card">
        <div class="disc" :class="{ spinning: isCurrent && player.playing }">
          <Icon name="music" :size="34" />
        </div>
        <div class="info">
          <h1>{{ song.name }}</h1>
          <div class="meta-row">
            <router-link :to="`/singer/${song.singerId}`" class="singer">
              <Icon name="mic" :size="14" /> {{ song.singerName }}
            </router-link>
            <span class="tag" style="cursor: default">{{ song.style }}</span>
            <span class="m"><Icon name="clock" :size="14" /> {{ formatDuration(song.duration) }}</span>
            <span class="m"><Icon name="play" :size="12" /> {{ formatPlays(song.playCount) }} 次播放</span>
            <span class="m">发行于 {{ formatDate(song.createTime) }}</span>
          </div>
          <div class="acts">
            <button class="btn btn-primary" @click="play" data-test="song-play">
              <Icon :name="isCurrent && player.playing ? 'pause' : 'play'" :size="15" />
              {{ isCurrent && player.playing ? '暂停' : '播放' }}
            </button>
            <button class="btn btn-ghost" :class="{ liked }" @click="toggleLike" data-test="song-like">
              <Icon name="heart" :size="15" :style="liked ? 'fill: currentColor' : ''" />
              {{ liked ? '已喜欢' : '喜欢' }}
            </button>
            <button class="btn btn-ghost" @click="openAddToPlaylist(song)">
              <Icon name="plus" :size="15" /> 加入歌单
            </button>
            <a class="btn btn-ghost" :href="song.url" target="_blank" rel="noopener">
              <Icon name="download" :size="15" /> 下载
            </a>
          </div>
        </div>
      </section>

      <!-- 同歌手其他作品 -->
      <section class="block" v-if="song.singerId">
        <h2 class="page-title" style="margin-bottom: 14px">
          <Icon name="mic" :size="17" /> {{ song.singerName }}的其他作品
        </h2>
        <SameSingerSongs :singer-id="song.singerId" :exclude-id="song.id" />
      </section>
      <!-- 评论区 -->
      <section class="block">
        <h2 class="page-title" style="margin-bottom: 14px">
          <Icon name="users" :size="17" /> 评论（{{ comments.length }}）
        </h2>

        <div class="comment-editor" v-if="userStore.isLoggedIn">
          <img :src="userStore.user.avatar" class="c-avatar" alt="" />
          <div class="c-input-wrap">
            <textarea v-model="commentText" class="textarea" rows="2" maxlength="200"
              placeholder="说说你的听后感…" data-test="comment-input" />
            <div class="c-input-foot">
              <span class="counter">{{ commentText.length }}/200</span>
              <button class="btn btn-primary btn-sm" :disabled="submitting" @click="submitComment" data-test="comment-submit">
                {{ submitting ? '发布中…' : '发表评论' }}
              </button>
            </div>
          </div>
        </div>
        <div v-else class="login-tip">
          <router-link to="/login">登录</router-link> 后参与评论，聊聊这首歌
        </div>

        <div class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment">
            <img :src="c.avatar" class="c-avatar" alt="" />
            <div class="c-body">
              <div class="c-head">
                <span class="c-name">{{ c.username }}</span>
                <span class="c-time">{{ formatDate(c.createTime) }}</span>
                <button
                  v-if="userStore.user && (userStore.user.id === c.userId || userStore.isAdmin)"
                  class="c-del" title="删除" @click="removeComment(c)"
                ><Icon name="trash" :size="13" /></button>
              </div>
              <p class="c-content">{{ c.content }}</p>
            </div>
          </div>
          <div v-if="!comments.length" class="empty" style="padding: 30px 0">还没有评论，来抢沙发～</div>
        </div>
      </section>
    </template>
    <div v-else class="empty"><div class="empty-icon">🫥</div>歌曲不存在或已被删除</div>
  </div>
</template>

<style scoped>
.head { display: flex; gap: 26px; align-items: center; padding: 28px; }
.disc {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 10px 34px rgba(139, 92, 246, 0.4);
}
.spinning { animation: spin 6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.info { min-width: 0; }
.info h1 { margin: 0 0 12px; font-size: 24px; }
.meta-row { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 18px; }
.meta-row .m { display: inline-flex; align-items: center; gap: 5px; color: var(--text-3); font-size: 12.5px; }
.singer {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--primary-2);
  font-size: 13.5px;
  font-weight: 600;
}
.acts { display: flex; gap: 10px; flex-wrap: wrap; }
.acts .liked { color: var(--accent); border-color: rgba(236, 72, 153, 0.4); }
.block { margin-top: 28px; }

.comment-editor { display: flex; gap: 12px; margin-bottom: 22px; }
.c-avatar { width: 38px; height: 38px; border-radius: 50%; background: var(--bg-soft); flex-shrink: 0; }
.c-input-wrap { flex: 1; }
.c-input-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.counter { font-size: 12px; color: var(--text-3); }

.login-tip {
  background: var(--card);
  border: 1px dashed var(--border);
  border-radius: var(--radius);
  padding: 16px;
  text-align: center;
  color: var(--text-3);
  font-size: 13px;
  margin-bottom: 22px;
}

.comment-list { display: flex; flex-direction: column; gap: 16px; }
.comment { display: flex; gap: 12px; }
.c-body { flex: 1; background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; }
.c-head { display: flex; align-items: center; gap: 10px; }
.c-name { font-weight: 600; font-size: 13px; }
.c-time { font-size: 11.5px; color: var(--text-3); }
.c-del { margin-left: auto; border: none; background: none; color: var(--text-3); cursor: pointer; padding: 3px; border-radius: 6px; }
.c-del:hover { color: var(--danger); background: rgba(244, 63, 94, 0.1); }
.c-content { margin: 8px 0 0; font-size: 13.5px; line-height: 1.7; color: var(--text); }
</style>
