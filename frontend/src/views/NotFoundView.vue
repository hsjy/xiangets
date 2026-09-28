<script setup>
import { useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'

const router = useRouter()
</script>

<template>
  <div class="page nf">
    <div class="code">404</div>
    <p>这个音符掉进了平行宇宙…</p>
    <button class="btn btn-primary" @click="router.push('/discover')">
      <Icon name="compass" :size="15" /> 回到发现音乐
    </button>
  </div>
</template>

<style scoped>
.nf { text-align: center; padding: 90px 0; }
.code {
  font-size: 88px;
  font-weight: 900;
  background: var(--grad);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
}
p { color: var(--text-3); margin: 16px 0 26px; }
</style>
