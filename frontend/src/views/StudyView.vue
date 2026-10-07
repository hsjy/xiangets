<script setup>
import { ref, onMounted } from 'vue'
import { poemFavApi, poemCommentApi } from '../api'
import { confirmAction } from '../stores/ui'
import { toast } from '../utils/toast'
import { formatDate } from '../utils/format'

const loading = ref(true)
const favs = ref([])
const mine = ref([])

const firstLine = (p) => (p.paragraphs || '').split('\n')[0] || ''

async function load() {
  loading.value = true
  try {
    const [f, m] = await Promise.allSettled([poemFavApi.list(), poemCommentApi.mine()])
    if (f.status === 'fulfilled') favs.value = f.value.data
    if (m.status === 'fulfilled') mine.value = m.value.data
  } finally {
    loading.value = false
  }
}

async function removeComment(c) {
  const yes = await confirmAction(`删除写在《${c.poemTitle}》上的这条感悟吗？`, '删除感悟')
  if (!yes) return
  try {
    await poemCommentApi.remove(c.id)
    mine.value = mine.value.filter((x) => x.id !== c.id)
    toast.success('已删除')
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(load)

function exportMd() {
  const lines = ['# 弦歌 · 书斋导出', '', `> 导出时间：${new Date().toLocaleString('zh-CN')}`, '']
  if (favs.value.length) {
    lines.push('## 收藏的诗\n')
    favs.value.forEach((f, i) => {
      lines.push(`${i + 1}. 《${f.title}${f.subtitle ? '·' + f.subtitle : ''}》 — ${f.poetName || ''}`)
    })
    lines.push('')
  }
  if (myComments.value?.length) {
    lines.push('## 我的感悟\n')
    myComments.value.forEach((c) => {
      lines.push(`> **《${c.poemTitle || ''}》**\n> ${c.content}\n`)
    })
  }
  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '弦歌·书斋.md'
  a.click()
}
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <div>
          <h1 class="page-title">书斋</h1>
          <p class="page-sub">你收藏的诗，与你写下的感悟</p>
        </div>
        <button class="btn btn-ghost" @click="exportMd" data-test="export-md">导出 Markdown</button>
      </div>
    </div>

    <div v-if="loading" class="spinner" />
    <template v-else>
      <section class="sec">
        <h2 class="sec-title">收藏的诗（{{ favs.length }}）</h2>
        <div v-if="!favs.length" class="empty" style="padding: 30px 0">
          书斋还空着——去<a href="#/poems">诗选</a>把心动的诗收进来
        </div>
        <div v-else class="fav-grid">
          <router-link v-for="p in favs" :key="p.poemId" :to="`/poem/${p.poemId}`" class="fav-card">
            <div class="f-head">
              <span class="tag" v-if="p.mood">{{ p.mood }}</span>
              <span class="f-dyn">{{ p.dynasty }} · {{ p.poetName }}</span>
            </div>
            <h3>《{{ p.title }}{{ p.subtitle ? '·' + p.subtitle : '' }}》</h3>
            <p class="f-line">{{ firstLine(p) }}</p>
            <p class="f-famous" v-if="p.famousLine">{{ p.famousLine }}</p>
          </router-link>
        </div>
      </section>

      <section class="sec">
        <h2 class="sec-title">我的感悟（{{ mine.length }}）</h2>
        <div v-if="!mine.length" class="empty" style="padding: 30px 0">
          还没有题过笔——意境页的诗行旁，可以随时写下感悟
        </div>
        <div v-else class="mine-list">
          <div v-for="c in mine" :key="c.id" class="mine-item">
            <div class="m-body">
              <p class="m-text">{{ c.content }}</p>
              <p class="m-meta">
                写于《<router-link :to="`/poem/${c.poemId}`">{{ c.poemTitle }}</router-link>》 · {{ formatDate(c.createTime) }}
              </p>
            </div>
            <button class="m-del" @click="removeComment(c)">删除</button>
          </div>
        </div>
      </section>
    
</template>
  </div>
</template>

<style scoped>
.sec { margin-bottom: 32px; }
.sec-title { margin: 0 0 14px; font-size: 18px; }
.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}
.fav-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
  color: var(--text);
  transition: all 0.18s;
}
.fav-card:hover { border-color: var(--primary); transform: translateY(-2px); }
.f-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.f-dyn { font-size: 11.5px; color: var(--text-3); }
.fav-card h3 { margin: 0 0 8px; font-size: 15.5px; }
.f-line { margin: 0 0 8px; font-size: 13px; color: var(--text-2); line-height: 1.8; }
.f-famous { margin: 0; font-size: 12px; color: var(--accent); }

.mine-list { display: flex; flex-direction: column; gap: 12px; }
.mine-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 14px 16px;
}
.m-body { flex: 1; }
.m-text { margin: 0 0 6px; font-size: 13.5px; line-height: 1.9; }
.m-meta { margin: 0; font-size: 12px; color: var(--text-3); }
.m-meta a { color: var(--primary-2); }
.m-del { border: none; background: none; color: var(--text-3); font-size: 12px; cursor: pointer; }
.m-del:hover { color: var(--danger); }
.export-btn { margin-top: 8px; }
</style>
