import request from '../utils/request'

export interface SiteConfig {
  navTitle: string
  navLogoUrl: string
  heroTitle: string
  heroSubtitle: string
  authorName: string
  authorBio: string
  authorAvatar: string
  /** 页脚模板，可用 {year} 表示当前年份 */
  footerText: string
}

/** 前台无需登录 */
export function getSiteConfig() {
  return request({ url: '/public/site/config', method: 'get' })
}

/** 管理端 */
export function getSiteBrandingAdmin() {
  return request({ url: '/site-branding', method: 'get' })
}

export function saveSiteBranding(data: Partial<SiteConfig>) {
  return request({ url: '/site-branding', method: 'put', data })
}
