<script setup>
import { ui, confirmAction } from '../stores/ui'
import Icon from './Icon.vue'

function answer(val) {
  ui.confirm.visible = false
  ui.confirm._resolve?.(val)
  ui.confirm._resolve = null
}
</script>

<template>
  <Teleport to="body">
    <div v-if="ui.confirm.visible" class="modal-mask" @click.self="answer(false)">
      <div class="modal">
        <h3 class="modal-title">{{ ui.confirm.title }}</h3>
        <p style="color: var(--text-2); line-height: 1.7; margin: 0">{{ ui.confirm.message }}</p>
        <div class="modal-actions">
          <button class="btn btn-ghost" @click="answer(false)">取消</button>
          <button class="btn btn-primary" data-test="confirm-ok" @click="answer(true)">确定</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
