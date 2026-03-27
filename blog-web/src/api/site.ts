import request from '../utils/request'

/** @deprecated 请使用 siteBranding.getSiteConfig */
export function getSiteAuthor() {
  return request({ url: '/public/site/author', method: 'get' })
}
