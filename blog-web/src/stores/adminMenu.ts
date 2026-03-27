import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMenuVisibility } from '../api/adminSystem'

/** 与后端 AdminMenuVisibilityService.MENU_KEYS 一致 */
export const ADMIN_MENU_KEYS = [
  'dashboard',
  'site-settings',
  'articles',
  'categories',
  'tags',
  'friend-links'
] as const

export type AdminMenuKey = (typeof ADMIN_MENU_KEYS)[number]

function defaultVisibility(): Record<string, boolean> {
  const m: Record<string, boolean> = {}
  for (const k of ADMIN_MENU_KEYS) m[k] = true
  return m
}

export const useAdminMenuStore = defineStore('adminMenu', () => {
  const visibility = ref<Record<string, boolean>>(defaultVisibility())
  const loaded = ref(false)

  function isVisible(key: string) {
    return visibility.value[key] !== false
  }

  function applyData(data: Record<string, boolean> | null | undefined) {
    const next = defaultVisibility()
    if (data) {
      for (const k of ADMIN_MENU_KEYS) {
        if (Object.prototype.hasOwnProperty.call(data, k)) {
          next[k] = data[k] === true
        }
      }
    }
    visibility.value = next
  }

  async function load() {
    try {
      const res: any = await getMenuVisibility()
      if (res.code === 200 && res.data) {
        applyData(res.data)
      }
    } catch {
      /* 保持默认全显示 */
    } finally {
      loaded.value = true
    }
  }

  function setFromSave(data: Record<string, boolean>) {
    applyData(data)
  }

  return {
    visibility,
    loaded,
    isVisible,
    load,
    setFromSave
  }
})
