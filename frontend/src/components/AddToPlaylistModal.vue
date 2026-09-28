<script setup>
import { ref, watch, computed } from 'vue'
import { ui } from '../stores/ui'
import { playlistApi, playlistSongApi } from '../api'
import { useUserStore } from '../stores/user'
import { toast } from '../utils/toast'
import Icon from './Icon.vue'

const userStore = useUserStore()
const loading = ref(false)
const created = ref([])
const favorited = ref([])
const newPlaylistName = ref('')
const creating = ref(false)
const activeTarget = ref(null) // 选中的歌单 id

const open = computed(() => ui.addToPlaylist.visible)
const song = computed(() => ui.addToPlaylist.song)

watch(open, async (v) => {
  if (!v) return
  activeTarget.value = null
  newPlaylistName.value = ''
  await loadMine()
})

async function loadMine() {
  loading.value = true
  try {
    const { data } = await playlistApi.my()
    created.value = data.created
    favorited.value = data.favorited
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function createAndAdd() {
  const name = newPlaylistName.value.trim()
  if (!name) return toast.error('请输入歌单名')
  creating.value = true
  try {
    const { data } = await playlistApi.add({ name })
    await playlistSongApi.add(data.id, song.value.id)
    toast.success(`已创建歌单「${name}」并加入歌曲`)
    close()
  } catch (e) {
    toast.error(e.message)
  } finally {
    creating.value = false
  }
}

async function addTo(target) {
  try {
    await playlistSongApi.add(target.id, song.value.id)
    toast.success(`已加入「${target.name}」`)
    close()
  } catch (e) {
    toast.error(e.message)
  }
}

function close() {
  ui.addToPlaylist.visible = false
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-mask" @click.self="close">
      <div class="modal" data-test="add-playlist-modal">
        <h3 class="modal-title">把「{{ song?.name }}」加入歌单</h3>

        <div v-if="loading" class="spinner" />
        <template v-else>
          <div class="section" v-if="created.length">
            <div class="section-title">我的歌单</div>
            <div class="pl-scroll">
              <button v-for="p in created" :key="p.id" class="pl-item" @click="addTo(p)">
                <img :src="p.cover" class="pl-cover" alt="" />
                <div class="pl-meta">
                  <div class="pl-name">{{ p.name }}</div>
                  <div class="pl-sub">{{ p.songCount }} 首</div>
                </div>
                <Icon name="plus" :size="16" class="pl-plus" />
              </button>
            </div>
          </div>

          <div class="section" v-if="favorited.length">
            <div class="section-title">收藏的歌单</div>
            <div class="pl-scroll">
              <button v-for="p in favorited" :key="p.id" class="pl-item" @click="addTo(p)">
                <img :src="p.cover" class="pl-cover" alt="" />
                <div class="pl-meta">
                  <div class="pl-name">{{ p.name }}</div>
                  <div class="pl-sub">{{ p.songCount }} 首 · 创建者：{{ p.creatorName }}</div>
                </div>
                <Icon name="plus" :size="16" class="pl-plus" />
              </button>
            </div>
          </div>

          <div class="section" v-if="!created.length && !favorited.length">
            <div class="empty" style="padding: 20px 0">还没有可用歌单，新建一个吧</div>
          </div>

          <div class="new-row">
            <input v-model="newPlaylistName" class="input" placeholder="新建歌单名，如：跑步节奏" maxlength="30" />
            <button class="btn btn-primary" :disabled="creating" @click="createAndAdd">创建并加入</button>
          </div>
        </template>

        <div class="modal-actions">
          <button class="btn btn-ghost" @click="close">取消</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.section { margin-bottom: 14px; }
.section-title { font-size: 12.5px; color: var(--text-3); margin-bottom: 8px; }
.pl-scroll { max-height: 180px; overflow-y: auto; display: flex; flex-direction: column; gap: 4px; }
.pl-item {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid transparent;
  background: var(--card);
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
  color: var(--text);
  text-align: left;
}
.pl-item:hover { border-color: var(--primary); }
.pl-cover { width: 36px; height: 36px; border-radius: 8px; object-fit: cover; background: var(--bg-soft); }
.pl-meta { flex: 1; min-width: 0; }
.pl-name { font-size: 13.5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pl-sub { font-size: 11.5px; color: var(--text-3); }
.pl-plus { color: var(--text-3); }
.pl-item:hover .pl-plus { color: var(--primary-2); }
.new-row { display: flex; gap: 8px; margin-top: 6px; }
.new-row .input { flex: 1; }
</style>
