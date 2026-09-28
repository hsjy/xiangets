/**
 * Mock JWT：HS256（Web Crypto 实现）。
 * 与真实后端的 jjwt 签发/校验行为一致：Bearer Token、有效期、签名校验。
 */
const SECRET = 'yuetin-mock-jwt-secret-2024'
const TTL = 7 * 24 * 3600 // 秒

const enc = new TextEncoder()

function b64urlFromBytes(bytes) {
  let s = ''
  for (const b of bytes) s += String.fromCharCode(b)
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function b64urlToBytes(str) {
  const pad = str.length % 4 ? 4 - (str.length % 4) : 0
  const b64 = atob(str.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat(pad))
  return Uint8Array.from(b64, (c) => c.charCodeAt(0))
}

async function hmacSign(data) {
  const key = await crypto.subtle.importKey('raw', enc.encode(SECRET), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign'])
  const sig = await crypto.subtle.sign('HMAC', key, enc.encode(data))
  return b64urlFromBytes(new Uint8Array(sig))
}

/** 签发 token：header.payload.signature */
export async function signToken(payload) {
  const body = { ...payload, iat: Math.floor(Date.now() / 1000), exp: Math.floor(Date.now() / 1000) + TTL }
  const head = b64urlFromBytes(enc.encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' })))
  const pay = b64urlFromBytes(enc.encode(JSON.stringify(body)))
  const sig = await hmacSign(`${head}.${pay}`)
  return `${head}.${pay}.${sig}`
}

/** 校验并解析 token，失败返回 null */
export async function verifyToken(token) {
  if (!token) return null
  const parts = token.split('.')
  if (parts.length !== 3) return null
  const [head, pay, sig] = parts
  if ((await hmacSign(`${head}.${pay}`)) !== sig) return null
  try {
    const payload = JSON.parse(new TextDecoder().decode(b64urlToBytes(pay)))
    if (payload.exp && payload.exp < Math.floor(Date.now() / 1000)) return null
    return payload
  } catch {
    return null
  }
}
