<script setup>
import { toast } from '../utils/toast'
</script>

<template>
  <div class="toast-wrap">
    <div v-for="t in toast.list" :key="t.id" class="toast" :class="`toast-${t.type}`">
      {{ t.message }}
    </div>
  </div>
</template>
