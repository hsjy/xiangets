<script setup>
/**
 * 好友：搜索加友 / 请求管理 / 好友列表 / 私信聊天 / 关注
 * 会话框采用古风设计：宣纸底、墨色边框、书法字体标题
 */
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { friendApi, followApi } from '../api'
import { useUserStore } from '../stores/user'
import { toast } from '../utils/toast'
import UserAvatar from '../components/UserAvatar.vue'
import { formatDate } from '../utils/format'

const userStore = useUserStore()
const tab = ref('friends')    // friends | incoming | outgoing | following | followers
const searchKey = ref('')
const searchResults = ref([])
const searching = ref(false)

const friends = ref([])
const incoming = ref([])
const outgoing = ref([])
const following = ref([])
const followers = ref([])
const loading = ref(true)

/* 聊天 */
const activeFriend = ref(null)
const messages = ref([])
const chatText = ref('')
const chatRef = ref(null)
const sending = ref(false)

const tabs = [
  { id: 'friends', label: '好友' },
  { id: 'incoming', label: '请求' },
  { id: 'outgoing', label: '已发出' },
  { id: 'following', label: '关注' },
  { id: 'followers', label: '粉丝' }
]

async function load() {
  loading.value = true
  try {
    const [f, inc, out, fol, fols] = await Promise.allSettled([
      friendApi.list(), friendApi.incoming(), friendApi.outgoing(),
      followApi.following(), followApi.followers()
    ])
    if (f.status === 'fulfilled') friends.value = f.value.data
    if (inc.status === 'fulfilled') incoming.value = inc.value.data
    if (out.status === 'fulfilled') outgoing.value = out.value.data
    if (fol.status === 'fulfilled') following.value = fol.value.data
    if (fols.status === 'fulfilled') followers.value = fols.value.data
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function doSearch() {
  const k = searchKey.value.trim()
  if (!k) return
  searching.value = true
  try {
    const { data } = await friendApi.search(k)
    searchResults.value = data
  } catch (e) {
    toast.error(e.message)
  } finally {
    searching.value = false
  }
}

async function addFriend(username) {
  try {
    await friendApi.request(username)
    toast.success(`已向「${username}」发送好友请求`)
    searchResults.value = searchResults.value.map(r =>
      r.username === username ? { ...r, relation: 'pending' } : r)
  } catch (e) { toast.error(e.message) }
}

async function respond(relId, accept) {
  try {
    await friendApi.respond(relId, accept)
    toast.success(accept ? '已同意' : '已拒绝')
    await load()
  } catch (e) { toast.error(e.message) }
}

async function removeFriend(relId) {
  if (!confirm('确定删除这位好友吗？')) return
  try {
    await friendApi.remove(relId)
    toast.success('已删除好友')
    if (activeFriend.value && friends.value.find(f => f.relId === relId)?.friendId === activeFriend.value.friendId) {
      activeFriend.value = null
      messages.value = []
    }
    await load()
  } catch (e) { toast.error(e.message) }
}

/* 关注 */
async function toggleFollow(u) {
  try {
    const { data } = await followApi.toggle(u.id)
    u.following = data.following
    toast.success(data.following ? `已关注 ${u.name}` : `已取消关注 ${u.name}`)
  } catch (e) { toast.error(e.message) }
}

/* 聊天 */
async function openChat(friend) {
  activeFriend.value = friend
  try {
    const { data } = await friendApi.messages(friend.friendId)
    messages.value = data.messages
    friend.unread = data.unread
  } catch (e) { toast.error(e.message) }
  await nextTick()
  scrollChat()
}
function scrollChat() {
  if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
}
async function sendMsg() {
  const content = chatText.value.trim()
  if (!content || sending.value || !activeFriend.value) return
  sending.value = true
  try {
    await friendApi.sendMessage(activeFriend.value.friendId, content)
    chatText.value = ''
    const { data } = await friendApi.messages(activeFriend.value.friendId)
    messages.value = data.messages
    await nextTick()
    scrollChat()
  } catch (e) { toast.error(e.message) } finally { sending.value = false }
}

const pendingCount = computed(() => incoming.value.length)
</script>

<template>
  <div class="page friends-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">好友</h1>
        <p class="page-sub">以诗会友 · 搜索用户名添加，互相关注后可私信</p>
      </div>
    </div>

    <div v-if="loading" class="spinner" />
    <template v-else>
      <!-- 搜索加友 -->
      <div class="search-bar card">
        <input
          v-model.trim="searchKey" class="input"
          placeholder="输入用户名搜索…" data-test="friend-search"
          @keyup.enter="doSearch"
        />
        <button class="btn btn-primary btn-sm" :disabled="searching" @click="doSearch">搜索</button>
      </div>

      <div v-if="searchResults.length" class="search-results card">
        <div v-for="u in searchResults" :key="u.id" class="sr-item">
          <UserAvatar :name="u.name" :size="34" circle />
          <div class="sr-info">
            <span class="sr-name">{{ u.name }}</span>
            <span class="sr-uname">@{{ u.username }}</span>
          </div>
          <button v-if="u.relation === 'none'" class="btn btn-ghost btn-xs" @click="addFriend(u.username)">加好友</button>
          <span v-else-if="u.relation === 'pending'" class="sr-status">已请求</span>
          <span v-else class="sr-status ok">已是好友</span>
          <button class="btn btn-ghost btn-xs follow-btn" @click="toggleFollow(u)" :class="{ on: u.following }">
            {{ u.following ? '✓ 已关注' : '+ 关注' }}
          </button>
        </div>
      </div>

      <!-- Tab -->
      <div class="tabs">
        <button v-for="t in tabs" :key="t.id"
          class="tab-btn" :class="{ on: tab === t.id }" @click="tab = t.id"
        >
          {{ t.label }}
          <span v-if="t.id === 'incoming' && pendingCount" class="tab-badge">{{ pendingCount }}</span>
        </button>
      </div>

      <!-- 列表 + 聊天 -->
      <div class="friends-layout">
        <div class="friend-list card">
          <!-- 好友 -->
          <template v-if="tab === 'friends'">
            <div v-if="!friends.length" class="empty-hint">暂无好友——搜索用户名添加吧</div>
            <div v-for="f in friends" :key="f.relId" class="friend-item"
              :class="{ active: activeFriend?.friendId === f.friendId }"
              @click="openChat(f)"
            >
              <UserAvatar :name="f.name" :size="38" circle />
              <div class="fi-info">
                <span class="fi-name">{{ f.name }}</span>
                <span class="fi-uname">@{{ f.username }}</span>
              </div>
              <span v-if="f.unread" class="fi-badge">{{ f.unread }}</span>
            </div>
          </template>

          <!-- 待处理请求 -->
          <template v-if="tab === 'incoming'">
            <div v-if="!incoming.length" class="empty-hint">暂无待处理请求</div>
            <div v-for="req in incoming" :key="req.relId" class="friend-item">
              <UserAvatar :name="req.name" :size="38" circle />
              <div class="fi-info">
                <span class="fi-name">{{ req.name }}</span>
                <span class="fi-uname">@{{ req.username }} · {{ formatDate(req.requestTime) }}</span>
              </div>
              <div class="fi-actions">
                <button class="btn btn-primary btn-xs" @click="respond(req.relId, true)">同意</button>
                <button class="btn btn-ghost btn-xs" @click="respond(req.relId, false)">拒绝</button>
              </div>
            </div>
          </template>

          <!-- 已发出 -->
          <template v-if="tab === 'outgoing'">
            <div v-if="!outgoing.length" class="empty-hint">暂无已发出的请求</div>
            <div v-for="req in outgoing" :key="req.relId" class="friend-item">
              <UserAvatar :name="req.name" :size="38" circle />
              <div class="fi-info">
                <span class="fi-name">{{ req.name }}</span>
                <span class="fi-uname">@{{ req.username }} · {{ formatDate(req.requestTime) }}</span>
              </div>
              <span class="sr-status">等待中</span>
            </div>
          </template>

          <!-- 关注 -->
          <template v-if="tab === 'following'">
            <div v-if="!following.length" class="empty-hint">还没有关注任何人</div>
            <div v-for="u in following" :key="u.id" class="friend-item">
              <UserAvatar :name="u.name" :size="36" circle />
              <div class="fi-info">
                <span class="fi-name">{{ u.name }}</span>
                <span class="fi-uname">@{{ u.username }}</span>
              </div>
            </div>
          </template>

          <!-- 粉丝 -->
          <template v-if="tab === 'followers'">
            <div v-if="!followers.length" class="empty-hint">还没有粉丝</div>
            <div v-for="u in followers" :key="u.id" class="friend-item">
              <UserAvatar :name="u.name" :size="36" circle />
              <div class="fi-info">
                <span class="fi-name">{{ u.name }}</span>
                <span class="fi-uname">@{{ u.username }}</span>
              </div>
            </div>
          </template>
        </div>

        <!-- 古风会话框 -->
        <div class="chat-panel card" v-if="activeFriend">
          <div class="cp-head">
            <UserAvatar :name="activeFriend.name" :size="34" circle />
            <div class="cp-title">
              <span class="cp-name font-brush">{{ activeFriend.name }}</span>
              <span class="cp-seal">笺</span>
            </div>
            <button class="cp-close" @click="activeFriend = null">✕</button>
          </div>
          <div ref="chatRef" class="cp-body" data-test="chat-body">
            <div v-for="(m, i) in messages" :key="m.id"
              class="msg" :class="{ mine: m.senderId === userStore.user?.id }"
            >
              <div class="msg-bubble">
                <p class="msg-text">{{ m.content }}</p>
              </div>
            </div>
            <div v-if="!messages.length" class="empty-hint">写条消息，以诗会友</div>
          </div>
          <form class="cp-foot" @submit.prevent="sendMsg">
            <input v-model="chatText" class="input cp-input" maxlength="500"
              placeholder="写点什么…" data-test="chat-input"
            />
            <button class="btn btn-primary btn-sm cp-send" :disabled="sending" data-test="chat-send">发送</button>
          </form>
        </div>
        <div v-else class="chat-empty card">
          <div class="ce-icon font-brush">笺</div>
          <p>选择一位好友，以诗会友</p>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.search-bar { display: flex; gap: 10px; padding: 12px 16px; margin-bottom: 12px; align-items: center; }
.search-bar .input { flex: 1; }

.search-results { padding: 10px 14px; margin-bottom: 16px; max-height: 200px; overflow-y: auto; }
.sr-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--border); }
.sr-item:last-child { border-bottom: none; }
.sr-info { flex: 1; display: flex; flex-direction: column; }
.sr-name { font-size: 14px; font-weight: 600; }
.sr-uname { font-size: 12px; color: var(--text-3); }
.sr-status { font-size: 12.5px; color: var(--text-3); }
.sr-status.ok { color: var(--accent); }
.follow-btn { font-size: 11.5px; padding: 3px 8px; }
.follow-btn.on { color: var(--accent); border-color: var(--accent); }

