import request from '../utils/request'

export function listGuestbook(params: Record<string, unknown>) {
  return request({ url: '/public/guestbook', method: 'get', params })
}

export function createGuestbook(data: { nickname: string; email?: string; content: string }) {
  return request({ url: '/public/guestbook', method: 'post', data })
}
