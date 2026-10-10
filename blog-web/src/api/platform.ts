import request from '../utils/request'

// ==================== 模型管理 ====================

export function listModels(params?: { type?: string }) {
  return request({ url: '/admin/platform/models', method: 'get', params })
}

export function getModel(id: number) {
  return request({ url: `/admin/platform/models/${id}`, method: 'get' })
}

export function createModel(data: unknown) {
  return request({ url: '/admin/platform/models', method: 'post', data })
}

export function updateModel(id: number, data: unknown) {
  return request({ url: `/admin/platform/models/${id}`, method: 'put', data })
}

export function setDefaultModel(id: number) {
  return request({ url: `/admin/platform/models/${id}/default`, method: 'put' })
}

export function deleteModel(id: number) {
  return request({ url: `/admin/platform/models/${id}`, method: 'delete' })
}

// ==================== 提示词管理 ====================

export function getActivePrompt(code: string) {
  return request({ url: '/admin/platform/prompts/active', method: 'get', params: { code } })
}

export function getPromptVersions(code: string) {
  return request({ url: '/admin/platform/prompts/versions', method: 'get', params: { code } })
}

export function savePrompt(data: unknown) {
  return request({ url: '/admin/platform/prompts', method: 'post', data })
}

export function rollbackPrompt(promptId: number) {
  return request({ url: `/admin/platform/prompts/rollback/${promptId}`, method: 'put' })
}

// ==================== 消息流 ====================

export function listSessions() {
  return request({ url: '/admin/platform/messages/sessions', method: 'get' })
}

export function getMessagesBySession(sessionId: string, params?: { role?: string }) {
  return request({ url: `/admin/platform/messages/session/${sessionId}`, method: 'get', params })
}

export function getMessagesByRun(runId: string) {
  return request({ url: `/admin/platform/messages/run/${runId}`, method: 'get' })
}

// ==================== 向量库 ====================

export function listVectorStores(params?: { all?: boolean }) {
  return request({ url: '/admin/platform/vector-stores', method: 'get', params })
}

export function createVectorStore(data: unknown) {
  return request({ url: '/admin/platform/vector-stores', method: 'post', data })
}

export function toggleVectorStoreEnabled(id: number, enabled: boolean) {
  return request({ url: `/admin/platform/vector-stores/${id}/enabled`, method: 'put', params: { enabled } })
}

// ==================== 记忆 ====================

export function listMemories(userId: number) {
  return request({ url: '/admin/platform/memories', method: 'get', params: { userId } })
}

export function getMemory(sessionId: string) {
  return request({ url: `/admin/platform/memories/${sessionId}`, method: 'get' })
}

export function clearMemory(sessionId: string) {
  return request({ url: `/admin/platform/memories/${sessionId}`, method: 'delete' })
}

// ==================== RAG ====================

export function ragBackfill() {
  return request({ url: '/admin/platform/rag/backfill', method: 'post', timeout: 300000 })
}

export function ragSearch(query: string, limit?: number) {
  return request({ url: '/admin/platform/rag/search', method: 'get', params: { query, limit } })
}

export function ragContext(query: string, limit?: number) {
  return request({ url: '/admin/platform/rag/context', method: 'get', params: { query, limit } })
}
