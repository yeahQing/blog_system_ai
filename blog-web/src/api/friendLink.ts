import request from '../utils/request'

export function listPublicFriendLinks() {
  return request({ url: '/public/friend-links', method: 'get' })
}

export function listAdminFriendLinks() {
  return request({ url: '/friend-link/list', method: 'get' })
}

export function saveFriendLink(data: {
  id?: number
  name: string
  url: string
  description?: string
  sortOrder?: number
  enabled?: boolean
}) {
  return request({ url: '/friend-link/save', method: 'post', data })
}

export function deleteFriendLink(id: number | string) {
  return request({ url: `/friend-link/${id}`, method: 'delete' })
}
