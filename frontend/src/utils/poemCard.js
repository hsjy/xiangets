/**
 * 诗笺生成器：把一首诗渲染成竖排笺纸 PNG（Canvas 绘制，浏览器端下载）。
 * 版式：右侧起竖排——题目（大字）、诗行（按句读分列）、落款与朱砂印。
 */
const W = 900
const H = 1240
const PAPER = '#f7f1e1'
const EDGE = 'rgba(74, 60, 36, 0.38)'
const INK = '#2f2a20'
const SUB = '#8a7f66'
const SEAL = '#a5402d'

/** 按句读切分诗行 → 竖排列数组 */
function columns(paragraphs) {
  const cols = []
  for (const line of (paragraphs || '').split('\n')) {
    const parts = (line.match(/[^，。？！；、]+[，。？！；、]?/g) || [])
      .map((x) => x.trim())
      .filter(Boolean)
    cols.push(...parts)
  }
  return cols
}

function roundRect(g, x, y, w, h, r) {
  g.beginPath()
  g.moveTo(x + r, y)
  g.arcTo(x + w, y, x + w, y + h, r)
  g.arcTo(x + w, y + h, x, y + h, r)
  g.arcTo(x, y + h, x, y, r)
  g.arcTo(x, y, x + w, y, r)
  g.closePath()
}

export function renderPoemCard(poem) {
  const c = document.createElement('canvas')
  c.width = W
  c.height = H
  const g = c.getContext('2d')

  // 笺纸与内框
  g.fillStyle = PAPER
  g.fillRect(0, 0, W, H)
  g.strokeStyle = EDGE
  g.lineWidth = 2
  g.strokeRect(42, 42, W - 84, H - 84)
  g.lineWidth = 1
  g.strokeRect(52, 52, W - 104, H - 104)

  const serif = "'Ma Shan Zheng','LXGW WenKai','Songti SC',serif"
  const wenkai = "'LXGW WenKai','Songti SC','STSong',serif"
  g.textAlign = 'center'
  g.textBaseline = 'top'

  // 内容列：题 → 诗
  const cols = []
  const title = poem.title + (poem.subtitle ? '·' + poem.subtitle : '')
  cols.push({ text: title, big: true })
  cols.push(...columns(poem.paragraphs).map((t) => ({ text: t, big: false })))

  const bigSize = 40
  const size = 34
  const lineSkip = 52
  let x = W - 128
  const top = 132
  for (const col of cols) {
    if (x < 118) break
    if (col.big) {
      g.font = `${bigSize}px ${serif}`
      g.fillStyle = SEAL
      for (let i = 0; i < col.text.length; i++) {
        g.fillText(col.text[i], x, top + i * (bigSize + 10))
      }
      x -= lineSkip + 14
    } else {
      g.font = `${size}px ${wenkai}`
      g.fillStyle = INK
      for (let i = 0; i < col.text.length; i++) {
        g.fillText(col.text[i], x, top + i * (size + 12))
      }
      x -= lineSkip
    }
  }

  // 落款：朝代 · 诗人
  g.font = `22px ${wenkai}`
  g.fillStyle = SUB
  g.textAlign = 'left'
  g.fillText(`${poem.dynasty || ''} · ${poem.poetName || ''}`, 118, H - 132)

  // 朱砂印（末字）
  const sealChar = (poem.poetName || '诗').slice(-1)
  g.fillStyle = SEAL
  roundRect(g, 118, H - 112, 52, 52, 8)
  g.fill()
  g.font = `30px ${serif}`
  g.fillStyle = '#fbf6ea'
  g.textAlign = 'center'
  g.fillText(sealChar, 144, H - 108)

  // 笺尾小字
  g.font = `15px ${wenkai}`
  g.fillStyle = SUB
  g.textAlign = 'right'
  g.fillText('弦歌 · 诗乐雅集', W - 118, H - 92)

  return c
}

export function downloadPoemCard(poem) {
  const c = renderPoemCard(poem)
  const url = c.toDataURL('image/png')
  const a = document.createElement('a')
  a.href = url
  a.download = `弦歌·${poem.title}${poem.subtitle ? '·' + poem.subtitle : ''}.png`
  a.click()
}
