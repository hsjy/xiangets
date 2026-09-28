<script setup>
/**
 * 同歌手其他作品列表（用于歌曲详情页底部）
 */
import { ref, onMounted } from 'vue'
import { songApi } from '../api'
import SongTable from './SongTable.vue'

const props = defineProps({
  singerId: { type: Number, required: true },
  excludeId: { type: Number, default: 0 }
})

const songs = ref([])

onMounted(async () => {
  try {
    const { data } = await songApi.list({ singerId: props.singerId })
    songs.value = data.filter((s) => s.id !== props.excludeId)
  } catch { /* 静默 */ }
})
</script>

<template>
  <SongTable v-if="songs.length" :songs="songs" />
  <div v-else class="empty" style="padding: 24px 0; font-size: 12.5px">暂无其他作品</div>
</template>