.tabs { display: flex; gap: 6px; margin-bottom: 16px; flex-wrap: wrap; }
.tab-btn {
  position: relative;
  border: 1px solid var(--border-strong); background: var(--card);
  color: var(--text-2); border-radius: 999px; padding: 6px 16px;
  font-size: 13px; cursor: pointer; transition: all 0.15s;
}
.tab-btn.on { background: var(--primary); border-color: var(--primary); color: #fbf6ea; }
.tab-badge {
  position: absolute; top: -4px; right: -4px;
  background: var(--seal); color: #fbf6ea;
  border-radius: 999px; font-size: 10px; min-width: 16px; height: 16px;
  line-height: 16px; text-align: center; padding: 0 3px;
}

.friends-layout { display: grid; grid-template-columns: 300px 1fr; gap: 14px; align-items: start; }
.friend-list { padding: 8px; max-height: 520px; overflow-y: auto; }
.friend-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 8px; cursor: pointer;
  transition: background 0.12s;
}
.friend-item:hover { background: var(--card-hover); }
.friend-item.active { background: var(--grad-soft); }
.fi-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.fi-name { font-size: 14px; font-weight: 600; }
.fi-uname { font-size: 11.5px; color: var(--text-3); }
.fi-badge {
  background: var(--seal); color: #fbf6ea;
  border-radius: 999px; font-size: 10px; min-width: 16px; height: 16px;
  line-height: 16px; text-align: center; padding: 0 3px;
}
.fi-actions { display: flex; gap: 6px; }

