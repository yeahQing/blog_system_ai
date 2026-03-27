import request from '../utils/request'

export function changeAdminPassword(data: { currentPassword: string; newPassword: string }) {
  return request({ url: '/admin/account/password', method: 'put', data })
}

export function getMenuVisibility() {
  return request({ url: '/admin/menu-visibility', method: 'get' })
}

export function saveMenuVisibility(data: Record<string, boolean>) {
  return request({ url: '/admin/menu-visibility', method: 'put', data })
}
