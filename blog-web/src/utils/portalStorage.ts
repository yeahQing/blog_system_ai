const KEY_H = 'blog_portal_history'
const KEY_F = 'blog_portal_favorites'
const MAX_H = 40

export function getHistory(): { id: number; title: string; at: string }[] {
  try {
    return JSON.parse(localStorage.getItem(KEY_H) || '[]')
  } catch {
    return []
  }
}

export function clearHistory() {
  localStorage.removeItem(KEY_H)
}

export function pushArticleHistory(id: number, title: string) {
  try {
    let list = getHistory()
    list = list.filter((x) => x.id !== id)
    list.unshift({ id, title: title || `文章 #${id}`, at: new Date().toISOString() })
    if (list.length > MAX_H) {
      list = list.slice(0, MAX_H)
    }
    localStorage.setItem(KEY_H, JSON.stringify(list))
  } catch {
    /* ignore */
  }
}

export function getFavorites(): { id: number; title: string }[] {
  try {
    return JSON.parse(localStorage.getItem(KEY_F) || '[]')
  } catch {
    return []
  }
}

export function isFavorite(id: number): boolean {
  return getFavorites().some((x) => x.id === id)
}

export function toggleFavorite(id: number, title: string): boolean {
  const list = getFavorites()
  const i = list.findIndex((x) => x.id === id)
  let next: typeof list
  let nowFav: boolean
  if (i >= 0) {
    next = list.filter((x) => x.id !== id)
    nowFav = false
  } else {
    next = [...list, { id, title: title || `文章 #${id}` }]
    nowFav = true
  }
  localStorage.setItem(KEY_F, JSON.stringify(next))
  return nowFav
}

export function removeFavorite(id: number) {
  const next = getFavorites().filter((x) => x.id !== id)
  localStorage.setItem(KEY_F, JSON.stringify(next))
}
