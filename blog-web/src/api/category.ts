import request from '../utils/request'

export function listCategories() {
  return request({ url: '/category/list', method: 'get' })
}
