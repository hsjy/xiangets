import { reactive } from 'vue'

const state = reactive([])
let seq = 0

export function toast(message, type = 'info', duration = 2600) {
  const id = ++seq
  state.push({ id, message, type })
  setTimeout(() => {
    const i = state.findIndex((t) => t.id === id)
    if (i > -1) state.splice(i, 1)
  }, duration)
}
toast.success = (m) => toast(m, 'success')
toast.error = (m) => toast(m, 'error')
toast.list = state
