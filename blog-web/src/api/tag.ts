import request from '../utils/request'

/** 前台 */
export function listPublicTags() {
  return request({ url: '/public/tags', method: 'get' })
}

/** 后台 */
export function listAdminTags() {
  return request({ url: '/tag/list', method: 'get' })
}

export function saveTag(data: { id?: number; name: string; slug?: string }) {
  return request({ url: '/tag/save', method: 'post', data })
}

export function deleteTag(id: number | string) {
  return request({ url: `/tag/${id}`, method: 'delete' })
}
