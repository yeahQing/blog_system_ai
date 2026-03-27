import request from '../utils/request'

/**
 * 认证相关接口
 */
export function login(data: any) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}
