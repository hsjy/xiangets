<script setup>
import { formatPlays } from '../utils/format'
import Icon from './Icon.vue'

defineProps({
  playlist: { type: Object, required: true }
})
</script>

<template>
  <router-link :to="`/playlist/${playlist.id}`" class="pl-card" data-test="playlist-card">
    <div class="cover-wrap">
      <img :src="playlist.cover" :alt="playlist.name" loading="lazy" />
      <div class="overlay">
        <span class="play-btn"><Icon name="play" :size="18" /></span>
      </div>
      <span class="count-chip"><Icon name="music" :size="12" /> {{ playlist.songCount ?? 0 }}</span>
    </div>
    <div class="pl-name">{{ playlist.name }}</div>
    <div class="pl-sub">
      <span v-if="playlist.style" class="style-tag">{{ playlist.style }}</span>
      <span class="creator">{{ playlist.creatorName }}</span>
    </div>
  </router-link>
</template>

<style scoped>
.pl-card { display: block; color: var(--text); }
.cover-wrap {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--card);
  border: 1px solid var(--border);
}
.cover-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.pl-card:hover .cover-wrap img { transform: scale(1.06); }
.overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 55%, rgba(8, 9, 14, 0.75));
  opacity: 0;
  transition: opacity 0.25s;
  display: flex;
  align-items: flex-end;
  padding: 12px;
}
.pl-card:hover .overlay { opacity: 1; }
.play-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;
  box-shadow: 0 4px 14px rgba(46, 38, 24, 0.45);
}
.count-chip {
  position: absolute;
  top: 8px;
  right: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(8, 9, 14, 0.65);
  backdrop-filter: blur(4px);
  color: #fff;
  font-size: 11.5px;
  border-radius: 999px;
  padding: 3px 8px;
}
.pl-name {
  margin-top: 10px;
  font-size: 13.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pl-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3);
}
.style-tag {
  background: var(--grad-soft);
  color: var(--primary-2);
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
}
.creator { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
