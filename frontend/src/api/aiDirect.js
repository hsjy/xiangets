/**
 * AI 直连 DeepSeek：mock 模式（Pages / App 壳）下没有后端，用户在设置里
 * 填入自己的 DeepSeek API Key 后，问诗/诗人对话/按需注译赏由浏览器直接调
 * api.deepseek.com（CORS 已验证放行）。Key 只存本机 localStorage，不经过
 * 任何第三方服务器。prompt 与后端 service 保持同款，两端体验一致。
 *
 * localStorage key：yuetin_deepseek_key
 */

const KEY_STORAGE = 'yuetin_deepseek_key'
const BASE = 'https://api.deepseek.com/chat/completions'
const MODEL = 'deepseek-v4-flash' // 与后端 yuetin.ai.deepseek.model 默认值一致

export function getDeepSeekKey() {
  try { return (localStorage.getItem(KEY_STORAGE) || '').trim() } catch { return '' }
}

export function setDeepSeekKey(key) {
  try {
    const k = (key || '').trim()
    if (k) localStorage.setItem(KEY_STORAGE, k)
    else localStorage.removeItem(KEY_STORAGE)
  } catch { /* 隐私模式忽略 */ }
}

async function chat(messages, { temperature = 0.8, maxTokens = 2000, timeoutMs = 60000 } = {}) {
  const key = getDeepSeekKey()
  if (!key) {
    throw Object.assign(new Error('未配置 DeepSeek Key：点顶部 ✦ 填入你的 Key，AI 伴读即刻开启'), { status: 400 })
  }
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), timeoutMs)
  let res
  try {
    res = await fetch(BASE, {
      method: 'POST',
      signal: ctrl.signal,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` },
      body: JSON.stringify({ model: MODEL, messages, temperature, max_tokens: maxTokens })
    })
  } catch (e) {
    throw Object.assign(new Error(e.name === 'AbortError' ? 'DeepSeek 响应超时，请重试' : '网络异常，无法连接 DeepSeek'), { status: 502 })
  } finally {
    clearTimeout(timer)
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    let hint = ''
    if (res.status === 401) hint = 'Key 无效或已过期'
    else if (res.status === 402) hint = '余额不足，请到 DeepSeek 平台充值'
    else if (res.status === 429) hint = '请求过快触发限流，稍等再试'
    throw Object.assign(new Error(`DeepSeek ${res.status}${hint ? '：' + hint : text ? '：' + text.slice(0, 160) : ''}`), { status: 502 })
  }
  const data = await res.json()
  return (data.choices?.[0]?.message?.content || '').trim()
}

/** 收敛多轮历史：role 归一 + 只保留最近 6 轮，与后端约定一致 */
function trimHistory(history) {
  const out = []
  for (const turn of history || []) {
    if (out.length >= 6) break
    const role = String(turn?.role || '').toLowerCase() === 'user' ? 'user' : 'assistant'
    const content = String(turn?.content || '').trim()
    if (content) out.push({ role, content })
  }
  return out
}

/* ---------------- 问诗 ---------------- */

const ASK_SYSTEM = `你是一位学识渊博而温雅的中国古典诗词学者，正在与一位读诗的人对话。
下面是你们正在共读的一首诗，以及它的注释、译文与赏析，供你随时取用。
回答要求：
1. 紧扣这首诗与提问，可以延展到诗人生平、时代背景、相关名篇，但不离题万里；
2. 温雅、简净、有见地，像一位师长随口谈艺，不堆砌辞藻，不用套话开头；
3. 一般不超过 200 字；对方若明确要求展开，可以写到 400 字；
4. 不使用 markdown 标记，直接以自然段回话。`

/** 问诗：poem 为 mock 数据（含 annotation/translation/appreciation）。返回回答文本。 */
export async function askPoem(poem, question, history) {
  if (!question || !question.trim()) throw Object.assign(new Error('问题不能为空'), { status: 400 })
  const aux = []
  if (poem.annotation) aux.push(`【注释】${poem.annotation}`)
  if (poem.translation) aux.push(`【译文】${poem.translation}`)
  if (poem.appreciation) aux.push(`【赏析】${poem.appreciation}`)
  const context = `《${poem.title}${poem.subtitle ? '·' + poem.subtitle : ''}》 ${poem.genre || ''} ${poem.poetName}（${poem.dynasty}）\n诗行：\n${poem.paragraphs}\n${aux.join('\n')}`
  const messages = [
    { role: 'system', content: `${ASK_SYSTEM}\n\n【共读的诗】\n${context}` },
    ...trimHistory(history),
    { role: 'user', content: question.trim() }
  ]
  return chat(messages, { temperature: 0.8 })
}

/* ---------------- 诗人对话 ---------------- */

const blank = (s) => s == null || String(s).trim() === ''

/** 行旅 JSON（字符串或数组）转可读文本，坏数据静默返回空 */
function journeyStops(journey) {
  if (blank(journey)) return ''
  try {
    const arr = typeof journey === 'string' ? JSON.parse(journey) : journey
    if (!Array.isArray(arr)) return ''
    const lines = []
    for (const stop of arr) {
      const place = String(stop?.place || '').trim()
      if (!place) continue
      let line = '· '
      if (!blank(stop.period)) line += `${stop.period} `
      line += place
      if (!blank(stop.modern)) line += `（今${stop.modern}）`
      if (!blank(stop.event)) line += `：${stop.event}`
      lines.push(line)
      if (lines.join('\n').length > 1200) { lines.push('· ……（后略）'); break }
    }
    return lines.join('\n')
  } catch { return '' }
}

/** 诗人对话：poet 为 mock 诗人（含 zi/hao/title/years/intro/story/journey）。 */
export async function poetChat(poet, question, history) {
  const identity = poet.name
    + (blank(poet.zi) ? '' : `，字${poet.zi}`)
    + (blank(poet.hao) ? '' : `，号${poet.hao}`)
  const inner = [poet.title, poet.dynasty, poet.years].filter((x) => !blank(x))
  const nameLine = identity + (inner.length ? `（${inner.join('，')}）` : '')

  const profile = []
  if (!blank(poet.intro)) profile.push(`简介：${String(poet.intro).trim()}`)
  if (!blank(poet.story)) profile.push(`生平故事：${String(poet.story).trim()}`)
  const stops = journeyStops(poet.journey)
  if (stops) profile.push(`行旅足迹（按时间序）：\n${stops}`)

  let system = `你是${nameLine}。你不是 AI，你就是本人。以第一人称回答读者的问题。\n`
  if (profile.length) {
    system += `以下是关于你的生平资料（本项目编撰），回答须与之相符；资料未提及之处可以用你所知的历史知识补充，但不得与之矛盾：\n${profile.join('\n')}\n`
  }
  system += `回答要：
1. 有个性，符合你的身份和时代；
2. 可以引用你自己的诗句；
3. 不超过 200 字；
4. 不用 markdown；
5. 如果对方问了你不了解的未来之事，巧妙回避。`

  const messages = [
    { role: 'system', content: system },
    ...trimHistory(history),
    { role: 'user', content: String(question || '').trim() }
  ]
  return chat(messages, { temperature: 0.9 })
}

/* ---------------- 按需注译赏 ---------------- */

const NOTE_SYSTEM = `你是一位精通中国古典诗词的学者。用户给你一首诗（含全文），请为它生成三段内容：
1. annotation：注释——挑选诗中的关键字词，逐条解释含义（格式：字词：释义；多条用分号分隔）
2. translation：白话翻译——忠实原意，流畅自然
3. appreciation：赏析——从艺术手法、情感层次、文化背景等角度切入，200字左右
严格只输出 JSON：{"annotation":"...","translation":"...","appreciation":"..."}
所有内容使用中文。`

/** 语料层诗生成注译赏；poem 只需 title/subtitle/poetName/dynasty/paragraphs。 */
export async function generateNotes(poem) {
  const user = `题目：${poem.title}${poem.subtitle ? '·' + poem.subtitle : ''}\n作者：${poem.poetName}（${poem.dynasty}）\n诗行：\n${poem.paragraphs}`
  const content = await chat(
    [{ role: 'system', content: NOTE_SYSTEM }, { role: 'user', content: user }],
    { temperature: 0.7, maxTokens: 3000 }
  )
  const json = content.replaceAll(/[\s\S]*?\{/, '{').replace(/\}[^}]*$/, '}')
  const parsed = JSON.parse(json)
  const out = {
    annotation: String(parsed.annotation || ''),
    translation: String(parsed.translation || ''),
    appreciation: String(parsed.appreciation || '')
  }
  if (!out.annotation && !out.translation && !out.appreciation) {
    throw Object.assign(new Error('DeepSeek 返回内容为空'), { status: 502 })
  }
  return out
}
