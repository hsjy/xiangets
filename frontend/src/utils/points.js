/**
 * 积分与称号：飞花令对句等互动累计。
 * 等级线：初窥门径 → 小有所成 → 渐入佳境 → 炉火纯青 → 登峰造极
 */
const LEVELS = [
  { min: 0, title: '初窥门径' },
  { min: 100, title: '小有所成' },
  { min: 300, title: '渐入佳境' },
  { min: 600, title: '炉火纯青' },
  { min: 1000, title: '登峰造极' }
]

export function levelOf(points) {
  let cur = LEVELS[0], next = null
  for (let i = 0; i < LEVELS.length; i++) {
    if (points >= LEVELS[i].min) cur = LEVELS[i]
    if (i + 1 < LEVELS.length && points < LEVELS[i + 1].min) { next = LEVELS[i + 1]; break }
  }
  if (points >= LEVELS[LEVELS.length - 1].min) cur = LEVELS[LEVELS.length - 1]
  return {
    title: cur.title,
    next: next ? { title: next.title, need: next.min - points } : null
  }
}
