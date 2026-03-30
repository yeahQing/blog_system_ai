/**
 * 仅解析 JWT payload（不校验签名），用于前端判断过期、路由守卫。
 */
function decodePayload(token: string): { exp?: number } | null {
  try {
    const part = token.split('.')[1]
    if (!part) return null
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const pad = base64.length % 4 === 0 ? '' : '='.repeat(4 - (base64.length % 4))
    const json = atob(base64 + pad)
    return JSON.parse(json) as { exp?: number }
  } catch {
    return null
  }
}

export function isJwtExpired(token: string): boolean {
  const payload = decodePayload(token)
  if (!payload) return true
  if (payload.exp == null) return false
  return payload.exp * 1000 <= Date.now()
}
