<script setup>
import { ref, onMounted } from 'vue'
import { playlistApi } from '../api'
import { useUserStore } from '../stores/user'
import { confirmAction } from '../stores/ui'
import { toast } from '../utils/toast'
import PlaylistCard from '../components/PlaylistCard.vue'
import Icon from '../components/Icon.vue'

const userStore = useUserStore()
const loading = ref(true)
const created = ref([])
const favorited = ref([])
const browse = ref([])
const showCreate = ref(false)
const form = ref({ name: '', style: '我的收藏', intro: '' })
const creating = ref(false)

const ALL_STYLES = ['我的收藏', '流行', '摇滚', '民谣', '电子', '古风', 'R&B', '学习', '通勤', '助眠']

async function load() {
  loading.value = true
  try {
    const [mine, all] = await Promise.all([playlistApi.my(), playlistApi.list()])
    created.value = mine.data.created
    favorited.value = mine.data.favorited
    browse.value = all.data
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!form.value.name.trim()) return toast.error('请输入歌单名')
  creating.value = true
  try {
    await playlistApi.add({ ...form.value })
    toast.success('歌单创建成功')
    showCreate.value = false
    form.value = { name: '', style: '我的收藏', intro: '' }
    await load()
  } catch (e) {
    toast.error(e.message)
  } finally {
    creating.value = false
  }
}

async function removePl(p) {
  const yes = await confirmAction(`确定删除歌单「${p.name}」吗？其中的歌曲引用关系将一并清除。`, '删除歌单')
  if (!yes) return
  try {
    await playlistApi.remove(p.id)
    toast.success('歌单已删除')
    await load()
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的歌单</h1>
        <p class="page-sub">创建的歌单 {{ created.length }} 个 · 收藏的歌单 {{ favorited.length }} 个</p>
      </div>
      <button class="btn btn-primary" @click="showCreate = true" data-test="create-playlist">
        <Icon name="plus" :size="15" /> 新建歌单
      </button>
    </div>

    <div v-if="loading" class="spinner" />
    <template v-else>
      <section class="block">
        <h2 class="sec-title">🎧 我创建的歌单</h2>
        <div v-if="created.length" class="grid-cards">
          <div v-for="p in created" :key="p.id" class="pl-wrap">
            <PlaylistCard :playlist="p" />
            <button class="btn btn-danger btn-sm del-btn" @click="removePl(p)">
              <Icon name="trash" :size="13" /> 删除
            </button>
          </div>
        </div>
        <div v-else class="empty" style="padding: 30px 0">还没有创建歌单，点击右上角「新建歌单」开始吧</div>
      </section>

      <section class="block" v-if="favorited.length">
        <h2 class="sec-title">⭐ 收藏的歌单</h2>
        <div class="grid-cards">
          <PlaylistCard v-for="p in favorited" :key="p.id" :playlist="p" />
        </div>
      </section>

      <section class="block">
        <h2 class="sec-title">🌐 全站歌单</h2>
        <div class="grid-cards">
          <PlaylistCard v-for="p in browse" :key="p.id" :playlist="p" />
        </div>
      </section>
    </template>

    <!-- 新建歌单弹窗 -->
    <Teleport to="body">
      <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
        <div class="modal" data-test="create-playlist-modal">
          <h3 class="modal-title">新建歌单</h3>
          <div class="form-item">
            <label class="form-label">歌单名</label>
            <input v-model="form.name" class="input" maxlength="30" placeholder="如：跑步节奏 / 深夜emo" data-test="new-playlist-name" />
          </div>
          <div class="form-item">
            <label class="form-label">风格标签</label>
            <select v-model="form.style" class="select">
              <option v-for="st in ALL_STYLES" :key="st" :value="st">{{ st }}</option>
            </select>
          </div>
          <div class="form-item">
            <label class="form-label">简介（可选）</label>
            <textarea v-model="form.intro" class="textarea" maxlength="120" placeholder="一句话介绍这个歌单" />
          </div>
          <div class="modal-actions">
            <button class="btn btn-ghost" @click="showCreate = false">取消</button>
            <button class="btn btn-primary" :disabled="creating" @click="create" data-test="create-playlist-submit">
              {{ creating ? '创建中…' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.block { margin-bottom: 30px; }
.sec-title { font-size: 16px; font-weight: 700; margin: 0 0 14px; }
.pl-wrap { position: relative; }
.del-btn {
  position: absolute;
  top: 8px;
  left: 8px;
  opacity: 0;
  transition: opacity 0.15s;
  background: rgba(46, 38, 24, 0.65);
  border-color: rgba(127, 29, 29, 0.5);
}
.pl-wrap:hover .del-btn { opacity: 1; }
</style>
