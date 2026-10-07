document.getElementById('go').addEventListener('click', search)
document.getElementById('kw').addEventListener('keyup', (e) => { if (e.key === 'Enter') search() })

async function search() {
  const kw = document.getElementById('kw').value.trim()
  if (!kw) return
  try {
    const res = await fetch(`${API_BASE}/poem/page?keyword=${encodeURIComponent(kw)}&size=5`)
    const { data } = await res.json()
    const el = document.getElementById('result')
    if (!data.list.length) { el.innerHTML = '<p style="color:#9a9077;text-align:center;padding:12px 0">未寻得此诗</p>'; return }
    el.innerHTML = data.list.map(p =>
      `<a href="${API_BASE.replace('/api','')}/#/poem/${p.id}" target="_blank">《${p.title}${p.subtitle ? '·'+p.subtitle : ''}》 · ${p.poetName}</a>`
    ).join('')
  } catch {
    document.getElementById('result').innerHTML = '<p style="color:#9a9077;font-size:12px;text-align:center;padding:8px 0">无法连接弦歌服务</p>'
  }
}