/* ===== 古风会话框 ===== */
.chat-panel {
  display: flex; flex-direction: column; height: 520px;
  background: var(--panel);
  border: 1px solid var(--border-strong);
  border-radius: 12px;
  overflow: hidden;
}
.cp-head {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border);
  background: linear-gradient(180deg, rgba(184, 134, 11, 0.06), transparent);
}
.cp-title { display: flex; align-items: center; gap: 6px; }
.cp-name { font-size: 16px; letter-spacing: 2px; }
.cp-seal {
  width: 20px; height: 20px;
  border-radius: 3px;
  background: var(--seal); color: #fbf6ea;
  font-size: 12px;
  display: inline-flex; align-items: center; justify-content: center;
  font-family: 'Ma Shan Zheng', 'LXGW WenKai', serif;
}
.cp-close { margin-left: auto; border: none; background: none; color: var(--text-3); cursor: pointer; font-size: 15px; }
.cp-close:hover { color: var(--seal); }

.cp-body {
  flex: 1; overflow-y: auto; padding: 16px 18px;
  display: flex; flex-direction: column; gap: 12px;
}
.msg { display: flex; }
.msg.mine { justify-content: flex-end; }
.msg-bubble {
  max-width: 72%;
  padding: 10px 16px;
  border-radius: 4px 12px 12px 12px;
  background: var(--card);
  border: 1px solid var(--border);
  position: relative;
}
.msg.mine .msg-bubble {
  border-radius: 12px 4px 12px 12px;
  background: var(--primary);
  border-color: var(--primary);
}
.msg-text { margin: 0; font-size: 14px; line-height: 1.9; color: var(--text); }
.msg.mine .msg-text { color: #fbf6ea; }

.cp-foot {
  display: flex; gap: 10px; padding: 12px 16px;
  border-top: 1px solid var(--border);
  align-items: center;
  background: linear-gradient(0deg, rgba(184, 134, 11, 0.04), transparent);
}
.cp-input { flex: 1; }
.cp-send { flex-shrink: 0; }

.chat-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 520px; color: var(--text-3); font-size: 13px; gap: 12px;
}
.ce-icon {
  width: 48px; height: 48px;
  border-radius: 8px;
  background: var(--seal); color: #fbf6ea;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px;
  opacity: 0.35;
}
.empty-hint { text-align: center; color: var(--text-3); font-size: 13px; padding: 24px 0; }
.btn-xs { padding: 3px 10px; font-size: 12px; }

@media (max-width: 900px) {
  .friends-layout { grid-template-columns: 1fr; }
  .friend-list { max-height: 300px; }
  .chat-panel { height: 400px; }
}
</style>
