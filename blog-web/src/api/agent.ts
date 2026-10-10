import request from '../utils/request'

/**
 * 智能体对话（同步）。
 * 智能体可能调用多个工具完成写作，耗时较长，超时设为 5 分钟。
 */
export function agentChat(data: { message: string; sessionId?: string; userId?: number }) {
  return request({
    url: '/admin/agent/chat',
    method: 'post',
    data,
    timeout: 300000
  })
}
