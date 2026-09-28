/**
 * 秒 -> mm:ss
 */
export function formatDuration(sec) {
  if (!sec || sec < 0 || !isFinite(sec)) return '00:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/**
 * 播放次数 -> 1.2万 格式
 */
export function formatPlays(n) {
  if (n == null) return '0'
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

/**
 * 时间戳 -> yyyy-MM-dd
 */
export function formatDate(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const p = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/**
 * 数值 -> 1,234,567
 */
export function formatNumber(n) {
  return (n ?? 0).toLocaleString('zh-CN')
}
