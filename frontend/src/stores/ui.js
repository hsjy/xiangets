import { reactive } from 'vue'

/** 轻量全局 UI 状态：跨页面弹窗（删除确认等） */
export const ui = reactive({
  confirm: { visible: false, title: '确认操作', message: '', _resolve: null }
})

/** 返回 Promise<boolean>，替代原生 confirm（避免阻塞自动化测试） */
export function confirmAction(message, title = '确认操作') {
  return new Promise((resolve) => {
    ui.confirm.title = title
    ui.confirm.message = message
    ui.confirm._resolve = resolve
    ui.confirm.visible = true
  })
}
