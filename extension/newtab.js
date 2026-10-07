document.getElementById('date').textContent =
  new Date().toISOString().slice(0, 10) + ' · 今日一诗'

async function loadToday() {
  try {
    const res = await fetch(`${API_BASE}/poem/today`)
    const { data } = await res.json()
    if (!data?.poem) return
    const p = data.poem
    document.getElementById('famous').textContent =
      `「${p.famousLine || p.title}」`
    document.getElementById('meta').textContent =
      `《${p.title}${p.subtitle ? '·' + p.subtitle : ''}》 · ${p.poetName} · ${p.dynasty}`
    document.getElementById('firstLine').textContent =
      (p.paragraphs || '').split('\n')[0] || ''
    document.getElementById('openApp').href = `${API_BASE.replace('/api', '')}/#/poem/${p.id}`
  } catch {
    document.getElementById('famous').textContent = '弦歌不辍'
  }
}
loadToday()
